package at.stoicera.migrationlab.e2e.support;

import java.time.Year;

/**
 * Expectations derived from the committed seed (legacy/db/init/02-daten.sql)
 * that depend on the WALL-CLOCK year — kept in one place so no test hard-codes
 * a year-coupled value.
 *
 * The coupling: the backend builds document numbers as
 * {@code <prefix>-<current year>-<max+1 within that year>}
 * (Calendar.getInstance() in WerkstattService.neuerAuftrag/erstelleRechnung).
 * The seed contains orders A-2026-0001..0016 and invoices R-2026-0001..0008,
 * all dated 2026. Therefore, after a DB reset:
 * in 2026 the next numbers are A-2026-0017 / R-2026-0009; in any later year the
 * year block is empty and numbering restarts at 0001. Assumes the test JVM and
 * the stand agree on the calendar year — given for a local/CI stand; the only
 * theoretical mismatch is a run across New Year's midnight.
 *
 * The seed's FROZEN report data (months, invoices, top customers) is all dated
 * 2026 — report tests always select year 2026 explicitly, which stays possible
 * forever because the legacy year dropdown is built from the current year down
 * to 2016 (bericht-controller.js).
 */
public final class Seed {

	/** All seed documents are dated in this year; frozen forever. */
	public static final String BERICHT_JAHR = "2026";

	/** Orders in the seed (A-2026-0001..0016). */
	public static final int ANZAHL_AUFTRAEGE = 16;

	/** "Gesamt / Aufträge" cell of the 2026 report on a fresh seed. */
	public static final String BERICHT_2026_GESAMT_AUFTRAEGE = "16";

	/** "Gesamt / Aufträge" cell for any year WITHOUT seed data (fresh seed). */
	public static final String BERICHT_LEERES_JAHR_GESAMT_AUFTRAEGE = "0";

	private Seed() {
	}

	/** Number the backend will assign to the next created order (see class doc). */
	public static String naechsteAuftragNr() {
		int jahr = Year.now().getValue();
		return "A-" + jahr + "-" + (jahr == 2026 ? "0017" : "0001");
	}

	/** Number the backend will assign to the next created invoice (see class doc). */
	public static String naechsteRechnungNr() {
		int jahr = Year.now().getValue();
		return "R-" + jahr + "-" + (jahr == 2026 ? "0009" : "0001");
	}
}
