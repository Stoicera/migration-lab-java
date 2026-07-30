package at.stoicera.migrationlab.e2e.tests;

import static org.assertj.core.api.Assertions.assertThat;

import at.stoicera.migrationlab.e2e.pages.KundeDetailPage;
import at.stoicera.migrationlab.e2e.pages.KundenPage;
import at.stoicera.migrationlab.e2e.support.ScenarioTest;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

/**
 * Scenario: opening a seed customer shows the full master data and the customer's vehicles. (The
 * legacy kunde-detail view shows NO orders — only master data and vehicles; order history lives in
 * the Aufträge list.) Exercises the data-load gate: the form template renders empty before the
 * async GET populates it (see KundeDetailPage.waitLoaded).
 */
@DisplayName("Kunde-Detail: Stammdaten und Fahrzeuge eines Seed-Kunden")
class KundeDetailTest extends ScenarioTest {

  @Test
  @Order(1)
  void stammdatenUndFahrzeugeAusDemSeed() {
    KundeDetailPage detail = new KundenPage(driver, waits).open().openByName("HOFER, Franz");

    assertThat(detail.heading()).contains("HOFER, Franz");
    assertThat(detail.anredeValue()).isEqualTo("Herr");
    assertThat(detail.vornameValue()).isEqualTo("Franz");
    assertThat(detail.nachnameValue()).isEqualTo("Hofer");
    assertThat(detail.telefonValue()).isEqualTo("+43 664 1111111");
    assertThat(detail.emailValue()).isEqualTo("franz.hofer@aon.at");
    assertThat(detail.strasseValue()).isEqualTo("Hauptstraße 4");
    assertThat(detail.plzValue()).isEqualTo("4320");
    assertThat(detail.ortValue()).isEqualTo("Perg");
    assertThat(detail.notizValue()).isEqualTo("Stammkunde seit 2016");

    // header pin: vehicle cells are read positionally
    assertThat(detail.fahrzeugHeaderTexte())
        .containsExactly("Kennzeichen", "Fahrzeug", "Baujahr", "Pickerl bis", "");
    List<List<String>> fahrzeuge = detail.fahrzeugZeilen();
    assertThat(fahrzeuge).hasSize(2);
    assertThat(fahrzeuge.get(0))
        .containsExactly("PE-123AB", "VW Golf V 1.9 TDI", "2006", "30.11.2026", "X");
    assertThat(fahrzeuge.get(1))
        .containsExactly("PE-456CD", "Skoda Octavia Combi", "2015", "31.03.2027", "X");
  }
}
