package at.werkstatt.crm.gen;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import at.werkstatt.crm.model.Rechnung;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class RechnungGeneratedTest {

    @Test
    @DisplayName("should create a new Rechnung with default values")
    void testDefaultConstructor() {
        Rechnung rechnung = new Rechnung();

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
    @DisplayName("should set and get all fields correctly")
    void testAllSettersAndGetters() {
        Rechnung rechnung = new Rechnung();

        Long id = 123L;
        String rechnungNr = "R2023-001";
        Long auftragId = 456L;
        Date ausgestelltAm = new Date();
        double summeNetto = 100.0;
        double ust = 20.0;
        double summeBrutto = 120.0;
        boolean bezahlt = true;
        Date bezahltAm = new Date();
        String auftragNr = "A2023-001";
        String kundeName = "Max Mustermann";

        rechnung.setId(id);
        rechnung.setRechnungNr(rechnungNr);
        rechnung.setAuftragId(auftragId);
        rechnung.setAusgestelltAm(ausgestelltAm);
        rechnung.setSummeNetto(summeNetto);
        rechnung.setUst(ust);
        rechnung.setSummeBrutto(summeBrutto);
        rechnung.setBezahlt(bezahlt);
        rechnung.setBezahltAm(bezahltAm);
        rechnung.setAuftragNr(auftragNr);
        rechnung.setKundeName(kundeName);

        assertThat(rechnung.getId()).isEqualTo(id);
        assertThat(rechnung.getRechnungNr()).isEqualTo(rechnungNr);
        assertThat(rechnung.getAuftragId()).isEqualTo(auftragId);
        assertThat(rechnung.getAusgestelltAm()).isEqualTo(ausgestelltAm);
        assertThat(rechnung.getSummeNetto()).isEqualTo(summeNetto);
        assertThat(rechnung.getUst()).isEqualTo(ust);
        assertThat(rechnung.getSummeBrutto()).isEqualTo(summeBrutto);
        assertThat(rechnung.isBezahlt()).isEqualTo(bezahlt);
        assertThat(rechnung.getBezahltAm()).isEqualTo(bezahltAm);
        assertThat(rechnung.getAuftragNr()).isEqualTo(auftragNr);
        assertThat(rechnung.getKundeName()).isEqualTo(kundeName);
    }

    @Test
    @DisplayName("should handle zero and negative amounts correctly")
    void testZeroAndNegativeAmounts() {
        Rechnung rechnung = new Rechnung();

        rechnung.setSummeNetto(0.0);
        rechnung.setUst(-10.0);
        rechnung.setSummeBrutto(-5.0);

        assertThat(rechnung.getSummeNetto()).isEqualTo(0.0);
        assertThat(rechnung.getUst()).isEqualTo(-10.0);
        assertThat(rechnung.getSummeBrutto()).isEqualTo(-5.0);
    }

    @Test
    @DisplayName("should handle null dates correctly")
    void testNullDates() {
        Rechnung rechnung = new Rechnung();

        rechnung.setAusgestelltAm(null);
        rechnung.setBezahltAm(null);

        assertThat(rechnung.getAusgestelltAm()).isNull();
        assertThat(rechnung.getBezahltAm()).isNull();
    }

    @Test
    @DisplayName("should handle empty and whitespace strings correctly")
    void testEmptyAndWhitespaceStrings() {
        Rechnung rechnung = new Rechnung();

        rechnung.setRechnungNr("");
        rechnung.setAuftragNr("   ");
        rechnung.setKundeName(null);

        assertThat(rechnung.getRechnungNr()).isEqualTo("");
        assertThat(rechnung.getAuftragNr()).isEqualTo("   ");
        assertThat(rechnung.getKundeName()).isNull();
    }

    @Test
    @DisplayName("should handle boolean bezahlt state correctly")
    void testBezahltState() {
        Rechnung rechnung = new Rechnung();

        assertThat(rechnung.isBezahlt()).isFalse();

        rechnung.setBezahlt(true);
        assertThat(rechnung.isBezahlt()).isTrue();

        rechnung.setBezahlt(false);
        assertThat(rechnung.isBezahlt()).isFalse();
    }

    @Test
    @DisplayName("should handle large numeric values correctly")
    void testLargeNumericValues() {
        Rechnung rechnung = new Rechnung();

        double largeValue = 1_000_000.0;
        rechnung.setSummeNetto(largeValue);
        rechnung.setUst(largeValue);
        rechnung.setSummeBrutto(largeValue);

        assertThat(rechnung.getSummeNetto()).isEqualTo(largeValue);
        assertThat(rechnung.getUst()).isEqualTo(largeValue);
        assertThat(rechnung.getSummeBrutto()).isEqualTo(largeValue);
    }

    @Test
    @DisplayName("should handle very small numeric values correctly")
    void testSmallNumericValues() {
        Rechnung rechnung = new Rechnung();

        double smallValue = 0.001;
        rechnung.setSummeNetto(smallValue);
        rechnung.setUst(smallValue);
        rechnung.setSummeBrutto(smallValue);

        assertThat(rechnung.getSummeNetto()).isEqualTo(smallValue);
        assertThat(rechnung.getUst()).isEqualTo(smallValue);
        assertThat(rechnung.getSummeBrutto()).isEqualTo(smallValue);
    }

    @Test
    @DisplayName("should allow setting same date for ausgestelltAm and bezahltAm")
    void testSameDateForAusgestelltAmAndBezahltAm() {
        Rechnung rechnung = new Rechnung();
        Date date = new Date();

        rechnung.setAusgestelltAm(date);
        rechnung.setBezahltAm(date);

        assertThat(rechnung.getAusgestelltAm()).isEqualTo(date);
        assertThat(rechnung.getBezahltAm()).isEqualTo(date);
    }

    @Test
    @DisplayName("should handle negative IDs correctly")
    void testNegativeIds() {
        Rechnung rechnung = new Rechnung();

        rechnung.setId(-1L);
        rechnung.setAuftragId(-100L);

        assertThat(rechnung.getId()).isEqualTo(-1L);
        assertThat(rechnung.getAuftragId()).isEqualTo(-100L);
    }
}
