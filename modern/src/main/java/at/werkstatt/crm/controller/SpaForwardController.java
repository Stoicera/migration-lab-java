package at.werkstatt.crm.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Stage 5 (Strangler Fig, ADR-0009): the Angular UI uses path routing, so every route the SPA owns
 * must be answered with the SPA shell — a deep link or reload would otherwise 404. One mapping is
 * added per ported route slice; the list is the server-side mirror of
 * frontend/src/app/app.routes.ts.
 */
@Controller
public class SpaForwardController {

  @GetMapping({
    "/start",
    "/kunden",
    "/kunden/neu",
    "/kunden/{id:\\d+}",
    "/fahrzeuge",
    "/auftraege",
    "/auftraege/neu",
    "/auftraege/{id:\\d+}",
    "/rechnungen",
    "/rechnungen/{id:\\d+}",
    "/bericht",
    "/admin"
  })
  public String spa() {
    return "forward:/index.html";
  }
}
