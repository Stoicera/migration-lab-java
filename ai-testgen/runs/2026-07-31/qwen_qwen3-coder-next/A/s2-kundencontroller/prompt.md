# Prompt instance — at.werkstatt.crm.controller.KundenController (corpus A, stratum S2)

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
  package at.werkstatt.crm.gen, class name KundenControllerGeneratedTest.
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import at.werkstatt.crm.model.Fahrzeug;
import at.werkstatt.crm.model.Kunde;
import at.werkstatt.crm.service.WerkstattService;

@RestController
@RequestMapping("/api/kunden")
public class KundenController {

	@Autowired
	private WerkstattService werkstattService;

	@GetMapping
	public List<Kunde> liste(@RequestParam(value = "suche", required = false) String suche) {
		if (suche != null && suche.trim().length() > 0) {
			return werkstattService.sucheKunden(suche.trim());
		}
		return werkstattService.getAlleKunden();
	}

	@GetMapping("/{id}")
	public ResponseEntity<Kunde> einzeln(@PathVariable long id) {
		Kunde kunde = werkstattService.getKunde(id);
		if (kunde == null) {
			return ResponseEntity.notFound().build();
		}
		return ResponseEntity.ok(kunde);
	}

	@GetMapping("/{id}/fahrzeuge")
	public List<Fahrzeug> fahrzeuge(@PathVariable long id) {
		return werkstattService.getFahrzeugeZuKunde(id);
	}

	@PostMapping
	public Kunde anlegen(@RequestBody Kunde kunde) {
		// keine Validierung, das Frontend schickt schon das Richtige
		kunde.setId(null);
		return werkstattService.speichereKunde(kunde);
	}

	@PutMapping("/{id}")
	public ResponseEntity<?> aendern(@PathVariable long id, @RequestBody Kunde kunde) {
		try {
			kunde.setId(id);
			return ResponseEntity.ok(werkstattService.speichereKunde(kunde));
		} catch (Exception e) {
			return ResponseEntity.status(500).body(e.getMessage());
		}
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<?> loeschen(@PathVariable long id) {
		try {
			werkstattService.loescheKunde(id);
			return ResponseEntity.ok().build();
		} catch (Exception e) {
			// Meldung 1:1 zum Client, hilft beim Support-Telefonat
			return ResponseEntity.status(500).body(e.getMessage());
		}
	}

}

Direct dependency types visible to the class (signatures only):
// at.werkstatt.crm.model.Fahrzeug
public class Fahrzeug;
public Fahrzeug();
public Long getId();
public void setId(Long id);
public Long getKundeId();
public void setKundeId(Long kundeId);
public String getKennzeichen();
public void setKennzeichen(String kennzeichen);
public String getMarke();
public void setMarke(String marke);
public String getModell();
public void setModell(String modell);
public String getFahrgestellnr();
public void setFahrgestellnr(String fahrgestellnr);
public Integer getBaujahr();
public void setBaujahr(Integer baujahr);
public Integer getKmStand();
public void setKmStand(Integer kmStand);
public Date getPickerlDatum();
public void setPickerlDatum(Date pickerlDatum);
public Date getAngelegtAm();
public void setAngelegtAm(Date angelegtAm);
public String getKundeName();
public void setKundeName(String kundeName);
public String getBezeichnung();

// at.werkstatt.crm.model.Kunde
public class Kunde;
public Kunde();
public Long getId();
public void setId(Long id);
public String getAnrede();
public void setAnrede(String anrede);
public String getVorname();
public void setVorname(String vorname);
public String getNachname();
public void setNachname(String nachname);
public String getTelefon();
public void setTelefon(String telefon);
public String getEmail();
public void setEmail(String email);
public String getStrasse();
public void setStrasse(String strasse);
public String getPlz();
public void setPlz(String plz);
public String getOrt();
public void setOrt(String ort);
public String getNotiz();
public void setNotiz(String notiz);
public Date getAngelegtAm();
public void setAngelegtAm(Date angelegtAm);
public String getAnzeigeName();

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
