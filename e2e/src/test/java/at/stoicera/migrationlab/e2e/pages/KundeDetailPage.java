package at.stoicera.migrationlab.e2e.pages;

import static at.stoicera.migrationlab.e2e.selectors.SelectorMap.css;

import java.util.List;

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

	/**
	 * Data-load gate for EXISTING customers (flaky log race: kunde-detail.html
	 * has no ng-if around the form — the empty template renders before the
	 * async GET populates the model). Reading before this gate returns empty
	 * strings; typing before it is worse: the late response overwrites the
	 * model and silently discards the input. Every navigation to an existing
	 * customer must pass through here (KundenPage.openByName does). The
	 * new-customer form (KundenPage.neuerKunde) is legitimately empty and must
	 * NOT use this gate.
	 */
	public KundeDetailPage waitLoaded() {
		// both GETs (kunde + fahrzeuge) done -> model applied, vehicle table rendered
		waits.idle();
		waits.until(css("kundeDetail.nachname"), feld -> {
			String value = feld.getDomProperty("value");
			return value != null && !value.isEmpty();
		});
		return this;
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
		waits.idle();
		waits.textIn(css("kundeDetail.heading"), erwarteterAnzeigeName);
		return this;
	}

	/**
	 * Saves expecting the client-side validation alert (e.g. missing Nachname);
	 * returns the alert text so the test pins the exact message. No idle wait:
	 * the validation fires before any request.
	 */
	public String speichernErwarteAlert() {
		waits.clickable(css("kundeDetail.saveButton")).click();
		return waits.alertTextAndAccept();
	}

	// --- master data reads (only meaningful after waitLoaded() on existing customers) ---

	public String anredeValue() {
		return new Select(waits.visible(css("kundeDetail.anrede"))).getFirstSelectedOption().getText();
	}

	public String vornameValue() {
		return fieldValue("kundeDetail.vorname");
	}

	public String nachnameValue() {
		return fieldValue("kundeDetail.nachname");
	}

	public String telefonValue() {
		return fieldValue("kundeDetail.telefon");
	}

	public String emailValue() {
		return fieldValue("kundeDetail.email");
	}

	public String strasseValue() {
		return fieldValue("kundeDetail.strasse");
	}

	public String plzValue() {
		return fieldValue("kundeDetail.plz");
	}

	public String ortValue() {
		return fieldValue("kundeDetail.ort");
	}

	public String notizValue() {
		return fieldValue("kundeDetail.notiz");
	}

	private String fieldValue(String key) {
		return waits.visible(css(key)).getDomProperty("value");
	}

	public String heading() {
		return waits.visible(css("kundeDetail.heading")).getText();
	}

	// --- vehicles of this customer (right column of kunde-detail) ---

	/** Header texts of the vehicle table — tests pin these before any cell-position access. */
	public List<String> fahrzeugHeaderTexte() {
		return waits.allVisible(css("kundeDetail.fahrzeugHeaderCells")).stream()
				.map(WebElement::getText).toList();
	}

	public int fahrzeugAnzahl() {
		return driver.findElements(css("kundeDetail.fahrzeugRows")).size();
	}

	/** Cell texts of all vehicle rows: [Kennzeichen, Fahrzeug, Baujahr, Pickerl, delete]. */
	public List<List<String>> fahrzeugZeilen() {
		return driver.findElements(css("kundeDetail.fahrzeugRows")).stream()
				.map(row -> row.findElements(css("kundeDetail.fahrzeugRowCells")).stream()
						.map(WebElement::getText).toList())
				.toList();
	}

	public KundeDetailPage fahrzeugHinzufuegen(String kennzeichen, String marke, String modell,
			String baujahr, String kmStand) {
		int before = fahrzeugAnzahl();
		waits.clickable(css("kundeDetail.fahrzeugFormToggle")).click();
		type("kundeDetail.fahrzeugKennzeichen", kennzeichen);
		type("kundeDetail.fahrzeugMarke", marke);
		type("kundeDetail.fahrzeugModell", modell);
		type("kundeDetail.fahrzeugBaujahr", baujahr);
		type("kundeDetail.fahrzeugKm", kmStand);
		waits.clickable(css("kundeDetail.fahrzeugSaveButton")).click();
		// POST + reload GET land in one model swap: the new row appearing IS the round trip
		waits.countIs(css("kundeDetail.fahrzeugRows"), before + 1);
		return this;
	}

	public KundeDetailPage fahrzeugLoeschen(String kennzeichen) {
		int before = fahrzeugAnzahl();
		for (WebElement row : driver.findElements(css("kundeDetail.fahrzeugRows"))) {
			List<WebElement> cells = row.findElements(css("kundeDetail.fahrzeugRowCells"));
			// Kennzeichen is the first column — pinned by the header assertion in the tests
			if (!cells.isEmpty() && cells.get(0).getText().equals(kennzeichen)) {
				row.findElement(css("kundeDetail.fahrzeugRowDeleteButton")).click();
				waits.alertAndAccept();
				waits.countIs(css("kundeDetail.fahrzeugRows"), before - 1);
				return this;
			}
		}
		throw new AssertionError("Fahrzeug not in customer's list: " + kennzeichen);
	}
}
