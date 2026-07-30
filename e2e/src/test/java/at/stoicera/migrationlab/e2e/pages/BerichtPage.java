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
		// real gate on the FIRST load only: rows go 0 -> 12 while the initial GET renders
		waits.countIs(css("bericht.monthRows"), 12);
		// the parallel topkunden GET has no row-count gate of its own — settle it too
		waits.idle();
		return this;
	}

	/**
	 * Selects a report year and waits until the table PROVABLY shows that
	 * year's data. Two cases, both deterministic:
	 *
	 * 1. The requested year is already selected (initial controller state is the
	 *    wall-clock year, so this is the everyday case while that is 2026):
	 *    selecting the selected option fires NO change event, ng-change="laden()"
	 *    does not run — a deliberate no-op. The visible data already belongs to
	 *    that year (loaded by the controller's initial laden()).
	 *
	 * 2. A real change: ng-change fires two GETs, but waiting on the month-row
	 *    count is vacuous — the API always returns 12 rows (generate_series) and
	 *    ng-repeat re-renders them in place. The only honest gate is a cell whose
	 *    value provably differs between the two years. Callers pass the expected
	 *    "Gesamt / Aufträge" cell ({@code #summe-auftraege}): on a fresh seed it
	 *    is 16 for 2026 and 0 for every other year, so any year switch the suite
	 *    performs crosses a visible value change. The exact-text wait then also
	 *    covers case 1 for free (the cell already carries the value).
	 */
	public BerichtPage jahr(String jahr, String erwarteteGesamtAuftraege) {
		Select select = new Select(waits.visible(css("bericht.yearSelect")));
		if (!select.getFirstSelectedOption().getText().equals(jahr)) {
			select.selectByVisibleText(jahr);
		}
		waits.idle();
		waits.textIs(css("bericht.summeAuftraege"), erwarteteGesamtAuftraege);
		return this;
	}

	/** Cells of one month row: [Monat, Aufträge, Rechnungen, netto, brutto]. */
	public List<String> monthRow(String monatName) {
		for (WebElement row : driver.findElements(css("bericht.monthRows"))) {
			List<WebElement> cells = row.findElements(css("bericht.rowCells"));
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
		return first.findElements(css("bericht.rowCells")).stream()
				.map(WebElement::getText).toList();
	}
}
