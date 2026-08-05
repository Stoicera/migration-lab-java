package at.stoicera.migrationlab.aitestgen;

import java.nio.file.Path;

/**
 * One unit under test: a single class, in one corpus. The list of units is fixed at protocol freeze
 * (PROTOCOL.md §2) — the harness never discovers units, it only reads the catalogue, so a later "we
 * also measured X" is impossible without a visible commit.
 *
 * <p>Two dimensions, deliberately kept apart in the vocabulary: the <b>model arms</b> M1/M2
 * (PROTOCOL.md §3) and the <b>corpora</b> A/B — the legacy code and its migrated counterpart
 * (PROTOCOL.md §4).
 *
 * @param id stable slug used in paths, e.g. {@code s1-werkstattservice}
 * @param stratum S1…S4 per PROTOCOL.md §2
 * @param corpus A = legacy code under test, B = migrated counterpart
 * @param fqn fully qualified class name
 * @param source path to the .java file, relative to the repository root
 */
public record Unit(String id, String stratum, Corpus corpus, String fqn, Path source) {

  /**
   * Package the generated tests live in — apart from the code under test, so that JaCoCo and PIT
   * can exclude the tests themselves from the measurement.
   */
  public static final String TEST_PACKAGE = "at.werkstatt.crm.gen";

  public enum Corpus {
    A("legacy", "legacy Java 8 / Spring Boot 1.5"),
    B("modern", "Java 25 / Spring Boot 4.1");

    private final String module;
    private final String stack;

    Corpus(String module, String stack) {
      this.module = module;
      this.stack = stack;
    }

    /** Repository module this corpus's code under test lives in. */
    public String module() {
      return module;
    }

    /**
     * Where this corpus's schema DDL lives, relative to the repository root.
     *
     * <p>It used to be {@code <module>/db/init/01-schema.sql} for both corpora. Stage 6 moved the
     * modern stand's schema into a Flyway migration (ADR-0013) and deleted {@code modern/db/init/},
     * which broke prompt rendering outright — the harness refuses to render a prompt from a missing
     * schema rather than silently emitting one without DDL, and that refusal is what surfaced this.
     *
     * <p>The frozen prompts must not change ({@code PROTOCOL.md}, tag {@code
     * ai-testgen-protocol-v1}). They do not: only {@code CREATE TABLE} blocks are extracted, the
     * Flyway file's added header is a comment outside them, and the two stands' DDL is held
     * identical by {@code scripts/check-schema-drift.sh}. Verified rather than argued — every
     * recorded prompt in {@code runs/2026-07-31/} re-renders byte-identically after this change.
     */
    public java.nio.file.Path schemaFile() {
      return this == A
          ? java.nio.file.Path.of("legacy", "db", "init", "01-schema.sql")
          : java.nio.file.Path.of(
              "modern", "src", "main", "resources", "db", "migration", "V1__baseline_schema.sql");
    }

    /**
     * The one prompt fragment that differs between corpora: a factual statement about the stack the
     * code belongs to. Everything else in both prompts is byte-identical, so an A/B difference can
     * only come from the code, not from the wording.
     */
    public String stack() {
      return stack;
    }
  }

  /** Simple class name, e.g. {@code WerkstattService}. */
  public String simpleName() {
    return fqn.substring(fqn.lastIndexOf('.') + 1);
  }

  /** Name of the class the model is asked to produce (PROTOCOL.md §4). */
  public String testClassName() {
    return simpleName() + "GeneratedTest";
  }
}
