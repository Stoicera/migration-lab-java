package at.werkstatt.crm;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Architecture rules for the modern stand (ENGINEERING_STANDARDS §3, ledgered as deferred(G6) in
 * docs/DEVIATIONS.md until 2026-07-31).
 *
 * <p>These rules exist for one migration purpose: what stages 2–5 achieved by hand must not erode
 * silently. The constructor-injection sweep of stage 4 (the precondition for testable units, see
 * ai-testgen/PROTOCOL.md) is worth exactly as much as the guarantee that the next `@Autowired`
 * field does not slip back in — a code review catches that on a good day, a rule catches it always.
 *
 * <p>No rule here describes a target architecture nobody agreed on: at 16 backend classes the
 * layering is what it is, and the God class stays a God class on purpose (it is G6's study object).
 */
class ArchitectureRulesTest {

  private static JavaClasses productionClasses;

  @BeforeAll
  static void importProductionCode() {
    productionClasses =
        new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("at.werkstatt.crm");
  }

  @Test
  void keine_feldinjektion_mehr() {
    // stage 4 removed every @Autowired field; this keeps it removed
    ArchRule rule =
        noFields()
            .should()
            .beAnnotatedWith("org.springframework.beans.factory.annotation.Autowired")
            .because(
                "constructor injection is what makes these classes unit-testable at all "
                    + "(stage 4, ADR-0002); a field-injected dependency cannot be handed a mock");
    rule.check(productionClasses);
  }

  @Test
  void abhaengigkeiten_injizierter_zustand_bleibt_final() {
    ArchRule rule =
        fields()
            .that()
            .areDeclaredInClassesThat()
            .resideInAnyPackage("..controller..", "..service..")
            .and()
            .areNotStatic()
            .should()
            .beFinal()
            .because("constructor-injected collaborators are set once and never reassigned");
    rule.check(productionClasses);
  }

  @Test
  void der_service_kennt_die_controller_nicht() {
    ArchRule rule =
        noClasses()
            .that()
            .resideInAPackage("..service..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..controller..")
            .because("the web layer depends on the domain, never the other way round");
    rule.check(productionClasses);
  }

  @Test
  void sql_bleibt_im_service() {
    ArchRule rule =
        noClasses()
            .that()
            .resideOutsideOfPackage("..service..")
            .should()
            .dependOnClassesThat()
            .haveFullyQualifiedName("org.springframework.jdbc.core.JdbcTemplate")
            .because(
                "data access lives in one place — that is what made the B4 injection fix a "
                    + "single-file change (SD-1, ADR-0004)");
    rule.check(productionClasses);
  }

  @Test
  void modelle_bleiben_frei_von_spring() {
    ArchRule rule =
        noClasses()
            .that()
            .resideInAPackage("..model..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("org.springframework..")
            .because("the domain objects are the API contract; they must stay plain Java");
    rule.check(productionClasses);
  }
}
