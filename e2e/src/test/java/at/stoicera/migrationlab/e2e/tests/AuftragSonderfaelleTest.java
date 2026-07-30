package at.stoicera.migrationlab.e2e.tests;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import at.stoicera.migrationlab.e2e.pages.AuftragDetailPage;
import at.stoicera.migrationlab.e2e.pages.AuftraegePage;
import at.stoicera.migrationlab.e2e.support.ScenarioTest;

/**
 * Scenario: the order flows besides the happy path — cancellation, the
 * FERTIG→IN_ARBEIT special case ("doch noch was gefunden"), and position
 * editing with the net sum recalculating. All on seed orders.
 */
@DisplayName("Auftrag-Sonderfälle: Storno, zurück in Arbeit, Positionen ändern")
class AuftragSonderfaelleTest extends ScenarioTest {

	@Test
	@Order(1)
	void stornoAusAngenommen() {
		AuftragDetailPage detail = new AuftraegePage(driver, waits).open().openAuftrag("A-2026-0014");
		assertThat(detail.status()).isEqualTo("Angenommen");
		detail.stornieren();
		assertThat(detail.status()).isEqualTo("Storniert");

		AuftraegePage liste = new AuftraegePage(driver, waits).open();
		assertThat(liste.statusOf("A-2026-0014")).isEqualTo("Storniert");
	}

	@Test
	@Order(2)
	void fertigZurueckInArbeit() {
		AuftragDetailPage detail = new AuftraegePage(driver, waits).open().openAuftrag("A-2026-0012");
		assertThat(detail.status()).isEqualTo("Fertig");
		detail.zurueckInArbeit();
		assertThat(detail.status()).isEqualTo("In Arbeit");
	}

	@Test
	@Order(3)
	void positionHinzufuegenUndEntfernen() {
		// A-2026-0011 (IN_ARBEIT): 3.5 × 88.00 + 289.00 = 597.00 from the seed
		AuftragDetailPage detail = new AuftraegePage(driver, waits).open().openAuftrag("A-2026-0011");
		assertThat(detail.status()).isEqualTo("In Arbeit");
		// header pin: position cells (Bezeichnung, delete) are read positionally
		assertThat(detail.posHeaderTexte())
				.containsExactly("Typ", "Bezeichnung", "Menge", "Einzelpreis", "Gesamt", "");
		assertThat(detail.summeNetto()).isEqualTo("597,00 €");

		detail.addPosition("MATERIAL", "E2E Kleinmaterial", "1", "50.00");
		assertThat(detail.summeNetto()).isEqualTo("647,00 €");

		detail.removePosition("E2E Kleinmaterial");
		assertThat(detail.summeNetto()).isEqualTo("597,00 €");
	}
}
