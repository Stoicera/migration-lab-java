package at.stoicera.migrationlab.e2e.config;

/**
 * Resolves everything that differs between the two application stands.
 * Selected via -Dtarget=legacy|modern; individual values can be overridden
 * (-DbaseUrl=..., -DdbUrl=...) e.g. for CI service containers.
 */
public final class TestConfig {

	private static final String TARGET = System.getProperty("target", "legacy");

	private TestConfig() {
	}

	public static String target() {
		return TARGET;
	}

	public static String baseUrl() {
		String override = System.getProperty("baseUrl");
		if (override != null) {
			return override;
		}
		switch (TARGET) {
			case "legacy":
				return "http://localhost:8080";
			case "modern":
				return "http://localhost:8090";
			default:
				throw new IllegalStateException("Unknown target: " + TARGET);
		}
	}

	public static String dbUrl() {
		String override = System.getProperty("dbUrl");
		if (override != null) {
			return override;
		}
		switch (TARGET) {
			case "legacy":
				return "jdbc:postgresql://localhost:5433/werkstatt";
			case "modern":
				return "jdbc:postgresql://localhost:5434/werkstatt";
			default:
				throw new IllegalStateException("Unknown target: " + TARGET);
		}
	}

	public static String dbUser() {
		return System.getProperty("dbUser", "werkstatt");
	}

	public static String dbPassword() {
		return System.getProperty("dbPassword", "werkstatt");
	}
}
