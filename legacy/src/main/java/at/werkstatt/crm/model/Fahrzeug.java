package at.werkstatt.crm.model;

import java.util.Date;

public class Fahrzeug {

	private Long id;
	private Long kundeId;
	private String kennzeichen;
	private String marke;
	private String modell;
	private String fahrgestellnr;
	private Integer baujahr;
	private Integer kmStand;
	// naechste §57a Begutachtung ("Pickerl")
	private Date pickerlDatum;
	private Date angelegtAm;

	// wird bei manchen Abfragen mitgeladen, bei manchen nicht (je nach SQL)
	private String kundeName;

	public Fahrzeug() {
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getKundeId() {
		return kundeId;
	}

	public void setKundeId(Long kundeId) {
		this.kundeId = kundeId;
	}

	public String getKennzeichen() {
		return kennzeichen;
	}

	public void setKennzeichen(String kennzeichen) {
		this.kennzeichen = kennzeichen;
	}

	public String getMarke() {
		return marke;
	}

	public void setMarke(String marke) {
		this.marke = marke;
	}

	public String getModell() {
		return modell;
	}

	public void setModell(String modell) {
		this.modell = modell;
	}

	public String getFahrgestellnr() {
		return fahrgestellnr;
	}

	public void setFahrgestellnr(String fahrgestellnr) {
		this.fahrgestellnr = fahrgestellnr;
	}

	public Integer getBaujahr() {
		return baujahr;
	}

	public void setBaujahr(Integer baujahr) {
		this.baujahr = baujahr;
	}

	public Integer getKmStand() {
		return kmStand;
	}

	public void setKmStand(Integer kmStand) {
		this.kmStand = kmStand;
	}

	public Date getPickerlDatum() {
		return pickerlDatum;
	}

	public void setPickerlDatum(Date pickerlDatum) {
		this.pickerlDatum = pickerlDatum;
	}

	public Date getAngelegtAm() {
		return angelegtAm;
	}

	public void setAngelegtAm(Date angelegtAm) {
		this.angelegtAm = angelegtAm;
	}

	public String getKundeName() {
		return kundeName;
	}

	public void setKundeName(String kundeName) {
		this.kundeName = kundeName;
	}

	public String getBezeichnung() {
		return (marke == null ? "" : marke) + " " + (modell == null ? "" : modell);
	}

}
