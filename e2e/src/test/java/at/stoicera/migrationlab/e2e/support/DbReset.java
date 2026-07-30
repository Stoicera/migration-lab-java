package at.stoicera.migrationlab.e2e.support;

import at.stoicera.migrationlab.e2e.config.TestConfig;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Determinism anchor of the safety net: before every scenario class the database is reset to the
 * committed seed (legacy/db/init/02-daten.sql). E2E assertions may therefore rely on exact seed
 * values; tests that write do so into the current month and never touch the frozen seed months.
 */
public final class DbReset {

  private DbReset() {}

  public static void toSeedState() {
    Path seed = locateSeedFile();
    try (Connection connection =
            DriverManager.getConnection(
                TestConfig.dbUrl(), TestConfig.dbUser(), TestConfig.dbPassword());
        Statement statement = connection.createStatement()) {
      statement.execute(
          "TRUNCATE kunde, fahrzeug, auftrag, auftrag_position, rechnung RESTART IDENTITY CASCADE");
      for (String sql : parseStatements(seed)) {
        statement.execute(sql);
      }
    } catch (Exception e) {
      throw new IllegalStateException(
          "DB reset to seed state failed — is the "
              + TestConfig.target()
              + " stand running? ("
              + TestConfig.dbUrl()
              + ")",
          e);
    }
  }

  private static Path locateSeedFile() {
    // works from repo root (CI) and from the e2e module directory (IDE)
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

  /**
   * Line-based splitter: a statement ends where a line ends with ';'. That is enough for the
   * canonical seed and deliberately NOT a SQL parser — but it would silently corrupt statements
   * containing ';' inside string literals or dollar-quoted blocks. The guards below turn that
   * silent corruption into a loud failure pointing here, so whoever extends the seed finds out at
   * the first run, not via a mysteriously half-loaded database.
   */
  private static List<String> parseStatements(Path seedFile) throws Exception {
    List<String> statements = new ArrayList<>();
    StringBuilder current = new StringBuilder();
    for (String line : Files.readAllLines(seedFile)) {
      String trimmed = line.trim();
      if (trimmed.isEmpty() || trimmed.startsWith("--")) {
        continue;
      }
      if (line.contains("$$")) {
        throw new IllegalStateException(
            "Seed file "
                + seedFile
                + " contains a dollar-quote ($$). "
                + "The line-based statement splitter in DbReset.parseStatements cannot handle "
                + "dollar-quoted blocks and would corrupt them silently — extend the splitter "
                + "before using $$ in the seed.");
      }
      current.append(line).append('\n');
      if (trimmed.endsWith(";")) {
        requireBalancedQuotes(current.toString(), seedFile);
        statements.add(current.toString());
        current.setLength(0);
      }
    }
    if (!current.toString().isBlank()) {
      throw new IllegalStateException(
          "Seed file "
              + seedFile
              + " ends with an unterminated statement "
              + "(no trailing ';'). The splitter in DbReset.parseStatements would silently drop it:\n"
              + current);
    }
    return statements;
  }

  /**
   * An odd number of single quotes means the split cut through a string literal (e.g. a ';' inside
   * a literal at a line end). SQL escapes quotes by doubling them (''), which keeps the count even
   * — so odd is always a split error, never legal seed content.
   */
  private static void requireBalancedQuotes(String statement, Path seedFile) {
    long quotes = statement.chars().filter(c -> c == '\'').count();
    if (quotes % 2 != 0) {
      throw new IllegalStateException(
          "Statement with unbalanced single quotes after splitting "
              + seedFile
              + " — most likely a ';' inside a string literal at a line end, which the "
              + "line-based splitter in DbReset.parseStatements mis-split. Offending fragment:\n"
              + statement);
    }
  }
}
