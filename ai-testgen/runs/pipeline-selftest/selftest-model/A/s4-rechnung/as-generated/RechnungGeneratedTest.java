package at.werkstatt.crm.gen;

import static org.assertj.core.api.Assertions.assertThat;

import at.werkstatt.crm.model.Rechnung;
import org.junit.jupiter.api.Test;

class RechnungGeneratedTest {
  @Test
  void passes() {
    Rechnung r = new Rechnung();
    r.setRechnungNr("R-1");
    assertThat(r.getRechnungNr()).isEqualTo("R-1");
  }

  @Test
  void fails_on_purpose() {
    assertThat(new Rechnung().isBezahlt()).isTrue();
  }
}
