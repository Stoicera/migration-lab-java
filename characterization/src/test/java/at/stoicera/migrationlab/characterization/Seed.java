package at.stoicera.migrationlab.characterization;

import java.time.Year;

/**
 * Expectations that depend on the committed seed (legacy/db/init/02-daten.sql)
 * AND on the wall clock.
 *
 * Calendar coupling: the application builds Auftrag/Rechnung numbers from the
 * CURRENT year (Calendar.getInstance().get(YEAR)) and MAX+1 within that year.
 * The seed contains orders A-2026-0001..0016 and invoices R-2026-0001..0008 —
 * all dated 2026, none in any other year. So while the suite runs in 2026 the
 * next numbers are A-2026-0017 / R-2026-0009; in any later year the MAX+1
 * scan finds nothing and the numbering restarts at A-&lt;year&gt;-0001 /
 * R-&lt;year&gt;-0001. Computing the expectation here (instead of hard-coding
 * 2026 values) keeps the suite green after New Year without touching a test.
 */
final class Seed {

	/** every auftrag/rechnung row in the committed seed is dated in this year */
	private static final int SEED_JAHR = 2026;
	private static final int MAX_AUFTRAG_LAUFNR_IM_SEED = 16;
	private static final int MAX_RECHNUNG_LAUFNR_IM_SEED = 8;

	private Seed() {
	}

	/** the auftrag_nr the app will assign to the next created order */
	static String naechsteAuftragNr() {
		return naechsteNr("A", MAX_AUFTRAG_LAUFNR_IM_SEED);
	}

	/** the rechnung_nr the app will assign to the next created invoice */
	static String naechsteRechnungNr() {
		return naechsteNr("R", MAX_RECHNUNG_LAUFNR_IM_SEED);
	}

	private static String naechsteNr(String praefix, int maxLaufnrImSeed) {
		int jahr = Year.now().getValue();
		int laufnr = jahr == SEED_JAHR ? maxLaufnrImSeed + 1 : 1;
		return praefix + "-" + jahr + "-" + String.format("%04d", laufnr);
	}
}
