package at.stoicera.migrationlab.e2e.pages;

import static at.stoicera.migrationlab.e2e.selectors.SelectorMap.css;

import java.util.Optional;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import at.stoicera.migrationlab.e2e.support.Waits;

public class AuftraegePage {

	private final WebDriver driver;
	private final Waits waits;

	public AuftraegePage(WebDriver driver, Waits waits) {
		this.driver = driver;
		this.waits = waits;
	}

	public AuftraegePage open() {
		// full page load first: clicking the CURRENT route's nav link does not
		// re-instantiate the view in ngRoute (found during stabilisation, flaky log #2)
		driver.get(at.stoicera.migrationlab.e2e.config.TestConfig.baseUrl() + "/");
		waits.clickable(css("nav.auftraege")).click();
		waits.visible(css("auftraege.newButton"));
		waits.countAtLeast(css("auftraege.rows"), 1);
		return this;
	}

	public AuftragNeuPage neuerAuftrag() {
		waits.clickable(css("auftraege.newButton")).click();
		waits.visible(css("auftragNeu.kundeSelect"));
		return new AuftragNeuPage(driver, waits);
	}

	public String statusOf(String auftragNr) {
		return row(auftragNr).findElement(css("auftraege.rowStatusLabel")).getText();
	}

	/** Row count of the settled list (open() already waited for the load). */
	public int rowCount() {
		return driver.findElements(css("auftraege.rows")).size();
	}

	public AuftragDetailPage openAuftrag(String auftragNr) {
		row(auftragNr).click();
		waits.visible(css("auftragDetail.statusLabel"));
		return new AuftragDetailPage(driver, waits);
	}

	private WebElement row(String auftragNr) {
		Optional<WebElement> row = driver.findElements(css("auftraege.rows")).stream()
				.filter(r -> r.findElement(css("auftraege.rowNumberCell")).getText().equals(auftragNr))
				.findFirst();
		return row.orElseThrow(() -> new AssertionError("Auftrag not in list: " + auftragNr));
	}
}
