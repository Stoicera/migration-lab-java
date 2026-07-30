package at.stoicera.migrationlab.e2e.tests;

import static org.assertj.core.api.Assertions.assertThat;

import at.stoicera.migrationlab.e2e.pages.AdminPage;
import at.stoicera.migrationlab.e2e.support.ScenarioTest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

/**
 * Scenario: the boss's admin page — Kennzahlen from the seed and the destructive 90-day cleanup
 * behind its confirm dialog. Closed a review-session-10 gap: after SD-2 the modern admin UI was
 * pinned by NOTHING (the characterization fork proves the shell and the JSON, not the page), so it
 * could have been deleted outright with every suite green. One scenario, both stands, via the
 * selector map — JSP and SPA implement the same flow.
 *
 * <p>The cleanup count is date-dependent by design (STORNIERT older than 90 days: 1 seed order
 * today, 2 from mid-October 2026), so the test derives the expected table delta from the meldung
 * instead of hard-coding it — same technique as the DB-state characterization pin.
 */
@DisplayName("Admin: Kennzahlen aus dem Seed, Bereinigung mit Confirm und Meldung")
class AdminTest extends ScenarioTest {

  private static final Pattern MELDUNG =
      Pattern.compile("(\\d+) stornierte Aufträge wurden endgültig gelöscht\\.");

  @Test
  @Order(1)
  void kennzahlenAusDemSeed() {
    AdminPage admin = new AdminPage(driver, waits).open();
    assertThat(admin.kennzahl("Kunden")).isEqualTo("10");
    assertThat(admin.kennzahl("Fahrzeuge")).isEqualTo("13");
    assertThat(admin.kennzahl("Aufträge gesamt")).isEqualTo("16");
    assertThat(admin.kennzahl("Aufträge offen")).isEqualTo("4");
    assertThat(admin.kennzahl("Rechnungen gesamt")).isEqualTo("8");
    assertThat(admin.kennzahl("Rechnungen unbezahlt")).isEqualTo("1");
  }

  @Test
  @Order(2)
  void bereinigungLoeschtHinterConfirmUndMeldetExakt() {
    AdminPage admin = new AdminPage(driver, waits).open();
    int vorher = Integer.parseInt(admin.kennzahl("Aufträge gesamt"));

    String meldung = admin.bereinigen();
    Matcher m = MELDUNG.matcher(meldung);
    assertThat(m.matches()).as("meldung format: '%s'", meldung).isTrue();
    int geloescht = Integer.parseInt(m.group(1));
    assertThat(geloescht).isGreaterThanOrEqualTo(1);

    // the Kennzahlen refresh reflects the delete (JSP: re-rendered page; SPA: async reload)
    admin.waitKennzahl("Aufträge gesamt", String.valueOf(vorher - geloescht));
  }
}
