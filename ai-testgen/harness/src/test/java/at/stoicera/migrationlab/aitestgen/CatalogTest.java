package at.stoicera.migrationlab.aitestgen;

import static org.assertj.core.api.Assertions.assertThat;

import at.stoicera.migrationlab.aitestgen.Unit.Corpus;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Guards the pre-registered selection against silent drift: a renamed or deleted class under test
 * would otherwise turn into a quietly missing measurement.
 */
class CatalogTest {

  @ParameterizedTest
  @EnumSource(Corpus.class)
  void jedes_ausgewaehlte_unit_existiert_im_repository(Corpus corpus) {
    Path root = Repo.root();

    for (Unit unit : Catalog.units(corpus)) {
      assertThat(root.resolve(unit.source()))
          .as("%s (%s) is in the frozen selection but not in the tree", unit.fqn(), corpus)
          .isRegularFile();
    }
  }

  @Test
  void beide_korpora_enthalten_dieselben_sechs_klassen() {
    assertThat(Catalog.units(Corpus.A)).hasSize(6);
    assertThat(Catalog.units(Corpus.B)).hasSize(6);
    assertThat(Catalog.units(Corpus.A).stream().map(Unit::fqn).toList())
        .isEqualTo(Catalog.units(Corpus.B).stream().map(Unit::fqn).toList());
    assertThat(Catalog.allUnits().stream().map(Unit::id).distinct()).hasSize(6);
  }

  @Test
  void die_strata_decken_die_vier_vorregistrierten_schichten_ab() {
    assertThat(Catalog.units(Corpus.A).stream().map(Unit::stratum).distinct().sorted().toList())
        .containsExactly("S1", "S2", "S3", "S4");
  }

  @Test
  void der_generierte_test_liegt_nicht_im_paket_des_pruefobjekts() {
    // JaCoCo and PIT exclude at/werkstatt/crm/gen: measurement must not cover the tests
    assertThat(Unit.TEST_PACKAGE).isEqualTo("at.werkstatt.crm.gen");
    for (Unit unit : Catalog.allUnits()) {
      assertThat(unit.fqn()).doesNotContain(Unit.TEST_PACKAGE);
    }
  }

  @Test
  void das_legacy_modul_unter_test_ist_weiterhin_testfrei() throws Exception {
    // hard repo rule: legacy/ is the exhibit and never receives tests
    Path legacyTests = Repo.root().resolve("legacy/src/test");

    assertThat(Files.exists(legacyTests)).isFalse();
  }
}
