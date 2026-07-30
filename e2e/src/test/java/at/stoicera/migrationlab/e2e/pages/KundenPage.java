package at.stoicera.migrationlab.e2e.pages;

import static at.stoicera.migrationlab.e2e.selectors.SelectorMap.css;

import java.util.List;
import java.util.Optional;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import at.stoicera.migrationlab.e2e.support.Waits;

public class KundenPage {

	private final WebDriver driver;
	private final Waits waits;

	public KundenPage(WebDriver driver, Waits waits) {
		this.driver = driver;
		this.waits = waits;
	}

	public KundenPage open() {
		// full page load first: clicking the CURRENT route's nav link does not
		// re-instantiate the view in ngRoute (found during stabilisation, flaky log #2)
		driver.get(at.stoicera.migrationlab.e2e.config.TestConfig.baseUrl() + "/");
		waits.clickable(css("nav.kunden")).click();
		waits.visible(css("kunden.searchField"));
		// settle the initial list load BEFORE any interaction: the legacy UI has
		// no request cancellation, two in-flight loads re-render last-response-wins
		// (found during stabilisation — see playbook ch. 1, flaky log entry #1)
		waits.countAtLeast(css("kunden.rows"), 1);
		return this;
	}

	public KundenPage search(String begriff) {
		WebElement feld = waits.visible(css("kunden.searchField"));
		feld.clear();
		feld.sendKeys(begriff);
		waits.clickable(css("kunden.searchButton")).click();
		return this;
	}

	public List<String> namesInList() {
		return driver.findElements(css("kunden.rows")).stream()
				.map(row -> row.findElement(css("kunden.rowNameLink")).getText())
				.toList();
	}

	public int rowCount() {
		return driver.findElements(css("kunden.rows")).size();
	}

	public void waitForRowCount(int expected) {
		waits.countIs(css("kunden.rows"), expected);
	}

	public KundeDetailPage neuerKunde() {
		waits.clickable(css("kunden.newButton")).click();
		// new-customer form: legitimately empty, so NO waitLoaded() here
		waits.visible(css("kundeDetail.nachname"));
		return new KundeDetailPage(driver, waits);
	}

	public KundeDetailPage openByName(String anzeigeName) {
		Optional<WebElement> link = driver.findElements(css("kunden.rows")).stream()
				.map(row -> row.findElement(css("kunden.rowNameLink")))
				.filter(a -> a.getText().equals(anzeigeName))
				.findFirst();
		link.orElseThrow(() -> new AssertionError("Kunde not in list: " + anzeigeName)).click();
		waits.visible(css("kundeDetail.nachname"));
		// existing customer: gate on the async data load before anyone reads or types
		return new KundeDetailPage(driver, waits).waitLoaded();
	}

	public void deleteByName(String anzeigeName) {
		List<WebElement> rows = driver.findElements(css("kunden.rows"));
		for (WebElement row : rows) {
			if (row.findElement(css("kunden.rowNameLink")).getText().equals(anzeigeName)) {
				row.findElement(css("kunden.rowDeleteButton")).click();
				waits.alertAndAccept();
				return;
			}
		}
		throw new AssertionError("Kunde not in list: " + anzeigeName);
	}
}
