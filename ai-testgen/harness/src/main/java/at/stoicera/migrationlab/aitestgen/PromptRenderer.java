package at.stoicera.migrationlab.aitestgen;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Builds the two prompts mechanically from the pre-registered templates (PROTOCOL.md §4). There is
 * no per-class hand editing anywhere in the pipeline — the templates below and the source files are
 * the only inputs, so every prompt instance is reproducible from the frozen commit.
 *
 * <p>{@code PromptTemplateDriftTest} asserts that these two constants are byte-identical to the
 * templates printed in PROTOCOL.md. The protocol and the code cannot drift apart silently.
 */
public final class PromptRenderer {

  private PromptRenderer() {}

  public static final String SYSTEM_TEMPLATE =
      """
      You are generating JUnit 5 unit tests for a <STACK> codebase.
      Constraints:
      - JUnit 5.14.x, Mockito (with mockito-junit-jupiter), AssertJ. From spring-test, only
        org.springframework.test.util.ReflectionTestUtils is available. No other libraries.
      - Unit tests only: no Spring context, no MockMvc, no real database, no network, no file I/O.
      - Mock all dependencies of the class under test and instantiate it as it is — the class under
        test must not be modified.
      - Output exactly one complete, compilable test class in a single ```java code block,
        package <TEST_PACKAGE>, class name <TEST_CLASS>.
      - Aim for behavioural coverage: happy paths, edge cases, error/rejection paths.
      - Do not invent methods that do not exist in the provided source.
      """;

  public static final String USER_TEMPLATE =
      """
      Class under test (full source):
      <SOURCE>

      Direct dependency types visible to the class (signatures only):
      <DEPENDENCY_SIGNATURES>

      Database schema excerpt referenced by the SQL in this class (DDL, if any):
      <DDL_EXCERPT>

      Write the test class now.
      """;

  /** A rendered prompt pair for one unit. */
  public record Prompt(Unit unit, String system, String user) {

    /** Human-readable instance, committed next to the response (PROTOCOL.md §4). */
    public String toMarkdown() {
      return "# Prompt instance — "
          + unit().fqn()
          + " (corpus "
          + unit().corpus()
          + ", stratum "
          + unit().stratum()
          + ")\n\n"
          + "Rendered mechanically by the harness from the frozen templates. Do not edit.\n\n"
          + "## System\n\n````text\n"
          + system()
          + "````\n\n## User\n\n````text\n"
          + user()
          + "````\n";
    }
  }

  public static Prompt render(Path repositoryRoot, Unit unit) throws IOException {
    String source = Files.readString(repositoryRoot.resolve(unit.source()), StandardCharsets.UTF_8);
    String system =
        SYSTEM_TEMPLATE
            .replace("<STACK>", unit.corpus().stack())
            .replace("<TEST_PACKAGE>", Unit.TEST_PACKAGE)
            .replace("<TEST_CLASS>", unit.testClassName());
    String ddl = SourceFacts.ddlExcerpt(repositoryRoot, unit);
    String user =
        USER_TEMPLATE
            .replace("<SOURCE>", source.strip())
            .replace(
                "<DEPENDENCY_SIGNATURES>", SourceFacts.dependencySignatures(repositoryRoot, unit))
            .replace("<DDL_EXCERPT>", ddl.isEmpty() ? "(none — this class contains no SQL)" : ddl);
    return new Prompt(unit, system, user);
  }
}
