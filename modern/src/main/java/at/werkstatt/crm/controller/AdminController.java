package at.werkstatt.crm.controller;

import at.werkstatt.crm.service.WerkstattService;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Stage 5 (SD-2, ADR-0004): the JSP admin page is absorbed into the SPA — this controller now
 * serves JSON for the Angular /admin route (gson and the whole JSP/JSTL stack died with the page).
 * POST /admin/bereinigen keeps its path and its exact German meldung: that contract is pinned by
 * the characterization suite on both stands.
 */
@RestController
public class AdminController {

  private final WerkstattService werkstattService;
  private final String firmaName;
  private final String version;

  public AdminController(
      WerkstattService werkstattService,
      @Value("${werkstatt.firma.name}") String firmaName,
      @Value("${werkstatt.version:?}") String version) {
    this.werkstattService = werkstattService;
    this.firmaName = firmaName;
    this.version = version;
  }

  @GetMapping("/api/admin/statistik")
  public Map<String, Object> statistik() {
    Map<String, Object> antwort = new HashMap<>();
    antwort.put("firmaName", firmaName);
    antwort.put("version", version);
    antwort.put("statistik", werkstattService.getAdminStatistik());
    return antwort;
  }

  @PostMapping("/admin/bereinigen")
  public Map<String, Object> bereinigen() {
    int anzahl = werkstattService.bereinigeStornierte();
    Map<String, Object> antwort = new HashMap<>();
    antwort.put("geloescht", anzahl);
    antwort.put("meldung", anzahl + " stornierte Aufträge wurden endgültig gelöscht.");
    return antwort;
  }
}
