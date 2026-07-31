package at.stoicera.migrationlab.aitestgen;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * OpenRouter chat-completions client, pinned to the sampling parameters of PROTOCOL.md §3:
 * temperature 0, top_p 1, one call per unit and model.
 *
 * <p>temperature 0 reduces but does not guarantee determinism — providers batch and quantize
 * differently, so a replication may see different text. That is why the raw response is recorded
 * verbatim instead of being re-derived later.
 */
public final class OpenRouterClient implements LlmClient {

  private static final URI ENDPOINT = URI.create("https://openrouter.ai/api/v1/chat/completions");
  private static final int MAX_OUTPUT_TOKENS = 16_000;

  private final String apiKey;
  private final HttpClient http;
  private final ObjectMapper json = new ObjectMapper();

  public OpenRouterClient(String apiKey) {
    this.apiKey = apiKey;
    this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build();
  }

  @Override
  public Completion complete(String model, String system, String user)
      throws IOException, InterruptedException {
    String payload = requestBody(model, system, user);
    long startedAt = System.nanoTime();
    HttpResponse<String> response;
    int retries = 0;
    try {
      response = send(payload);
      if (isTransportFailure(response.statusCode())) {
        // PROTOCOL.md §3: one retry on transport error only, never on unsatisfying content
        retries = 1;
        response = send(payload);
      }
    } catch (IOException firstAttempt) {
      retries = 1;
      response = send(payload);
    }
    long latencyMillis = (System.nanoTime() - startedAt) / 1_000_000;

    if (response.statusCode() != 200) {
      throw new IOException(
          "OpenRouter returned HTTP " + response.statusCode() + ": " + response.body());
    }
    JsonNode body = json.readTree(response.body());
    JsonNode choice = body.path("choices").path(0);
    JsonNode message = choice.path("message");
    JsonNode content = message.path("content");
    JsonNode usage = body.path("usage");
    // A reasoning model can spend the whole output budget on thinking and be cut off before it
    // emits any answer — then `content` is JSON null, not missing. Reading that with asText()
    // yields the literal string "null", which is how the 2026-07-31 run recorded it (amendment
    // A1). Distinguishing the two is what makes such a cell readable as "no answer produced"
    // rather than "the model wrote the word null".
    String text = content.isNull() || content.isMissingNode() ? "" : content.asText();
    return new Completion(
        payload,
        response.body(),
        text,
        body.path("provider").asText("unknown"),
        usage.path("prompt_tokens").asLong(-1),
        usage.path("completion_tokens").asLong(-1),
        latencyMillis,
        retries,
        choice.path("finish_reason").asText("unknown"),
        !text.isBlank());
  }

  private HttpResponse<String> send(String payload) throws IOException, InterruptedException {
    HttpRequest request =
        HttpRequest.newBuilder(ENDPOINT)
            .header("Authorization", "Bearer " + apiKey)
            .header("Content-Type", "application/json")
            .header("X-Title", "migration-lab ai-testgen")
            .timeout(Duration.ofMinutes(10))
            .POST(HttpRequest.BodyPublishers.ofString(payload))
            .build();
    return http.send(request, HttpResponse.BodyHandlers.ofString());
  }

  private static boolean isTransportFailure(int statusCode) {
    return statusCode == 429 || statusCode >= 500;
  }

  String requestBody(String model, String system, String user) {
    ObjectNode request = json.createObjectNode();
    request.put("model", model);
    ArrayNode messages = request.putArray("messages");
    messages.addObject().put("role", "system").put("content", system);
    messages.addObject().put("role", "user").put("content", user);
    request.put("temperature", 0);
    request.put("top_p", 1);
    request.put("max_tokens", MAX_OUTPUT_TOKENS);
    request.putObject("usage").put("include", true);
    return request.toPrettyString();
  }
}
