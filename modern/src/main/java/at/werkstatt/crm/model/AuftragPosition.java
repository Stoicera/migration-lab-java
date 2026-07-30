package at.werkstatt.crm.model;

public class AuftragPosition {

  public static final String TYP_ARBEIT = "ARBEIT";
  public static final String TYP_MATERIAL = "MATERIAL";

  private Long id;
  private Long auftragId;
  private String typ;
  private String bezeichnung;
  private double menge;
  private double einzelpreis;

  public AuftragPosition() {}

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getAuftragId() {
    return auftragId;
  }

  public void setAuftragId(Long auftragId) {
    this.auftragId = auftragId;
  }

  public String getTyp() {
    return typ;
  }

  public void setTyp(String typ) {
    this.typ = typ;
  }

  public String getBezeichnung() {
    return bezeichnung;
  }

  public void setBezeichnung(String bezeichnung) {
    this.bezeichnung = bezeichnung;
  }

  public double getMenge() {
    return menge;
  }

  public void setMenge(double menge) {
    this.menge = menge;
  }

  public double getEinzelpreis() {
    return einzelpreis;
  }

  public void setEinzelpreis(double einzelpreis) {
    this.einzelpreis = einzelpreis;
  }

  public double getGesamtpreis() {
    return menge * einzelpreis;
  }
}
