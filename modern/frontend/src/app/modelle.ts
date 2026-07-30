// Wire types of the WerkstattCRM REST API. Field names mirror the
// characterization goldens (characterization/src/test/resources/golden/) —
// the API contract is pinned there and MUST NOT be adapted to the frontend.

export type AuftragStatus = 'ANGENOMMEN' | 'IN_ARBEIT' | 'FERTIG' | 'ABGEHOLT' | 'STORNIERT';

export interface Kunde {
  id?: number;
  anrede: string;
  vorname: string;
  nachname: string;
  anzeigeName?: string;
  telefon?: string;
  email?: string;
  strasse?: string;
  plz?: string;
  ort?: string;
  notiz?: string;
  /** epoch millis (Boot 1.5 wire format, pinned — ADR-0005) */
  angelegtAm?: number;
}

export interface Fahrzeug {
  id?: number;
  kundeId: number;
  kundeName?: string;
  kennzeichen: string;
  marke?: string;
  modell?: string;
  bezeichnung?: string;
  baujahr?: number;
  kmStand?: number;
  fahrgestellnr?: string;
  /** the only DATE column of the schema; serialized "yyyy-MM-dd" (ADR-0005) */
  pickerlDatum?: string;
  angelegtAm?: number;
}

export interface AuftragPosition {
  id?: number;
  auftragId?: number;
  typ: string;
  bezeichnung: string;
  menge: number;
  einzelpreis?: number;
  gesamtpreis?: number;
}

export interface Auftrag {
  id: number;
  auftragNr: string;
  kundeId: number;
  kundeName: string;
  fahrzeugId: number;
  kennzeichen: string;
  fahrzeugBezeichnung: string;
  status: AuftragStatus;
  beschreibung: string;
  kmStand: number | null;
  angenommenAm: number;
  fertigAm: number | null;
  abgeholtAm: number | null;
  positionen: AuftragPosition[];
  summeNetto: number;
}

/** payload of POST /api/auftraege (what the AngularJS form sent, unchanged) */
export interface NeuerAuftrag {
  kundeId?: number;
  fahrzeugId?: number | null;
  beschreibung?: string;
  kmStand?: number;
}

export interface Rechnung {
  id: number;
  rechnungNr: string;
  auftragId: number;
  auftragNr: string;
  kundeName: string;
  summeNetto: number;
  ust: number;
  summeBrutto: number;
  bezahlt: boolean;
  ausgestelltAm: number;
  bezahltAm: number | null;
}

export interface MonatsBericht {
  jahr: number;
  monat: number;
  monatName: string;
  anzahlAuftraege: number;
  anzahlRechnungen: number;
  umsatzNetto: number;
  umsatzBrutto: number;
}

/** GET /api/bericht/topkunden — raw JdbcTemplate map, snake_case and all */
export interface TopKunde {
  id: number;
  vorname: string;
  nachname: string;
  anzahl_auftraege: number;
  umsatz: number;
}
