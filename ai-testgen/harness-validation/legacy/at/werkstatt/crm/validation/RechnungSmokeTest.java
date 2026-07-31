package at.werkstatt.crm.validation;

import static org.assertj.core.api.Assertions.assertThat;

import at.werkstatt.crm.model.Rechnung;
import org.junit.jupiter.api.Test;

/**
 * Harness validation, stratum shape S4 (pure data holder — the negative control). See
 * WerkstattServiceSmokeTest.
 */
class RechnungSmokeTest {

  @Test
  void getter_geben_zurueck_was_die_setter_bekommen_haben() {
    Rechnung rechnung = new Rechnung();
    rechnung.setRechnungNr("R-2026-0009");
    rechnung.setSummeNetto(117.00d);
    rechnung.setBezahlt(true);

    assertThat(rechnung.getRechnungNr()).isEqualTo("R-2026-0009");
    assertThat(rechnung.getSummeNetto()).isEqualTo(117.00d);
    assertThat(rechnung.isBezahlt()).isTrue();
  }
}
