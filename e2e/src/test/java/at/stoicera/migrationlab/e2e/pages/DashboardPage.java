package at.stoicera.migrationlab.e2e.pages;

import static at.stoicera.migrationlab.e2e.selectors.SelectorMap.css;

import at.stoicera.migrationlab.e2e.support.Waits;
import java.util.List;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/** Start page (#/start, also the default route). */
public class DashboardPage {

  private final WebDriver driver;
  private final Waits waits;

  public DashboardPage(WebDriver driver, Waits waits) {
    this.driver = driver;
    this.waits = waits;
  }

  public DashboardPage open() {
    // the default route redirects to #/start; a full page load re-instantiates
    // the controller regardless of the previous route (flaky log #2)
    driver.get(at.stoicera.migrationlab.e2e.config.TestConfig.baseUrl() + "/");
    waits.visible(css("dashboard.kpiOffeneAuftraege"));
    // KPI spans render "0"/empty from the bare template before the two async
    // GETs land — idle() is the only honest gate before reading them
    waits.idle();
    return this;
  }

  public String kpiOffeneAuftraege() {
    return waits.visible(css("dashboard.kpiOffeneAuftraege")).getText();
  }

  public String kpiFertigeAuftraege() {
    return waits.visible(css("dashboard.kpiFertigeAuftraege")).getText();
  }

  public String kpiOffeneRechnungen() {
    return waits.visible(css("dashboard.kpiOffeneRechnungen")).getText();
  }

  /**
   * The two dashboard tables have no ids in the 2016 markup — the selectors are positional (column
   * div order). Tests MUST pin the section headings below so a silent reordering fails loudly
   * instead of asserting the wrong table.
   */
  public String werkstattHeading() {
    return waits.visible(css("dashboard.werkstattHeading")).getText();
  }

  public String abholbereitHeading() {
    return waits.visible(css("dashboard.abholbereitHeading")).getText();
  }

  public List<String> werkstattHeaderTexte() {
    return headerTexte("dashboard.werkstattHeaderCells");
  }

  public List<String> abholbereitHeaderTexte() {
    return headerTexte("dashboard.abholbereitHeaderCells");
  }

  /** Cell texts of the "In der Werkstatt" rows: [Auftrag, Kennzeichen, Kunde, Status]. */
  public List<List<String>> werkstattZeilen() {
    return zeilen("dashboard.werkstattRows");
  }

  /** Cell texts of the "Abholbereit" rows: [Auftrag, Kennzeichen, Kunde, Fertig seit]. */
  public List<List<String>> abholbereitZeilen() {
    return zeilen("dashboard.abholbereitRows");
  }

  private List<String> headerTexte(String key) {
    return waits.allVisible(css(key)).stream().map(WebElement::getText).toList();
  }

  private List<List<String>> zeilen(String rowsKey) {
    return driver.findElements(css(rowsKey)).stream()
        .map(
            row ->
                row.findElements(css("dashboard.rowCells")).stream()
                    .map(WebElement::getText)
                    .toList())
        .toList();
  }
}
