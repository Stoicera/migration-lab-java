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
