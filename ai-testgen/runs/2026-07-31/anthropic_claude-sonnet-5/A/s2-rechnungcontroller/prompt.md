# Prompt instance — at.werkstatt.crm.controller.RechnungController (corpus A, stratum S2)

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
  package at.werkstatt.crm.gen, class name RechnungControllerGeneratedTest.
- Aim for behavioural coverage: happy paths, edge cases, error/rejection paths.
- Do not invent methods that do not exist in the provided source.
````

## User

````text
Class under test (full source):
package at.werkstatt.crm.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import at.werkstatt.crm.model.Rechnung;
import at.werkstatt.crm.service.WerkstattService;

@RestController
@RequestMapping("/api/rechnungen")
public class RechnungController {

	@Autowired
	private WerkstattService werkstattService;

	@GetMapping
	public List<Rechnung> liste() {
		return werkstattService.getAlleRechnungen();
	}

	@GetMapping("/{id}")
	public ResponseEntity<Rechnung> einzeln(@PathVariable long id) {
		Rechnung rechnung = werkstattService.getRechnung(id);
		if (rechnung == null) {
			return ResponseEntity.notFound().build();
		}
		return ResponseEntity.ok(rechnung);
	}

	@PostMapping("/auftrag/{auftragId}")
	public ResponseEntity<?> erstellen(@PathVariable long auftragId) {
		try {
			return ResponseEntity.ok(werkstattService.erstelleRechnung(auftragId));
		} catch (Exception e) {
			return ResponseEntity.status(500).body(e.getMessage());
		}
	}

	@PutMapping("/{id}/bezahlt")
	public ResponseEntity<?> bezahlt(@PathVariable long id) {
		try {
			return ResponseEntity.ok(werkstattService.setzeBezahlt(id));
		} catch (Exception e) {
			return ResponseEntity.status(500).body(e.getMessage());
		}
	}

}

Direct dependency types visible to the class (signatures only):
// at.werkstatt.crm.model.Rechnung
public class Rechnung;
public Rechnung();
public Long getId();
public void setId(Long id);
public String getRechnungNr();
public void setRechnungNr(String rechnungNr);
public Long getAuftragId();
public void setAuftragId(Long auftragId);
public Date getAusgestelltAm();
public void setAusgestelltAm(Date ausgestelltAm);
public double getSummeNetto();
public void setSummeNetto(double summeNetto);
public double getUst();
public void setUst(double ust);
public double getSummeBrutto();
public void setSummeBrutto(double summeBrutto);
public boolean isBezahlt();
public void setBezahlt(boolean bezahlt);
public Date getBezahltAm();
public void setBezahltAm(Date bezahltAm);
public String getAuftragNr();
public void setAuftragNr(String auftragNr);
public String getKundeName();
public void setKundeName(String kundeName);

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
