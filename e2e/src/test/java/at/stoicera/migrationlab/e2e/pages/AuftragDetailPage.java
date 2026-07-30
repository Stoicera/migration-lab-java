package at.stoicera.migrationlab.e2e.pages;

import static at.stoicera.migrationlab.e2e.selectors.SelectorMap.css;

import at.stoicera.migrationlab.e2e.support.Waits;
import java.util.List;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

public class AuftragDetailPage {

  private final WebDriver driver;
  private final Waits waits;

  public AuftragDetailPage(WebDriver driver, Waits waits) {
    this.driver = driver;
    this.waits = waits;
  }

  /** e.g. "A-2026-0017", parsed from the heading "Auftrag A-2026-0017 ...". */
  public String auftragNr() {
    String heading = waits.visible(css("auftragDetail.heading")).getText();
    for (String word : heading.split("\\s+")) {
      if (word.matches("A-\\d{4}-\\d{4}")) {
        return word;
      }
    }
    throw new AssertionError("No Auftrag number in heading: " + heading);
  }

  public String status() {
    return waits.visible(css("auftragDetail.statusLabel")).getText();
  }

  public AuftragDetailPage inArbeit() {
    waits.clickable(css("auftragDetail.inArbeitButton")).click();
    waits.textIn(css("auftragDetail.statusLabel"), "In Arbeit");
    return this;
  }

  public AuftragDetailPage fertig() {
    waits.clickable(css("auftragDetail.fertigButton")).click();
    waits.textIn(css("auftragDetail.statusLabel"), "Fertig");
    return this;
  }

  public AuftragDetailPage abgeholt() {
    waits.clickable(css("auftragDetail.abgeholtButton")).click();
    waits.textIn(css("auftragDetail.statusLabel"), "Abgeholt");
    return this;
  }

  public AuftragDetailPage stornieren() {
    waits.clickable(css("auftragDetail.stornoButton")).click();
    waits.textIn(css("auftragDetail.statusLabel"), "Storniert");
    return this;
  }

  /** FERTIG → IN_ARBEIT special case ("doch noch was gefunden"). */
  public AuftragDetailPage zurueckInArbeit() {
    waits.clickable(css("auftragDetail.wiederArbeitButton")).click();
    waits.textIn(css("auftragDetail.statusLabel"), "In Arbeit");
    return this;
  }

  /** Header texts of the position table — tests pin these before any cell-position access. */
  public List<String> posHeaderTexte() {
    return waits.allVisible(css("auftragDetail.posHeaderCells")).stream()
        .map(WebElement::getText)
        .toList();
  }

  public AuftragDetailPage addPosition(
      String typ, String bezeichnung, String menge, String einzelpreis) {
    int before = driver.findElements(css("auftragDetail.posRows")).size();
    new Select(waits.visible(css("auftragDetail.posTyp"))).selectByValue(typ);
    type("auftragDetail.posBezeichnung", bezeichnung);
    type("auftragDetail.posMenge", menge);
    type("auftragDetail.posPreis", einzelpreis);
    waits.clickable(css("auftragDetail.posSaveButton")).click();
    // POST + reload GET land in one model swap: row count AND sum update together
    waits.countIs(css("auftragDetail.posRows"), before + 1);
    return this;
  }

  public AuftragDetailPage removePosition(String bezeichnung) {
    int before = driver.findElements(css("auftragDetail.posRows")).size();
    for (WebElement row : driver.findElements(css("auftragDetail.posRows"))) {
      if (row.findElement(css("auftragDetail.posRowBezeichnungCell"))
          .getText()
          .equals(bezeichnung)) {
        // no confirm dialog on position delete in the legacy UI (unlike vehicles/customers)
        row.findElement(css("auftragDetail.posRowDeleteButton")).click();
        waits.countIs(css("auftragDetail.posRows"), before - 1);
        return this;
      }
    }
    throw new AssertionError("Position not in table: " + bezeichnung);
  }

  public String summeNetto() {
    return waits.visible(css("auftragDetail.summeNetto")).getText();
  }

  public RechnungDetailPage rechnungErstellen() {
    waits.clickable(css("auftragDetail.rechnungButton")).click();
    waits.alertAndAccept();
    waits.visible(css("rechnungDetail.nummer"));
    return new RechnungDetailPage(driver, waits);
  }

  /**
   * Clicks "Rechnung erstellen" expecting the SERVER to reject it (e.g. the order already has an
   * invoice). Dialog sequence: confirm first, then the error alert with the server message — which
   * is returned for exact pinning.
   */
  public String rechnungErstellenErwarteFehler() {
    waits.clickable(css("auftragDetail.rechnungButton")).click();
    waits.alertAndAccept();
    return waits.alertTextAndAccept();
  }

  private void type(String key, String wert) {
    WebElement feld = waits.visible(css(key));
    feld.clear();
    feld.sendKeys(wert);
  }
}
