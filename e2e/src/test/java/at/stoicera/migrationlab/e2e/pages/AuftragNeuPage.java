package at.stoicera.migrationlab.e2e.pages;

import static at.stoicera.migrationlab.e2e.selectors.SelectorMap.css;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

import at.stoicera.migrationlab.e2e.support.Waits;

public class AuftragNeuPage {

	private final WebDriver driver;
	private final Waits waits;

	public AuftragNeuPage(WebDriver driver, Waits waits) {
		this.driver = driver;
		this.waits = waits;
	}

	/**
	 * Customer options load async (GET api/kunden fired on controller start) —
	 * same guard as {@link #fahrzeug(String)}: wait for the wanted entry before
	 * selecting, selectByVisibleText on a not-yet-populated select throws.
	 */
	public AuftragNeuPage kunde(String anzeigeName) {
		waits.until(css("auftragNeu.kundeSelect"), select -> new Select(select).getOptions().stream()
				.anyMatch(o -> o.getText().equals(anzeigeName)));
		new Select(driver.findElement(css("auftragNeu.kundeSelect"))).selectByVisibleText(anzeigeName);
		return this;
	}

	/** Vehicle options load after customer selection — wait for the entry. */
	public AuftragNeuPage fahrzeug(String kennzeichen) {
		waits.until(css("auftragNeu.fahrzeugSelect"), select -> {
			Select s = new Select(select);
			return s.getOptions().stream().anyMatch(o -> o.getText().startsWith(kennzeichen));
		});
		Select select = new Select(driver.findElement(css("auftragNeu.fahrzeugSelect")));
		select.getOptions().stream()
				.filter(o -> o.getText().startsWith(kennzeichen))
				.findFirst()
				.orElseThrow(() -> new AssertionError("Fahrzeug not offered: " + kennzeichen))
				.click();
		return this;
	}

	public AuftragNeuPage beschreibung(String text) {
		WebElement feld = waits.visible(css("auftragNeu.beschreibung"));
		feld.clear();
		feld.sendKeys(text);
		return this;
	}

	public AuftragNeuPage km(int kmStand) {
		WebElement feld = waits.visible(css("auftragNeu.km"));
		feld.clear();
		feld.sendKeys(String.valueOf(kmStand));
		return this;
	}

	public AuftragDetailPage anlegen() {
		waits.clickable(css("auftragNeu.submitButton")).click();
		waits.visible(css("auftragDetail.statusLabel"));
		return new AuftragDetailPage(driver, waits);
	}

	/**
	 * Submits expecting the client-side validation alert (customer/vehicle
	 * missing); returns the alert text so the test pins the exact message.
	 */
	public String anlegenErwarteAlert() {
		waits.clickable(css("auftragNeu.submitButton")).click();
		return waits.alertTextAndAccept();
	}
}
