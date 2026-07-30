package at.stoicera.migrationlab.e2e.tests;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import at.stoicera.migrationlab.e2e.pages.AuftragDetailPage;
import at.stoicera.migrationlab.e2e.pages.AuftraegePage;
import at.stoicera.migrationlab.e2e.pages.RechnungDetailPage;
import at.stoicera.migrationlab.e2e.pages.RechnungenPage;
import at.stoicera.migrationlab.e2e.selectors.SelectorMap;
import at.stoicera.migrationlab.e2e.support.ScenarioTest;
import at.stoicera.migrationlab.e2e.support.Seed;

/**
 * Scenario: invoice creation from the seed order A-2026-0009 (FERTIG,
 * Klimaservice, 123.00 netto), detail navigation from the list, and the
 * server-side rejection of a second invoice for the same order. The number of
 * the created invoice depends on the wall-clock year — computed in
 * {@link Seed#naechsteRechnungNr()}, never hard-coded.
 */
@DisplayName("Rechnung: erstellen aus fertigem Auftrag, 20% USt, bezahlt setzen, Detail, Doppel-Abweisung")
class RechnungTest extends ScenarioTest {

	private static final String RECHNUNG_NR = Seed.naechsteRechnungNr();

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

	@Test
	@Order(3)
	void rechnungDetailAusDerListe() {
		// frozen seed invoice R-2026-0004 (order A-2026-0005, Zahnriemen): the euro
		// strings pin the UI formatting contract — see README, formatting contract
		RechnungDetailPage detail = new RechnungenPage(driver, waits).open().openRechnung("R-2026-0004");
		assertThat(detail.nummerText()).isEqualTo("Rechnung R-2026-0004");

		assertThat(detail.posHeaderTexte())
				.containsExactly("Position", "Menge", "Einzelpreis", "Betrag");
		List<List<String>> positionen = detail.positionZeilen();
		assertThat(positionen).hasSize(3);
		assertThat(positionen.get(0)).containsExactly("Zahnriemen + WaPu tauschen", "4.5", "88,00 €", "396,00 €");
		assertThat(positionen.get(1)).containsExactly("Zahnriemensatz", "1", "185,00 €", "185,00 €");
		assertThat(positionen.get(2)).containsExactly("Wasserpumpe", "1", "98,00 €", "98,00 €");

		assertThat(detail.netto()).isEqualTo("679,00 €");
		assertThat(detail.ust()).isEqualTo("135,80 €");
		assertThat(detail.brutto()).isEqualTo("814,80 €");
	}

	@Test
	@Order(4)
	void zweiteRechnungZumSelbenAuftragWirdAbgelehnt() {
		// A-2026-0009 stays FERTIG after invoicing (test 1), so the button is still
		// there — the backend rejects the duplicate with 500 and the message
		// "Zum Auftrag ... gibt es schon eine Rechnung". What the USER sees is
		// per-stand (sanctioned divergence SD-3, ADR-0004), so the expected alert
		// text lives in the selector map: the LEGACY UI shows literally
		// "undefined" (Boot 1.5 labels the plain-String 500 body as JSON,
		// AngularJS 1.8's response transform throws [$http:baddata], the error
		// callback alerts fehler.data of an Error — pinned as-is, honesty rule);
		// the stage-5 Angular UI surfaces the backend's German message. The
		// HTTP-level message contract itself is pinned by the error-contract
		// characterization tests, identically on both stands.
		AuftragDetailPage auftrag = new AuftraegePage(driver, waits).open().openAuftrag("A-2026-0009");
		assertThat(auftrag.status()).isEqualTo("Fertig");

		String meldung = auftrag.rechnungErstellenErwarteFehler();
		assertThat(meldung).isEqualTo(SelectorMap.value("alert.rechnungDuplikat"));
		// the rejection left no trace: the order is still FERTIG, no second invoice
		assertThat(auftrag.status()).isEqualTo("Fertig");
	}
}
