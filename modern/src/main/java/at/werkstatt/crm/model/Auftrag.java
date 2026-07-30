package at.werkstatt.crm.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Auftrag {

	// Statusfluss: ANGENOMMEN -> IN_ARBEIT -> FERTIG -> ABGEHOLT
	//              (STORNIERT geht von ANGENOMMEN und IN_ARBEIT aus)
	public static final String STATUS_ANGENOMMEN = "ANGENOMMEN";
	public static final String STATUS_IN_ARBEIT = "IN_ARBEIT";
	public static final String STATUS_FERTIG = "FERTIG";
	public static final String STATUS_ABGEHOLT = "ABGEHOLT";
	public static final String STATUS_STORNIERT = "STORNIERT";

	private Long id;
	private String auftragNr;
	private Long fahrzeugId;
	private Long kundeId;
	private String status;
	private String beschreibung;
	private Integer kmStand;
	private Date angenommenAm;
	private Date fertigAm;
	private Date abgeholtAm;

	// Anzeige-Felder, werden je nach Abfrage befuellt oder auch nicht
	private String kundeName;
	private String kennzeichen;
	private String fahrzeugBezeichnung;

	private List<AuftragPosition> positionen = new ArrayList<AuftragPosition>();

	public Auftrag() {
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getAuftragNr() {
		return auftragNr;
	}

	public void setAuftragNr(String auftragNr) {
		this.auftragNr = auftragNr;
	}

	public Long getFahrzeugId() {
		return fahrzeugId;
	}

	public void setFahrzeugId(Long fahrzeugId) {
		this.fahrzeugId = fahrzeugId;
	}

	public Long getKundeId() {
		return kundeId;
	}

	public void setKundeId(Long kundeId) {
		this.kundeId = kundeId;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getBeschreibung() {
		return beschreibung;
	}

	public void setBeschreibung(String beschreibung) {
		this.beschreibung = beschreibung;
	}

	public Integer getKmStand() {
		return kmStand;
	}

	public void setKmStand(Integer kmStand) {
		this.kmStand = kmStand;
	}

	public Date getAngenommenAm() {
		return angenommenAm;
	}

	public void setAngenommenAm(Date angenommenAm) {
		this.angenommenAm = angenommenAm;
	}

	public Date getFertigAm() {
		return fertigAm;
	}

	public void setFertigAm(Date fertigAm) {
		this.fertigAm = fertigAm;
	}

	public Date getAbgeholtAm() {
		return abgeholtAm;
	}

	public void setAbgeholtAm(Date abgeholtAm) {
		this.abgeholtAm = abgeholtAm;
	}

	public String getKundeName() {
		return kundeName;
	}

	public void setKundeName(String kundeName) {
		this.kundeName = kundeName;
	}

	public String getKennzeichen() {
		return kennzeichen;
	}

	public void setKennzeichen(String kennzeichen) {
		this.kennzeichen = kennzeichen;
	}

	public String getFahrzeugBezeichnung() {
		return fahrzeugBezeichnung;
	}

	public void setFahrzeugBezeichnung(String fahrzeugBezeichnung) {
		this.fahrzeugBezeichnung = fahrzeugBezeichnung;
	}

	public List<AuftragPosition> getPositionen() {
		return positionen;
	}

	public void setPositionen(List<AuftragPosition> positionen) {
		this.positionen = positionen;
	}

	// Summe netto ueber alle Positionen; Achtung double, siehe Rechnung
	public double getSummeNetto() {
		double summe = 0;
		if (positionen != null) {
			for (AuftragPosition p : positionen) {
				summe = summe + p.getGesamtpreis();
			}
		}
		return summe;
	}

}
