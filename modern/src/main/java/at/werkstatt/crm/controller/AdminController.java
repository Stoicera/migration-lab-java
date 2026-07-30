package at.werkstatt.crm.controller;

import java.util.Map;

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

	private final WerkstattService werkstattService;
	private final String firmaName;
	private final String version;

	// gson statt jackson, weil das Beispiel damals aus einem Forum kopiert wurde
	private final Gson gson = new Gson();

	public AdminController(WerkstattService werkstattService,
			@Value("${werkstatt.firma.name}") String firmaName,
			@Value("${werkstatt.version:?}") String version) {
		this.werkstattService = werkstattService;
		this.firmaName = firmaName;
		this.version = version;
	}

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
