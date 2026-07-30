package at.stoicera.migrationlab.e2e.tests;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import at.stoicera.migrationlab.e2e.pages.AuftragNeuPage;
import at.stoicera.migrationlab.e2e.pages.AuftraegePage;
import at.stoicera.migrationlab.e2e.pages.KundeDetailPage;
import at.stoicera.migrationlab.e2e.pages.KundenPage;
import at.stoicera.migrationlab.e2e.support.ScenarioTest;
import at.stoicera.migrationlab.e2e.support.Seed;

/**
 * Scenario: what the legacy UI actually does when required input is missing —
 * a bare JS alert from the controller, no request sent, form state kept.
 * The alert TEXTS are pinned exactly: they are part of the observed behavior
 * the stage-5 UI must either reproduce or change via ADR-0004.
 */
@DisplayName("Validierung: Pflichtfelder melden sich per Alert, nichts wird gespeichert")
class ValidierungTest extends ScenarioTest {

	@Test
	@Order(1)
	void kundeOhneNachnameWirdAbgewiesen() {
		KundeDetailPage detail = new KundenPage(driver, waits).open().neuerKunde();
		detail.vorname("Theresa");
		String meldung = detail.speichernErwarteAlert();
		assertThat(meldung).isEqualTo("Nachname ist Pflicht!");
		// still on the form, nothing left the browser
		waits.urlContains("/kunden/neu");

		// and nothing was created: the seed has no "Theresa" anywhere
		KundenPage liste = new KundenPage(driver, waits).open().search("Theresa");
		liste.waitForRowCount(0);
		assertThat(liste.namesInList()).isEmpty();
	}

	@Test
	@Order(2)
	void auftragOhneKundeUndFahrzeugWirdAbgewiesen() {
		AuftragNeuPage neu = new AuftraegePage(driver, waits).open().neuerAuftrag();
		String meldung = neu.anlegenErwarteAlert();
		assertThat(meldung).isEqualTo("Bitte Kunde und Fahrzeug auswählen!");
		waits.urlContains("/auftraege/neu");

		// order list unchanged — exactly the seed orders
		AuftraegePage liste = new AuftraegePage(driver, waits).open();
		assertThat(liste.rowCount()).isEqualTo(Seed.ANZAHL_AUFTRAEGE);
	}
}
