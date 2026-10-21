package at.stoicera.migrationlab.e2e.nachweis;

import static org.assertj.core.api.Assertions.assertThat;

import at.stoicera.migrationlab.e2e.config.TestConfig;
import at.stoicera.migrationlab.e2e.support.ScenarioTest;
import java.util.List;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;

/**
 * The /nachweis evidence page of the modern stand. NOT one of the equivalence scenarios: the page
 * does not exist on the 2016 stand, so it carries the tag "nachweis", is excluded from the default
 * run (e2e/pom.xml) and runs as its own step in e2e.yml — the 34-per-stand count it reports stays
 * the count of the equivalence suite. Selectors are direct data-testid anchors on purpose: there is
 * no legacy counterpart for a selector map to abstract.
 */
@Tag("nachweis")
@DisplayName("Nachweis-Seite: Matrix, Zahlen, Vergleich, Belege (nur moderner Stand)")
class NachweisSeiteTest extends ScenarioTest {

  private static By testid(String id) {
    return By.cssSelector("[data-testid='" + id + "']");
  }

  @BeforeAll
  void nurModern() {
    Assumptions.assumeTrue("modern".equals(TestConfig.target()), "page exists on modern only");
  }

  @Test
  @Order(1)
  void bannerAufDerStartseiteFuehrtZumNachweis() {
    driver.get(TestConfig.baseUrl() + "/");
    waits.clickable(testid("demo-hinweis-nachweis")).click();
    waits.urlContains("/nachweis");
    waits.visible(testid("nachweis-matrix"));
  }

  @Test
  @Order(2)
  void direktaufrufZeigtMatrixMitStimmigenZahlen() {
    // document request, not router navigation: pins the SpaForwardController mapping
    driver.get(TestConfig.baseUrl() + "/nachweis");
    waits.countAtLeast(testid("nachweis-zeile"), 1);

    List<WebElement> zeilen = driver.findElements(testid("nachweis-zeile"));
    int gesamt = zahl("nachweis-gesamt");
    assertThat(zeilen).as("one matrix row per characterization test").hasSize(gesamt);
    assertThat(
            zahl("nachweis-anzahl-identisch")
                + zahl("nachweis-anzahl-sanktioniert")
                + zahl("nachweis-anzahl-regression"))
        .as("verdict counts add up to the total")
        .isEqualTo(gesamt);
    long sanktioniert =
        zeilen.stream().filter(z -> "sanktioniert".equals(z.getAttribute("data-urteil"))).count();
    assertThat(sanktioniert).isEqualTo(zahl("nachweis-anzahl-sanktioniert"));
    assertThat(driver.findElement(testid("nachweis-e2e-legacy")).getText())
        .matches("\\d+ von \\d+ grün");
    assertThat(driver.findElement(testid("nachweis-e2e-modern")).getText())
        .matches("\\d+ von \\d+ grün");
  }

  @Test
  @Order(3)
  void zeilenklickZeigtGoldenUndModerneAntwortNebeneinander() {
    WebElement goldenZeile =
        driver.findElements(testid("nachweis-zeile")).stream()
            .filter(z -> z.getText().contains("kunden.json"))
            .findFirst()
            .orElseThrow();
    klicke(goldenZeile.findElement(testid("nachweis-umschalter")));
    waits.visible(testid("nachweis-vergleich"));
    String golden = driver.findElement(testid("nachweis-golden")).getText();
    String modern = driver.findElement(testid("nachweis-modern")).getText();
    assertThat(golden).isNotBlank().contains("nachname");
    assertThat(modern).isEqualTo(golden);
  }

  @Test
  @Order(4)
  void sanktionierteZeileZeigtErklaerungUndRegisterBeleg() {
    WebElement zeile =
        driver.findElements(testid("nachweis-zeile")).stream()
            .filter(z -> "sanktioniert".equals(z.getAttribute("data-urteil")))
            .findFirst()
            .orElseThrow();
    klicke(zeile.findElement(testid("nachweis-umschalter")));
    waits.visible(testid("nachweis-erklaerung"));
    assertThat(
            zeile.findElements(testid("nachweis-beleg")).stream()
                .map(a -> a.getAttribute("href"))
                .toList())
        .anyMatch(href -> href.contains("/blob/") && href.contains("docs/adr/"));
  }

  @Test
  @Order(5)
  void jederBelegZeigtAufEinenFestenCommitUndKeinInlineSkript() {
    for (WebElement a : driver.findElements(testid("nachweis-beleg"))) {
      assertThat(a.getAttribute("href"))
          .as("evidence link pinned to a commit SHA")
          .matches("https://github\\.com/.+/blob/[0-9a-f]{40}/.+");
    }
    Object inline =
        ((JavascriptExecutor) driver)
            .executeScript("return document.querySelectorAll('script:not([src])').length");
    assertThat(((Number) inline).intValue()).as("no inline script (strict CSP)").isZero();
  }

  /** The fixed 2016 footer covers the lower viewport edge; centre the target before clicking. */
  private void klicke(WebElement element) {
    ((JavascriptExecutor) driver)
        .executeScript("arguments[0].scrollIntoView({block: 'center'});", element);
    element.click();
  }

  private int zahl(String id) {
    return Integer.parseInt(waits.visible(testid(id)).getText().trim());
  }
}
