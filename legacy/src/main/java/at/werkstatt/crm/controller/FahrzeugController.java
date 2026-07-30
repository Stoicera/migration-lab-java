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
import org.springframework.web.bind.annotation.RestController;

import at.werkstatt.crm.model.Fahrzeug;
import at.werkstatt.crm.service.WerkstattService;

@RestController
@RequestMapping("/api/fahrzeuge")
public class FahrzeugController {

	@Autowired
	private WerkstattService werkstattService;

	@GetMapping
	public List<Fahrzeug> liste() {
		return werkstattService.getAlleFahrzeuge();
	}

	@GetMapping("/{id}")
	public ResponseEntity<Fahrzeug> einzeln(@PathVariable long id) {
		Fahrzeug fahrzeug = werkstattService.getFahrzeug(id);
		if (fahrzeug == null) {
			return ResponseEntity.notFound().build();
		}
		return ResponseEntity.ok(fahrzeug);
	}

	@PostMapping
	public Fahrzeug anlegen(@RequestBody Fahrzeug fahrzeug) {
		fahrzeug.setId(null);
		return werkstattService.speichereFahrzeug(fahrzeug);
	}

	@PutMapping("/{id}")
	public ResponseEntity<?> aendern(@PathVariable long id, @RequestBody Fahrzeug fahrzeug) {
		try {
			fahrzeug.setId(id);
			return ResponseEntity.ok(werkstattService.speichereFahrzeug(fahrzeug));
		} catch (Exception e) {
			return ResponseEntity.status(500).body(e.getMessage());
		}
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<?> loeschen(@PathVariable long id) {
		try {
			werkstattService.loescheFahrzeug(id);
			return ResponseEntity.ok().build();
		} catch (Exception e) {
			return ResponseEntity.status(500).body(e.getMessage());
		}
	}

}
