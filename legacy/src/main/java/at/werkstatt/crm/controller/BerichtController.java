package at.werkstatt.crm.controller;

import java.util.Calendar;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import at.werkstatt.crm.model.MonatsBericht;
import at.werkstatt.crm.service.WerkstattService;

@RestController
@RequestMapping("/api/bericht")
public class BerichtController {

	@Autowired
	private WerkstattService werkstattService;

	@GetMapping("/monat")
	public List<MonatsBericht> monatsBericht(@RequestParam(value = "jahr", required = false) Integer jahr) {
		if (jahr == null) {
			jahr = Calendar.getInstance().get(Calendar.YEAR);
		}
		return werkstattService.getMonatsBericht(jahr);
	}

	@GetMapping("/topkunden")
	public List<Map<String, Object>> topKunden(@RequestParam(value = "jahr", required = false) Integer jahr) {
		if (jahr == null) {
			jahr = Calendar.getInstance().get(Calendar.YEAR);
		}
		return werkstattService.getTopKunden(jahr);
	}

}
