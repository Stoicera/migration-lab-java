package at.stoicera.migrationlab.e2e.pages;

import static at.stoicera.migrationlab.e2e.selectors.SelectorMap.css;

import at.stoicera.migrationlab.e2e.support.Waits;
import java.util.List;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Admin page — JSP on the legacy stand, SPA route since stage 5 (SD-2, ADR-0004). Reachable by URL
 * only on both stands (never linked from the nav, faithfully). The same flow drives both
 * implementations via the selector map: read the Kennzahlen table, trigger the destructive
 * Bereinigung behind its confirm dialog, read the result meldung.
 *
 * <p>No {@code idle()} here on purpose: the legacy admin page is server-rendered JSP without
 * AngularJS, so the angularjs idle probe would never resolve. The gates are content-based instead —
 * on the SPA the whole page body renders only after the statistik response (@if (daten())), so the
 * table's presence IS the async gate, and value waits go through {@link #waitKennzahl}.
 */
public class AdminPage {

  private final WebDriver driver;
  private final Waits waits;

  public AdminPage(WebDriver driver, Waits waits) {
    this.driver = driver;
    this.waits = waits;
  }

  public AdminPage open() {
    driver.get(at.stoicera.migrationlab.e2e.config.TestConfig.baseUrl() + "/admin");
    waits.visible(css("admin.statistikTabelle"));
    return this;
  }

  /** Value cell of the Kennzahl row with the given label (e.g. "Kunden" → "10"). */
  public String kennzahl(String label) {
    String wert = kennzahlOderNull(label);
    if (wert == null) {
      throw new AssertionError("Kennzahl not on admin page: " + label);
    }
    return wert;
  }

  /** Waits until a Kennzahl shows the expected value (SPA refreshes the table async). */
  public AdminPage waitKennzahl(String label, String erwartet) {
    waits.until(css("admin.statistikTabelle"), ignored -> erwartet.equals(kennzahlOderNull(label)));
    return this;
  }

  /**
   * Triggers the 90-day cleanup, accepting the confirm dialog, and returns the result meldung. The
   * meldung element appearing is the round-trip gate on both stands (JSP: full page re-render; SPA:
   * async POST + signal update).
   */
  public String bereinigen() {
    waits.clickable(css("admin.bereinigenButton")).click();
    waits.alertAndAccept();
    return waits.visible(css("admin.meldung")).getText();
  }

  private String kennzahlOderNull(String label) {
    for (WebElement row : driver.findElements(css("admin.statistikRows"))) {
      List<WebElement> cells = row.findElements(css("admin.rowCells"));
      if (cells.size() >= 2 && cells.get(0).getText().equals(label)) {
        return cells.get(1).getText();
      }
    }
    return null;
  }
}
