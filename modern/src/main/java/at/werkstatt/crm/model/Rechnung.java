package at.werkstatt.crm.model;

import java.util.Date;

public class Rechnung {

  private Long id;
  private String rechnungNr;
  private Long auftragId;
  private Date ausgestelltAm;
  // Betraege als double, gerundet wird beim Erstellen (siehe WerkstattService)
  private double summeNetto;
  private double ust;
  private double summeBrutto;
  private boolean bezahlt;
  private Date bezahltAm;

  // Anzeige
  private String auftragNr;
  private String kundeName;

  public Rechnung() {}

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getRechnungNr() {
    return rechnungNr;
  }

  public void setRechnungNr(String rechnungNr) {
    this.rechnungNr = rechnungNr;
  }

  public Long getAuftragId() {
    return auftragId;
  }

  public void setAuftragId(Long auftragId) {
    this.auftragId = auftragId;
  }

  public Date getAusgestelltAm() {
    return ausgestelltAm;
  }

  public void setAusgestelltAm(Date ausgestelltAm) {
    this.ausgestelltAm = ausgestelltAm;
  }

  public double getSummeNetto() {
    return summeNetto;
  }

  public void setSummeNetto(double summeNetto) {
    this.summeNetto = summeNetto;
  }

  public double getUst() {
    return ust;
  }

  public void setUst(double ust) {
    this.ust = ust;
  }

  public double getSummeBrutto() {
    return summeBrutto;
  }

  public void setSummeBrutto(double summeBrutto) {
    this.summeBrutto = summeBrutto;
  }

  public boolean isBezahlt() {
    return bezahlt;
  }

  public void setBezahlt(boolean bezahlt) {
    this.bezahlt = bezahlt;
  }

  public Date getBezahltAm() {
    return bezahltAm;
  }

  public void setBezahltAm(Date bezahltAm) {
    this.bezahltAm = bezahltAm;
  }

  public String getAuftragNr() {
    return auftragNr;
  }

  public void setAuftragNr(String auftragNr) {
    this.auftragNr = auftragNr;
  }

  public String getKundeName() {
    return kundeName;
  }

  public void setKundeName(String kundeName) {
    this.kundeName = kundeName;
  }
}
