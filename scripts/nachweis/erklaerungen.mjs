#!/usr/bin/env node
// Erklärungen für sanktionierte Abweichungen — build-time, committed, reviewed. Never at request time.
//
//   node scripts/nachweis/erklaerungen.mjs            # writes docs/nachweis/erklaerungen.json
//
// For every sanctioned divergence the script reads the register entry (ADR-0004 table row, or the
// ADR that sanctions it) and the forked test expectation ("the diff": what each stand is pinned to),
// and writes one German paragraph per entry with provenance fields.
//
//   OPENROUTER_API_KEY set  → the paragraph is formulated by a model via OpenRouter
//                             (source: openrouter, model, prompt_sha256, date).
//                             Model: NACHWEIS_MODEL, default mistralai/mistral-large-2512 — a config
//                             value, not a code dependency; any OpenRouter model id works.
//   no key                  → the paragraph is composed deterministically from the register text
//                             (source: register). Same fields, no model.
//
// Locally the key comes from 1Password, never from a file:
//   OPENROUTER_API_KEY=$(op read op://Agents/OpenRouter/credential) node scripts/nachweis/erklaerungen.mjs
//
// The register entry stays authoritative; the paragraph is a reading aid. register_sha256 pins the
// exact register text it was derived from, so a later register change is visible as drift.

import { readFileSync, writeFileSync } from 'node:fs';
import { createHash } from 'node:crypto';

const ADR4 = 'docs/adr/0004-functional-equivalence-and-sanctioned-divergence.md';
const ADR9 = 'docs/adr/0009-strangler-fig-url-seam-no-ngupgrade.md';
const API_TEST = 'characterization/src/test/java/at/stoicera/migrationlab/characterization/ApiCharacterizationTest.java';
const OUT = 'docs/nachweis/erklaerungen.json';
const MODEL = process.env.NACHWEIS_MODEL || 'mistralai/mistral-large-2512';
const sha256 = (s) => createHash('sha256').update(s).digest('hex');
const read = (p) => readFileSync(p, 'utf8');

function registerRow(ref) {
  const row = read(ADR4).split("\n").map((l) => l.trim()).find((l) => l.startsWith(`| ${ref} |`));
  if (!row) throw new Error(`${ref} not found in ${ADR4}`);
  const [, seit, abweichung, grund] = row.split(' | ').map((c) => c.replace(/^\| ?| ?\|$/g, '').trim());
  return { seit, abweichung, grund, text: row };
}

/** Body of a test method — the forked expectation is the diff between the stands. */
function methodBody(path, name) {
  const src = read(path);
  const start = src.indexOf(`void ${name}(`);
  if (start < 0) throw new Error(`${name} not found in ${path}`);
  let depth = 0;
  for (let i = src.indexOf('{', start); i < src.length; i++) {
    if (src[i] === '{') depth++;
    if (src[i] === '}' && --depth === 0) return src.slice(start, i + 1);
  }
  throw new Error(`unbalanced ${name}`);
}

function adr9Decision1() {
  const src = read(ADR9);
  const from = src.indexOf('1. **Coexistence');
  return src.slice(from, src.indexOf('2. **', from)).trim();
}

const prop = (file, key) => read(file).split('\n').find((l) => l.startsWith(`${key}=`));

const EINTRAEGE = [
  {
    ref: 'SD-1',
    titel: 'Suche mit feindlicher Eingabe',
    quelle: ADR4,
    register: () => registerRow('SD-1').text,
    diff: () => methodBody(API_TEST, 'sucheMitInjektionsEingabe') + '\n\n' + methodBody(API_TEST, 'sucheMitEinzelnemHochkomma'),
  },
  {
    ref: 'SD-2',
    titel: 'Adminseite',
    quelle: ADR4,
    register: () => registerRow('SD-2').text,
    diff: () => methodBody(API_TEST, 'adminPageMatchesGolden'),
  },
  {
    ref: 'SD-3',
    titel: 'Meldung bei abgelehnter Aktion',
    quelle: ADR4,
    register: () => registerRow('SD-3').text,
    diff: () =>
      [
        'legacy:  ' + prop('e2e/src/test/resources/selectors/legacy.properties', 'alert.rechnungDuplikat'),
        'modern:  ' + prop('e2e/src/test/resources/selectors/modern.properties', 'alert.rechnungDuplikat'),
      ].join('\n'),
  },
  {
    ref: 'ADR-0009',
    titel: 'Seitenadressen ohne #!',
    quelle: ADR9,
    register: adr9Decision1,
    diff: () => methodBody(API_TEST, 'spaRouteServesDocument'),
  },
];

const SYSTEM = [
  'Du erklärst einer IT-Leitung oder Geschäftsführung in Österreich eine absichtliche Verhaltensänderung',
  'zwischen einer Altanwendung (Stand 2016) und ihrer modernisierten Fassung.',
  'Regeln: genau ein Absatz, drei bis fünf Sätze, österreichisches Standarddeutsch, sachlich und ruhig.',
  'Keine Superlative, keine Werbesprache, keine Aufzählungen, kein Markdown.',
  'Verwende nur Fakten aus dem Registereintrag und dem Testauszug; erfinde keine Zahlen, Kunden oder Folgen.',
  'Sag, was sich unterscheidet, warum es absichtlich so ist, und dass beide Seiten durch einen Test festgehalten sind.',
  'Technische Begriffe nur, wenn nötig, dann kurz erklärt.',
].join(' ');

async function viaOpenRouter(e, register, diff) {
  const messages = [
    { role: 'system', content: SYSTEM },
    { role: 'user', content: `Registereintrag (${e.ref}, ${e.quelle}):\n${register}\n\nFestgehaltenes Verhalten je Stand (Testauszug):\n${diff}` },
  ];
  const body = { model: MODEL, messages, temperature: 0.2, max_tokens: 400 };
  const res = await fetch('https://openrouter.ai/api/v1/chat/completions', {
    method: 'POST',
    headers: { Authorization: `Bearer ${process.env.OPENROUTER_API_KEY}`, 'Content-Type': 'application/json', 'X-Title': 'migration-lab Nachweis' },
    body: JSON.stringify(body),
  });
  if (!res.ok) throw new Error(`OpenRouter ${res.status}: ${await res.text()}`);
  const json = await res.json();
  return {
    text: json.choices[0].message.content.trim().replace(/\s*\n+\s*/g, ' '),
    model: json.model ?? MODEL,
    prompt_sha256: sha256(JSON.stringify(messages)),
  };
}

function ausRegister(e, register) {
  const plain = (t) => t.replace(/\s*\*\(.*?\)\*/g, '').replace(/\*\*|`/g, '').replace(/\s+/g, ' ').trim().replace(/([^.!?])$/, '$1.');
  const zeile = e.ref.startsWith('SD-') ? registerRow(e.ref) : null;
  // ADR-0009: the first two sentences of decision 1 carry the change; the rest is rationale
  const entscheidung = zeile ? null : plain(register.replace(/^1\.\s*/, '')).split(/(?<=\.)\s+/).slice(0, 3).join(' ');
  const text = zeile
    ? `${e.titel} (${e.ref}, seit ${zeile.seit.replace('stage', 'Etappe')}): Der moderne Stand verhält sich hier absichtlich anders als der Stand von 2016; ` +
      `beide Seiten sind durch einen Test festgehalten. Registereintrag (englisch): ${plain(zeile.abweichung)} Begründung: ${plain(zeile.grund)}`
    : `${e.titel} (${e.ref}): Der moderne Stand verhält sich hier absichtlich anders als der Stand von 2016; ` +
      `beide Seiten sind durch einen Test festgehalten. Entscheidung (englisch): ${entscheidung}`;
  return { text, model: null, prompt_sha256: null };
}

const useModel = Boolean(process.env.OPENROUTER_API_KEY);
const heute = new Date().toISOString().slice(0, 10);
const eintraege = [];
for (const e of EINTRAEGE) {
  const register = e.register();
  const diff = e.diff();
  const r = useModel ? await viaOpenRouter(e, register, diff) : ausRegister(e, register);
  eintraege.push({
    ref: e.ref,
    titel: e.titel,
    quelle: e.quelle,
    text: r.text,
    source: useModel ? 'openrouter' : 'register',
    model: r.model,
    prompt_sha256: r.prompt_sha256,
    register_sha256: sha256(register),
    diff_sha256: sha256(diff),
    date: heute,
    review: 'offen',
  });
  console.log(`${e.ref}: ${useModel ? r.model : 'register'} — ${r.text.length} Zeichen`);
}
writeFileSync(
  OUT,
  JSON.stringify(
    {
      hinweis:
        'Lesehilfe zu sanktionierten Abweichungen, erzeugt zur Build-Zeit von scripts/nachweis/erklaerungen.mjs, im Review geprüft. Maßgeblich ist der Registereintrag.',
      systemPrompt: SYSTEM,
      eintraege,
    },
    null,
    2,
  ) + '\n',
);
console.log(`→ ${OUT}`);
