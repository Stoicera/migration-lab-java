package at.stoicera.migrationlab.aitestgen;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Mechanical code extraction, PROTOCOL.md §5: take the FIRST ```java fenced block of the response.
 * If there is none, or it does not parse as a fence, the result is {@code EXTRACTION-FAILED} and
 * that unit×model counts as non-compiling. It is never re-prompted and never hand-repaired at this
 * step — repairing before Phase A would falsify the compile rate.
 */
public final class CodeExtractor {

  private CodeExtractor() {}

  /** Opening fence: three-or-more backticks followed by the java info string on its own line. */
  private static final Pattern JAVA_FENCE =
      Pattern.compile("(?m)^[ \\t]*(`{3,})[ \\t]*java[ \\t]*\\r?\\n");

  /**
   * @return the content of the first ```java block, or empty if the response has none / the block
   *     is never closed
   */
  public static Optional<String> firstJavaBlock(String response) {
    if (response == null) {
      return Optional.empty();
    }
    Matcher opening = JAVA_FENCE.matcher(response);
    if (!opening.find()) {
      return Optional.empty();
    }
    String fence = opening.group(1);
    int bodyStart = opening.end();
    Matcher closing =
        Pattern.compile("(?m)^[ \\t]*" + fence + "[ \\t]*(\\r?\\n|$)").matcher(response);
    if (!closing.find(bodyStart)) {
      return Optional.empty(); // unterminated block: unusable, counts as extraction failure
    }
    String code = response.substring(bodyStart, closing.start());
    return code.isBlank() ? Optional.empty() : Optional.of(code);
  }
}
