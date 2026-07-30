package at.werkstatt.crm.controller;

import at.werkstatt.crm.model.Rechnung;
import at.werkstatt.crm.service.WerkstattService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rechnungen")
public class RechnungController {

  private final WerkstattService werkstattService;

  public RechnungController(WerkstattService werkstattService) {
    this.werkstattService = werkstattService;
  }

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
