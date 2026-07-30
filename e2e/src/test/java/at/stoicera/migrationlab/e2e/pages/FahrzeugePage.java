package at.stoicera.migrationlab.e2e.pages;

import static at.stoicera.migrationlab.e2e.selectors.SelectorMap.css;

import at.stoicera.migrationlab.e2e.support.Waits;
import java.util.List;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Global vehicle list (#/fahrzeuge). Read-only in the legacy UI — vehicles are created and deleted
 * on the owning customer's detail page (KundeDetailPage); the view itself says so ("Fahrzeuge
 * werden beim jeweiligen Kunden angelegt").
 */
public class FahrzeugePage {

  private final WebDriver driver;
  private final Waits waits;

  public FahrzeugePage(WebDriver driver, Waits waits) {
    this.driver = driver;
    this.waits = waits;
  }

  public FahrzeugePage open() {
    // full page load first: clicking the CURRENT route's nav link does not
    // re-instantiate the view in ngRoute (found during stabilisation, flaky log #2)
    driver.get(at.stoicera.migrationlab.e2e.config.TestConfig.baseUrl() + "/");
    waits.clickable(css("nav.fahrzeuge")).click();
    waits.visible(css("fahrzeuge.filterField"));
    // settle the initial load BEFORE any interaction (same race as the
    // customer list — see flaky log #1); rows arrive in one response
    waits.countAtLeast(css("fahrzeuge.rows"), 1);
    return this;
  }

  /** Header texts — tests pin these before any cell-position access. */
  public List<String> headerTexte() {
    return waits.allVisible(css("fahrzeuge.headerCells")).stream()
        .map(WebElement::getText)
        .toList();
  }

  public int rowCount() {
    return driver.findElements(css("fahrzeuge.rows")).size();
  }

  public void waitForRowCount(int expected) {
    waits.countIs(css("fahrzeuge.rows"), expected);
  }

  /**
   * Client-side filter (AngularJS filter:filter, no request). The digest has rendered when sendKeys
   * returns; callers gate on waitForRowCount.
   */
  public FahrzeugePage filter(String text) {
    WebElement feld = waits.visible(css("fahrzeuge.filterField"));
    feld.clear();
    feld.sendKeys(text);
    return this;
  }

  /** Cell texts of the row with the given Kennzeichen (first column, header-pinned by tests). */
  public List<String> zeile(String kennzeichen) {
    for (WebElement row : driver.findElements(css("fahrzeuge.rows"))) {
      List<WebElement> cells = row.findElements(css("fahrzeuge.rowCells"));
      if (!cells.isEmpty() && cells.get(0).getText().equals(kennzeichen)) {
        return cells.stream().map(WebElement::getText).toList();
      }
    }
    throw new AssertionError("Fahrzeug not in list: " + kennzeichen);
  }
}
