/** Shape of /nachweis.json, written by scripts/nachweis/aggregate.mjs from the CI results. */

export type Urteil = 'identisch' | 'sanktioniert' | 'regression' | 'unvollstaendig';

export interface Ergebnis {
  status: 'bestanden' | 'fehlgeschlagen' | 'abgebrochen' | 'deaktiviert' | 'fehlt';
  meldung?: string;
}

export interface Beleg {
  label: string;
  url: string;
}

export interface Erklaerung {
  ref: string;
  titel: string;
  text: string;
  source: 'openrouter' | 'register';
  model: string | null;
  prompt_sha256: string | null;
  register_sha256: string;
  date: string;
  review: string;
}

export interface Sanktion {
  ref: string;
  quelle: string;
  url: string | null;
  erklaerung: Erklaerung | null;
}

export interface Zeile {
  id: string;
  suite: string;
  name: string;
  test: { klasse: string; methode: string | null };
  endpunkt: string | null;
  legacy: Ergebnis;
  modern: Ergebnis;
  urteil: Urteil;
  golden: { datei: string; url: string | null } | null;
  vergleich: { golden: string; modern: string | null; legacy?: string; gleich: boolean } | null;
  sanktion: Sanktion | null;
  belege: Beleg[];
}

export interface Summe {
  gesamt: number;
  bestanden: number;
  fehlgeschlagen: number;
}

export interface Nachweis {
  schema: number;
  erzeugt: string;
  repo: string;
  commit: { sha: string; url: string } | null;
  ciLauf: { url: string } | null;
  register: { deviations: string | null; adr0004: string | null };
  charakterisierung: {
    gesamt: number;
    identisch: number;
    sanktioniert: number;
    regression: number;
    unvollstaendig: number;
    zeilen: Zeile[];
  };
  e2e: {
    legacy: Summe | null;
    modern: Summe | null;
    klassen: { klasse: string; url: string | null; legacy: Summe | null; modern: Summe | null }[];
    sanktionen: { ref: string; url: string | null; erklaerung: Erklaerung | null }[];
  };
  imageScan: {
    werkzeug: string;
    schweregrade: string;
    nurBehebbar: boolean;
    gesamt: number;
    ziele: { ziel: string; typ: string; anzahl: number }[];
  } | null;
}
