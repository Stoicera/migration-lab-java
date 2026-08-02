package at.werkstatt.crm.gen;

import static org.assertj.core.api.Assertions.assertThat;

import at.werkstatt.crm.model.Rechnung;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RechnungGeneratedTest {

  private Rechnung rechnung;

  @BeforeEach
  void setUp() {
    rechnung = new Rechnung();
  }

  @Test
  void defaultConstructor_initializesFieldsToDefaults() {
    assertThat(rechnung.getId()).isNull();
    assertThat(rechnung.getRechnungNr()).isNull();
    assertThat(rechnung.getAuftragId()).isNull();
    assertThat(rechnung.getAusgestelltAm()).isNull();
    assertThat(rechnung.getSummeNetto()).isEqualTo(0.0);
    assertThat(rechnung.getUst()).isEqualTo(0.0);
    assertThat(rechnung.getSummeBrutto()).isEqualTo(0.0);
    assertThat(rechnung.isBezahlt()).isFalse();
    assertThat(rechnung.getBezahltAm()).isNull();
    assertThat(rechnung.getAuftragNr()).isNull();
    assertThat(rechnung.getKundeName()).isNull();
  }

  @Test
  void setAndGetId() {
    rechnung.setId(42L);
    assertThat(rechnung.getId()).isEqualTo(42L);
  }

  @Test
  void setAndGetId_null() {
    rechnung.setId(1L);
    rechnung.setId(null);
    assertThat(rechnung.getId()).isNull();
  }

  @Test
  void setAndGetRechnungNr() {
    rechnung.setRechnungNr("RE-2024-001");
    assertThat(rechnung.getRechnungNr()).isEqualTo("RE-2024-001");
  }

  @Test
  void setAndGetRechnungNr_null() {
    rechnung.setRechnungNr("RE-1");
    rechnung.setRechnungNr(null);
    assertThat(rechnung.getRechnungNr()).isNull();
  }

  @Test
  void setAndGetAuftragId() {
    rechnung.setAuftragId(7L);
    assertThat(rechnung.getAuftragId()).isEqualTo(7L);
  }

  @Test
  void setAndGetAuftragId_null() {
    rechnung.setAuftragId(7L);
    rechnung.setAuftragId(null);
    assertThat(rechnung.getAuftragId()).isNull();
  }

  @Test
  void setAndGetAusgestelltAm() {
    Date now = new Date();
    rechnung.setAusgestelltAm(now);
    assertThat(rechnung.getAusgestelltAm()).isEqualTo(now);
  }

  @Test
  void setAndGetAusgestelltAm_null() {
    rechnung.setAusgestelltAm(new Date());
    rechnung.setAusgestelltAm(null);
    assertThat(rechnung.getAusgestelltAm()).isNull();
  }

  @Test
  void setAndGetSummeNetto() {
    rechnung.setSummeNetto(123.45);
    assertThat(rechnung.getSummeNetto()).isEqualTo(123.45);
  }

  @Test
  void setAndGetSummeNetto_negative() {
    rechnung.setSummeNetto(-50.0);
    assertThat(rechnung.getSummeNetto()).isEqualTo(-50.0);
  }

  @Test
  void setAndGetUst() {
    rechnung.setUst(20.5);
    assertThat(rechnung.getUst()).isEqualTo(20.5);
  }

  @Test
  void setAndGetUst_zero() {
    rechnung.setUst(0.0);
    assertThat(rechnung.getUst()).isEqualTo(0.0);
  }

  @Test
  void setAndGetSummeBrutto() {
    rechnung.setSummeBrutto(200.0);
    assertThat(rechnung.getSummeBrutto()).isEqualTo(200.0);
  }

  @Test
  void setAndGetSummeBrutto_negative() {
    rechnung.setSummeBrutto(-1.0);
    assertThat(rechnung.getSummeBrutto()).isEqualTo(-1.0);
  }

  @Test
  void setAndIsBezahlt_true() {
    rechnung.setBezahlt(true);
    assertThat(rechnung.isBezahlt()).isTrue();
  }

  @Test
  void setAndIsBezahlt_false() {
    rechnung.setBezahlt(true);
    rechnung.setBezahlt(false);
    assertThat(rechnung.isBezahlt()).isFalse();
  }

  @Test
  void setAndGetBezahltAm() {
    Date d = new Date();
    rechnung.setBezahltAm(d);
    assertThat(rechnung.getBezahltAm()).isEqualTo(d);
  }

  @Test
  void setAndGetBezahltAm_null() {
    rechnung.setBezahltAm(new Date());
    rechnung.setBezahltAm(null);
    assertThat(rechnung.getBezahltAm()).isNull();
  }

  @Test
  void setAndGetAuftragNr() {
    rechnung.setAuftragNr("AU-100");
    assertThat(rechnung.getAuftragNr()).isEqualTo("AU-100");
  }

  @Test
  void setAndGetAuftragNr_null() {
    rechnung.setAuftragNr("AU-100");
    rechnung.setAuftragNr(null);
    assertThat(rechnung.getAuftragNr()).isNull();
  }

  @Test
  void setAndGetKundeName() {
    rechnung.setKundeName("Max Mustermann");
    assertThat(rechnung.getKundeName()).isEqualTo("Max Mustermann");
  }

  @Test
  void setAndGetKundeName_null() {
    rechnung.setKundeName("Max Mustermann");
    rechnung.setKundeName(null);
    assertThat(rechnung.getKundeName()).isNull();
  }

  @Test
  void allFieldsTogether_independentState() {
    Date ausgestellt = new Date(1000L);
    Date bezahltAm = new Date(2000L);

    rechnung.setId(1L);
    rechnung.setRechnungNr("RE-1");
    rechnung.setAuftragId(2L);
    rechnung.setAusgestelltAm(ausgestellt);
    rechnung.setSummeNetto(100.0);
    rechnung.setUst(20.0);
    rechnung.setSummeBrutto(120.0);
    rechnung.setBezahlt(true);
    rechnung.setBezahltAm(bezahltAm);
    rechnung.setAuftragNr("AU-1");
    rechnung.setKundeName("Kunde X");

    assertThat(rechnung.getId()).isEqualTo(1L);
    assertThat(rechnung.getRechnungNr()).isEqualTo("RE-1");
    assertThat(rechnung.getAuftragId()).isEqualTo(2L);
    assertThat(rechnung.getAusgestelltAm()).isEqualTo(ausgestellt);
    assertThat(rechnung.getSummeNetto()).isEqualTo(100.0);
    assertThat(rechnung.getUst()).isEqualTo(20.0);
    assertThat(rechnung.getSummeBrutto()).isEqualTo(120.0);
    assertThat(rechnung.isBezahlt()).isTrue();
    assertThat(rechnung.getBezahltAm()).isEqualTo(bezahltAm);
    assertThat(rechnung.getAuftragNr()).isEqualTo("AU-1");
    assertThat(rechnung.getKundeName()).isEqualTo("Kunde X");
  }
}
