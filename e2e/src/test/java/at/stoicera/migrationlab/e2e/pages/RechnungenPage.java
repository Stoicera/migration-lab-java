package at.stoicera.migrationlab.e2e.pages;

import static at.stoicera.migrationlab.e2e.selectors.SelectorMap.css;

import at.stoicera.migrationlab.e2e.support.Waits;
import java.util.Optional;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class RechnungenPage {

  private final WebDriver driver;
  private final Waits waits;

  public RechnungenPage(WebDriver driver, Waits waits) {
    this.driver = driver;
    this.waits = waits;
  }

  public RechnungenPage open() {
    // full page load first: clicking the CURRENT route's nav link does not
    // re-instantiate the view in ngRoute (found during stabilisation, flaky log #2)
    driver.get(at.stoicera.migrationlab.e2e.config.TestConfig.baseUrl() + "/");
    waits.clickable(css("nav.rechnungen")).click();
    waits.countAtLeast(css("rechnungen.rows"), 1);
    return this;
  }

  public String statusOf(String rechnungNr) {
    return row(rechnungNr).findElement(css("rechnungen.rowStatusLabel")).getText();
  }

  public RechnungDetailPage openRechnung(String rechnungNr) {
    row(rechnungNr).findElement(css("rechnungen.rowNumberLink")).click();
    waits.visible(css("rechnungDetail.nummer"));
    return new RechnungDetailPage(driver, waits);
  }

  public RechnungenPage markPaid(String rechnungNr) {
    row(rechnungNr).findElement(css("rechnungen.rowPayButton")).click();
    waits.until(css("rechnungen.rows"), ignored -> "bezahlt".equals(statusOfQuiet(rechnungNr)));
    return this;
  }

  private String statusOfQuiet(String rechnungNr) {
    try {
      return statusOf(rechnungNr);
    } catch (RuntimeException | AssertionError e) {
      return null;
    }
  }

  private WebElement row(String rechnungNr) {
    Optional<WebElement> row =
        driver.findElements(css("rechnungen.rows")).stream()
            .filter(
                r -> r.findElement(css("rechnungen.rowNumberLink")).getText().equals(rechnungNr))
            .findFirst();
    return row.orElseThrow(() -> new AssertionError("Rechnung not in list: " + rechnungNr));
  }
}
