package at.stoicera.migrationlab.e2e.tests;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import at.stoicera.migrationlab.e2e.pages.BerichtPage;
import at.stoicera.migrationlab.e2e.support.ScenarioTest;

/**
 * Scenario: monthly report values against the frozen seed months (Jän–Mär
 * 2026). Other scenarios only ever write into the current month, so these
 * numbers are stable by design — see the flaky strategy in the playbook.
 */
@DisplayName("Monatsbericht: Kennzahlen aus dem Seed-Datenbestand")
class BerichtTest extends ScenarioTest {

	@Test
	@Order(1)
	void jaennerWerte() {
		BerichtPage bericht = new BerichtPage(driver, waits).open().jahr("2026");
		List<String> row = bericht.monthRow("Jänner");
		assertThat(row).containsExactly("Jänner", "2", "2", "580,40 €", "696,48 €");
	}

	@Test
	@Order(2)
	void februarUndMaerzWerte() {
		BerichtPage bericht = new BerichtPage(driver, waits).open().jahr("2026");
		assertThat(bericht.monthRow("Februar"))
				.containsExactly("Februar", "2", "1", "84,00 €", "100,80 €");
		assertThat(bericht.monthRow("März"))
				.containsExactly("März", "2", "2", "1439,00 €", "1726,80 €");
	}

	@Test
	@Order(3)
	void topKunde() {
		BerichtPage bericht = new BerichtPage(driver, waits).open().jahr("2026");
		List<String> erste = bericht.topKundeErsteZeile();
		assertThat(erste.get(1)).isEqualTo("Steiner Karl");
		assertThat(erste.get(3)).isEqualTo("912,00 €");
	}
}
