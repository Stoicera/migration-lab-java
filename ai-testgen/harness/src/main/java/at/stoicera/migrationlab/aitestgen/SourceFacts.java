package at.stoicera.migrationlab.aitestgen;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Fills the two mechanical placeholders of the user prompt (PROTOCOL.md §4). Both rules are
 * deliberately simple and deterministic — the same source always yields the same prompt, and a
 * reader can re-derive every prompt instance by hand:
 *
 * <ul>
 *   <li>{@code <DEPENDENCY_SIGNATURES>}: for every project-internal type the class references, the
 *       public declaration lines of that type, bodies stripped.
 *   <li>{@code <DDL_EXCERPT>}: the CREATE TABLE statements of every table named in the class's SQL,
 *       in schema order; empty for classes without SQL.
 * </ul>
 *
 * <p>No AST is used on purpose: a parser dependency would be one more thing that must still resolve
 * years from now to reproduce the experiment.
 */
public final class SourceFacts {

  private SourceFacts() {}

  private static final String INTERNAL_PACKAGE = "at.werkstatt.crm.";

  private static final Pattern IMPORT_INTERNAL =
      Pattern.compile("(?m)^import\\s+(" + Pattern.quote(INTERNAL_PACKAGE) + "[\\w.]+)\\s*;");

  /** table names in SQL string literals: FROM x, JOIN x, INTO x, UPDATE x */
  private static final Pattern SQL_TABLE =
      Pattern.compile("(?i)\\b(?:from|join|into|update)\\s+([a-z_][a-z0-9_]*)");

  private static final Pattern CREATE_TABLE =
      Pattern.compile("(?is)CREATE\\s+TABLE\\s+([a-z_][a-z0-9_]*)\\s*\\((?:[^;])*?\\);");

  /**
   * Public signatures of the project-internal types the class under test references, so the model
   * knows the domain API without being handed the whole code base.
   */
  public static String dependencySignatures(Path repositoryRoot, Unit unit) throws IOException {
    String source = Files.readString(repositoryRoot.resolve(unit.source()), StandardCharsets.UTF_8);
    Set<String> referenced = new LinkedHashSet<>();
    Matcher imports = IMPORT_INTERNAL.matcher(source);
    while (imports.find()) {
      referenced.add(imports.group(1));
    }
    // same-package types carry no import statement — add the ones actually mentioned
    Path packageDirectory = repositoryRoot.resolve(unit.source()).getParent();
    if (packageDirectory != null && Files.isDirectory(packageDirectory)) {
      try (var siblings = Files.list(packageDirectory)) {
        siblings
            .filter(p -> p.getFileName().toString().endsWith(".java"))
            .forEach(
                p -> {
                  String simple = p.getFileName().toString().replace(".java", "");
                  if (!simple.equals(unit.simpleName())
                      && Pattern.compile("\\b" + Pattern.quote(simple) + "\\b")
                          .matcher(source)
                          .find()) {
                    referenced.add(
                        unit.fqn().substring(0, unit.fqn().lastIndexOf('.') + 1) + simple);
                  }
                });
      }
    }

    StringBuilder rendered = new StringBuilder();
    for (String fqn : referenced.stream().sorted().toList()) {
      Path file =
          repositoryRoot
              .resolve(Path.of(unit.corpus().module(), "src", "main", "java"))
              .resolve(fqn.replace('.', '/') + ".java");
      if (!Files.isRegularFile(file)) {
        continue; // not a project type of this corpus (e.g. a package that no longer exists)
      }
      rendered.append("// ").append(fqn).append('\n');
      for (String signature : publicDeclarations(Files.readString(file, StandardCharsets.UTF_8))) {
        rendered.append(signature).append('\n');
      }
      rendered.append('\n');
    }
    return rendered.toString().strip();
  }

  /**
   * Public type/method/field declarations of one source file, bodies stripped.
   *
   * <p>Only the top-level type and its direct members are emitted. Nesting matters here: the God
   * service implements its row mapping as anonymous {@code RowMapper}s, whose {@code mapRow}
   * methods are public members of an inner class, not of the service. Emitting them would tell the
   * model about methods the class does not have — and then punish it for "inventing" them.
   */
  static List<String> publicDeclarations(String source) {
    List<String> declarations = new ArrayList<>();
    int depth = 0;
    boolean inBlockComment = false;
    for (String line : source.split("\\R")) {
      String trimmed = line.strip();
      if (!inBlockComment && depth <= 1 && trimmed.startsWith("public ")) {
        if (trimmed.endsWith("{")) {
          declarations.add(trimmed.substring(0, trimmed.length() - 1).strip() + ";");
        } else if (trimmed.endsWith(";")) {
          declarations.add(trimmed);
        }
      }
      Braces braces = countBraces(line, inBlockComment);
      depth += braces.open() - braces.close();
      inBlockComment = braces.stillInBlockComment();
    }
    return declarations;
  }

  private record Braces(int open, int close, boolean stillInBlockComment) {}

  /**
   * Counts braces outside string literals, char literals and comments — {@code {@code …}} in a
   * javadoc block would otherwise shift the nesting depth and hide real members.
   */
  private static Braces countBraces(String line, boolean inBlockComment) {
    int open = 0;
    int close = 0;
    boolean inString = false;
    boolean inChar = false;
    for (int i = 0; i < line.length(); i++) {
      char current = line.charAt(i);
      char next = i + 1 < line.length() ? line.charAt(i + 1) : '\0';
      if (inBlockComment) {
        if (current == '*' && next == '/') {
          inBlockComment = false;
          i++;
        }
        continue;
      }
      if (inString || inChar) {
        if (current == '\\') {
          i++;
        } else if (inString && current == '"') {
          inString = false;
        } else if (inChar && current == '\'') {
          inChar = false;
        }
        continue;
      }
      if (current == '/' && next == '/') {
        break;
      }
      if (current == '/' && next == '*') {
        inBlockComment = true;
        i++;
      } else if (current == '"') {
        inString = true;
      } else if (current == '\'') {
        inChar = true;
      } else if (current == '{') {
        open++;
      } else if (current == '}') {
        close++;
      }
    }
    return new Braces(open, close, inBlockComment);
  }

  /** CREATE TABLE statements for every table the class's SQL touches; empty string if none. */
  public static String ddlExcerpt(Path repositoryRoot, Unit unit) throws IOException {
    String source = Files.readString(repositoryRoot.resolve(unit.source()), StandardCharsets.UTF_8);
    Set<String> tables = new LinkedHashSet<>();
    Matcher references = SQL_TABLE.matcher(source);
    while (references.find()) {
      tables.add(references.group(1).toLowerCase(Locale.ROOT));
    }
    if (tables.isEmpty()) {
      return "";
    }
    Path schema =
        repositoryRoot.resolve(Path.of(unit.corpus().module(), "db", "init", "01-schema.sql"));
    if (!Files.isRegularFile(schema)) {
      throw new IOException("schema file missing, cannot render a reproducible prompt: " + schema);
    }
    StringBuilder excerpt = new StringBuilder();
    Matcher tableDefinitions =
        CREATE_TABLE.matcher(Files.readString(schema, StandardCharsets.UTF_8));
    while (tableDefinitions.find()) {
      if (tables.contains(tableDefinitions.group(1).toLowerCase(Locale.ROOT))) {
        excerpt.append(tableDefinitions.group()).append("\n\n");
      }
    }
    return excerpt.toString().strip();
  }
}
