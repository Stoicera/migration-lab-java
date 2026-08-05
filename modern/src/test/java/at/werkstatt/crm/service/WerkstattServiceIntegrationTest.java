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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Integration test against a real PostgreSQL, started by Testcontainers from the same image the
 * compose stand runs and built by the same Flyway migrations (ENGINEERING_STANDARDS §6, ledgered as
 * deferred(G6) in docs/DEVIATIONS.md until 2026-07-31).
 *
 * <p>Deliberately NOT a second characterization suite: the golden masters own the HTTP contract and
 * the DB state transitions against the running stand. What only this level can do is run the
 * service's SQL against a real engine <em>without</em> a deployed stand — so a contributor with
 * nothing but Docker and a JDK can catch a broken query before anything is built, and CI catches it
 * before the equivalence gate even starts.
 *
 * <p>Since stage 6 the schema comes from Flyway rather than from copied init scripts (ADR-0013):
 * Boot migrates the fresh container on context start, so this test also proves the migrations
 * themselves run — the compose stand and this test cannot diverge because there is only one source.
 *
 * <p>PostgreSQL 18 and the pinned locale are not cosmetic (ADR-0012). The image must be the one the
 * stand runs and the collation must be the one the legacy stand runs, because collation decides
 * ORDER BY — and ORDER BY is what the golden masters are sensitive to. The alpine variant of this
 * image reports {@code en_US.utf8} and sorts in C order; that is why the tag has no {@code
 * -alpine}.
 */
@SpringBootTest
@Testcontainers
@Transactional // every test rolls back; the seeded state stays pristine for the next one
class WerkstattServiceIntegrationTest {

  @Container
  @SuppressWarnings("resource") // stopped by the Testcontainers JUnit extension
  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:18")
          .withDatabaseName("werkstatt")
          .withUsername("werkstatt")
          .withPassword("werkstatt")
          .withEnv("LANG", "en_US.utf8")
          .withEnv("POSTGRES_INITDB_ARGS", "--locale=en_US.utf8");

  @DynamicPropertySource
  static void datasource(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
  }

  @Autowired private WerkstattService service;

  // only for the collation guard below — the production SQL stays in the service
  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void die_seed_kunden_kommen_sortiert_aus_der_echten_datenbank() {
    List<Kunde> kunden = service.getAlleKunden();

    assertThat(kunden).hasSize(10);
    assertThat(kunden).extracting(Kunde::getNachname).isSorted();
  }

  /**
   * The guard the PostgreSQL 9.6 → 18 upgrade needed and did not have (ADR-0012).
   *
   * <p>A database can report a collation it does not use. {@code postgres:18-alpine} answers {@code
   * datcollate = en_US.utf8} and then sorts in C order, because musl accepts the locale name and
   * ignores it — so a review step that reads {@code pg_database.datcollate} passes while every
   * sorted list in the application quietly changes order. Reading the setting is therefore
   * worthless as a check; only sorting is evidence, which is what this test does.
   *
   * <p>The probe strings are chosen so the two orders cannot coincide: under glibc {@code en_US}
   * punctuation and case are secondary (so {@code de Vries} sorts by D and the space in {@code
   * Huber Transporte} is ignored), while under C it is raw byte order (so every capital precedes
   * every lower-case letter and {@code Ö} lands last).
   */
  @Test
  void die_datenbank_sortiert_wie_der_legacy_stand_und_sagt_es_nicht_nur() {
    List<String> wieDieDatenbankSortiert =
        jdbcTemplate.queryForList(
            """
            SELECT x FROM (VALUES ('Huber Transporte GmbH'), ('Hubermann'), ('de Vries'),
                                  ('Öhler'), ('Ohler'), ('Zach'), ('van Dijk')) t(x)
            ORDER BY x
            """,
            String.class);

    assertThat(wieDieDatenbankSortiert)
        .as("glibc en_US.utf8 ordering, measured on the legacy 9.6 stand on 2026-08-05")
        .containsExactly(
            "de Vries", "Hubermann", "Huber Transporte GmbH", "Ohler", "Öhler", "van Dijk", "Zach");
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
