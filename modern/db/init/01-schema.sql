-- =========================================================
-- WerkstattCRM Schema
-- Stand: gewachsen seit 2016. Wird haendisch am Server
-- eingespielt, ein Migrationswerkzeug gibt es nicht.
-- =========================================================

CREATE TABLE kunde (
	id BIGSERIAL PRIMARY KEY,
	anrede VARCHAR(20),
	vorname VARCHAR(100),
	nachname VARCHAR(100) NOT NULL,
	telefon VARCHAR(50),
	email VARCHAR(150),
	strasse VARCHAR(150),
	plz VARCHAR(10),
	ort VARCHAR(100),
	notiz TEXT,
	angelegt_am TIMESTAMP DEFAULT now()
);

-- kunde_id hat absichtlich(?) keinen Fremdschluessel - beim Loeschen von
-- Kunden bleiben Fahrzeuge einfach stehen. Bekannt, stoert im Betrieb nicht.
CREATE TABLE fahrzeug (
	id BIGSERIAL PRIMARY KEY,
	kunde_id BIGINT,
	kennzeichen VARCHAR(20) NOT NULL,
	marke VARCHAR(50),
	modell VARCHAR(80),
	fahrgestellnr VARCHAR(30),
	baujahr INTEGER,
	km_stand INTEGER,
	pickerl_datum DATE,
	angelegt_am TIMESTAMP DEFAULT now()
);

-- auftrag_nr ohne UNIQUE - die Nummern kommen aus MAX+1 in der Anwendung
CREATE TABLE auftrag (
	id BIGSERIAL PRIMARY KEY,
	auftrag_nr VARCHAR(20) NOT NULL,
	fahrzeug_id BIGINT,
	kunde_id BIGINT,
	status VARCHAR(20) NOT NULL,
	beschreibung TEXT,
	km_stand INTEGER,
	angenommen_am TIMESTAMP DEFAULT now(),
	fertig_am TIMESTAMP,
	abgeholt_am TIMESTAMP
);

-- der einzige Fremdschluessel; kam 2018 nachtraeglich, nachdem verwaiste
-- Positionen die Monatsauswertung verfaelscht hatten
CREATE TABLE auftrag_position (
	id BIGSERIAL PRIMARY KEY,
	auftrag_id BIGINT REFERENCES auftrag(id),
	typ VARCHAR(20),
	bezeichnung VARCHAR(200),
	menge NUMERIC(10,2) DEFAULT 1,
	einzelpreis NUMERIC(10,2) DEFAULT 0
);

CREATE TABLE rechnung (
	id BIGSERIAL PRIMARY KEY,
	rechnung_nr VARCHAR(20) NOT NULL,
	auftrag_id BIGINT,
	ausgestellt_am TIMESTAMP DEFAULT now(),
	summe_netto NUMERIC(12,2),
	ust NUMERIC(12,2),
	summe_brutto NUMERIC(12,2),
	bezahlt BOOLEAN DEFAULT FALSE,
	bezahlt_am TIMESTAMP
);
