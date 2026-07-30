package at.werkstatt.crm.model;

import java.util.Date;

public class Kunde {

  private Long id;
  private String anrede;
  private String vorname;
  private String nachname;
  private String telefon;
  private String email;
  private String strasse;
  private String plz;
  private String ort;
  private String notiz;
  private Date angelegtAm;

  public Kunde() {}

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getAnrede() {
    return anrede;
  }

  public void setAnrede(String anrede) {
    this.anrede = anrede;
  }

  public String getVorname() {
    return vorname;
  }

  public void setVorname(String vorname) {
    this.vorname = vorname;
  }

  public String getNachname() {
    return nachname;
  }

  public void setNachname(String nachname) {
    this.nachname = nachname;
  }

  public String getTelefon() {
    return telefon;
  }

  public void setTelefon(String telefon) {
    this.telefon = telefon;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getStrasse() {
    return strasse;
  }

  public void setStrasse(String strasse) {
    this.strasse = strasse;
  }

  public String getPlz() {
    return plz;
  }

  public void setPlz(String plz) {
    this.plz = plz;
  }

  public String getOrt() {
    return ort;
  }

  public void setOrt(String ort) {
    this.ort = ort;
  }

  public String getNotiz() {
    return notiz;
  }

  public void setNotiz(String notiz) {
    this.notiz = notiz;
  }

  public Date getAngelegtAm() {
    return angelegtAm;
  }

  public void setAngelegtAm(Date angelegtAm) {
    this.angelegtAm = angelegtAm;
  }

  // praktisch fuer die Anzeige, wird auch vom Frontend verwendet
  public String getAnzeigeName() {
    StringBuilder sb = new StringBuilder();
    if (nachname != null) {
      sb.append(nachname.toUpperCase());
    }
    sb.append(", ");
    if (vorname != null) {
      sb.append(vorname);
    }
    return sb.toString();
  }
}
