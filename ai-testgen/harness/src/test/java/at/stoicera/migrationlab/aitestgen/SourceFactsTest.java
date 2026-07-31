package at.stoicera.migrationlab.aitestgen;

import static org.assertj.core.api.Assertions.assertThat;

import at.stoicera.migrationlab.aitestgen.Unit.Corpus;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * What the prompt tells the model about the surrounding code decides what the model can get right.
 * These rules are therefore pinned, not tuned after seeing results.
 */
class SourceFactsTest {

  private static final Path ROOT = Repo.root();

  private static Unit unit(Corpus corpus, String id) {
    return Catalog.units(corpus).stream().filter(u -> u.id().equals(id)).findFirst().orElseThrow();
  }

  @Test
  void nur_member_der_aeusseren_klasse_nicht_die_anonymen_rowmapper() {
    String source =
        """
        public class Service {
          public List<Kunde> getAlleKunden() {
            return jdbc.query(sql, new RowMapper<Kunde>() {
              public Kunde mapRow(ResultSet rs, int rowNum) {
                return null;
              }
            });
          }
          public void loescheKunde(long id) {
          }
        }
        """;

    List<String> declarations = SourceFacts.publicDeclarations(source);

    assertThat(declarations)
        .containsExactly(
            "public class Service;",
            "public List<Kunde> getAlleKunden();",
            "public void loescheKunde(long id);");
  }

  @Test
  void geschweifte_klammern_in_javadoc_und_strings_verschieben_die_ebene_nicht() {
    String source =
        """
        public class Weird {
          /** Uses {@code Map<String, Object>} and {@link Foo}. */
          public String sql() {
            return "SELECT '{' FROM x";
          }
          public int zahl() {
            return 1;
          }
        }
        """;

    assertThat(SourceFacts.publicDeclarations(source))
        .contains("public String sql();", "public int zahl();");
  }

  @Test
  void der_gottesdienst_liefert_seine_echten_methoden_ohne_mapRow() throws IOException {
    String signatures =
        SourceFacts.dependencySignatures(ROOT, unit(Corpus.A, "s3-admincontroller"));

    assertThat(signatures)
        .contains("public Map<String, Object> getAdminStatistik();")
        .contains("public int bereinigeStornierte();")
        .doesNotContain("mapRow");
  }

  @Test
  void ddl_enthaelt_genau_die_tabellen_der_klasse() throws IOException {
    String service = SourceFacts.ddlExcerpt(ROOT, unit(Corpus.A, "s1-werkstattservice"));
    String controller = SourceFacts.ddlExcerpt(ROOT, unit(Corpus.A, "s2-kundencontroller"));

    assertThat(service)
        .contains("CREATE TABLE kunde")
        .contains("CREATE TABLE fahrzeug")
        .contains("CREATE TABLE auftrag")
        .contains("CREATE TABLE auftrag_position")
        .contains("CREATE TABLE rechnung");
    assertThat(controller).as("the controller holds no SQL of its own").isEmpty();
  }

  @Test
  void beide_korpora_liefern_dieselbe_domaenen_api() throws IOException {
    // the models differ only in formatting between the corpora — the signature list must not
    // silently give one corpus more information than the other
    String legacy = SourceFacts.dependencySignatures(ROOT, unit(Corpus.A, "s2-rechnungcontroller"));
    String modern = SourceFacts.dependencySignatures(ROOT, unit(Corpus.B, "s2-rechnungcontroller"));

    assertThat(legacy).contains("public String getRechnungNr();");
    assertThat(modern).contains("public String getRechnungNr();");
  }
}
