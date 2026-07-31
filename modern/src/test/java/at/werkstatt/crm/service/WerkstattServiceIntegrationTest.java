package at.werkstatt.crm.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import at.werkstatt.crm.model.Auftrag;
import at.werkstatt.crm.model.Kunde;
import at.werkstatt.crm.model.MonatsBericht;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

/**
 * Integration test against a real PostgreSQL, started by Testcontainers from the same image and the
 * same init scripts the compose stand uses (ENGINEERING_STANDARDS §6, ledgered as deferred(G6) in
 * docs/DEVIATIONS.md until 2026-07-31).
 *
 * <p>Deliberately NOT a second characterization suite: the golden masters own the HTTP contract and
 * the DB state transitions against the running stand. What only this level can do is run the
 * service's SQL against a real engine <em>without</em> a deployed stand — so a contributor with
 * nothing but Docker and a JDK can catch a broken query before anything is built, and CI catches it
 * before the equivalence gate even starts.
 *
 * <p>PostgreSQL 9.6 on purpose: the stands still run it (DEVIATIONS P2, upgrade owned by G7), and a
 * different engine version here would test something the application never meets — collation and
 * ordering differences are exactly what the golden masters are sensitive to.
 */
@SpringBootTest
@Testcontainers
@Transactional // every test rolls back; the seeded state stays pristine for the next one
class WerkstattServiceIntegrationTest {

  @Container
  @SuppressWarnings("resource") // stopped by the Testcontainers JUnit extension
  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:9.6")
          .withDatabaseName("werkstatt")
          .withUsername("werkstatt")
          .withPassword("werkstatt")
          // the very files the compose stand mounts — no second copy of the schema to drift
          .withCopyFileToContainer(
              MountableFile.forHostPath("db/init"), "/docker-entrypoint-initdb.d/");

  @DynamicPropertySource
  static void datasource(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
  }

  @Autowired private WerkstattService service;

  @Test
  void die_seed_kunden_kommen_sortiert_aus_der_echten_datenbank() {
    List<Kunde> kunden = service.getAlleKunden();

    assertThat(kunden).hasSize(10);
    assertThat(kunden).extracting(Kunde::getNachname).isSorted();
  }

  @Test
  void die_suche_findet_ueber_nachname_vorname_und_ort() {
    assertThat(service.sucheKunden("hofer")).isNotEmpty();
    assertThat(service.sucheKunden("HOFER"))
        .as("the search lowercases both sides")
        .hasSameSizeAs(service.sucheKunden("hofer"));
    assertThat(service.sucheKunden("Perg")).as("the search also covers the town").hasSize(3);
  }

  @Test
  void feindliche_eingaben_bleiben_daten_nicht_sql() {
    // SD-1 (ADR-0004): the same input leaks all customers on the legacy stand.
    // Pinned by characterization over HTTP — and here, one layer lower, over real SQL.
    assertThat(service.sucheKunden("%' OR '1'='1")).isEmpty();
  }

  @Test
  void der_monatsbericht_aggregiert_das_eingespielte_jahr() {
    List<MonatsBericht> bericht = service.getMonatsBericht(2026);

    assertThat(bericht).isNotEmpty();
    assertThat(bericht).allSatisfy(monat -> assertThat(monat.getMonat()).isBetween(1, 12));
    assertThat(bericht.stream().mapToInt(MonatsBericht::getAnzahlAuftraege).sum()).isPositive();
  }

  @Test
  void ein_neuer_kunde_wird_geschrieben_und_wieder_gelesen() {
    Kunde neu = new Kunde();
    neu.setAnrede("Frau");
    neu.setNachname("Integrationstest");
    neu.setOrt("Linz");

    Kunde gespeichert = service.speichereKunde(neu);

    assertThat(gespeichert.getId()).isNotNull();
    assertThat(service.getKunde(gespeichert.getId()).getNachname()).isEqualTo("Integrationstest");
  }

  @Test
  void der_statusfluss_lehnt_verbotene_uebergaenge_ab() {
    Auftrag angenommen =
        service.getAuftraege(null).stream()
            .filter(a -> "ANGENOMMEN".equals(a.getStatus()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("seed data must contain an accepted order"));

    // ANGENOMMEN -> ABGEHOLT skips the flow; the God class rejects it with a German message
    assertThatThrownBy(() -> service.setzeStatus(angenommen.getId(), "ABGEHOLT"))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Statuswechsel");
  }
}
