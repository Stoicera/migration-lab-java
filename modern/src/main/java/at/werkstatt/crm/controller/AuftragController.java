package at.werkstatt.crm.controller;

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

import at.werkstatt.crm.model.Auftrag;
import at.werkstatt.crm.model.AuftragPosition;
import at.werkstatt.crm.service.WerkstattService;

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
