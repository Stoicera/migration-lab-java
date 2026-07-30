package at.stoicera.migrationlab.e2e.tests;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import at.stoicera.migrationlab.e2e.pages.AuftragDetailPage;
import at.stoicera.migrationlab.e2e.pages.AuftraegePage;
import at.stoicera.migrationlab.e2e.pages.RechnungDetailPage;
import at.stoicera.migrationlab.e2e.pages.RechnungenPage;
import at.stoicera.migrationlab.e2e.support.ScenarioTest;

/**
 * Scenario: invoice creation from the seed order A-2026-0009 (FERTIG,
 * Klimaservice, 123.00 netto). After the DB reset the next invoice number is
 * deterministically R-2026-0009 — asserted exactly, characterization-style.
 */
@DisplayName("Rechnung: erstellen aus fertigem Auftrag, 20% USt, bezahlt setzen")
class RechnungTest extends ScenarioTest {

	private static final String RECHNUNG_NR = "R-2026-0009";

	@Test
	@Order(1)
	void rechnungAusFertigemAuftragErstellen() {
		AuftragDetailPage auftrag = new AuftraegePage(driver, waits).open().openAuftrag("A-2026-0009");
		assertThat(auftrag.status()).isEqualTo("Fertig");
		assertThat(auftrag.summeNetto()).isEqualTo("123,00 €");

		RechnungDetailPage rechnung = auftrag.rechnungErstellen();
		assertThat(rechnung.nummerText()).contains(RECHNUNG_NR);
		assertThat(rechnung.netto()).isEqualTo("123,00 €");
		assertThat(rechnung.ust()).isEqualTo("24,60 €");
		assertThat(rechnung.brutto()).isEqualTo("147,60 €");
	}

	@Test
	@Order(2)
	void rechnungBezahltSetzen() {
		RechnungenPage liste = new RechnungenPage(driver, waits).open();
		assertThat(liste.statusOf(RECHNUNG_NR)).isEqualTo("offen");
		liste.markPaid(RECHNUNG_NR);
		assertThat(liste.statusOf(RECHNUNG_NR)).isEqualTo("bezahlt");
	}
}
