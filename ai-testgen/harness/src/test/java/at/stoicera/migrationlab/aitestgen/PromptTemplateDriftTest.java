package at.stoicera.migrationlab.aitestgen;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * A pre-registration is worth exactly as much as the guarantee that the registered text is the text
 * that ran. PROTOCOL.md prints the prompt templates; PromptRenderer holds them as constants. This
 * test fails the build if the two ever differ by a single character.
 */
class PromptTemplateDriftTest {

  private static final Pattern TEXT_BLOCK =
      Pattern.compile("(?s)````text\\r?\\n(.*?)````", Pattern.MULTILINE);

  @Test
  void die_templates_im_code_sind_die_templates_im_protokoll() throws IOException {
    List<String> blocks = textBlocksOf(Files.readString(Repo.protocol()));

    assertThat(blocks)
        .as("PROTOCOL.md §4 must print exactly two ````text blocks: system prompt, user prompt")
        .hasSize(2);
    assertThat(blocks.get(0)).isEqualTo(PromptRenderer.SYSTEM_TEMPLATE);
    assertThat(blocks.get(1)).isEqualTo(PromptRenderer.USER_TEMPLATE);
  }

  @Test
  void das_protokoll_nennt_jede_platzhalter_variable_die_der_renderer_ersetzt() throws IOException {
    String protocol = Files.readString(Repo.protocol());

    assertThat(protocol)
        .contains("<STACK>")
        .contains("<TEST_PACKAGE>")
        .contains("<TEST_CLASS>")
        .contains("<SOURCE>")
        .contains("<DEPENDENCY_SIGNATURES>")
        .contains("<DDL_EXCERPT>");
  }

  private static List<String> textBlocksOf(String markdown) {
    Matcher matcher = TEXT_BLOCK.matcher(markdown);
    List<String> blocks = new ArrayList<>();
    while (matcher.find()) {
      blocks.add(matcher.group(1));
    }
    return blocks;
  }
}
