package at.stoicera.migrationlab.e2e.tests;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import at.stoicera.migrationlab.e2e.pages.AuftragDetailPage;
import at.stoicera.migrationlab.e2e.pages.AuftraegePage;
import at.stoicera.migrationlab.e2e.support.ScenarioTest;

/**
 * Scenario: full repair-order lifecycle on a seed customer/vehicle —
 * ANGENOMMEN → IN_ARBEIT (+ position) → FERTIG → ABGEHOLT.
 */
@DisplayName("Auftrag-Lebenszyklus: Annahme bis Abholung")
class AuftragLebenszyklusTest extends ScenarioTest {

	private static String auftragNr;

	@Test
	@Order(1)
	void auftragAnnehmen() {
		AuftragDetailPage detail = new AuftraegePage(driver, waits).open()
				.neuerAuftrag()
				.kunde("HOFER, Franz")
				.fahrzeug("PE-123AB")
				.beschreibung("E2E: Bremsen quietschen vorne rechts")
				.km(248100)
				.anlegen();
		auftragNr = detail.auftragNr();
		assertThat(auftragNr).matches("A-\\d{4}-\\d{4}");
		assertThat(detail.status()).isEqualTo("Angenommen");
	}

	@Test
	@Order(2)
	void inArbeitNehmenUndPositionErfassen() {
		AuftragDetailPage detail = new AuftraegePage(driver, waits).open().openAuftrag(auftragNr);
		detail.inArbeit()
				.addPosition("ARBEIT", "E2E Fehlersuche Bremse", "1.5", "78.00");
		assertThat(detail.summeNetto()).isEqualTo("117,00 €");
	}

	@Test
	@Order(3)
	void fertigMeldenUndAbholen() {
		AuftragDetailPage detail = new AuftraegePage(driver, waits).open().openAuftrag(auftragNr);
		detail.fertig().abgeholt();
		assertThat(detail.status()).isEqualTo("Abgeholt");
	}

	@Test
	@Order(4)
	void statusInDerListeKorrekt() {
		AuftraegePage liste = new AuftraegePage(driver, waits).open();
		assertThat(liste.statusOf(auftragNr)).isEqualTo("Abgeholt");
	}
}
