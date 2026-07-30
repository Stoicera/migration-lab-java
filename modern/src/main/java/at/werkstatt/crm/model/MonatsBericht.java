package at.werkstatt.crm.model;

public class MonatsBericht {

  private int jahr;
  private int monat;
  private int anzahlAuftraege;
  private int anzahlRechnungen;
  private double umsatzNetto;
  private double umsatzBrutto;

  public MonatsBericht() {}

  public int getJahr() {
    return jahr;
  }

  public void setJahr(int jahr) {
    this.jahr = jahr;
  }

  public int getMonat() {
    return monat;
  }

  public void setMonat(int monat) {
    this.monat = monat;
  }

  public int getAnzahlAuftraege() {
    return anzahlAuftraege;
  }

  public void setAnzahlAuftraege(int anzahlAuftraege) {
    this.anzahlAuftraege = anzahlAuftraege;
  }

  public int getAnzahlRechnungen() {
    return anzahlRechnungen;
  }

  public void setAnzahlRechnungen(int anzahlRechnungen) {
    this.anzahlRechnungen = anzahlRechnungen;
  }

  public double getUmsatzNetto() {
    return umsatzNetto;
  }

  public void setUmsatzNetto(double umsatzNetto) {
    this.umsatzNetto = umsatzNetto;
  }

  public double getUmsatzBrutto() {
    return umsatzBrutto;
  }

  public void setUmsatzBrutto(double umsatzBrutto) {
    this.umsatzBrutto = umsatzBrutto;
  }

  // Monatsname fuer die Anzeige im Bericht
  public String getMonatName() {
    switch (monat) {
      case 1:
        return "Jänner";
      case 2:
        return "Februar";
      case 3:
        return "März";
      case 4:
        return "April";
      case 5:
        return "Mai";
      case 6:
        return "Juni";
      case 7:
        return "Juli";
      case 8:
        return "August";
      case 9:
        return "September";
      case 10:
        return "Oktober";
      case 11:
        return "November";
      case 12:
        return "Dezember";
      default:
        return "?";
    }
  }
}
