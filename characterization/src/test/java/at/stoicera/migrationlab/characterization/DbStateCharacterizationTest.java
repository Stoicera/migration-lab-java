package at.stoicera.migrationlab.characterization;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Characterizes the DB state transitions of the write paths — the complete status transition
 * matrix, number generation, invoice amounts (including a double-rounding boundary case) and the
 * quirks (vehicle km side effect, orphaned rows on delete, kept fertig_am on the FERTIG→IN_ARBEIT
 * special case). These document CURRENT behaviour; the migration must reproduce it until a stage
 * explicitly changes it (with ADR + playbook entry).
 *
 * <p>Every test resets the DB to the committed seed first and works only against seed rows — no
 * ordering dependency between tests. Seed anchors used below: Auftrag 1 ABGEHOLT, 9/12 FERTIG,
 * 11/13 IN_ARBEIT, 14/15 ANGENOMMEN, 4/16 STORNIERT; Rechnung 5 unbezahlt.
 */
@DisplayName("DB-Zustands-Charakterisierung: Schreibpfade")
class DbStateCharacterizationTest {

  @BeforeEach
  void resetDatabase() {
    Stand.resetToSeed();
  }

  @Test
  void auftragAnlegen_schreibtAuftragUndAktualisiertFahrzeugKm() throws Exception {
    var response =
        Stand.send(
            "POST",
            "/api/auftraege",
            "{\"fahrzeugId\":1,\"kundeId\":1,\"beschreibung\":\"Charakterisierung\",\"kmStand\":248200}");
    assertThat(response.statusCode()).isEqualTo(200);

    try (Connection c = Stand.connect();
        Statement s = c.createStatement()) {
      ResultSet auftrag =
          s.executeQuery(
              "SELECT auftrag_nr, status, angenommen_am, fertig_am FROM auftrag WHERE id = 17");
      assertThat(auftrag.next()).isTrue();
      // number is derived from the CURRENT year (MAX+1 per year) — see Seed
      assertThat(auftrag.getString("auftrag_nr")).isEqualTo(Seed.naechsteAuftragNr());
      assertThat(auftrag.getString("status")).isEqualTo("ANGENOMMEN");
      assertThat(auftrag.getTimestamp("angenommen_am")).isNotNull();
      assertThat(auftrag.getTimestamp("fertig_am")).isNull();

      // side effect: order acceptance silently updates the vehicle's km
      ResultSet fahrzeug = s.executeQuery("SELECT km_stand FROM fahrzeug WHERE id = 1");
      assertThat(fahrzeug.next()).isTrue();
      assertThat(fahrzeug.getInt("km_stand")).isEqualTo(248200);
    }
  }

  // -----------------------------------------------------------------
  // Status transition matrix — every LEGAL transition, one test each.
  // Illegal transitions are pinned in ErrorContractCharacterizationTest.
  // -----------------------------------------------------------------

  @Test
  void angenommenNachInArbeit_setztNurStatus() throws Exception {
    // Auftrag 15 ist ANGENOMMEN
    var response = Stand.send("PUT", "/api/auftraege/15/status?neu=IN_ARBEIT", null);
    assertThat(response.statusCode()).isEqualTo(200);

    try (Connection c = Stand.connect();
        Statement s = c.createStatement()) {
      ResultSet rs =
          s.executeQuery("SELECT status, fertig_am, abgeholt_am FROM auftrag WHERE id = 15");
      assertThat(rs.next()).isTrue();
      assertThat(rs.getString("status")).isEqualTo("IN_ARBEIT");
      assertThat(rs.getTimestamp("fertig_am")).isNull();
      assertThat(rs.getTimestamp("abgeholt_am")).isNull();
    }
  }

  @Test
  void angenommenNachStorniert_setztNurStatus() throws Exception {
    // Auftrag 14 ist ANGENOMMEN
    var response = Stand.send("PUT", "/api/auftraege/14/status?neu=STORNIERT", null);
    assertThat(response.statusCode()).isEqualTo(200);

    try (Connection c = Stand.connect();
        Statement s = c.createStatement()) {
      ResultSet rs =
          s.executeQuery("SELECT status, fertig_am, abgeholt_am FROM auftrag WHERE id = 14");
      assertThat(rs.next()).isTrue();
      assertThat(rs.getString("status")).isEqualTo("STORNIERT");
      assertThat(rs.getTimestamp("fertig_am")).isNull();
      assertThat(rs.getTimestamp("abgeholt_am")).isNull();
    }
  }

  @Test
  void inArbeitNachFertig_setztZeitstempel() throws Exception {
    // Auftrag 11 ist IN_ARBEIT
    var response = Stand.send("PUT", "/api/auftraege/11/status?neu=FERTIG", null);
    assertThat(response.statusCode()).isEqualTo(200);

    try (Connection c = Stand.connect();
        Statement s = c.createStatement()) {
      ResultSet rs =
          s.executeQuery("SELECT status, fertig_am, abgeholt_am FROM auftrag WHERE id = 11");
      assertThat(rs.next()).isTrue();
      assertThat(rs.getString("status")).isEqualTo("FERTIG");
      assertThat(rs.getTimestamp("fertig_am")).isNotNull();
      assertThat(rs.getTimestamp("abgeholt_am")).isNull();
    }
  }

  @Test
  void inArbeitNachStorniert_setztNurStatus() throws Exception {
    // Auftrag 13 ist IN_ARBEIT
    var response = Stand.send("PUT", "/api/auftraege/13/status?neu=STORNIERT", null);
    assertThat(response.statusCode()).isEqualTo(200);

    try (Connection c = Stand.connect();
        Statement s = c.createStatement()) {
      ResultSet rs =
          s.executeQuery("SELECT status, fertig_am, abgeholt_am FROM auftrag WHERE id = 13");
      assertThat(rs.next()).isTrue();
      assertThat(rs.getString("status")).isEqualTo("STORNIERT");
      assertThat(rs.getTimestamp("fertig_am")).isNull();
      assertThat(rs.getTimestamp("abgeholt_am")).isNull();
    }
  }

  @Test
  void fertigZurueckInArbeit_sonderfallBehaeltFertigAm() throws Exception {
    // Auftrag 9 ist FERTIG (fertig_am aus dem Seed) — Sonderfall "doch
    // noch was gefunden": zurueck in Arbeit ist erlaubt, aber der alte
    // fertig_am-Stempel bleibt stehen (quirk: das UPDATE setzt nur status)
    var response = Stand.send("PUT", "/api/auftraege/9/status?neu=IN_ARBEIT", null);
    assertThat(response.statusCode()).isEqualTo(200);

    try (Connection c = Stand.connect();
        Statement s = c.createStatement()) {
      ResultSet rs =
          s.executeQuery("SELECT status, fertig_am, abgeholt_am FROM auftrag WHERE id = 9");
      assertThat(rs.next()).isTrue();
      assertThat(rs.getString("status")).isEqualTo("IN_ARBEIT");
      assertThat(rs.getTimestamp("fertig_am")).isEqualTo(Timestamp.valueOf("2026-05-20 09:30:00"));
      assertThat(rs.getTimestamp("abgeholt_am")).isNull();
    }
  }

  @Test
  void fertigNachAbgeholt_setztAbgeholtAmUndBehaeltFertigAm() throws Exception {
    // Auftrag 12 ist FERTIG (fertig_am aus dem Seed)
    var response = Stand.send("PUT", "/api/auftraege/12/status?neu=ABGEHOLT", null);
    assertThat(response.statusCode()).isEqualTo(200);

    try (Connection c = Stand.connect();
        Statement s = c.createStatement()) {
      ResultSet rs =
          s.executeQuery("SELECT status, fertig_am, abgeholt_am FROM auftrag WHERE id = 12");
      assertThat(rs.next()).isTrue();
      assertThat(rs.getString("status")).isEqualTo("ABGEHOLT");
      assertThat(rs.getTimestamp("fertig_am")).isEqualTo(Timestamp.valueOf("2026-07-08 13:10:00"));
      assertThat(rs.getTimestamp("abgeholt_am")).isNotNull();
    }
  }

  // -----------------------------------------------------------------
  // Rechnungen
  // -----------------------------------------------------------------

  @Test
  void rechnungErstellen_schreibtBetraegeUndNummer() throws Exception {
    // Auftrag 9 ist FERTIG mit Positionen 1.00x78.00 + 1.00x45.00
    var response = Stand.send("POST", "/api/rechnungen/auftrag/9", null);
    assertThat(response.statusCode()).isEqualTo(200);

    try (Connection c = Stand.connect();
        Statement s = c.createStatement()) {
      ResultSet rs =
          s.executeQuery(
              "SELECT rechnung_nr, summe_netto, ust, summe_brutto, bezahlt "
                  + "FROM rechnung WHERE auftrag_id = 9");
      assertThat(rs.next()).isTrue();
      // number is derived from the CURRENT year (MAX+1 per year) — see Seed
      assertThat(rs.getString("rechnung_nr")).isEqualTo(Seed.naechsteRechnungNr());
      assertThat(rs.getBigDecimal("summe_netto")).isEqualByComparingTo("123.00");
      assertThat(rs.getBigDecimal("ust")).isEqualByComparingTo("24.60");
      assertThat(rs.getBigDecimal("summe_brutto")).isEqualByComparingTo("147.60");
      assertThat(rs.getBoolean("bezahlt")).isFalse();
    }
  }

  @Test
  void rechnungRundung_doppeltesRundenAmGrenzfall() throws Exception {
    // B8 double money math, pinned at a decimal half-way case: 0.50 x
    // 16.99 = 8.4950 exactly. The app's chain (netto as double, then
    // Math.round(netto*100)/100) sees 0.5*16.99 = 8.494999999999999 and
    // rounds DOWN to 8.49 — naive BigDecimal HALF_UP arithmetic would
    // give 8.50/10.20. ust = round(8.49*20)/100 = 1.70, brutto = 10.19.
    // A migration that swaps in "clean" decimal math changes invoices by
    // one cent — exactly what this pin exists to catch on both stands.
    var auftrag =
        Stand.send(
            "POST",
            "/api/auftraege",
            "{\"fahrzeugId\":1,\"kundeId\":1,\"beschreibung\":\"Rundungsgrenzfall\",\"kmStand\":248000}");
    assertThat(auftrag.statusCode()).isEqualTo(200);

    Stand.send(
        "POST",
        "/api/auftraege/17/positionen",
        "{\"typ\":\"ARBEIT\",\"bezeichnung\":\"Kleinarbeit 0,5h\",\"menge\":0.50,\"einzelpreis\":16.99}");
    Stand.send("PUT", "/api/auftraege/17/status?neu=IN_ARBEIT", null);
    Stand.send("PUT", "/api/auftraege/17/status?neu=FERTIG", null);
    var response = Stand.send("POST", "/api/rechnungen/auftrag/17", null);
    assertThat(response.statusCode()).isEqualTo(200);

    try (Connection c = Stand.connect();
        Statement s = c.createStatement()) {
      ResultSet rs =
          s.executeQuery(
              "SELECT summe_netto, ust, summe_brutto FROM rechnung WHERE auftrag_id = 17");
      assertThat(rs.next()).isTrue();
      assertThat(rs.getBigDecimal("summe_netto")).isEqualByComparingTo("8.49");
      assertThat(rs.getBigDecimal("ust")).isEqualByComparingTo("1.70");
      assertThat(rs.getBigDecimal("summe_brutto")).isEqualByComparingTo("10.19");
    }
  }

  @Test
  void rechnungBezahlt_setztFlagUndZeitstempel() throws Exception {
    // Rechnung 5 (R-2026-0005) ist die einzige unbezahlte im Seed
    var response = Stand.send("PUT", "/api/rechnungen/5/bezahlt", null);
    assertThat(response.statusCode()).isEqualTo(200);

    try (Connection c = Stand.connect();
        Statement s = c.createStatement()) {
      ResultSet rs = s.executeQuery("SELECT bezahlt, bezahlt_am FROM rechnung WHERE id = 5");
      assertThat(rs.next()).isTrue();
      assertThat(rs.getBoolean("bezahlt")).isTrue();
      assertThat(rs.getTimestamp("bezahlt_am")).isNotNull();
    }
  }

  // -----------------------------------------------------------------
  // Destruktive Pfade
  // -----------------------------------------------------------------

  @Test
  void kundeLoeschen_hinterlaesstVerwaisteFahrzeuge() throws Exception {
    // quirk B13 (LEGACY_NOTES): no FK, no cascade — orphans are current behaviour
    var response = Stand.send("DELETE", "/api/kunden/2", null);
    assertThat(response.statusCode()).isEqualTo(200);

    try (Connection c = Stand.connect();
        Statement s = c.createStatement()) {
      ResultSet kunde = s.executeQuery("SELECT COUNT(*) AS n FROM kunde WHERE id = 2");
      kunde.next();
      assertThat(kunde.getInt("n")).isZero();

      ResultSet fahrzeug = s.executeQuery("SELECT kunde_id FROM fahrzeug WHERE id = 3");
      assertThat(fahrzeug.next()).isTrue();
      assertThat(fahrzeug.getLong("kunde_id")).isEqualTo(2L);
    }
  }

  @Test
  void adminBereinigen_loeschtGenauStornierteAelterAls90Tage() throws Exception {
    // The predicate ("STORNIERT and older than 90 days") moves with the
    // wall clock over fixed seed dates — so the EXPECTED set is computed
    // at runtime with the app's own predicate, and the pin holds on any
    // date: today that is Auftrag 4, from mid-October 2026 also 16, in
    // any later year both.
    List<Long> erwartetGeloescht = new ArrayList<>();
    int auftraegeVorher;
    int positionenVorher;
    int positionenDerGeloeschten = 0;
    try (Connection c = Stand.connect();
        Statement s = c.createStatement()) {
      ResultSet rs =
          s.executeQuery(
              "SELECT id FROM auftrag WHERE status = 'STORNIERT' "
                  + "AND angenommen_am < now() - interval '90 days' ORDER BY id");
      while (rs.next()) {
        erwartetGeloescht.add(rs.getLong("id"));
      }
      ResultSet a = s.executeQuery("SELECT COUNT(*) AS n FROM auftrag");
      a.next();
      auftraegeVorher = a.getInt("n");
      ResultSet p = s.executeQuery("SELECT COUNT(*) AS n FROM auftrag_position");
      p.next();
      positionenVorher = p.getInt("n");
      for (Long id : erwartetGeloescht) {
        ResultSet pn =
            s.executeQuery("SELECT COUNT(*) AS n FROM auftrag_position WHERE auftrag_id = " + id);
        pn.next();
        positionenDerGeloeschten += pn.getInt("n");
      }
    }

    var response = Stand.send("POST", "/admin/bereinigen", null);
    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(response.body())
        .contains(erwartetGeloescht.size() + " stornierte Aufträge wurden endgültig gelöscht.");
    if (Stand.isModern()) {
      // SD-2: the modern endpoint answers JSON the SPA depends on — pin the
      // exact shape, not just the substring (review session 10: renaming the
      // "meldung" key would have kept the contains() green while silently
      // breaking the admin page's result display).
      assertThat(response.headers().firstValue("Content-Type").orElse(""))
          .contains("application/json");
      var json = new com.fasterxml.jackson.databind.ObjectMapper().readTree(response.body());
      List<String> felder = new ArrayList<>();
      json.fieldNames().forEachRemaining(felder::add);
      assertThat(felder).containsExactlyInAnyOrder("geloescht", "meldung");
      assertThat(json.get("geloescht").intValue()).isEqualTo(erwartetGeloescht.size());
      assertThat(json.get("meldung").asText())
          .isEqualTo(erwartetGeloescht.size() + " stornierte Aufträge wurden endgültig gelöscht.");
    }

    try (Connection c = Stand.connect();
        Statement s = c.createStatement()) {
      for (Long id : erwartetGeloescht) {
        ResultSet auftrag = s.executeQuery("SELECT COUNT(*) AS n FROM auftrag WHERE id = " + id);
        auftrag.next();
        assertThat(auftrag.getInt("n")).as("Auftrag " + id + " geloescht").isZero();
        ResultSet positionen =
            s.executeQuery("SELECT COUNT(*) AS n FROM auftrag_position WHERE auftrag_id = " + id);
        positionen.next();
        assertThat(positionen.getInt("n"))
            .as("Positionen zu Auftrag " + id + " geloescht")
            .isZero();
      }
      ResultSet auftraege = s.executeQuery("SELECT COUNT(*) AS n FROM auftrag");
      auftraege.next();
      assertThat(auftraege.getInt("n"))
          .as("alle anderen Auftraege ueberleben")
          .isEqualTo(auftraegeVorher - erwartetGeloescht.size());
      ResultSet positionen = s.executeQuery("SELECT COUNT(*) AS n FROM auftrag_position");
      positionen.next();
      assertThat(positionen.getInt("n"))
          .as("alle anderen Positionen ueberleben")
          .isEqualTo(positionenVorher - positionenDerGeloeschten);
    }

    // destructive endpoint — leave the stand in seed state for whoever runs next
    Stand.resetToSeed();
  }
}
