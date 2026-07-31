package at.stoicera.migrationlab.aitestgen;

import at.stoicera.migrationlab.aitestgen.Unit.Corpus;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/**
 * The stratified selection, fixed at protocol freeze (PROTOCOL.md §2). Six units per corpus — the
 * same six classes on both sides of the migration.
 *
 * <p>Excluded on purpose and never selected later: FahrzeugController and BerichtController
 * (shape-redundant with the S2 picks), the remaining models (redundant with S4), and
 * WerkstattCrmApplication (bootstrap).
 */
public final class Catalog {

  private Catalog() {}

  private static final List<String[]> SELECTION =
      List.of(
          // {id, stratum, fqn} — execution order S1 → S4 (PROTOCOL.md §6)
          new String[] {"s1-werkstattservice", "S1", "at.werkstatt.crm.service.WerkstattService"},
          new String[] {
            "s2-kundencontroller", "S2", "at.werkstatt.crm.controller.KundenController"
          },
          new String[] {
            "s2-auftragcontroller", "S2", "at.werkstatt.crm.controller.AuftragController"
          },
          new String[] {
            "s2-rechnungcontroller", "S2", "at.werkstatt.crm.controller.RechnungController"
          },
          new String[] {"s3-admincontroller", "S3", "at.werkstatt.crm.controller.AdminController"},
          new String[] {"s4-rechnung", "S4", "at.werkstatt.crm.model.Rechnung"});

  /** The six units of one corpus, in the fixed execution order. */
  public static List<Unit> units(Corpus corpus) {
    return SELECTION.stream()
        .map(row -> new Unit(row[0], row[1], corpus, row[2], sourceOf(corpus, row[2])))
        .toList();
  }

  /** Both corpora, A before B. */
  public static List<Unit> allUnits() {
    return Stream.concat(units(Corpus.A).stream(), units(Corpus.B).stream()).toList();
  }

  private static Path sourceOf(Corpus corpus, String fqn) {
    return Path.of(corpus.module(), "src", "main", "java").resolve(fqn.replace('.', '/') + ".java");
  }
}
