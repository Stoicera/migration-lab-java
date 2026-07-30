package at.stoicera.migrationlab.e2e.tests;

import static at.stoicera.migrationlab.e2e.selectors.SelectorMap.css;

import at.stoicera.migrationlab.e2e.config.TestConfig;
import at.stoicera.migrationlab.e2e.support.ScenarioTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

/**
 * Scenario: a user's saved hash-bang bookmark from the 2016 UI keeps working. On legacy that is
 * native ngRoute behaviour; on modern a shim rewrites {@code /#!/pfad} to the path route before the
 * router's initial navigation (modern/frontend/src/main.ts — added in review session 10, which
 * found that cutover had silently orphaned every legacy bookmark). Same assertion on both stands:
 * the bookmark lands on the Bericht page with its data loaded.
 */
@DisplayName("Deep-Link: alte #!-Lesezeichen erreichen ihre Seite auf beiden Ständen")
class DeepLinkTest extends ScenarioTest {

  @Test
  @Order(1)
  void hashBangLesezeichenLandetAufDemBericht() {
    driver.get(TestConfig.baseUrl() + "/#!/bericht");
    waits.visible(css("bericht.yearSelect"));
    waits.countIs(css("bericht.monthRows"), 12);
  }
}
