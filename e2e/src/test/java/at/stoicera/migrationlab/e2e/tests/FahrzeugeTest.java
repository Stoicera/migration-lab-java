package at.stoicera.migrationlab.e2e.tests;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import at.stoicera.migrationlab.e2e.pages.FahrzeugePage;
import at.stoicera.migrationlab.e2e.pages.KundeDetailPage;
import at.stoicera.migrationlab.e2e.pages.KundenPage;
import at.stoicera.migrationlab.e2e.support.ScenarioTest;

/**
 * Scenario: vehicle module. The global list is read-only in the legacy UI;
 * create/delete happen on the owning customer's detail page — the only real
 * flow (the standalone list even says so). Uses a Kennzeichen that never
 * occurs in the seed, so list assertions are unambiguous.
 */
@DisplayName("Fahrzeuge: Liste aus dem Seed, anlegen und löschen beim Kunden")
class FahrzeugeTest extends ScenarioTest {

	private static final String KENNZEICHEN = "PE-999XY";

	@Test
	@Order(1)
	void listeZeigtSeedFahrzeuge() {
		FahrzeugePage liste = new FahrzeugePage(driver, waits).open();
		// header pin: row cells are read positionally — column reorder fails here
		assertThat(liste.headerTexte()).containsExactly(
				"Kennzeichen", "Marke", "Modell", "Baujahr", "km-Stand", "Pickerl bis", "Besitzer");
		liste.waitForRowCount(13);
		assertThat(liste.zeile("PE-123AB")).containsExactly(
				"PE-123AB", "VW", "Golf V 1.9 TDI", "2006", "248000", "30.11.2026", "Hofer Franz");
	}

	@Test
	@Order(2)
	void fahrzeugBeimKundenAnlegen() {
		KundeDetailPage kunde = new KundenPage(driver, waits).open().openByName("HOFER, Franz");
		assertThat(kunde.fahrzeugHeaderTexte())
				.containsExactly("Kennzeichen", "Fahrzeug", "Baujahr", "Pickerl bis", "");
		assertThat(kunde.fahrzeugAnzahl()).isEqualTo(2);

		kunde.fahrzeugHinzufuegen(KENNZEICHEN, "Dacia", "Duster", "2019", "55000");
		assertThat(kunde.fahrzeugAnzahl()).isEqualTo(3);
		assertThat(kunde.fahrzeugZeilen())
				.anySatisfy(zeile -> assertThat(zeile.get(0)).isEqualTo(KENNZEICHEN));

		// list delta: the new vehicle shows up in the global list
		FahrzeugePage liste = new FahrzeugePage(driver, waits).open();
		liste.waitForRowCount(14);
		liste.filter(KENNZEICHEN);
		liste.waitForRowCount(1);
		assertThat(liste.zeile(KENNZEICHEN)).containsExactly(
				KENNZEICHEN, "Dacia", "Duster", "2019", "55000", "", "Hofer Franz");
	}

	@Test
	@Order(3)
	void fahrzeugLoeschen() {
		KundeDetailPage kunde = new KundenPage(driver, waits).open().openByName("HOFER, Franz");
		assertThat(kunde.fahrzeugAnzahl()).isEqualTo(3);
		kunde.fahrzeugLoeschen(KENNZEICHEN);
		assertThat(kunde.fahrzeugAnzahl()).isEqualTo(2);

		FahrzeugePage liste = new FahrzeugePage(driver, waits).open();
		liste.waitForRowCount(13);
	}
}
