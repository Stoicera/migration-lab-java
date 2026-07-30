package at.stoicera.migrationlab.e2e.pages;

import static at.stoicera.migrationlab.e2e.selectors.SelectorMap.css;

import java.util.List;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import at.stoicera.migrationlab.e2e.support.Waits;

public class RechnungDetailPage {

	private final WebDriver driver;
	private final Waits waits;

	public RechnungDetailPage(WebDriver driver, Waits waits) {
		this.driver = driver;
		this.waits = waits;
	}

	/** e.g. "Rechnung R-2026-0009" — callers assert on the contained number. */
	public String nummerText() {
		return waits.visible(css("rechnungDetail.nummer")).getText();
	}

	public String netto() {
		return waits.visible(css("rechnungDetail.netto")).getText();
	}

	public String ust() {
		return waits.visible(css("rechnungDetail.ust")).getText();
	}

	public String brutto() {
		return waits.visible(css("rechnungDetail.brutto")).getText();
	}

	/** Header texts of the position table — tests pin these before any cell-position access. */
	public List<String> posHeaderTexte() {
		return waits.allVisible(css("rechnungDetail.posHeaderCells")).stream()
				.map(WebElement::getText).toList();
	}

	/**
	 * Cell texts of all position rows: [Position, Menge, Einzelpreis, Betrag].
	 * The view loads the invoice first, then the order NESTED (second GET fires
	 * only after the first resolves) — the row wait gates on the second load;
	 * all rows of one order render in a single digest.
	 */
	public List<List<String>> positionZeilen() {
		waits.countAtLeast(css("rechnungDetail.posRows"), 1);
		return driver.findElements(css("rechnungDetail.posRows")).stream()
				.map(row -> row.findElements(css("rechnungDetail.rowCells")).stream()
						.map(WebElement::getText).toList())
				.toList();
	}
}
