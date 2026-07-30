package at.stoicera.migrationlab.characterization;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Pins the 4xx/5xx surface the UIs actually consume: 404-without-body for missing resources, the
 * 500-with-German-message contract of the write paths, and the Boot default error JSON where no
 * controller catch exists (LEGACY_NOTES B12: no input validation — the DB NOT-NULL constraint is
 * the only "validation" and its failure reaches the client).
 *
 * <p>Legacy and modern must answer identically on every path here. The modern stand needs a
 * wire-compat shim for the two error-JSON fields ("exception", "message") that Boot 2+ hides by
 * default — see spring.web.error.* in modern/src/main/resources/application.properties (same
 * philosophy as JacksonWireCompatConfig, ADR-0005).
 *
 * <p>Every test resets the DB to the committed seed first — no ordering dependency, safe for the
 * one test that mutates state (doppelteRechnung).
 */
@DisplayName("Fehlerkontrakt-Charakterisierung: 4xx/5xx-Oberfläche")
class ErrorContractCharacterizationTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  @BeforeEach
  void resetDatabase() {
    Stand.resetToSeed();
  }

  @ParameterizedTest(name = "GET {0} → 404 ohne Body")
  @ValueSource(
      strings = {
        "/api/kunden/999",
        "/api/auftraege/999",
        "/api/rechnungen/999",
        "/api/fahrzeuge/999"
      })
  void nichtVorhandeneRessource_liefert404OhneBody(String endpoint) {
    var response = Stand.get(endpoint);
    assertThat(response.statusCode()).as("HTTP status of " + endpoint).isEqualTo(404);
    assertThat(response.body()).as("404 carries no body").isEmpty();
  }

  @Test
  void unerlaubterStatuswechsel_liefert500MitDeutscherMeldung() {
    // Auftrag 1 ist ABGEHOLT — von dort ist kein Uebergang mehr erlaubt
    var response = Stand.send("PUT", "/api/auftraege/1/status?neu=IN_ARBEIT", null);
    assertThat(response.statusCode()).isEqualTo(500);
    assertThat(response.body()).isEqualTo("Statuswechsel ABGEHOLT -> IN_ARBEIT ist nicht erlaubt");
  }

  @Test
  void rechnungFuerNichtFertigenAuftrag_liefert500MitMeldung() {
    // Auftrag 11 ist IN_ARBEIT — Rechnung geht nur bei FERTIG
    var response = Stand.send("POST", "/api/rechnungen/auftrag/11", null);
    assertThat(response.statusCode()).isEqualTo(500);
    assertThat(response.body())
        .isEqualTo("Rechnung geht nur bei Status FERTIG, Auftrag A-2026-0011 ist IN_ARBEIT");
  }

  @Test
  void doppelteRechnung_liefert500MitMeldung() {
    // Auftrag 9 ist FERTIG ohne Rechnung — die erste geht durch ...
    var erste = Stand.send("POST", "/api/rechnungen/auftrag/9", null);
    assertThat(erste.statusCode()).isEqualTo(200);

    // ... die zweite nicht
    var zweite = Stand.send("POST", "/api/rechnungen/auftrag/9", null);
    assertThat(zweite.statusCode()).isEqualTo(500);
    assertThat(zweite.body()).isEqualTo("Zum Auftrag A-2026-0009 gibt es schon eine Rechnung");
  }

  @Test
  void kundeOhneNachname_liefert500AusDbConstraint() throws Exception {
    // B12: der Controller validiert nichts — der NOT-NULL-Constraint der DB
    // schlaegt als Boot-Standard-Fehler-JSON bis zum Client durch.
    var response = Stand.send("POST", "/api/kunden", "{\"vorname\":\"Test\"}");
    assertThat(response.statusCode()).isEqualTo(500);

    JsonNode body = MAPPER.readTree(response.body());
    assertThat(body.path("status").asInt()).isEqualTo(500);
    assertThat(body.path("error").asText()).isEqualTo("Internal Server Error");
    assertThat(body.path("exception").asText())
        .isEqualTo("org.springframework.dao.DataIntegrityViolationException");
    // message by containment, not equality: Spring 4.x appended
    // "; nested exception is ..." prose that Spring 6+ dropped — the
    // meaningful content (the PG constraint text) must be present on both
    // stands, the framework prose around it is not part of the contract.
    assertThat(body.path("message").asText()).contains("null value in column \"nachname\"");
    assertThat(body.path("path").asText()).isEqualTo("/api/kunden");
    assertThat(body.path("timestamp").isNumber()).as("timestamp bleibt epoch-millis").isTrue();
  }
}
