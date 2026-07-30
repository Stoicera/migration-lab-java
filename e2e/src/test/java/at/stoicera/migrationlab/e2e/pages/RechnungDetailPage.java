package at.stoicera.migrationlab.e2e.pages;

import static at.stoicera.migrationlab.e2e.selectors.SelectorMap.css;

import org.openqa.selenium.WebDriver;

import at.stoicera.migrationlab.e2e.support.Waits;

public class RechnungDetailPage {

	private final Waits waits;

	public RechnungDetailPage(WebDriver driver, Waits waits) {
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
}
