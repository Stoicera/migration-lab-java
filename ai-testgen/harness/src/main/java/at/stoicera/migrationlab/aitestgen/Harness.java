package at.stoicera.migrationlab.aitestgen;

import at.stoicera.migrationlab.aitestgen.Unit.Corpus;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Command line of the pre-registered experiment (PROTOCOL.md §6).
 *
 * <pre>
 *   plan                                     what would run, and what it is expected to cost
 *   render   --corpus A|B --model &lt;id&gt;       write prompt instances only (no API call, no key)
 *   generate --corpus A|B --model &lt;id&gt;       render + one call per unit + record everything
 * </pre>
 *
 * <p>The harness writes artifacts and nothing else: it never edits generated code, never
 * re-prompts, and has no notion of a "good" result. Everything it produces is committed, including
 * failures.
 */
public final class Harness {

  private static final ObjectMapper JSON = new ObjectMapper();

  public static void main(String[] args) throws Exception {
    if (args.length == 0) {
      usage("no command given");
      return;
    }
    Map<String, String> options = parseOptions(args);
    Path root = Repo.root();
    String date = options.getOrDefault("date", LocalDate.now().toString());

    switch (args[0]) {
      case "plan" -> plan(root);
      case "render" ->
          render(root, runsRoot(root, options), corpus(options), model(options), date, false);
      case "generate" ->
          render(root, runsRoot(root, options), corpus(options), model(options), date, true);
      default -> usage("unknown command: " + args[0]);
    }
  }

  // ---------------------------------------------------------------- commands

  private static void plan(Path root) throws IOException {
    System.out.println("Pre-registered matrix (PROTOCOL.md §2, §3):");
    for (Corpus corpus : Corpus.values()) {
      System.out.println(
          "\ncorpus " + corpus + " — " + corpus.module() + "/ (" + corpus.stack() + ")");
      for (Unit unit : Catalog.units(corpus)) {
        Path source = root.resolve(unit.source());
        System.out.printf(
            "  %-4s %-24s %-52s %4d lines%s%n",
            unit.stratum(),
            unit.id(),
            unit.fqn(),
            Files.exists(source) ? Files.readAllLines(source).size() : 0,
            Files.exists(source) ? "" : "   << MISSING");
      }
    }
    System.out.println("\nmodels (pinned prices, USD per 1M tokens in/out):");
    Pricing.USD_PER_MILLION.forEach(
        (id, price) -> System.out.printf("  %-30s %6.2f / %6.2f%n", id, price[0], price[1]));
    System.out.printf(
        "%nunits: %d   calls at k=1: %d   budget cap: EUR %s%n",
        Catalog.allUnits().size(),
        Catalog.allUnits().size() * Pricing.USD_PER_MILLION.size(),
        Pricing.BUDGET_CAP_EUR);
    System.out.println("spent so far (from recorded usage): EUR " + spentEur(root));
  }

  /**
   * Renders every prompt of one corpus for one model and — when {@code call} is set — performs the
   * single generation call per unit, recording request, response, usage and the extracted class.
   */
  private static void render(
      Path root, Path runsRoot, Corpus corpus, String model, String date, boolean call)
      throws Exception {
    LlmClient client = call ? new OpenRouterClient(apiKey()) : null;
    Pricing.EcbRate rate = call ? Pricing.ecbUsdPerEur() : null;
    Path runRoot = runsRoot.resolve(date).resolve(slug(model)).resolve(corpus.name());

    for (Unit unit : Catalog.units(corpus)) {
      Path unitDirectory = runRoot.resolve(unit.id());
      Files.createDirectories(unitDirectory);
      PromptRenderer.Prompt prompt = PromptRenderer.render(root, unit);
      write(unitDirectory.resolve("prompt.md"), prompt.toMarkdown());
      System.out.println((call ? "generating " : "rendered   ") + corpus + "/" + unit.id());
      if (!call) {
        continue;
      }

      BigDecimal spent = spentEur(root);
      if (spent.compareTo(Pricing.BUDGET_CAP_EUR) >= 0) {
        throw new IllegalStateException(
            "budget cap reached (EUR " + spent + " of " + Pricing.BUDGET_CAP_EUR + ") — aborting");
      }

      LlmClient.Completion completion = client.complete(model, prompt.system(), prompt.user());
      write(unitDirectory.resolve("request.json"), completion.requestJson());
      write(unitDirectory.resolve("response.json"), completion.responseJson());

      Optional<String> code = CodeExtractor.firstJavaBlock(completion.content());
      Path generated = unitDirectory.resolve("as-generated");
      Files.createDirectories(generated);
      if (code.isPresent()) {
        write(generated.resolve(unit.testClassName() + ".java"), code.get());
      } else {
        // PROTOCOL.md §5: counts as non-compiling, is not re-prompted, stays in the repo
        write(generated.resolve("EXTRACTION-FAILED.txt"), completion.content());
      }
      write(
          unitDirectory.resolve("usage.json"),
          usageJson(unit, model, date, completion, code.isPresent(), rate, root));
    }
    if (call) {
      System.out.println("total spent after this run: EUR " + spentEur(root));
    }
  }

  // ----------------------------------------------------------------- records

  private static String usageJson(
      Unit unit,
      String model,
      String date,
      LlmClient.Completion completion,
      boolean extracted,
      Pricing.EcbRate rate,
      Path root)
      throws IOException {
    BigDecimal usd =
        Pricing.usdCost(model, completion.promptTokens(), completion.completionTokens());
    ObjectNode usage = JSON.createObjectNode();
    usage.put("unit", unit.id());
    usage.put("stratum", unit.stratum());
    usage.put("corpus", unit.corpus().name());
    usage.put("classUnderTest", unit.fqn());
    usage.put("model", model);
    usage.put("runDate", date);
    usage.put("provider", completion.provider());
    usage.put("promptTokens", completion.promptTokens());
    usage.put("completionTokens", completion.completionTokens());
    usage.put("latencyMillis", completion.latencyMillis());
    usage.put("transportRetries", completion.transportRetries());
    usage.put("extraction", extracted ? "OK" : "EXTRACTION-FAILED");
    usage.put("costUsd", usd);
    usage.put("ecbRateDate", rate.publishedOn());
    usage.put("usdPerEur", rate.usdPerEur());
    usage.put("costEur", usd.divide(rate.usdPerEur(), 6, RoundingMode.HALF_UP));
    usage.put("protocolSha256", sha256(root.resolve("ai-testgen/PROTOCOL.md")));
    return usage.toPrettyString();
  }

  /** Sum of every recorded call so far — the budget guard is global, not per invocation. */
  private static BigDecimal spentEur(Path root) throws IOException {
    Path runs = root.resolve("ai-testgen/runs");
    if (!Files.isDirectory(runs)) {
      return BigDecimal.ZERO;
    }
    try (Stream<Path> files = Files.walk(runs)) {
      List<Path> usages =
          files.filter(p -> p.getFileName().toString().equals("usage.json")).toList();
      BigDecimal total = BigDecimal.ZERO;
      for (Path usage : usages) {
        total = total.add(JSON.readTree(Files.readString(usage)).path("costEur").decimalValue());
      }
      return total.setScale(4, RoundingMode.HALF_UP);
    }
  }

  // ------------------------------------------------------------------ plumbing

  /**
   * The API key, from the environment or from the git-ignored {@code .env} at the repository root.
   * Reading {@code .env} keeps the credential out of shell history — the environment still wins, so
   * CI and one-off overrides behave as expected.
   */
  private static String apiKey() throws IOException {
    String key = System.getenv("OPENROUTER_API_KEY");
    if (key == null || key.isBlank()) {
      key = fromDotEnv(Repo.root().resolve(".env"));
    }
    if (key == null || key.isBlank()) {
      throw new IllegalStateException(
          """
          OPENROUTER_API_KEY is not set.

          The generation step is the one part of this experiment that cannot run without a
          credential. Create a key at https://openrouter.ai/keys, then either

            cp .env.example .env     # and put the key in it — .env is git-ignored

          or export it for one run:

            export OPENROUTER_API_KEY='sk-or-...'

          then:

            ./mvnw -q -f ai-testgen/harness/pom.xml compile exec:java \\
              -Dexec.args="generate --corpus A --model anthropic/claude-sonnet-5"

          Nothing else in the repository needs a secret (.env.example documents this).""");
    }
    return key.strip();
  }

  private static String fromDotEnv(Path dotEnv) throws IOException {
    if (!Files.isRegularFile(dotEnv)) {
      return null;
    }
    for (String line : Files.readAllLines(dotEnv, StandardCharsets.UTF_8)) {
      String trimmed = line.strip();
      if (trimmed.startsWith("OPENROUTER_API_KEY=")) {
        return trimmed.substring("OPENROUTER_API_KEY=".length()).replaceAll("^[\"']|[\"']$", "");
      }
    }
    return null;
  }

  /**
   * Where run artifacts go. Defaults to the committed {@code ai-testgen/runs}; {@code --out} exists
   * so a prompt can be inspected without creating artifacts of a run that never happened.
   */
  private static Path runsRoot(Path root, Map<String, String> options) {
    String out = options.get("out");
    return out == null ? root.resolve("ai-testgen/runs") : Path.of(out).toAbsolutePath();
  }

  private static Corpus corpus(Map<String, String> options) {
    String value = options.get("corpus");
    if (value == null) {
      throw new IllegalArgumentException("--corpus A|B is required");
    }
    return Corpus.valueOf(value.toUpperCase(java.util.Locale.ROOT));
  }

  private static String model(Map<String, String> options) {
    String model = options.get("model");
    if (model == null) {
      throw new IllegalArgumentException(
          "--model is required; pinned models: " + Pricing.USD_PER_MILLION.keySet());
    }
    if (!Pricing.USD_PER_MILLION.containsKey(model)) {
      throw new IllegalArgumentException(
          "model '"
              + model
              + "' is not in the frozen price table "
              + Pricing.USD_PER_MILLION.keySet()
              + " — swapping models after the freeze needs an amendment (PROTOCOL.md)");
    }
    return model;
  }

  private static Map<String, String> parseOptions(String[] args) {
    Map<String, String> options = new java.util.LinkedHashMap<>();
    for (int i = 1; i < args.length - 1; i++) {
      if (args[i].startsWith("--")) {
        options.put(args[i].substring(2), args[i + 1]);
      }
    }
    return options;
  }

  private static String slug(String model) {
    return model.replace('/', '_');
  }

  private static void write(Path target, String content) throws IOException {
    Files.writeString(target, content, StandardCharsets.UTF_8);
  }

  private static String sha256(Path file) throws IOException {
    try {
      return HexFormat.of()
          .formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file)));
    } catch (NoSuchAlgorithmException impossible) {
      throw new IllegalStateException(impossible);
    }
  }

  private static void usage(String problem) {
    System.err.println(
        problem
            + """

            usage:
              plan
              render   --corpus A|B --model <id> [--date yyyy-mm-dd]
              generate --corpus A|B --model <id> [--date yyyy-mm-dd]
            """);
    System.exit(2);
  }

  private Harness() {}
}
