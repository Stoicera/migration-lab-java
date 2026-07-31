# Prompt instance — at.werkstatt.crm.controller.AdminController (corpus A, stratum S3)

Rendered mechanically by the harness from the frozen templates. Do not edit.

## System

````text
You are generating JUnit 5 unit tests for a legacy Java 8 / Spring Boot 1.5 codebase.
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

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import com.google.gson.Gson;

import at.werkstatt.crm.service.WerkstattService;

/**
 * Alte Admin-Seite als JSP, stammt noch aus der Tomcat-Zeit vor Spring Boot.
 * Wird nur vom Chef verwendet. Kein Login davor - die Seite kennt eh niemand.
 */
@Controller
public class AdminController {

	@Autowired
	private WerkstattService werkstattService;

	@Value("${werkstatt.firma.name}")
	private String firmaName;

	@Value("${werkstatt.version:?}")
	private String version;

	// gson statt jackson, weil das Beispiel damals aus einem Forum kopiert wurde
	private Gson gson = new Gson();

	@GetMapping("/admin")
	public String admin(Model model) {
		Map<String, Object> statistik = werkstattService.getAdminStatistik();
		model.addAttribute("statistik", statistik);
		model.addAttribute("statistikJson", gson.toJson(statistik));
		model.addAttribute("firmaName", firmaName);
		model.addAttribute("version", version);
		model.addAttribute("meldung", null);
		return "admin";
	}

	@PostMapping("/admin/bereinigen")
	public String bereinigen(Model model) {
		int anzahl = werkstattService.bereinigeStornierte();
		Map<String, Object> statistik = werkstattService.getAdminStatistik();
		model.addAttribute("statistik", statistik);
		model.addAttribute("statistikJson", gson.toJson(statistik));
		model.addAttribute("firmaName", firmaName);
		model.addAttribute("version", version);
		model.addAttribute("meldung", anzahl + " stornierte Aufträge wurden endgültig gelöscht.");
		return "admin";
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
