package at.werkstatt.crm.service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import at.werkstatt.crm.model.Auftrag;
import at.werkstatt.crm.model.AuftragPosition;
import at.werkstatt.crm.model.Fahrzeug;
import at.werkstatt.crm.model.Kunde;
import at.werkstatt.crm.model.MonatsBericht;
import at.werkstatt.crm.model.Rechnung;

/**
 * Zentrale Service-Klasse der Werkstatt. Hier ist ueber die Jahre alles
 * zusammengewachsen: Kunden, Fahrzeuge, Auftraege, Rechnungen, Berichte.
 *
 * ACHTUNG: Bitte nichts umbauen ohne Ruecksprache mit Hrn. B.! Die
 * Rechnungslogik haengt am Statusfluss der Auftraege.
 */
@Service
public class WerkstattService {

	private static final Logger LOG = LoggerFactory.getLogger(WerkstattService.class);

	// eine Instanz fuer alle Threads, hat bis jetzt eh immer funktioniert
	private static final SimpleDateFormat DATUM = new SimpleDateFormat("dd.MM.yyyy");

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Value("${werkstatt.ust.satz:20}")
	private int ustSatz;

	// =====================================================================
	// KUNDEN
	// =====================================================================

	public List<Kunde> getAlleKunden() {
		String sql = "SELECT * FROM kunde ORDER BY nachname, vorname";
		return jdbcTemplate.query(sql, new RowMapper<Kunde>() {
			public Kunde mapRow(ResultSet rs, int rowNum) throws SQLException {
				Kunde k = new Kunde();
				k.setId(rs.getLong("id"));
				k.setAnrede(rs.getString("anrede"));
				k.setVorname(rs.getString("vorname"));
				k.setNachname(rs.getString("nachname"));
				k.setTelefon(rs.getString("telefon"));
				k.setEmail(rs.getString("email"));
				k.setStrasse(rs.getString("strasse"));
				k.setPlz(rs.getString("plz"));
				k.setOrt(rs.getString("ort"));
				k.setNotiz(rs.getString("notiz"));
				k.setAngelegtAm(rs.getTimestamp("angelegt_am"));
				return k;
			}
		});
	}

	/**
	 * Suche wie sie 2016 halt gemacht wurde. Funktioniert, solange niemand
	 * boese ist. TODO: irgendwann auf Prepared Statements umstellen (Hr. B., 11/2017)
	 */
	public List<Kunde> sucheKunden(String suchbegriff) {
		// Achtung: Suchbegriff wird direkt in das SQL eingebaut!
		String sql = "SELECT * FROM kunde WHERE lower(nachname) LIKE '%" + suchbegriff.toLowerCase()
				+ "%' OR lower(vorname) LIKE '%" + suchbegriff.toLowerCase() + "%' OR lower(ort) LIKE '%"
				+ suchbegriff.toLowerCase() + "%' ORDER BY nachname";
		LOG.debug("Kundensuche: " + sql);
		return jdbcTemplate.query(sql, new RowMapper<Kunde>() {
			public Kunde mapRow(ResultSet rs, int rowNum) throws SQLException {
				// Mapping nochmal, war schneller als umbauen
				Kunde k = new Kunde();
				k.setId(rs.getLong("id"));
				k.setAnrede(rs.getString("anrede"));
				k.setVorname(rs.getString("vorname"));
				k.setNachname(rs.getString("nachname"));
				k.setTelefon(rs.getString("telefon"));
				k.setEmail(rs.getString("email"));
				k.setStrasse(rs.getString("strasse"));
				k.setPlz(rs.getString("plz"));
				k.setOrt(rs.getString("ort"));
				k.setNotiz(rs.getString("notiz"));
				k.setAngelegtAm(rs.getTimestamp("angelegt_am"));
				return k;
			}
		});
	}

	public Kunde getKunde(long id) {
		List<Kunde> liste = jdbcTemplate.query("SELECT * FROM kunde WHERE id = " + id, new RowMapper<Kunde>() {
			public Kunde mapRow(ResultSet rs, int rowNum) throws SQLException {
				Kunde k = new Kunde();
				k.setId(rs.getLong("id"));
				k.setAnrede(rs.getString("anrede"));
				k.setVorname(rs.getString("vorname"));
				k.setNachname(rs.getString("nachname"));
				k.setTelefon(rs.getString("telefon"));
				k.setEmail(rs.getString("email"));
				k.setStrasse(rs.getString("strasse"));
				k.setPlz(rs.getString("plz"));
				k.setOrt(rs.getString("ort"));
				k.setNotiz(rs.getString("notiz"));
				k.setAngelegtAm(rs.getTimestamp("angelegt_am"));
				return k;
			}
		});
		if (liste.isEmpty()) {
			return null;
		}
		return liste.get(0);
	}

	public Kunde speichereKunde(Kunde kunde) {
		if (kunde.getId() == null) {
			Long neueId = jdbcTemplate.queryForObject(
					"INSERT INTO kunde (anrede, vorname, nachname, telefon, email, strasse, plz, ort, notiz, angelegt_am) "
							+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, now()) RETURNING id",
					Long.class, kunde.getAnrede(), kunde.getVorname(), kunde.getNachname(), kunde.getTelefon(),
					kunde.getEmail(), kunde.getStrasse(), kunde.getPlz(), kunde.getOrt(), kunde.getNotiz());
			kunde.setId(neueId);
			LOG.info("Kunde angelegt: " + kunde.getAnzeigeName() + " (id=" + neueId + ")");
		} else {
			jdbcTemplate.update(
					"UPDATE kunde SET anrede=?, vorname=?, nachname=?, telefon=?, email=?, strasse=?, plz=?, ort=?, notiz=? WHERE id=?",
					kunde.getAnrede(), kunde.getVorname(), kunde.getNachname(), kunde.getTelefon(), kunde.getEmail(),
					kunde.getStrasse(), kunde.getPlz(), kunde.getOrt(), kunde.getNotiz(), kunde.getId());
		}
		return getKunde(kunde.getId());
	}

	public void loescheKunde(long id) {
		// Fahrzeuge und Auftraege bleiben stehen, hat noch nie Probleme gemacht
		LOG.info("Loesche Kunde " + id);
		jdbcTemplate.update("DELETE FROM kunde WHERE id = " + id);
	}

	// =====================================================================
	// FAHRZEUGE
	// =====================================================================

	public List<Fahrzeug> getAlleFahrzeuge() {
		String sql = "SELECT f.*, k.nachname, k.vorname FROM fahrzeug f LEFT JOIN kunde k ON k.id = f.kunde_id ORDER BY f.kennzeichen";
		return jdbcTemplate.query(sql, new RowMapper<Fahrzeug>() {
			public Fahrzeug mapRow(ResultSet rs, int rowNum) throws SQLException {
				Fahrzeug f = mapFahrzeug(rs);
				String nn = rs.getString("nachname");
				String vn = rs.getString("vorname");
				if (nn != null) {
					f.setKundeName(nn + " " + (vn == null ? "" : vn));
				}
				return f;
			}
		});
	}

	public List<Fahrzeug> getFahrzeugeZuKunde(long kundeId) {
		return jdbcTemplate.query("SELECT * FROM fahrzeug WHERE kunde_id = " + kundeId + " ORDER BY kennzeichen",
				new RowMapper<Fahrzeug>() {
					public Fahrzeug mapRow(ResultSet rs, int rowNum) throws SQLException {
						return mapFahrzeug(rs);
					}
				});
	}

	public Fahrzeug getFahrzeug(long id) {
		List<Fahrzeug> liste = jdbcTemplate.query("SELECT * FROM fahrzeug WHERE id = " + id, new RowMapper<Fahrzeug>() {
			public Fahrzeug mapRow(ResultSet rs, int rowNum) throws SQLException {
				return mapFahrzeug(rs);
			}
		});
		return liste.isEmpty() ? null : liste.get(0);
	}

	// die eine Stelle, an der das Mapping ausgelagert wurde (2018, Hr. F.)
	private Fahrzeug mapFahrzeug(ResultSet rs) throws SQLException {
		Fahrzeug f = new Fahrzeug();
		f.setId(rs.getLong("id"));
		f.setKundeId(rs.getLong("kunde_id"));
		f.setKennzeichen(rs.getString("kennzeichen"));
		f.setMarke(rs.getString("marke"));
		f.setModell(rs.getString("modell"));
		f.setFahrgestellnr(rs.getString("fahrgestellnr"));
		int bj = rs.getInt("baujahr");
		if (!rs.wasNull()) {
			f.setBaujahr(bj);
		}
		int km = rs.getInt("km_stand");
		if (!rs.wasNull()) {
			f.setKmStand(km);
		}
		f.setPickerlDatum(rs.getDate("pickerl_datum"));
		f.setAngelegtAm(rs.getTimestamp("angelegt_am"));
		return f;
	}

	public Fahrzeug speichereFahrzeug(Fahrzeug fahrzeug) {
		if (fahrzeug.getId() == null) {
			Long neueId = jdbcTemplate.queryForObject(
					"INSERT INTO fahrzeug (kunde_id, kennzeichen, marke, modell, fahrgestellnr, baujahr, km_stand, pickerl_datum, angelegt_am) "
							+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?, now()) RETURNING id",
					Long.class, fahrzeug.getKundeId(), fahrzeug.getKennzeichen(), fahrzeug.getMarke(),
					fahrzeug.getModell(), fahrzeug.getFahrgestellnr(), fahrzeug.getBaujahr(), fahrzeug.getKmStand(),
					fahrzeug.getPickerlDatum());
			fahrzeug.setId(neueId);
		} else {
			jdbcTemplate.update(
					"UPDATE fahrzeug SET kunde_id=?, kennzeichen=?, marke=?, modell=?, fahrgestellnr=?, baujahr=?, km_stand=?, pickerl_datum=? WHERE id=?",
					fahrzeug.getKundeId(), fahrzeug.getKennzeichen(), fahrzeug.getMarke(), fahrzeug.getModell(),
					fahrzeug.getFahrgestellnr(), fahrzeug.getBaujahr(), fahrzeug.getKmStand(),
					fahrzeug.getPickerlDatum(), fahrzeug.getId());
		}
		return getFahrzeug(fahrzeug.getId());
	}

	public void loescheFahrzeug(long id) {
		jdbcTemplate.update("DELETE FROM fahrzeug WHERE id = " + id);
	}

	// =====================================================================
	// AUFTRAEGE
	// =====================================================================

	public List<Auftrag> getAuftraege(String status) {
		String sql = "SELECT a.*, k.nachname, k.vorname, f.kennzeichen, f.marke, f.modell "
				+ "FROM auftrag a LEFT JOIN kunde k ON k.id = a.kunde_id LEFT JOIN fahrzeug f ON f.id = a.fahrzeug_id";
		if (status != null && status.length() > 0) {
			// Status kommt nur aus dem eigenen Frontend, passt schon
			sql = sql + " WHERE a.status = '" + status + "'";
		}
		sql = sql + " ORDER BY a.angenommen_am DESC";
		return jdbcTemplate.query(sql, new RowMapper<Auftrag>() {
			public Auftrag mapRow(ResultSet rs, int rowNum) throws SQLException {
				Auftrag a = mapAuftrag(rs);
				String nn = rs.getString("nachname");
				if (nn != null) {
					a.setKundeName(nn + " " + defaultString(rs.getString("vorname")));
				}
				a.setKennzeichen(rs.getString("kennzeichen"));
				a.setFahrzeugBezeichnung(defaultString(rs.getString("marke")) + " " + defaultString(rs.getString("modell")));
				return a;
			}
		});
	}

	public Auftrag getAuftrag(long id) {
		String sql = "SELECT a.*, k.nachname, k.vorname, f.kennzeichen, f.marke, f.modell "
				+ "FROM auftrag a LEFT JOIN kunde k ON k.id = a.kunde_id LEFT JOIN fahrzeug f ON f.id = a.fahrzeug_id "
				+ "WHERE a.id = " + id;
		List<Auftrag> liste = jdbcTemplate.query(sql, new RowMapper<Auftrag>() {
			public Auftrag mapRow(ResultSet rs, int rowNum) throws SQLException {
				Auftrag a = mapAuftrag(rs);
				String nn = rs.getString("nachname");
				if (nn != null) {
					a.setKundeName(nn + " " + defaultString(rs.getString("vorname")));
				}
				a.setKennzeichen(rs.getString("kennzeichen"));
				a.setFahrzeugBezeichnung(defaultString(rs.getString("marke")) + " " + defaultString(rs.getString("modell")));
				return a;
			}
		});
		if (liste.isEmpty()) {
			return null;
		}
		Auftrag auftrag = liste.get(0);
		auftrag.setPositionen(getPositionen(id));
		return auftrag;
	}

	private Auftrag mapAuftrag(ResultSet rs) throws SQLException {
		Auftrag a = new Auftrag();
		a.setId(rs.getLong("id"));
		a.setAuftragNr(rs.getString("auftrag_nr"));
		a.setFahrzeugId(rs.getLong("fahrzeug_id"));
		a.setKundeId(rs.getLong("kunde_id"));
		a.setStatus(rs.getString("status"));
		a.setBeschreibung(rs.getString("beschreibung"));
		int km = rs.getInt("km_stand");
		if (!rs.wasNull()) {
			a.setKmStand(km);
		}
		a.setAngenommenAm(rs.getTimestamp("angenommen_am"));
		a.setFertigAm(rs.getTimestamp("fertig_am"));
		a.setAbgeholtAm(rs.getTimestamp("abgeholt_am"));
		return a;
	}

	public List<AuftragPosition> getPositionen(long auftragId) {
		return jdbcTemplate.query("SELECT * FROM auftrag_position WHERE auftrag_id = " + auftragId + " ORDER BY id",
				new RowMapper<AuftragPosition>() {
					public AuftragPosition mapRow(ResultSet rs, int rowNum) throws SQLException {
						AuftragPosition p = new AuftragPosition();
						p.setId(rs.getLong("id"));
						p.setAuftragId(rs.getLong("auftrag_id"));
						p.setTyp(rs.getString("typ"));
						p.setBezeichnung(rs.getString("bezeichnung"));
						p.setMenge(rs.getDouble("menge"));
						p.setEinzelpreis(rs.getDouble("einzelpreis"));
						return p;
					}
				});
	}

	/**
	 * Neuen Auftrag anlegen. Nummer wird aus MAX+1 gebaut - bei zwei
	 * gleichzeitigen Annahmen knallt es theoretisch, ist aber bei uns noch nie
	 * passiert (eine Annahme-Theke).
	 */
	public Auftrag neuerAuftrag(Auftrag auftrag) {
		int jahr = Calendar.getInstance().get(Calendar.YEAR);
		Integer max = jdbcTemplate.queryForObject(
				"SELECT COALESCE(MAX(CAST(SUBSTRING(auftrag_nr FROM 8) AS INTEGER)), 0) FROM auftrag WHERE auftrag_nr LIKE 'A-" + jahr + "-%'",
				Integer.class);
		String nr = "A-" + jahr + "-" + String.format("%04d", max + 1);
		auftrag.setAuftragNr(nr);
		auftrag.setStatus(Auftrag.STATUS_ANGENOMMEN);

		Long neueId = jdbcTemplate.queryForObject(
				"INSERT INTO auftrag (auftrag_nr, fahrzeug_id, kunde_id, status, beschreibung, km_stand, angenommen_am) "
						+ "VALUES (?, ?, ?, ?, ?, ?, now()) RETURNING id",
				Long.class, nr, auftrag.getFahrzeugId(), auftrag.getKundeId(), auftrag.getStatus(),
				auftrag.getBeschreibung(), auftrag.getKmStand());
		auftrag.setId(neueId);

		// km-Stand gleich am Fahrzeug mitziehen (ohne Transaktion, siehe Notizen)
		if (auftrag.getKmStand() != null) {
			jdbcTemplate.update("UPDATE fahrzeug SET km_stand = " + auftrag.getKmStand() + " WHERE id = "
					+ auftrag.getFahrzeugId());
		}

		LOG.info("Auftrag " + nr + " angelegt fuer Fahrzeug " + auftrag.getFahrzeugId());
		return getAuftrag(neueId);
	}

	/**
	 * Statuswechsel mit den erlaubten Uebergaengen. Die Regeln stehen so auch
	 * im Werkstatt-Ordner bei der Annahme-Theke.
	 */
	public Auftrag setzeStatus(long auftragId, String neuerStatus) {
		Auftrag auftrag = getAuftrag(auftragId);
		if (auftrag == null) {
			throw new RuntimeException("Auftrag " + auftragId + " nicht gefunden");
		}
		String alt = auftrag.getStatus();

		boolean erlaubt = false;
		if (alt.equals(Auftrag.STATUS_ANGENOMMEN)) {
			if (neuerStatus.equals(Auftrag.STATUS_IN_ARBEIT) || neuerStatus.equals(Auftrag.STATUS_STORNIERT)) {
				erlaubt = true;
			}
		} else if (alt.equals(Auftrag.STATUS_IN_ARBEIT)) {
			if (neuerStatus.equals(Auftrag.STATUS_FERTIG) || neuerStatus.equals(Auftrag.STATUS_STORNIERT)) {
				erlaubt = true;
			}
		} else if (alt.equals(Auftrag.STATUS_FERTIG)) {
			if (neuerStatus.equals(Auftrag.STATUS_ABGEHOLT)) {
				erlaubt = true;
			}
			// Sonderfall: doch noch was gefunden -> zurueck in Arbeit
			if (neuerStatus.equals(Auftrag.STATUS_IN_ARBEIT)) {
				erlaubt = true;
			}
		}

		if (!erlaubt) {
			throw new RuntimeException("Statuswechsel " + alt + " -> " + neuerStatus + " ist nicht erlaubt");
		}

		String sql = "UPDATE auftrag SET status = '" + neuerStatus + "'";
		if (neuerStatus.equals(Auftrag.STATUS_FERTIG)) {
			sql = sql + ", fertig_am = now()";
		}
		if (neuerStatus.equals(Auftrag.STATUS_ABGEHOLT)) {
			sql = sql + ", abgeholt_am = now()";
		}
		sql = sql + " WHERE id = " + auftragId;
		jdbcTemplate.update(sql);

		LOG.info("Auftrag " + auftrag.getAuftragNr() + ": " + alt + " -> " + neuerStatus);
		return getAuftrag(auftragId);
	}

	public AuftragPosition neuePosition(long auftragId, AuftragPosition position) {
		Auftrag auftrag = getAuftrag(auftragId);
		if (auftrag == null) {
			throw new RuntimeException("Auftrag " + auftragId + " nicht gefunden");
		}
		if (auftrag.getStatus().equals(Auftrag.STATUS_ABGEHOLT) || auftrag.getStatus().equals(Auftrag.STATUS_STORNIERT)) {
			throw new RuntimeException("Auftrag " + auftrag.getAuftragNr() + " ist abgeschlossen, keine Positionen mehr moeglich");
		}
		Long neueId = jdbcTemplate.queryForObject(
				"INSERT INTO auftrag_position (auftrag_id, typ, bezeichnung, menge, einzelpreis) VALUES (?, ?, ?, ?, ?) RETURNING id",
				Long.class, auftragId, position.getTyp(), position.getBezeichnung(), position.getMenge(),
				position.getEinzelpreis());
		position.setId(neueId);
		position.setAuftragId(auftragId);
		return position;
	}

	public void loeschePosition(long positionId) {
		jdbcTemplate.update("DELETE FROM auftrag_position WHERE id = " + positionId);
	}

	// =====================================================================
	// RECHNUNGEN
	// =====================================================================

	public List<Rechnung> getAlleRechnungen() {
		String sql = "SELECT r.*, a.auftrag_nr, k.nachname, k.vorname FROM rechnung r "
				+ "LEFT JOIN auftrag a ON a.id = r.auftrag_id LEFT JOIN kunde k ON k.id = a.kunde_id "
				+ "ORDER BY r.ausgestellt_am DESC, r.id DESC";
		return jdbcTemplate.query(sql, new RowMapper<Rechnung>() {
			public Rechnung mapRow(ResultSet rs, int rowNum) throws SQLException {
				Rechnung r = mapRechnung(rs);
				r.setAuftragNr(rs.getString("auftrag_nr"));
				String nn = rs.getString("nachname");
				if (nn != null) {
					r.setKundeName(nn + " " + defaultString(rs.getString("vorname")));
				}
				return r;
			}
		});
	}

	public Rechnung getRechnung(long id) {
		String sql = "SELECT r.*, a.auftrag_nr, k.nachname, k.vorname FROM rechnung r "
				+ "LEFT JOIN auftrag a ON a.id = r.auftrag_id LEFT JOIN kunde k ON k.id = a.kunde_id WHERE r.id = " + id;
		List<Rechnung> liste = jdbcTemplate.query(sql, new RowMapper<Rechnung>() {
			public Rechnung mapRow(ResultSet rs, int rowNum) throws SQLException {
				Rechnung r = mapRechnung(rs);
				r.setAuftragNr(rs.getString("auftrag_nr"));
				String nn = rs.getString("nachname");
				if (nn != null) {
					r.setKundeName(nn + " " + defaultString(rs.getString("vorname")));
				}
				return r;
			}
		});
		return liste.isEmpty() ? null : liste.get(0);
	}

	private Rechnung mapRechnung(ResultSet rs) throws SQLException {
		Rechnung r = new Rechnung();
		r.setId(rs.getLong("id"));
		r.setRechnungNr(rs.getString("rechnung_nr"));
		r.setAuftragId(rs.getLong("auftrag_id"));
		r.setAusgestelltAm(rs.getTimestamp("ausgestellt_am"));
		r.setSummeNetto(rs.getDouble("summe_netto"));
		r.setUst(rs.getDouble("ust"));
		r.setSummeBrutto(rs.getDouble("summe_brutto"));
		r.setBezahlt(rs.getBoolean("bezahlt"));
		r.setBezahltAm(rs.getTimestamp("bezahlt_am"));
		return r;
	}

	/**
	 * Rechnung zu einem fertigen Auftrag erstellen. Rundung auf 2 Stellen
	 * passiert hier haendisch - double reicht fuer unsere Betraege.
	 */
	public Rechnung erstelleRechnung(long auftragId) {
		Auftrag auftrag = getAuftrag(auftragId);
		if (auftrag == null) {
			throw new RuntimeException("Auftrag " + auftragId + " nicht gefunden");
		}
		if (!auftrag.getStatus().equals(Auftrag.STATUS_FERTIG)) {
			throw new RuntimeException(
					"Rechnung geht nur bei Status FERTIG, Auftrag " + auftrag.getAuftragNr() + " ist " + auftrag.getStatus());
		}
		Integer vorhanden = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM rechnung WHERE auftrag_id = " + auftragId, Integer.class);
		if (vorhanden != null && vorhanden > 0) {
			throw new RuntimeException("Zum Auftrag " + auftrag.getAuftragNr() + " gibt es schon eine Rechnung");
		}

		double netto = 0;
		for (AuftragPosition p : auftrag.getPositionen()) {
			netto = netto + p.getMenge() * p.getEinzelpreis();
		}
		netto = Math.round(netto * 100.0) / 100.0;
		double ust = Math.round(netto * ustSatz) / 100.0;
		double brutto = Math.round((netto + ust) * 100.0) / 100.0;

		int jahr = Calendar.getInstance().get(Calendar.YEAR);
		Integer max = jdbcTemplate.queryForObject(
				"SELECT COALESCE(MAX(CAST(SUBSTRING(rechnung_nr FROM 8) AS INTEGER)), 0) FROM rechnung WHERE rechnung_nr LIKE 'R-" + jahr + "-%'",
				Integer.class);
		String nr = "R-" + jahr + "-" + String.format("%04d", max + 1);

		Long neueId = jdbcTemplate.queryForObject(
				"INSERT INTO rechnung (rechnung_nr, auftrag_id, ausgestellt_am, summe_netto, ust, summe_brutto, bezahlt) "
						+ "VALUES (?, ?, now(), ?, ?, ?, false) RETURNING id",
				Long.class, nr, auftragId, netto, ust, brutto);

		LOG.info("Rechnung " + nr + " erstellt, brutto " + brutto);
		return getRechnung(neueId);
	}

	public Rechnung setzeBezahlt(long rechnungId) {
		jdbcTemplate.update("UPDATE rechnung SET bezahlt = true, bezahlt_am = now() WHERE id = " + rechnungId);
		return getRechnung(rechnungId);
	}

	// =====================================================================
	// BERICHT
	// =====================================================================

	public List<MonatsBericht> getMonatsBericht(int jahr) {
		// Auftraege und Rechnungen je Monat; Umsatz nur aus Rechnungen
		String sql = "SELECT m.monat, "
				+ " (SELECT COUNT(*) FROM auftrag a WHERE EXTRACT(YEAR FROM a.angenommen_am) = " + jahr
				+ "   AND EXTRACT(MONTH FROM a.angenommen_am) = m.monat) AS anzahl_auftraege, "
				+ " (SELECT COUNT(*) FROM rechnung r WHERE EXTRACT(YEAR FROM r.ausgestellt_am) = " + jahr
				+ "   AND EXTRACT(MONTH FROM r.ausgestellt_am) = m.monat) AS anzahl_rechnungen, "
				+ " (SELECT COALESCE(SUM(r.summe_netto),0) FROM rechnung r WHERE EXTRACT(YEAR FROM r.ausgestellt_am) = " + jahr
				+ "   AND EXTRACT(MONTH FROM r.ausgestellt_am) = m.monat) AS umsatz_netto, "
				+ " (SELECT COALESCE(SUM(r.summe_brutto),0) FROM rechnung r WHERE EXTRACT(YEAR FROM r.ausgestellt_am) = " + jahr
				+ "   AND EXTRACT(MONTH FROM r.ausgestellt_am) = m.monat) AS umsatz_brutto "
				+ "FROM generate_series(1,12) AS m(monat) ORDER BY m.monat";
		final int j = jahr;
		return jdbcTemplate.query(sql, new RowMapper<MonatsBericht>() {
			public MonatsBericht mapRow(ResultSet rs, int rowNum) throws SQLException {
				MonatsBericht b = new MonatsBericht();
				b.setJahr(j);
				b.setMonat(rs.getInt("monat"));
				b.setAnzahlAuftraege(rs.getInt("anzahl_auftraege"));
				b.setAnzahlRechnungen(rs.getInt("anzahl_rechnungen"));
				b.setUmsatzNetto(rs.getDouble("umsatz_netto"));
				b.setUmsatzBrutto(rs.getDouble("umsatz_brutto"));
				return b;
			}
		});
	}

	public List<Map<String, Object>> getTopKunden(int jahr) {
		String sql = "SELECT k.id, k.nachname, k.vorname, COUNT(DISTINCT a.id) AS anzahl_auftraege, "
				+ "COALESCE(SUM(r.summe_brutto),0) AS umsatz "
				+ "FROM kunde k JOIN auftrag a ON a.kunde_id = k.id "
				+ "LEFT JOIN rechnung r ON r.auftrag_id = a.id AND EXTRACT(YEAR FROM r.ausgestellt_am) = " + jahr + " "
				+ "WHERE EXTRACT(YEAR FROM a.angenommen_am) = " + jahr + " "
				+ "GROUP BY k.id, k.nachname, k.vorname ORDER BY umsatz DESC, anzahl_auftraege DESC LIMIT 10";
		return jdbcTemplate.queryForList(sql);
	}

	// =====================================================================
	// ADMIN / STATISTIK (fuer die JSP-Seite)
	// =====================================================================

	public Map<String, Object> getAdminStatistik() {
		Map<String, Object> statistik = new HashMap<String, Object>();
		statistik.put("kunden", jdbcTemplate.queryForObject("SELECT COUNT(*) FROM kunde", Integer.class));
		statistik.put("fahrzeuge", jdbcTemplate.queryForObject("SELECT COUNT(*) FROM fahrzeug", Integer.class));
		statistik.put("auftraege", jdbcTemplate.queryForObject("SELECT COUNT(*) FROM auftrag", Integer.class));
		statistik.put("auftraegeOffen", jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM auftrag WHERE status IN ('ANGENOMMEN','IN_ARBEIT')", Integer.class));
		statistik.put("rechnungen", jdbcTemplate.queryForObject("SELECT COUNT(*) FROM rechnung", Integer.class));
		statistik.put("rechnungenOffen",
				jdbcTemplate.queryForObject("SELECT COUNT(*) FROM rechnung WHERE bezahlt = false", Integer.class));
		statistik.put("stand", DATUM.format(new Date()));
		return statistik;
	}

	/**
	 * Raeumt stornierte Auftraege aelter als 90 Tage weg. Wird vom Chef
	 * einmal im Jahr aufgerufen. Loescht wirklich!
	 */
	public int bereinigeStornierte() {
		List<Long> ids = jdbcTemplate.queryForList(
				"SELECT id FROM auftrag WHERE status = 'STORNIERT' AND angenommen_am < now() - interval '90 days'",
				Long.class);
		for (Long id : ids) {
			jdbcTemplate.update("DELETE FROM auftrag_position WHERE auftrag_id = " + id);
			jdbcTemplate.update("DELETE FROM auftrag WHERE id = " + id);
		}
		LOG.warn("Bereinigung: " + ids.size() + " stornierte Auftraege geloescht");
		return ids.size();
	}

	// =====================================================================
	// Hilfsmethoden
	// =====================================================================

	private String defaultString(String s) {
		if (s == null) {
			return "";
		}
		return s;
	}


}
