#!/usr/bin/env node
// Erklärungen für sanktionierte Abweichungen — build-time, committed, reviewed. Never at request time.
//
//   node scripts/nachweis/erklaerungen.mjs            # writes docs/nachweis/erklaerungen.json
//
// For every sanctioned divergence the script reads the register entry (ADR-0004 table row, or the
// ADR that sanctions it) and the forked test expectation ("the diff": what each stand is pinned to),
// and writes one German paragraph per entry with provenance fields.
//
//   ANTHROPIC_API_KEY set   → Claude formulates the paragraph from register entry and diff
//                             (source: register+claude, model, prompt_sha256, date, usage).
//                             Model: NACHWEIS_MODEL, default claude-opus-5-5 — a config value;
//                             server-side refusal fallback "default" stays on.
//   no key                  → the paragraph is composed deterministically from the register text
//                             (source: register). Same fields, no model.
//
// The SDK is a devDependency of scripts/nachweis only (npm ci there); nothing ships with it.
// Locally the key comes from 1Password at run time, never from a file:
//   (cd scripts/nachweis && npm ci)
//   ANTHROPIC_API_KEY=$(op read "op://Agents/Claude Platform PROD 1/credential") node scripts/nachweis/erklaerungen.mjs
//
// The register entry stays authoritative; the paragraph is a reading aid. register_sha256 pins the
// exact register text it was derived from, so a later register change is visible as drift.

import { readFileSync, writeFileSync } from 'node:fs';
import { createHash } from 'node:crypto';

const ADR4 = 'docs/adr/0004-functional-equivalence-and-sanctioned-divergence.md';
const ADR9 = 'docs/adr/0009-strangler-fig-url-seam-no-ngupgrade.md';
const API_TEST = 'characterization/src/test/java/at/stoicera/migrationlab/characterization/ApiCharacterizationTest.java';
const OUT = 'docs/nachweis/erklaerungen.json';
const MODEL = process.env.NACHWEIS_MODEL || 'claude-opus-5-5';
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

const useModel = Boolean(process.env.ANTHROPIC_API_KEY);
const heute = new Date().toISOString().slice(0, 10);

const SYSTEM = [
  'Du erklärst einer IT-Leitung in Österreich eine sanktionierte Abweichung: eine absichtliche',
  'Verhaltensänderung zwischen einer Altanwendung (Stand 2016) und ihrer modernisierten Fassung.',
  'Schreib genau einen Absatz mit drei bis fünf Sätzen in österreichischem Standarddeutsch, sachlich und ruhig.',
  'Stütze dich ausschließlich auf den Registereintrag und den Testauszug (Diff); geh nie über den Registereintrag hinaus.',
  'Erfinde keine Zahlen, Kunden, Risiken oder Folgen. Keine Superlative, keine Werbesprache, kein Markdown.',
  'Keine Aussagen zu Compliance, Normen, Zertifizierungen oder Rechtskonformität.',
  'Sag, was sich unterscheidet, warum es laut Register absichtlich so ist, und dass beide Seiten durch einen Test festgehalten sind.',
  'Technische Begriffe nur, wenn nötig, dann kurz erklärt.',
  'Felder: erklaerung = der Absatz; quelle = "register"; modell = deine Modellbezeichnung; datum = das im Auftrag genannte Datum.',
].join(' ');

const SCHEMA = {
  type: 'object',
  properties: {
    erklaerung: { type: 'string' },
    quelle: { type: 'string', enum: ['register'] },
    modell: { type: 'string' },
    datum: { type: 'string' },
  },
  required: ['erklaerung', 'quelle', 'modell', 'datum'],
  additionalProperties: false,
};

let client = null;
const usage = { input_tokens: 0, output_tokens: 0, cache_read_input_tokens: 0, cache_creation_input_tokens: 0 };

async function viaClaude(e, register, diff) {
  if (!client) {
    const { default: Anthropic } = await import('@anthropic-ai/sdk');
    client = new Anthropic(); // reads ANTHROPIC_API_KEY from the environment
  }
  const messages = [
    {
      role: 'user',
      content: `Datum: ${heute}\n\nRegistereintrag (${e.ref}, ${e.quelle}):\n${register}\n\nFestgehaltenes Verhalten je Stand (Testauszug):\n${diff}`,
    },
  ];
  const request = {
    model: MODEL,
    max_tokens: 1200,
    betas: ['server-side-fallback-2026-07-01'],
    fallbacks: 'default',
    output_config: { effort: 'low', format: { type: 'json_schema', schema: SCHEMA } },
    // stable system prefix first; it is below the minimum cacheable length today, so expect no hits
    system: [{ type: 'text', text: SYSTEM, cache_control: { type: 'ephemeral' } }],
    messages,
  };
  const res = await client.beta.messages.create(request);
  for (const k of Object.keys(usage)) usage[k] += res.usage?.[k] ?? 0;
  if (res.stop_reason === 'refusal') {
    throw new Error(`${e.ref}: refused (${res.stop_details?.category ?? 'no category'}) — nothing written`);
  }
  if (res.stop_reason !== 'end_turn') throw new Error(`${e.ref}: stop_reason ${res.stop_reason} — nothing written`);
  const text = res.content.filter((b) => b.type === 'text').map((b) => b.text).join('');
  const out = JSON.parse(text);
  return {
    text: out.erklaerung.trim().replace(/\s*\n+\s*/g, ' '),
    // provenance from the API response, not from the model's self-report
    model: res.model,
    prompt_sha256: sha256(JSON.stringify({ system: SYSTEM, schema: SCHEMA, messages })),
    usage: res.usage,
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

const eintraege = [];
for (const e of EINTRAEGE) {
  const register = e.register();
  const diff = e.diff();
  const r = useModel ? await viaClaude(e, register, diff) : ausRegister(e, register);
  eintraege.push({
    ref: e.ref,
    titel: e.titel,
    quelle: e.quelle,
    text: r.text,
    source: useModel ? 'register+claude' : 'register',
    model: r.model,
    prompt_sha256: r.prompt_sha256,
    register_sha256: sha256(register),
    diff_sha256: sha256(diff),
    date: heute,
    review: 'offen',
  });
  console.log(`${e.ref}: ${useModel ? r.model : 'register'} — ${r.text.length} Zeichen${r.usage ? ` · in ${r.usage.input_tokens} / out ${r.usage.output_tokens}` : ''}`);
}
writeFileSync(
  OUT,
  JSON.stringify(
    {
      hinweis:
        'Lesehilfe zu sanktionierten Abweichungen, erzeugt zur Build-Zeit von scripts/nachweis/erklaerungen.mjs, im Review geprüft. Register entscheidet, Claude formuliert. Maßgeblich ist der Registereintrag.',
      systemPrompt: SYSTEM,
      eintraege,
    },
    null,
    2,
  ) + '\n',
);
console.log(`→ ${OUT}`);
if (useModel) console.log(`Tokens gesamt: ${JSON.stringify(usage)}`);
