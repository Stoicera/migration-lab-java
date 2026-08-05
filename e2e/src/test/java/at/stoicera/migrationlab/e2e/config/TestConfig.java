package at.stoicera.migrationlab.e2e.config;

/**
 * Resolves everything that differs between the two application stands. Selected via
 * -Dtarget=legacy|modern; individual values can be overridden (-DbaseUrl=..., -DdbUrl=...) e.g. for
 * CI service containers.
 *
 * <p>The flag is validated eagerly and the characterization suite's flag is rejected outright.
 * Session 13 fixed exactly this hole on the characterization side — {@code -Dtarget=modern} there
 * was silently ignored and the run went green against the <em>legacy</em> stand, i.e. an
 * equivalence proof that proved nothing. The mirror image of that bug lived here until stage 6:
 * {@code -Dstand=modern} was ignored and this suite happily tested legacy. A guard on one side of a
 * symmetric mistake is half a guard.
 */
public final class TestConfig {

  private static final String TARGET =
      validateTarget(rejectStandFlag(System.getProperty("target", "legacy")));

  private TestConfig() {}

  public static String target() {
    return TARGET;
  }

  private static String rejectStandFlag(String target) {
    String stand = System.getProperty("stand");
    if (stand != null) {
      throw new IllegalArgumentException(
          "-Dstand='"
              + stand
              + "' is a characterization flag and does nothing in the e2e suite. This run would"
              + " have gone green against whatever -Dtarget/-DbaseUrl point at (default: legacy,"
              + " http://localhost:8080) while you believed it tested '"
              + stand
              + "'. Use -Dtarget=legacy|modern here.");
    }
    return target;
  }

  private static String validateTarget(String value) {
    if (!"legacy".equals(value) && !"modern".equals(value)) {
      throw new IllegalArgumentException(
          "-Dtarget must be 'legacy' or 'modern' (ADR-0004), got: '" + value + "'");
    }
    return value;
  }

  public static String baseUrl() {
    String override = System.getProperty("baseUrl");
    if (override != null) {
      return override;
    }
    return "legacy".equals(TARGET) ? "http://localhost:8080" : "http://localhost:8090";
  }

  public static String dbUrl() {
    String override = System.getProperty("dbUrl");
    if (override != null) {
      return override;
    }
    return "legacy".equals(TARGET)
        ? "jdbc:postgresql://localhost:5433/werkstatt"
        : "jdbc:postgresql://localhost:5434/werkstatt";
  }

  public static String dbUser() {
    return System.getProperty("dbUser", "werkstatt");
  }

  public static String dbPassword() {
    return System.getProperty("dbPassword", "werkstatt");
  }
}
