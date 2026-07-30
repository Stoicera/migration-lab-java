package at.stoicera.migrationlab.e2e.tests;

import static at.stoicera.migrationlab.e2e.selectors.SelectorMap.css;
import static org.assertj.core.api.Assertions.assertThat;

import at.stoicera.migrationlab.e2e.pages.AuftraegePage;
import at.stoicera.migrationlab.e2e.pages.RechnungenPage;
import at.stoicera.migrationlab.e2e.support.ScenarioTest;
import at.stoicera.migrationlab.e2e.support.Seed;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebElement;

/**
 * Scenario: the two list conveniences — the order status filter (a server-side re-query) and the
 * "nur unbezahlte" invoice toggle (client-side). Pinned since review session 10: on the legacy
 * stand these were 2016 code whose oracle was the exhibit itself, but stage 5 REWROTE them — new
 * code with zero coverage is a gap, not a convenience (e2e/README, coverage philosophy).
 */
@DisplayName("Listen-Filter: Status-Buttons (Server-Query) und 'nur unbezahlte' (Client-Toggle)")
class ListenFilterTest extends ScenarioTest {

  @Test
  @Order(1)
  void statusFilterFragtServerSeitigNeuAb() {
    AuftraegePage liste = new AuftraegePage(driver, waits).open();
    // header pin: the filter assertions below read positional cells
    assertThat(liste.headerTexte())
        .containsExactly(
            "Nummer", "Angenommen", "Kennzeichen", "Fahrzeug", "Kunde", "Beschreibung", "Status");
    assertThat(liste.rowCount()).isEqualTo(Seed.ANZAHL_AUFTRAEGE);

    liste.statusFilter("Storniert");
    liste.waitForRowCount(2); // seed: A-2026-0004 and A-2026-0016, date-independent
    assertThat(liste.statusOf("A-2026-0004")).isEqualTo("Storniert");
    assertThat(liste.statusOf("A-2026-0016")).isEqualTo("Storniert");

    liste.statusFilter("Alle");
    liste.waitForRowCount(Seed.ANZAHL_AUFTRAEGE);
  }

  @Test
  @Order(2)
  void nurUnbezahlteToggleFiltertClientseitig() {
    RechnungenPage liste = new RechnungenPage(driver, waits).open();
    assertThat(liste.headerTexte())
        .containsExactly("Nummer", "Datum", "Auftrag", "Kunde", "Netto", "Brutto", "Status", "");
    assertThat(liste.rowCount()).isEqualTo(8);

    liste.nurOffene(true);
    liste.waitForRowCount(1); // seed: only R-2026-0005 is unpaid
    assertThat(liste.statusOf("R-2026-0005")).isEqualTo("offen");

    liste.nurOffene(false);
    liste.waitForRowCount(8);
  }

  /** Kunden list header pin lives here too — one place for the three positional-table pins. */
  @Test
  @Order(3)
  void kundenListeHeaderPin() {
    driver.get(at.stoicera.migrationlab.e2e.config.TestConfig.baseUrl() + "/");
    waits.clickable(css("nav.kunden")).click();
    waits.countAtLeast(css("kunden.rows"), 1);
    assertThat(
            waits.allVisible(css("kunden.headerCells")).stream().map(WebElement::getText).toList())
        .containsExactly("Name", "Telefon", "E-Mail", "Ort", "");
  }
}
