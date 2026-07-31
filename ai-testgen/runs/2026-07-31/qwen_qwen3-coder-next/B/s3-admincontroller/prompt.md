# Prompt instance — at.werkstatt.crm.controller.AdminController (corpus B, stratum S3)

Rendered mechanically by the harness from the frozen templates. Do not edit.

## System

````text
You are generating JUnit 5 unit tests for a Java 25 / Spring Boot 4.1 codebase.
Constraints:
- JUnit 5.14.x, Mockito (with mockito-junit-jupiter), AssertJ. From spring-test, only
  org.springframework.test.util.ReflectionTestUtils is available. No other libraries.
- Unit tests only: no Spring context, no MockMvc, no real database, no network, no file I/O.
- Mock all dependencies of the class under test and instantiate it as it is — the class under
  test must not be modified.
- Output exactly one complete, compilable test class in a single ```java code block,
  package at.werkstatt.crm.gen, class name AdminControllerGeneratedTest.
- Aim for behavioural coverage: happy paths, edge cases, error/rejection paths.
- Do not invent methods that do not exist in the provided source.
````

## User

````text
Class under test (full source):
package at.werkstatt.crm.controller;

import at.werkstatt.crm.service.WerkstattService;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Stage 5 (SD-2, ADR-0004): the JSP admin page is absorbed into the SPA — this controller now
 * serves JSON for the Angular /admin route (gson and the whole JSP/JSTL stack died with the page).
 * POST /admin/bereinigen keeps its path and its exact German meldung: that contract is pinned by
 * the characterization suite on both stands.
 */
@RestController
public class AdminController {

  private final WerkstattService werkstattService;
  private final String firmaName;
  private final String version;

  public AdminController(
      WerkstattService werkstattService,
      @Value("${werkstatt.firma.name}") String firmaName,
      @Value("${werkstatt.version:?}") String version) {
    this.werkstattService = werkstattService;
    this.firmaName = firmaName;
    this.version = version;
  }

  @GetMapping("/api/admin/statistik")
  public Map<String, Object> statistik() {
    Map<String, Object> antwort = new HashMap<>();
    antwort.put("firmaName", firmaName);
    antwort.put("version", version);
    antwort.put("statistik", werkstattService.getAdminStatistik());
    return antwort;
  }

  @PostMapping("/admin/bereinigen")
  public Map<String, Object> bereinigen() {
    int anzahl = werkstattService.bereinigeStornierte();
    Map<String, Object> antwort = new HashMap<>();
    antwort.put("geloescht", anzahl);
    antwort.put("meldung", anzahl + " stornierte Aufträge wurden endgültig gelöscht.");
    return antwort;
  }
}

Direct dependency types visible to the class (signatures only):
// at.werkstatt.crm.service.WerkstattService
public class WerkstattService;
public List<Kunde> getAlleKunden();
public List<Kunde> sucheKunden(String suchbegriff);
public Kunde getKunde(long id);
public Kunde speichereKunde(Kunde kunde);
public void loescheKunde(long id);
public List<Fahrzeug> getAlleFahrzeuge();
public List<Fahrzeug> getFahrzeugeZuKunde(long kundeId);
public Fahrzeug getFahrzeug(long id);
public Fahrzeug speichereFahrzeug(Fahrzeug fahrzeug);
public void loescheFahrzeug(long id);
public List<Auftrag> getAuftraege(String status);
public Auftrag getAuftrag(long id);
public List<AuftragPosition> getPositionen(long auftragId);
public Auftrag neuerAuftrag(Auftrag auftrag);
public Auftrag setzeStatus(long auftragId, String neuerStatus);
public AuftragPosition neuePosition(long auftragId, AuftragPosition position);
public void loeschePosition(long positionId);
public List<Rechnung> getAlleRechnungen();
public Rechnung getRechnung(long id);
public Rechnung erstelleRechnung(long auftragId);
public Rechnung setzeBezahlt(long rechnungId);
public List<MonatsBericht> getMonatsBericht(int jahr);
public List<Map<String, Object>> getTopKunden(int jahr);
public Map<String, Object> getAdminStatistik();
public int bereinigeStornierte();

Database schema excerpt referenced by the SQL in this class (DDL, if any):
(none — this class contains no SQL)

Write the test class now.
````
