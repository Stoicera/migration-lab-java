package at.stoicera.migrationlab.aitestgen;

import java.io.IOException;

/**
 * The one seam between the experiment and a vendor. Everything above this interface is
 * vendor-neutral, so a later replication can run the identical procedure against a different
 * gateway without touching prompts, extraction or metrics.
 */
public interface LlmClient {

  /**
   * Exactly one completion call, k = 1 (PROTOCOL.md §3). Implementations retry only on transport
   * errors and never on unsatisfying content.
   */
  Completion complete(String model, String system, String user)
      throws IOException, InterruptedException;

  /**
   * @param requestJson the exact payload sent, recorded verbatim
   * @param responseJson the exact body received, recorded verbatim
   * @param content the assistant message text (before code extraction)
   * @param provider the backend that actually served the call (threat T3)
   * @param transportRetries 0 or 1 — a retried call is still one generation
   */
  record Completion(
      String requestJson,
      String responseJson,
      String content,
      String provider,
      long promptTokens,
      long completionTokens,
      long latencyMillis,
      int transportRetries) {}
}
