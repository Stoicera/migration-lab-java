package at.stoicera.migrationlab.aitestgen;

import static org.assertj.core.api.Assertions.assertThat;

import at.stoicera.migrationlab.aitestgen.Unit.Corpus;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/** The prompt is an experiment artifact: it must be complete, deterministic and corpus-fair. */
class PromptRendererTest {

  private static final Path ROOT = Repo.root();

  private static Unit unit(Corpus corpus, String id) {
    return Catalog.units(corpus).stream().filter(u -> u.id().equals(id)).findFirst().orElseThrow();
  }

  @Test
  void kein_platzhalter_bleibt_unersetzt() throws IOException {
    for (Unit unit : Catalog.allUnits()) {
      PromptRenderer.Prompt prompt = PromptRenderer.render(ROOT, unit);

      assertThat(prompt.system() + prompt.user())
          .as("unresolved placeholder in the prompt for %s", unit.fqn())
          .doesNotContain("<STACK>")
          .doesNotContain("<TEST_PACKAGE>")
          .doesNotContain("<TEST_CLASS>")
          .doesNotContain("<SOURCE>")
          .doesNotContain("<DEPENDENCY_SIGNATURES>")
          .doesNotContain("<DDL_EXCERPT>");
    }
  }

  @Test
  void die_prompts_der_beiden_korpora_unterscheiden_sich_nur_im_stack_satz() throws IOException {
    String legacy = PromptRenderer.render(ROOT, unit(Corpus.A, "s4-rechnung")).system();
    String modern = PromptRenderer.render(ROOT, unit(Corpus.B, "s4-rechnung")).system();

    assertThat(legacy.replace(Corpus.A.stack(), "<STACK>"))
        .as("a corpus difference beyond the stack sentence would confound the A/B comparison")
        .isEqualTo(modern.replace(Corpus.B.stack(), "<STACK>"));
  }

  @Test
  void der_quelltext_der_klasse_steckt_vollstaendig_im_prompt() throws IOException {
    PromptRenderer.Prompt prompt =
        PromptRenderer.render(ROOT, unit(Corpus.A, "s2-kundencontroller"));

    assertThat(prompt.user())
        .contains("public class KundenController")
        .contains("public ResponseEntity<?> loeschen(@PathVariable long id)");
    assertThat(prompt.system()).contains("class name KundenControllerGeneratedTest");
    assertThat(prompt.system()).contains("package at.werkstatt.crm.gen");
  }

  @Test
  void klassen_mit_sql_bekommen_das_ddl_klassen_ohne_sql_nicht() throws IOException {
    String service = PromptRenderer.render(ROOT, unit(Corpus.A, "s1-werkstattservice")).user();
    String model = PromptRenderer.render(ROOT, unit(Corpus.A, "s4-rechnung")).user();

    assertThat(service).contains("CREATE TABLE kunde").contains("CREATE TABLE auftrag_position");
    assertThat(model).contains("(none — this class contains no SQL)");
  }

  @Test
  void abhaengige_domaentypen_kommen_als_signaturen_ohne_rumpf() throws IOException {
    String prompt = PromptRenderer.render(ROOT, unit(Corpus.A, "s2-rechnungcontroller")).user();

    assertThat(prompt).contains("// at.werkstatt.crm.model.Rechnung");
    assertThat(prompt).contains("public String getRechnungNr();");
    assertThat(prompt).doesNotContain("return rechnungNr;");
  }

  @Test
  void signaturen_sind_deterministisch_sortiert() throws IOException {
    Unit unit = unit(Corpus.B, "s1-werkstattservice");

    List<String> renders =
        List.of(PromptRenderer.render(ROOT, unit).user(), PromptRenderer.render(ROOT, unit).user());

    assertThat(renders.get(0)).isEqualTo(renders.get(1));
  }
}
