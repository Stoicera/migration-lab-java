package at.stoicera.migrationlab.aitestgen;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The price table pinned at protocol freeze (PROTOCOL.md §3) and the EUR conversion.
 *
 * <p>Prices are USD per million tokens, re-verified live against the OpenRouter model list on
 * freeze day. Costs are reported from the pinned table, not from whatever the API charges later —
 * otherwise a price change would silently rewrite a published result.
 */
public final class Pricing {

  private Pricing() {}

  /** model id → {USD per 1M prompt tokens, USD per 1M completion tokens} — verified 2026-07-31. */
  public static final Map<String, double[]> USD_PER_MILLION =
      Map.of(
          "anthropic/claude-sonnet-5", new double[] {2.00, 10.00},
          "qwen/qwen3-coder-next", new double[] {0.12, 0.80});

  /** Hard abort threshold for the whole experiment (PROTOCOL.md §3). */
  public static final BigDecimal BUDGET_CAP_EUR = new BigDecimal("20.00");

  public static BigDecimal usdCost(String model, long promptTokens, long completionTokens) {
    double[] price = USD_PER_MILLION.get(model);
    if (price == null) {
      throw new IllegalArgumentException(
          "no pinned price for model '"
              + model
              + "' — a model that is not in the frozen table cannot be part of this experiment");
    }
    return BigDecimal.valueOf(price[0])
        .multiply(BigDecimal.valueOf(promptTokens))
        .add(BigDecimal.valueOf(price[1]).multiply(BigDecimal.valueOf(completionTokens)))
        .divide(BigDecimal.valueOf(1_000_000), 6, RoundingMode.HALF_UP);
  }

  /** The ECB daily reference rate actually used for a run, with the day the ECB published it. */
  public record EcbRate(String publishedOn, BigDecimal usdPerEur) {}

  private static final String ECB_DAILY =
      "https://www.ecb.europa.eu/stats/eurofxref/eurofxref-daily.xml";

  private static final Pattern USD_CUBE =
      Pattern.compile("currency='USD'\\s+rate='([0-9.]+)'", Pattern.CASE_INSENSITIVE);

  private static final Pattern CUBE_TIME = Pattern.compile("time='(\\d{4}-\\d{2}-\\d{2})'");

  /**
   * USD per EUR from the ECB's daily reference rates. Fails loudly rather than falling back to a
   * guessed rate: an invented exchange rate in a cost table is a fabricated number, and this report
   * carries no fabricated numbers.
   */
  public static EcbRate ecbUsdPerEur() throws IOException, InterruptedException {
    HttpRequest request =
        HttpRequest.newBuilder(URI.create(ECB_DAILY)).timeout(Duration.ofSeconds(30)).GET().build();
    HttpResponse<String> response =
        HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build()
            .send(request, HttpResponse.BodyHandlers.ofString());
    if (response.statusCode() != 200) {
      throw new IOException("ECB reference rates unavailable (HTTP " + response.statusCode() + ")");
    }
    Matcher rate = USD_CUBE.matcher(response.body());
    Matcher published = CUBE_TIME.matcher(response.body());
    if (!rate.find() || !published.find()) {
      throw new IOException("ECB reference rates response did not contain a USD cube");
    }
    return new EcbRate(published.group(1), new BigDecimal(rate.group(1)));
  }
}
