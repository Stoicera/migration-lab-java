package at.stoicera.migrationlab.e2e.pages;

import static at.stoicera.migrationlab.e2e.selectors.SelectorMap.css;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

import at.stoicera.migrationlab.e2e.support.Waits;

public class KundeDetailPage {

	private final WebDriver driver;
	private final Waits waits;

	public KundeDetailPage(WebDriver driver, Waits waits) {
		this.driver = driver;
		this.waits = waits;
	}

	public KundeDetailPage anrede(String wert) {
		new Select(waits.visible(css("kundeDetail.anrede"))).selectByVisibleText(wert);
		return this;
	}

	public KundeDetailPage vorname(String wert) {
		return type("kundeDetail.vorname", wert);
	}

	public KundeDetailPage nachname(String wert) {
		return type("kundeDetail.nachname", wert);
	}

	public KundeDetailPage telefon(String wert) {
		return type("kundeDetail.telefon", wert);
	}

	public KundeDetailPage ort(String wert) {
		return type("kundeDetail.ort", wert);
	}

	private KundeDetailPage type(String key, String wert) {
		WebElement feld = waits.visible(css(key));
		feld.clear();
		feld.sendKeys(wert);
		return this;
	}

	/**
	 * Saves and waits for the round trip to COMPLETE. The heading check alone
	 * is vacuous for edits (it already shows the name before saving) and the
	 * legacy UI gives no save feedback — without the idle wait, navigating on
	 * can abort the in-flight PUT and lose the update (flaky log #3, found in CI).
	 */
	public KundeDetailPage speichern(String erwarteterAnzeigeName) {
		waits.clickable(css("kundeDetail.saveButton")).click();
		waits.angularIdle();
		waits.textIn(css("kundeDetail.heading"), erwarteterAnzeigeName);
		return this;
	}

	public String telefonValue() {
		return waits.visible(css("kundeDetail.telefon")).getDomProperty("value");
	}

	public String heading() {
		return waits.visible(css("kundeDetail.heading")).getText();
	}
}
