# Prompt instance — at.werkstatt.crm.controller.AuftragController (corpus B, stratum S2)

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
  package at.werkstatt.crm.gen, class name AuftragControllerGeneratedTest.
- Aim for behavioural coverage: happy paths, edge cases, error/rejection paths.
- Do not invent methods that do not exist in the provided source.
````

## User

````text
Class under test (full source):
package at.werkstatt.crm.controller;

import at.werkstatt.crm.model.Auftrag;
import at.werkstatt.crm.model.AuftragPosition;
import at.werkstatt.crm.service.WerkstattService;
import java.util.List;
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

@RestController
@RequestMapping("/api/auftraege")
public class AuftragController {

  private final WerkstattService werkstattService;

  public AuftragController(WerkstattService werkstattService) {
    this.werkstattService = werkstattService;
  }

  @GetMapping
  public List<Auftrag> liste(@RequestParam(value = "status", required = false) String status) {
    return werkstattService.getAuftraege(status);
  }

  @GetMapping("/{id}")
  public ResponseEntity<Auftrag> einzeln(@PathVariable long id) {
    Auftrag auftrag = werkstattService.getAuftrag(id);
    if (auftrag == null) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(auftrag);
  }

  @PostMapping
  public ResponseEntity<?> anlegen(@RequestBody Auftrag auftrag) {
    try {
      return ResponseEntity.ok(werkstattService.neuerAuftrag(auftrag));
    } catch (Exception e) {
      return ResponseEntity.status(500).body(e.getMessage());
    }
  }

  // Statuswechsel als PUT mit Query-Parameter, hat sich so eingebuergert
  @PutMapping("/{id}/status")
  public ResponseEntity<?> status(@PathVariable long id, @RequestParam("neu") String neuerStatus) {
    try {
      return ResponseEntity.ok(werkstattService.setzeStatus(id, neuerStatus));
    } catch (Exception e) {
      return ResponseEntity.status(500).body(e.getMessage());
    }
  }

  @PostMapping("/{id}/positionen")
  public ResponseEntity<?> position(@PathVariable long id, @RequestBody AuftragPosition position) {
    try {
      return ResponseEntity.ok(werkstattService.neuePosition(id, position));
    } catch (Exception e) {
      return ResponseEntity.status(500).body(e.getMessage());
    }
  }

  @DeleteMapping("/positionen/{positionId}")
  public ResponseEntity<?> positionLoeschen(@PathVariable long positionId) {
    try {
      werkstattService.loeschePosition(positionId);
      return ResponseEntity.ok().build();
    } catch (Exception e) {
      return ResponseEntity.status(500).body(e.getMessage());
    }
  }
}

Direct dependency types visible to the class (signatures only):
// at.werkstatt.crm.model.Auftrag
public class Auftrag;
public static final String STATUS_ANGENOMMEN = "ANGENOMMEN";
public static final String STATUS_IN_ARBEIT = "IN_ARBEIT";
public static final String STATUS_FERTIG = "FERTIG";
public static final String STATUS_ABGEHOLT = "ABGEHOLT";
public static final String STATUS_STORNIERT = "STORNIERT";
public Long getId();
public void setId(Long id);
public String getAuftragNr();
public void setAuftragNr(String auftragNr);
public Long getFahrzeugId();
public void setFahrzeugId(Long fahrzeugId);
public Long getKundeId();
public void setKundeId(Long kundeId);
public String getStatus();
public void setStatus(String status);
public String getBeschreibung();
public void setBeschreibung(String beschreibung);
public Integer getKmStand();
public void setKmStand(Integer kmStand);
public Date getAngenommenAm();
public void setAngenommenAm(Date angenommenAm);
public Date getFertigAm();
public void setFertigAm(Date fertigAm);
public Date getAbgeholtAm();
public void setAbgeholtAm(Date abgeholtAm);
public String getKundeName();
public void setKundeName(String kundeName);
public String getKennzeichen();
public void setKennzeichen(String kennzeichen);
public String getFahrzeugBezeichnung();
public void setFahrzeugBezeichnung(String fahrzeugBezeichnung);
public List<AuftragPosition> getPositionen();
public void setPositionen(List<AuftragPosition> positionen);
public double getSummeNetto();

// at.werkstatt.crm.model.AuftragPosition
public class AuftragPosition;
public static final String TYP_ARBEIT = "ARBEIT";
public static final String TYP_MATERIAL = "MATERIAL";
public Long getId();
public void setId(Long id);
public Long getAuftragId();
public void setAuftragId(Long auftragId);
public String getTyp();
public void setTyp(String typ);
public String getBezeichnung();
public void setBezeichnung(String bezeichnung);
public double getMenge();
public void setMenge(double menge);
public double getEinzelpreis();
public void setEinzelpreis(double einzelpreis);
public double getGesamtpreis();

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
