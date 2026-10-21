package at.stoicera.migrationlab.characterization;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;

/**
 * Report writer for the equivalence evidence ("Äquivalenz-Nachweis"). Records every test of the
 * suite with its result on the stand under test and writes {@code
 * target/nachweis/characterization-<stand>.json} when the run ends. CI joins the legacy and the
 * modern file into {@code nachweis.json} (scripts/nachweis/aggregate.mjs).
 *
 * <p>It observes and never decides: a test passes or fails exactly as without it. Tests may attach
 * the golden file, the endpoint and the received document via {@link #erfasse}, so the report can
 * show golden and answer side by side.
 */
public final class NachweisBericht implements BeforeTestExecutionCallback, TestWatcher {

  private static final ExtensionContext.Namespace NAMESPACE =
      ExtensionContext.Namespace.create(NachweisBericht.class);
  private static final ThreadLocal<Map<String, Object>> AKTUELL = new ThreadLocal<>();

  /** Attaches golden file, endpoint and received document to the running test. */
  public static void erfasse(String goldenDatei, String endpunkt, String empfangen) {
    Map<String, Object> eintrag = AKTUELL.get();
    if (eintrag == null) {
      return; // test class runs without the extension — nothing to record
    }
    eintrag.put("golden", goldenDatei);
    eintrag.put("endpunkt", endpunkt);
    eintrag.put("empfangen", empfangen);
  }

  @Override
  public void beforeTestExecution(ExtensionContext context) {
    AKTUELL.set(neuerEintrag(context));
  }

  @Override
  public void testSuccessful(ExtensionContext context) {
    abschliessen(context, "bestanden", null);
  }

  @Override
  public void testFailed(ExtensionContext context, Throwable cause) {
    abschliessen(context, "fehlgeschlagen", cause);
  }

  @Override
  public void testAborted(ExtensionContext context, Throwable cause) {
    abschliessen(context, "abgebrochen", cause);
  }

  @Override
  public void testDisabled(ExtensionContext context, Optional<String> reason) {
    abschliessen(context, "deaktiviert", null);
  }

  private static void abschliessen(ExtensionContext context, String status, Throwable cause) {
    Map<String, Object> eintrag = AKTUELL.get();
    if (eintrag == null || !context.getUniqueId().equals(eintrag.get("id"))) {
      eintrag = neuerEintrag(context); // failed before execution, e.g. in @BeforeEach
    }
    AKTUELL.remove();
    eintrag.put("status", status);
    if (cause != null) {
      String meldung = String.valueOf(cause.getMessage());
      eintrag.put("meldung", meldung.length() > 600 ? meldung.substring(0, 600) + " …" : meldung);
    }
    context
        .getRoot()
        .getStore(NAMESPACE)
        .getOrComputeIfAbsent(Bericht.class, k -> new Bericht(), Bericht.class)
        .add(eintrag);
  }

  private static Map<String, Object> neuerEintrag(ExtensionContext context) {
    Map<String, Object> eintrag = new LinkedHashMap<>();
    eintrag.put("id", context.getUniqueId());
    eintrag.put("klasse", context.getRequiredTestClass().getSimpleName());
    Optional<Method> methode = context.getTestMethod();
    eintrag.put("methode", methode.map(Method::getName).orElse(null));
    eintrag.put("name", context.getDisplayName());
    Sanktioniert sanktion = methode.map(m -> m.getAnnotation(Sanktioniert.class)).orElse(null);
    if (sanktion != null) {
      eintrag.put("sanktioniert", Map.of("ref", sanktion.value(), "quelle", sanktion.quelle()));
    }
    return eintrag;
  }

  /**
   * Collected for the whole engine run; written once when JUnit closes the root store. Both close
   * contracts are implemented (JUnit 5.13 moved from CloseableResource to AutoCloseable); the guard
   * makes a second call a no-op.
   */
  @SuppressWarnings("deprecation")
  private static final class Bericht
      implements ExtensionContext.Store.CloseableResource, AutoCloseable {
    private final List<Map<String, Object>> tests = new ArrayList<>();
    private boolean geschrieben;

    synchronized void add(Map<String, Object> eintrag) {
      tests.add(eintrag);
    }

    @Override
    public synchronized void close() throws Exception {
      if (geschrieben) {
        return;
      }
      geschrieben = true;
      Map<String, Object> dokument = new LinkedHashMap<>();
      dokument.put("suite", "characterization");
      dokument.put("stand", Stand.STAND);
      dokument.put("baseUrl", Stand.BASE_URL);
      dokument.put("erzeugt", Instant.now().toString());
      dokument.put("tests", tests);
      Path ziel = Path.of("target", "nachweis", "characterization-" + Stand.STAND + ".json");
      Files.createDirectories(ziel.getParent());
      new ObjectMapper()
          .enable(SerializationFeature.INDENT_OUTPUT)
          .writeValue(ziel.toFile(), dokument);
    }
  }
}
