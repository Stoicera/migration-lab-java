package at.stoicera.migrationlab.e2e.tests;

import static org.assertj.core.api.Assertions.assertThat;

import at.stoicera.migrationlab.e2e.pages.DashboardPage;
import at.stoicera.migrationlab.e2e.support.ScenarioTest;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

/**
 * Scenario: the Monday-morning glance — the start page must show the exact workshop state of the
 * seed. KPI derivation from the seed: offene = ANGENOMMEN (0014, 0015) + IN_ARBEIT (0011, 0013) =
 * 4; fertig = FERTIG (0009, 0012) = 2; unbezahlt = R-2026-0005 = 1.
 */
@DisplayName("Dashboard: Kennzahlen und Werkstatt-Listen aus dem Seed")
class DashboardTest extends ScenarioTest {

  @Test
  @Order(1)
  void kennzahlenAusDemSeed() {
    DashboardPage dashboard = new DashboardPage(driver, waits).open();
    assertThat(dashboard.kpiOffeneAuftraege()).isEqualTo("4");
    assertThat(dashboard.kpiFertigeAuftraege()).isEqualTo("2");
    assertThat(dashboard.kpiOffeneRechnungen()).isEqualTo("1");
  }

  @Test
  @Order(2)
  void werkstattUndAbholbereitListen() {
    DashboardPage dashboard = new DashboardPage(driver, waits).open();

    // heading + header pins: the tables are selected positionally (no ids in
    // the 2016 markup) — a silent section/column reorder must fail loudly here
    assertThat(dashboard.werkstattHeading()).isEqualTo("In der Werkstatt");
    assertThat(dashboard.werkstattHeaderTexte())
        .containsExactly("Auftrag", "Kennzeichen", "Kunde", "Status");
    List<List<String>> werkstatt = dashboard.werkstattZeilen();
    assertThat(werkstatt).hasSize(4);
    // newest first (angenommen_am DESC)
    assertThat(werkstatt.get(0))
        .containsExactly("A-2026-0015", "PE-123AB", "Hofer Franz", "Angenommen");
    assertThat(werkstatt.get(2))
        .containsExactly("A-2026-0013", "L-246WX", "Brunner Elisabeth", "In Arbeit");

    assertThat(dashboard.abholbereitHeading()).isEqualTo("Abholbereit");
    assertThat(dashboard.abholbereitHeaderTexte())
        .containsExactly("Auftrag", "Kennzeichen", "Kunde", "Fertig seit");
    List<List<String>> abholbereit = dashboard.abholbereitZeilen();
    assertThat(abholbereit).hasSize(2);
    assertThat(abholbereit.get(0))
        .containsExactly("A-2026-0012", "PE-655QS", "Huber Transporte GmbH", "08.07.2026");
    assertThat(abholbereit.get(1))
        .containsExactly("A-2026-0009", "PE-987ST", "Wagner Christine", "20.05.2026");
  }
}
