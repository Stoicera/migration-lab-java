package at.stoicera.migrationlab.e2e.support;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import at.stoicera.migrationlab.e2e.config.TestConfig;

/**
 * Determinism anchor of the safety net: before every scenario class the
 * database is reset to the committed seed (legacy/db/init/02-daten.sql).
 * E2E assertions may therefore rely on exact seed values; tests that write
 * do so into the current month and never touch the frozen seed months.
 */
public final class DbReset {

	private DbReset() {
	}

	public static void toSeedState() {
		Path seed = locateSeedFile();
		try (Connection connection = DriverManager.getConnection(
				TestConfig.dbUrl(), TestConfig.dbUser(), TestConfig.dbPassword());
				Statement statement = connection.createStatement()) {
			statement.execute("TRUNCATE kunde, fahrzeug, auftrag, auftrag_position, rechnung RESTART IDENTITY CASCADE");
			for (String sql : parseStatements(seed)) {
				statement.execute(sql);
			}
		} catch (Exception e) {
			throw new IllegalStateException("DB reset to seed state failed — is the "
					+ TestConfig.target() + " stand running? (" + TestConfig.dbUrl() + ")", e);
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
		throw new IllegalStateException("Seed file 02-daten.sql not found from " + Path.of("").toAbsolutePath());
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
