package at.stoicera.migrationlab.e2e.tests;

import static org.assertj.core.api.Assertions.assertThat;

import at.stoicera.migrationlab.e2e.pages.BerichtPage;
import at.stoicera.migrationlab.e2e.support.ScenarioTest;
import at.stoicera.migrationlab.e2e.support.Seed;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

/**
 * Scenario: monthly report values against the frozen seed months (Jän–Mär 2026). Other scenarios
 * only ever write into the current month, so these numbers are stable by design — see the flaky
 * strategy in the playbook.
 *
 * <p>Year handling: the tests select the SEED year 2026 explicitly. That stays possible on any
 * wall-clock date because the legacy year dropdown is built from the current year down to 2016.
 * While the wall clock is in 2026 the selection is a no-op (already selected); from 2027 on it
 * becomes a real change — both paths are deterministic, see BerichtPage.jahr.
 */
@DisplayName("Monatsbericht: Kennzahlen aus dem Seed-Datenbestand")
class BerichtTest extends ScenarioTest {

  @Test
  @Order(1)
  void jaennerWerte() {
    BerichtPage bericht =
        new BerichtPage(driver, waits)
            .open()
            .jahr(Seed.BERICHT_JAHR, Seed.BERICHT_2026_GESAMT_AUFTRAEGE);
    List<String> row = bericht.monthRow("Jänner");
    assertThat(row).containsExactly("Jänner", "2", "2", "580,40 €", "696,48 €");
  }

  @Test
  @Order(2)
  void februarUndMaerzWerte() {
    BerichtPage bericht =
        new BerichtPage(driver, waits)
            .open()
            .jahr(Seed.BERICHT_JAHR, Seed.BERICHT_2026_GESAMT_AUFTRAEGE);
    assertThat(bericht.monthRow("Februar"))
        .containsExactly("Februar", "2", "1", "84,00 €", "100,80 €");
    assertThat(bericht.monthRow("März"))
        .containsExactly("März", "2", "2", "1439,00 €", "1726,80 €");
  }

  @Test
  @Order(3)
  void topKunde() {
    BerichtPage bericht =
        new BerichtPage(driver, waits)
            .open()
            .jahr(Seed.BERICHT_JAHR, Seed.BERICHT_2026_GESAMT_AUFTRAEGE);
    List<String> erste = bericht.topKundeErsteZeile();
    assertThat(erste.get(1)).isEqualTo("Steiner Karl");
    assertThat(erste.get(3)).isEqualTo("912,00 €");
  }

  @Test
  @Order(4)
  void jahresWechselLaedtNeu() {
    // exercises the REAL year-change path of BerichtPage.jahr on any wall-clock
    // date (2025 is always in the dropdown and always empty on a fresh seed) —
    // the in-place ng-repeat re-render is gated by the Gesamt/Aufträge cell
    BerichtPage bericht =
        new BerichtPage(driver, waits)
            .open()
            .jahr("2025", Seed.BERICHT_LEERES_JAHR_GESAMT_AUFTRAEGE);
    assertThat(bericht.monthRow("Jänner")).containsExactly("Jänner", "0", "0", "0,00 €", "0,00 €");

    bericht.jahr(Seed.BERICHT_JAHR, Seed.BERICHT_2026_GESAMT_AUFTRAEGE);
    assertThat(bericht.monthRow("Jänner"))
        .containsExactly("Jänner", "2", "2", "580,40 €", "696,48 €");
  }
}
