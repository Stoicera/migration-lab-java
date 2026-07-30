package at.stoicera.migrationlab.e2e.tests;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import at.stoicera.migrationlab.e2e.pages.KundeDetailPage;
import at.stoicera.migrationlab.e2e.pages.KundenPage;
import at.stoicera.migrationlab.e2e.support.ScenarioTest;

/**
 * Scenario: customer master data CRUD. Uses a name that never occurs in the
 * seed, so list assertions are unambiguous.
 */
@DisplayName("Kunden-Stammdaten: anlegen, ändern, suchen, löschen")
class KundenCrudTest extends ScenarioTest {

	private static final String NACHNAME = "E2E-Zederbauer";
	private static final String ANZEIGE_NAME = "E2E-ZEDERBAUER, Theresa";

	@Test
	@Order(1)
	void kundeAnlegen() {
		KundeDetailPage detail = new KundenPage(driver, waits).open().neuerKunde();
		detail.anrede("Frau")
				.vorname("Theresa")
				.nachname(NACHNAME)
				.telefon("+43 664 5550001")
				.ort("Perg")
				.speichern(ANZEIGE_NAME);
		assertThat(detail.heading()).contains(ANZEIGE_NAME);
	}

	@Test
	@Order(2)
	void kundeAendernUndPersistenzPruefen() {
		KundenPage liste = new KundenPage(driver, waits).open().search(NACHNAME);
		liste.waitForRowCount(1);
		KundeDetailPage detail = liste.openByName(ANZEIGE_NAME);
		detail.telefon("+43 664 5559999").speichern(ANZEIGE_NAME);

		// re-open from the list — asserts the change survived the round trip
		liste = new KundenPage(driver, waits).open().search(NACHNAME);
		liste.waitForRowCount(1);
		detail = liste.openByName(ANZEIGE_NAME);
		assertThat(detail.telefonValue()).isEqualTo("+43 664 5559999");
	}

	@Test
	@Order(3)
	void kundeSuchen() {
		KundenPage liste = new KundenPage(driver, waits).open().search("zederbauer");
		liste.waitForRowCount(1);
		assertThat(liste.namesInList()).containsExactly(ANZEIGE_NAME);
	}

	@Test
	@Order(4)
	void kundeLoeschen() {
		KundenPage liste = new KundenPage(driver, waits).open().search(NACHNAME);
		liste.waitForRowCount(1);
		liste.deleteByName(ANZEIGE_NAME);
		liste.waitForRowCount(0);

		liste = new KundenPage(driver, waits).open().search(NACHNAME);
		liste.waitForRowCount(0);
		assertThat(liste.namesInList()).isEmpty();
	}
}
