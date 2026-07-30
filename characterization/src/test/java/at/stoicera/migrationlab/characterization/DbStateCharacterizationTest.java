package at.stoicera.migrationlab.characterization;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * Characterizes the DB state transitions of the write paths — including the
 * quirks (vehicle km side effect, orphaned rows on delete). These document
 * CURRENT behaviour; the migration must reproduce it until a stage explicitly
 * changes it (with ADR + playbook entry).
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("DB-Zustands-Charakterisierung: Schreibpfade")
class DbStateCharacterizationTest {

	@BeforeAll
	static void resetDatabase() {
		Stand.resetToSeed();
	}

	@Test
	@Order(1)
	void auftragAnlegen_schreibtAuftragUndAktualisiertFahrzeugKm() throws Exception {
		var response = Stand.send("POST", "/api/auftraege",
				"{\"fahrzeugId\":1,\"kundeId\":1,\"beschreibung\":\"Charakterisierung\",\"kmStand\":248200}");
		assertThat(response.statusCode()).isEqualTo(200);

		try (Connection c = Stand.connect(); Statement s = c.createStatement()) {
			ResultSet auftrag = s.executeQuery(
					"SELECT auftrag_nr, status, angenommen_am, fertig_am FROM auftrag WHERE id = 17");
			assertThat(auftrag.next()).isTrue();
			assertThat(auftrag.getString("auftrag_nr")).isEqualTo("A-2026-0017");
			assertThat(auftrag.getString("status")).isEqualTo("ANGENOMMEN");
			assertThat(auftrag.getTimestamp("angenommen_am")).isNotNull();
			assertThat(auftrag.getTimestamp("fertig_am")).isNull();

			// side effect: order acceptance silently updates the vehicle's km
			ResultSet fahrzeug = s.executeQuery("SELECT km_stand FROM fahrzeug WHERE id = 1");
			assertThat(fahrzeug.next()).isTrue();
			assertThat(fahrzeug.getInt("km_stand")).isEqualTo(248200);
		}
	}

	@Test
	@Order(2)
	void statusFertig_setztZeitstempel() throws Exception {
		Stand.send("PUT", "/api/auftraege/17/status?neu=IN_ARBEIT", null);
		var response = Stand.send("PUT", "/api/auftraege/17/status?neu=FERTIG", null);
		assertThat(response.statusCode()).isEqualTo(200);

		try (Connection c = Stand.connect(); Statement s = c.createStatement()) {
			ResultSet rs = s.executeQuery("SELECT status, fertig_am, abgeholt_am FROM auftrag WHERE id = 17");
			assertThat(rs.next()).isTrue();
			assertThat(rs.getString("status")).isEqualTo("FERTIG");
			assertThat(rs.getTimestamp("fertig_am")).isNotNull();
			assertThat(rs.getTimestamp("abgeholt_am")).isNull();
		}
	}

	@Test
	@Order(3)
	void rechnungErstellen_schreibtBetraegeUndNummer() throws Exception {
		Stand.send("POST", "/api/auftraege/17/positionen",
				"{\"typ\":\"ARBEIT\",\"bezeichnung\":\"Charakterisierung\",\"menge\":2,\"einzelpreis\":88.00}");
		var response = Stand.send("POST", "/api/rechnungen/auftrag/17", null);
		assertThat(response.statusCode()).isEqualTo(200);

		try (Connection c = Stand.connect(); Statement s = c.createStatement()) {
			ResultSet rs = s.executeQuery("SELECT rechnung_nr, summe_netto, ust, summe_brutto, bezahlt "
					+ "FROM rechnung WHERE auftrag_id = 17");
			assertThat(rs.next()).isTrue();
			assertThat(rs.getString("rechnung_nr")).isEqualTo("R-2026-0009");
			assertThat(rs.getBigDecimal("summe_netto")).isEqualByComparingTo("176.00");
			assertThat(rs.getBigDecimal("ust")).isEqualByComparingTo("35.20");
			assertThat(rs.getBigDecimal("summe_brutto")).isEqualByComparingTo("211.20");
			assertThat(rs.getBoolean("bezahlt")).isFalse();
		}
	}

	@Test
	@Order(4)
	void rechnungBezahlt_setztFlagUndZeitstempel() throws Exception {
		try (Connection c = Stand.connect(); Statement s = c.createStatement()) {
			ResultSet id = s.executeQuery("SELECT id FROM rechnung WHERE auftrag_id = 17");
			assertThat(id.next()).isTrue();
			long rechnungId = id.getLong("id");

			var response = Stand.send("PUT", "/api/rechnungen/" + rechnungId + "/bezahlt", null);
			assertThat(response.statusCode()).isEqualTo(200);

			ResultSet rs = s.executeQuery("SELECT bezahlt, bezahlt_am FROM rechnung WHERE id = " + rechnungId);
			assertThat(rs.next()).isTrue();
			assertThat(rs.getBoolean("bezahlt")).isTrue();
			assertThat(rs.getTimestamp("bezahlt_am")).isNotNull();
		}
	}

	@Test
	@Order(5)
	void kundeLoeschen_hinterlaesstVerwaisteFahrzeuge() throws Exception {
		// quirk B13 (LEGACY_NOTES): no FK, no cascade — orphans are current behaviour
		var response = Stand.send("DELETE", "/api/kunden/2", null);
		assertThat(response.statusCode()).isEqualTo(200);

		try (Connection c = Stand.connect(); Statement s = c.createStatement()) {
			ResultSet kunde = s.executeQuery("SELECT COUNT(*) AS n FROM kunde WHERE id = 2");
			kunde.next();
			assertThat(kunde.getInt("n")).isZero();

			ResultSet fahrzeug = s.executeQuery("SELECT kunde_id FROM fahrzeug WHERE id = 3");
			assertThat(fahrzeug.next()).isTrue();
			assertThat(fahrzeug.getLong("kunde_id")).isEqualTo(2L);
		}
	}
}
