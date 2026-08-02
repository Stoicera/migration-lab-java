package at.stoicera.migrationlab.characterization;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Access to the running stand under test: HTTP client, JDBC connection and the reset-to-seed that
 * makes every capture deterministic. The seed file in legacy/db/init/ is the single source of the
 * golden state — both stands run the identical schema and seed, so the same reset works against
 * either DB.
 *
 * <p>Defaults target the legacy stand; the modern stand is selected with
 * -DbaseUrl=http://localhost:8090 -DdbUrl=jdbc:postgresql://localhost:5434/werkstatt
 * -Dstand=modern.
 */
public final class Stand {

  public static final String BASE_URL = System.getProperty("baseUrl", "http://localhost:8080");
  private static final String DB_URL =
      System.getProperty("dbUrl", "jdbc:postgresql://localhost:5433/werkstatt");
  private static final String DB_USER = System.getProperty("dbUser", "werkstatt");
  private static final String DB_PASSWORD = System.getProperty("dbPassword", "werkstatt");

  /**
   * Which stand's expectations apply where behaviour legally diverges — ADR-0004 (sanctioned
   * divergence). Values: "legacy" (default) | "modern". Everything not explicitly forked on this
   * flag must behave identically on both stands.
   */
  public static final String STAND =
      validateStand(rejectTargetFlag(System.getProperty("stand", "legacy")));

  private static final HttpClient HTTP = HttpClient.newHttpClient();

  private Stand() {}

  public static boolean isModern() {
    return "modern".equals(STAND);
  }

  public static Connection connect() throws Exception {
    return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
  }

  public static void resetToSeed() {
    try (Connection connection = connect();
        Statement statement = connection.createStatement()) {
      statement.execute(
          "TRUNCATE kunde, fahrzeug, auftrag, auftrag_position, rechnung RESTART IDENTITY CASCADE");
      for (String sql : parseStatements(locateSeedFile())) {
        statement.execute(sql);
      }
    } catch (Exception e) {
      throw new IllegalStateException(
          "DB reset failed — is the " + STAND + " stand running? (" + DB_URL + ")", e);
    }
  }

  public static HttpResponse<String> get(String pathAndQuery) {
    try {
      HttpRequest request =
          HttpRequest.newBuilder(URI.create(BASE_URL + pathAndQuery)).GET().build();
      return HTTP.send(request, HttpResponse.BodyHandlers.ofString());
    } catch (Exception e) {
      throw new IllegalStateException("GET " + pathAndQuery + " failed", e);
    }
  }

  public static HttpResponse<String> send(String method, String pathAndQuery, String jsonBody) {
    try {
      HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(BASE_URL + pathAndQuery));
      if (jsonBody != null) {
        builder
            .header("Content-Type", "application/json")
            .method(method, HttpRequest.BodyPublishers.ofString(jsonBody));
      } else {
        builder.method(method, HttpRequest.BodyPublishers.noBody());
      }
      return HTTP.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    } catch (Exception e) {
      throw new IllegalStateException(method + " " + pathAndQuery + " failed", e);
    }
  }

  /**
   * Fails fast when someone passes -Dtarget, which belongs to the e2e suite and is meaningless
   * here. Ignoring it silently was worse than it sounds: `-Dtarget=modern` produced a fully GREEN
   * run against the LEGACY stand, i.e. an equivalence proof that proved nothing. A safety net that
   * can be pointed at the wrong stand without saying so is not a safety net.
   */
  private static String rejectTargetFlag(String stand) {
    String target = System.getProperty("target");
    if (target != null) {
      throw new IllegalArgumentException(
          "-Dtarget='"
              + target
              + "' is an e2e flag and does nothing in the characterization suite. This run would"
              + " have gone green against whatever -DbaseUrl/-DdbUrl point at (default: legacy,"
              + " http://localhost:8080) while you believed it tested '"
              + target
              + "'. Pass all three flags instead, e.g.: -DbaseUrl=http://localhost:8090"
              + " -DdbUrl=jdbc:postgresql://localhost:5434/werkstatt -Dstand=modern");
    }
    return stand;
  }

  private static String validateStand(String value) {
    if (!"legacy".equals(value) && !"modern".equals(value)) {
      throw new IllegalArgumentException(
          "-Dstand must be 'legacy' or 'modern' (ADR-0004), got: '" + value + "'");
    }
    return value;
  }

  private static Path locateSeedFile() {
    Path fromRoot = Path.of("legacy/db/init/02-daten.sql");
    Path fromModule = Path.of("../legacy/db/init/02-daten.sql");
    if (Files.exists(fromRoot)) {
      return fromRoot;
    }
    if (Files.exists(fromModule)) {
      return fromModule;
    }
    throw new IllegalStateException(
        "Seed file 02-daten.sql not found from " + Path.of("").toAbsolutePath());
  }

  private static List<String> parseStatements(Path seedFile) throws Exception {
    List<String> statements = new ArrayList<>();
    StringBuilder current = new StringBuilder();
    for (String line : Files.readAllLines(seedFile)) {
      String trimmed = line.trim();
      if (trimmed.isEmpty() || trimmed.startsWith("--")) {
        continue;
      }
      current.append(line).append('\n');
      if (trimmed.endsWith(";")) {
        statements.add(current.toString());
        current.setLength(0);
      }
    }
    return statements;
  }
}
