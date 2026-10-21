#!/usr/bin/env node
// Äquivalenz-Nachweis — joins what the CI gates already produced into one report.
//
//   node scripts/nachweis/aggregate.mjs --in <dir> --out <file>
//
// <dir> holds the downloaded CI artefacts (any nesting):
//   characterization-legacy.json / characterization-modern.json  (NachweisBericht, characterization suite)
//   nachweis-e2e-legacy/TEST-*.xml / nachweis-e2e-modern/TEST-*.xml  (surefire reports of the e2e suite)
//   trivy.json                                                    (image scan of the modern stand)
// Environment: NACHWEIS_SHA, NACHWEIS_RUN_URL, NACHWEIS_REPO (owner/name).
//
// The script decides nothing the suites did not decide: a row is "identisch" only when the
// test passed on BOTH stands and carries no @Sanktioniert marker. No dependencies, Node >= 20.

import { readFileSync, writeFileSync, readdirSync, statSync, existsSync, mkdirSync } from 'node:fs';
import { join, dirname, basename, relative } from 'node:path';

const args = Object.fromEntries(
  process.argv.slice(2).reduce((acc, cur, i, all) => (cur.startsWith('--') ? [...acc, [cur.slice(2), all[i + 1]]] : acc), []),
);
const IN = args.in ?? 'nachweis-in';
const OUT = args.out ?? 'nachweis.json';
const ROOT = args.root ?? process.cwd();
const REPO = process.env.NACHWEIS_REPO ?? 'Stoicera/migration-lab-java';
const SHA = process.env.NACHWEIS_SHA ?? '';
const RUN_URL = process.env.NACHWEIS_RUN_URL ?? '';
const GH = `https://github.com/${REPO}`;
const at = (path, line) => (SHA ? `${GH}/blob/${SHA}/${path}${line ? `#L${line}` : ''}` : null);

function walk(dir) {
  if (!existsSync(dir)) return [];
  return readdirSync(dir).flatMap((name) => {
    const p = join(dir, name);
    return statSync(p).isDirectory() ? walk(p) : [p];
  });
}
const files = walk(IN);
const find = (name) => files.find((f) => basename(f) === name);
const readJson = (p) => (p ? JSON.parse(readFileSync(p, 'utf8')) : null);

// ---------- characterization ----------
const legacy = readJson(find('characterization-legacy.json'));
const modern = readJson(find('characterization-modern.json'));
const byId = (doc) => new Map((doc?.tests ?? []).map((t) => [t.id, t]));
const L = byId(legacy);
const M = byId(modern);
const ids = [...new Set([...L.keys(), ...M.keys()])].sort(compareIds);

const SUITE = {
  ApiCharacterizationTest: 'API (Lese-Endpunkte)',
  ErrorContractCharacterizationTest: 'Fehlerkontrakt',
  DbStateCharacterizationTest: 'DB-Zustand (Schreibpfade)',
};
const SUITE_ORDER = Object.keys(SUITE);
function compareIds(a, b) {
  const ka = SUITE_ORDER.findIndex((k) => a.includes(`.${k}]`));
  const kb = SUITE_ORDER.findIndex((k) => b.includes(`.${k}]`));
  if (ka !== kb) return ka - kb;
  return a.localeCompare(b, 'en', { numeric: true });
}

const CHAR_SRC = 'characterization/src/test/java/at/stoicera/migrationlab/characterization';
const GOLDEN_DIR = 'characterization/src/test/resources/golden';
const lineOf = (path, needle) => {
  try {
    const i = readFileSync(join(ROOT, path), 'utf8').split('\n').findIndex((l) => l.includes(needle));
    return i >= 0 ? i + 1 : null;
  } catch {
    return null;
  }
};

const erklaerungen = readJson(existsSync(join(ROOT, 'docs/nachweis/erklaerungen.json')) ? join(ROOT, 'docs/nachweis/erklaerungen.json') : null) ?? { eintraege: [] };
const erklaerung = (ref) => erklaerungen.eintraege.find((e) => e.ref === ref) ?? null;
const registerLink = (ref, quelle) => at(quelle, lineOf(quelle, ref.startsWith('SD-') ? `| ${ref} |` : `# ${ref}`));

function canonical(text) {
  try {
    const sort = (v) =>
      Array.isArray(v) ? v.map(sort) : v && typeof v === 'object' ? Object.fromEntries(Object.keys(v).sort().map((k) => [k, sort(v[k])])) : v;
    return JSON.stringify(sort(JSON.parse(text)), null, 2);
  } catch {
    return text;
  }
}

const humanize = (t) => {
  if (!t.methode || t.name !== `${t.methode}()`) return t.name;
  const [was, ...rest] = t.methode.split('_');
  const words = (s) => s.replace(/([a-z0-9])([A-Z])/g, '$1 $2').toLowerCase();
  const head = words(was);
  return `${head.charAt(0).toUpperCase()}${head.slice(1)}${rest.length ? ' — ' + rest.map(words).join(' ') : ''}`;
};

const zeilen = ids.map((id) => {
  const l = L.get(id);
  const m = M.get(id);
  const t = m ?? l;
  const ergebnis = (x) => (x ? { status: x.status, ...(x.meldung ? { meldung: x.meldung } : {}) } : { status: 'fehlt' });
  let urteil;
  if (!l || !m) urteil = 'unvollstaendig';
  else if (l.status !== 'bestanden' || m.status !== 'bestanden') urteil = 'regression';
  else if (t.sanktioniert) urteil = 'sanktioniert';
  else urteil = 'identisch';

  const quelle = `${CHAR_SRC}/${t.klasse}.java`;
  const belege = [{ label: 'Test im Quelltext', url: at(quelle, lineOf(quelle, `void ${t.methode}(`)) }];
  let golden = null;
  let vergleich = null;
  const datei = t.golden ?? l?.golden ?? null;
  if (datei) {
    const path = `${GOLDEN_DIR}/${datei}`;
    golden = { datei, url: at(path) };
    belege.push({ label: `Golden ${datei}`, url: golden.url });
    let goldenText = null;
    try {
      goldenText = readFileSync(join(ROOT, path), 'utf8');
    } catch {
      /* golden missing in this checkout — the test itself fails loudly then */
    }
    if (goldenText !== null) {
      const g = canonical(goldenText);
      const mText = m?.empfangen != null ? canonical(m.empfangen) : null;
      const lText = l?.empfangen != null ? canonical(l.empfangen) : null;
      vergleich = {
        golden: g,
        modern: mText,
        ...(lText !== null && lText !== g ? { legacy: lText } : {}),
        gleich: mText !== null && mText === g,
      };
    }
  }
  let sanktion = null;
  if (t.sanktioniert) {
    const { ref, quelle: q } = t.sanktioniert;
    sanktion = { ref, quelle: q, url: registerLink(ref, q), erklaerung: erklaerung(ref) };
    belege.push({ label: `Register ${ref}`, url: sanktion.url });
  }
  return {
    id,
    suite: SUITE[t.klasse] ?? t.klasse,
    name: humanize(t),
    test: { klasse: t.klasse, methode: t.methode },
    endpunkt: t.endpunkt ?? l?.endpunkt ?? null,
    legacy: ergebnis(l),
    modern: ergebnis(m),
    urteil,
    golden,
    vergleich,
    sanktion,
    belege: belege.filter((b) => b.url),
  };
});
const zaehle = (u) => zeilen.filter((z) => z.urteil === u).length;

// ---------- e2e (surefire XML) ----------
function surefire(target) {
  const xmls = files.filter((f) => f.includes(`nachweis-e2e-${target}`) && /TEST-.*\.xml$/.test(f));
  if (xmls.length === 0) return null;
  const klassen = new Map();
  for (const f of xmls) {
    const xml = readFileSync(f, 'utf8');
    for (const m of xml.matchAll(/<testcase\s+([^>]*?)(\/>|>([\s\S]*?)<\/testcase>)/g)) {
      const attr = Object.fromEntries([...m[1].matchAll(/(\w+)="([^"]*)"/g)].map((a) => [a[1], a[2]]));
      const body = m[3] ?? '';
      const k = attr.classname.split('.').pop();
      const e = klassen.get(k) ?? { gesamt: 0, bestanden: 0, fehlgeschlagen: 0, uebersprungen: 0, paket: attr.classname };
      e.gesamt += 1;
      if (/<(failure|error)\b/.test(body)) e.fehlgeschlagen += 1;
      else if (/<skipped\b/.test(body)) e.uebersprungen += 1;
      else e.bestanden += 1;
      klassen.set(k, e);
    }
  }
  return klassen;
}
const e2eL = surefire('legacy');
const e2eM = surefire('modern');
const summe = (k) =>
  k ? [...k.values()].reduce((s, e) => ({ gesamt: s.gesamt + e.gesamt, bestanden: s.bestanden + e.bestanden, fehlgeschlagen: s.fehlgeschlagen + e.fehlgeschlagen }), { gesamt: 0, bestanden: 0, fehlgeschlagen: 0 }) : null;
const e2eSrc = (paket) => `e2e/src/test/java/${paket.replaceAll('.', '/')}.java`;
const e2eKlassen = [...new Set([...(e2eL?.keys() ?? []), ...(e2eM?.keys() ?? [])])].sort().map((k) => {
  const paket = (e2eM?.get(k) ?? e2eL?.get(k)).paket;
  const strip = (e) => (e ? { gesamt: e.gesamt, bestanden: e.bestanden, fehlgeschlagen: e.fehlgeschlagen } : null);
  return { klasse: k, url: at(e2eSrc(paket)), legacy: strip(e2eL?.get(k)), modern: strip(e2eM?.get(k)) };
});

// ---------- image scan ----------
const trivy = readJson(find('trivy.json'));
const imageScan = trivy
  ? {
      werkzeug: 'Trivy',
      schweregrade: 'CRITICAL, HIGH',
      nurBehebbar: true,
      gesamt: (trivy.Results ?? []).reduce((s, r) => s + (r.Vulnerabilities?.length ?? 0), 0),
      ziele: (trivy.Results ?? [])
        .filter((r) => (r.Vulnerabilities?.length ?? 0) > 0)
        .map((r) => ({ ziel: r.Target, typ: r.Type, anzahl: r.Vulnerabilities.length })),
    }
  : null;

const nachweis = {
  schema: 1,
  erzeugt: new Date().toISOString(),
  repo: GH,
  commit: SHA ? { sha: SHA, url: `${GH}/commit/${SHA}` } : null,
  ciLauf: RUN_URL ? { url: RUN_URL } : null,
  register: {
    deviations: at('docs/DEVIATIONS.md'),
    adr0004: at('docs/adr/0004-functional-equivalence-and-sanctioned-divergence.md'),
  },
  charakterisierung: {
    gesamt: zeilen.length,
    identisch: zaehle('identisch'),
    sanktioniert: zaehle('sanktioniert'),
    regression: zaehle('regression'),
    unvollstaendig: zaehle('unvollstaendig'),
    zeilen,
  },
  e2e: {
    legacy: summe(e2eL),
    modern: summe(e2eM),
    klassen: e2eKlassen,
    sanktionen: ['SD-3'].map((ref) => ({
      ref,
      url: registerLink(ref, 'docs/adr/0004-functional-equivalence-and-sanctioned-divergence.md'),
      erklaerung: erklaerung(ref),
    })),
  },
  imageScan,
};

mkdirSync(dirname(OUT) || '.', { recursive: true });
writeFileSync(OUT, JSON.stringify(nachweis, null, 2) + '\n');
const c = nachweis.charakterisierung;
const line = (s) => (s ? `${s.bestanden}/${s.gesamt}` : 'fehlt');
console.log(
  `Nachweis: Charakterisierung ${c.gesamt} (identisch ${c.identisch}, sanktioniert ${c.sanktioniert}, Regression ${c.regression}, unvollständig ${c.unvollstaendig}) · ` +
    `E2E legacy ${line(nachweis.e2e.legacy)}, modern ${line(nachweis.e2e.modern)} · Trivy ${imageScan ? imageScan.gesamt : 'fehlt'} → ${relative(process.cwd(), OUT)}`,
);
if (process.env.GITHUB_STEP_SUMMARY) {
  writeFileSync(
    process.env.GITHUB_STEP_SUMMARY,
    [
      '## Äquivalenz-Nachweis',
      '',
      '| | Anzahl |',
      '|---|---|',
      `| Charakterisierungstests | ${c.gesamt} |`,
      `| identisch | ${c.identisch} |`,
      `| sanktionierte Abweichung | ${c.sanktioniert} |`,
      `| Regression | ${c.regression} |`,
      `| unvollständig | ${c.unvollstaendig} |`,
      `| E2E legacy | ${line(nachweis.e2e.legacy)} |`,
      `| E2E modern | ${line(nachweis.e2e.modern)} |`,
      `| Trivy (CRITICAL/HIGH, behebbar) | ${imageScan ? imageScan.gesamt : 'fehlt'} |`,
      '',
    ].join('\n'),
    { flag: 'a' },
  );
}
// Exit non-zero on a regression so the job is red where the report is red; the gates stay the gates.
process.exitCode = c.regression > 0 || c.unvollstaendig > 0 ? 1 : 0;
