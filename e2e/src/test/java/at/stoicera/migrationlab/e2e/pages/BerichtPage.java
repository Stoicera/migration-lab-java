package at.stoicera.migrationlab.e2e.pages;

import static at.stoicera.migrationlab.e2e.selectors.SelectorMap.css;

import java.util.List;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

import at.stoicera.migrationlab.e2e.support.Waits;

public class BerichtPage {

	private final WebDriver driver;
	private final Waits waits;

	public BerichtPage(WebDriver driver, Waits waits) {
		this.driver = driver;
		this.waits = waits;
	}

	public BerichtPage open() {
		// full page load first: clicking the CURRENT route's nav link does not
		// re-instantiate the view in ngRoute (found during stabilisation, flaky log #2)
		driver.get(at.stoicera.migrationlab.e2e.config.TestConfig.baseUrl() + "/");
		waits.clickable(css("nav.bericht")).click();
		waits.countIs(css("bericht.monthRows"), 12);
		return this;
	}

	public BerichtPage jahr(String jahr) {
		new Select(waits.visible(css("bericht.yearSelect"))).selectByVisibleText(jahr);
		waits.countIs(css("bericht.monthRows"), 12);
		return this;
	}

	/** Cells of one month row: [Monat, Aufträge, Rechnungen, netto, brutto]. */
	public List<String> monthRow(String monatName) {
		for (WebElement row : driver.findElements(css("bericht.monthRows"))) {
			List<WebElement> cells = row.findElements(org.openqa.selenium.By.cssSelector("td"));
			if (!cells.isEmpty() && cells.get(0).getText().equals(monatName)) {
				return cells.stream().map(WebElement::getText).toList();
			}
		}
		throw new AssertionError("Month row not found: " + monatName);
	}

	/** Cells of the first top-customer row: [#, Kunde, Aufträge, Umsatz]. */
	public List<String> topKundeErsteZeile() {
		waits.countAtLeast(css("bericht.topKundenRows"), 1);
		WebElement first = driver.findElements(css("bericht.topKundenRows")).get(0);
		return first.findElements(org.openqa.selenium.By.cssSelector("td")).stream()
				.map(WebElement::getText).toList();
	}
}
