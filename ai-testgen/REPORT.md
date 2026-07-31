# REPORT.md — AI-assisted test generation, measured

**Status: Phase A (as generated) complete and reported below. Phase B (time-boxed repair)
not yet executed** — it runs in the next session and this document is extended, not rewritten.

Executed under the pre-registered `PROTOCOL.md` **v1.0**, git tag `ai-testgen-protocol-v1`,
SHA-256 `e7d02d2adfe03ca0cfc690794fbcb7db834a967781eab2aec1f6884c97c41037`. Run date
**2026-07-31**. Raw artifacts — prompts, requests, responses, generated code including the
broken files, JaCoCo and PIT reports — are in [`runs/2026-07-31/`](runs/2026-07-31/).
Nothing here was curated after the fact; the one post-freeze change is amendment A1, which
improved *recording* and explicitly did not re-run anything.

---

## Deutsche Zusammenfassung

**Die Frage:** Können Sprachmodelle die fehlenden Unit-Tests für ein Legacy-System
nachliefern — und ändert sich das, wenn man das System vorher migriert hat?

**Der Aufbau:** sechs Klassen (Gottklasse, drei REST-Controller, Adminseite, ein reiner
Datenhalter als Negativkontrolle), je zweimal gemessen — einmal im Zustand von 2016
(Korpus A) und einmal als migriertes Gegenstück auf Boot 4.1 / Java 25 (Korpus B). Zwei
Modelle, identische Prompts, ein Versuch pro Klasse. 24 Aufrufe, **Gesamtkosten € 0,65**.

**Das Ergebnis in einem Satz:** Wo ein Testklassen-Entwurf überhaupt kompilierte, war er
ausgezeichnet — aber er kompilierte nur in der **Hälfte** der Fälle, und die eigentliche
Zielklasse, die 613-Zeilen-Gottklasse, hat **kein einziges Modell in keinem Korpus**
brauchbar erzeugt.

| Kennzahl (Phase A, wie generiert) | Wert |
|---|---|
| Kompilierrate | **12 von 24 (50 %)** |
| Pass-Rate der kompilierenden Klassen | 151 von 154 Testmethoden (98,1 %) |
| Line-Coverage auf der Zielklasse | 222/222 = **100 %** |
| Branch-Coverage auf der Zielklasse | 30/30 = **100 %** |
| Mutation-Score (PIT, grüne Teilmenge) | 129/130 = **99,2 %** |
| Kosten gesamt | **€ 0,6482** (M1 € 0,614 · M2 € 0,034) |

**Vier Befunde, die ein Entscheider mitnehmen sollte:**

1. **Die Gottklasse ist die Mauer, nicht die Fleißarbeit.** Genau die Klasse, für die man
   sich KI-Hilfe wünscht, war der einzige totale Fehlschlag: Modell M1 verbrauchte sein
   gesamtes Ausgabebudget für internes „Nachdenken“ und lieferte **gar keine Antwort**
   (in beiden Korpora), Modell M2 lieferte in beiden Korpora nicht kompilierenden Code.
   Die 78 Zeilen langen Controller gelangen dagegen mühelos. *Große, ungetrennte Klassen
   sind auch für die KI teuer — der Reflex „die KI macht das schon“ trägt dort am
   wenigsten, wo der Schmerz am größten ist.*
2. **Wo es klappte, war die Qualität real, nicht nur Coverage-Kosmetik.** 100 % Zeilen- und
   Zweigabdeckung *und* 99,2 % Mutation-Score heißt: die Tests behaupten nicht nur, sie
   fangen tatsächlich eingebaute Fehler. Das ist der Unterschied, den ein Coverage-Report
   allein nie zeigt.
3. **Der Preisunterschied ist gewaltig, der Qualitätsunterschied auch.** Das offene Modell
   war **18-mal billiger** (€ 0,034 gegen € 0,614) — und kompilierte in 3 von 12 Fällen
   gegen 9 von 12. Billig ist hier nicht „fast so gut“.
4. **Kosten sind nicht das Problem.** Der komplette Versuch kostete weniger als ein Kaffee.
   Die relevanten Kosten stehen in Phase B (Nachbesserung) — und die wird erst gemessen.

**Was hier noch nicht steht:** der Nachbesserungsaufwand. Die Hälfte der Zellen ist
reparaturbedürftig, und erst Phase B beantwortet die eigentliche Wirtschaftlichkeitsfrage:
*Wie teuer ist der Weg von „generiert“ zu „brauchbar“?* Ohne diese Zahl ist jede Aussage über
den Nutzen von KI-Testgenerierung unvollständig — deshalb steht hier ausdrücklich keine.

**Belastbarkeit:** N = 6 pro Korpus, ein Versuch je Zelle. Das ist eine sauber gemessene
Illustration, **keine Studie mit statistischer Aussagekraft**. Alle Zahlen sind modell- und
datumsgebunden.

---

## English detail

### 1. What was run

Six units per corpus, fixed at freeze (`Catalog.java`, guarded by a test), each generated once
per model arm at temperature 0, `top_p 1`, `max_tokens 16000`:

| Arm | Model | Serving provider(s) observed |
|---|---|---|
| M1 | `anthropic/claude-sonnet-5` | Amazon Bedrock (12/12 calls) |
| M2 | `qwen/qwen3-coder-next` | Ionstream, Novita, Alibaba, Parasail, StreamLake |

Corpus A = `legacy/` (Java 8, Boot 1.5, field injection). Corpus B = `modern/` (Java 25,
Boot 4.1, constructor injection). The prompts differ in exactly one sentence — the stack
description — which is asserted by a test.

### 2. Phase A results, per unit

| Stratum | Unit | M1 corpus A | M2 corpus A | M1 corpus B | M2 corpus B |
|---|---|---|---|---|---|
| S1 | `WerkstattService` | — (budget) | compile ✗ | — (budget) | compile ✗ |
| S2 | `KundenController` | 14 tests, 14 pass, mut 17/17 | 13 tests, 13 pass, mut 16/17 | 13 tests, 13 pass, mut 17/17 | compile ✗ |
| S2 | `AuftragController` | 12 tests, 12 pass, mut 13/13 | 22 tests, 19 pass, mut 13/13 | 13 tests, 13 pass, mut 13/13 | — (budget) |
| S2 | `RechnungController` | 9 tests, 9 pass, mut 8/8 | 10 tests, 10 pass, mut 8/8 | 10 tests, 10 pass, mut 8/8 | compile ✗ |
| S3 | `AdminController` | 6 tests, 6 pass, mut 2/2 | compile ✗ | 8 tests, 8 pass, mut 2/2 | compile ✗ |
| S4 | `Rechnung` | compile ✗ | compile ✗ | 24 tests, 24 pass, mut 12/12 | compile ✗ |

"— (budget)" = the call stopped with `finish_reason=length`, i.e. it exhausted the
pre-registered 16 000-token output budget. Per protocol these count as non-compiling; per
amendment A1 they must be read as *"no answer within the pinned budget"*, never as *"the model
produced broken code"* — see §5.

Compile rate by cell: M1/A 4/6 · M2/A 3/6 · M1/B **5/6** · M2/B **0/6**.

### 3. Failure taxonomy (Phase A, before any repair)

| Cause | Count | Detail |
|---|---|---|
| Output budget exhausted | 3 | M1 on S1 in both corpora (entire budget spent on reasoning tokens, message content never started); M2 on `AuftragController` corpus B (visible truncation mid-method, after emitting an increasingly degenerate test-method name) |
| Missing imports | 8 | M2 dominated: `class Rechnung`, `ResponseEntity`, `ArgumentCaptor` used without importing them |
| Malformed import | 1 | M1 on S4 corpus A emitted `org.assertj.org.assertj.core.api` — a duplicated package prefix |

Every one of these is cheap to repair by hand, which is exactly what Phase B will quantify.
A 50 % compile rate with an expected-cheap repair profile is a very different economic story
from a 50 % rate that needs redesign — the numbers to distinguish them do not exist yet.

### 4. Observations that survive the small sample

**The God class defeated both models in both corpora.** This is the finding with the most
practical weight, because it inverts the intuition: the trivial 55-line controllers were
handled flawlessly, the 613-line class that actually needs the help produced nothing usable.
The two mechanisms differed — M1 ran out of budget while reasoning, M2 emitted non-compiling
code — but the outcome was identical four times out of four.

**Where output compiled, it was strong.** 100 % line and branch coverage on the target class
and a 99.2 % mutation score is not a coverage-theatre result: the tests kill the mutants.
The single surviving mutant (M2, `KundenController`, corpus A) and the three failing test
methods (M2, `AuftragController`, corpus A) are the honest exceptions.

**The negative control behaved only half as predicted.** `Rechnung`, a pure data holder, was
expected to show high coverage and *low* mutation value. It produced 24 tests, 100 % coverage
and 12/12 mutants killed — but note the ratio: 12 mutants over 34 covered lines, against
17 mutants over 19 lines for `KundenController`. The data holder does yield far less mutation
signal per line; it just does not yield a *lower kill rate*. Reported as measured rather than
as the expectation.

**Corpus B did not obviously beat corpus A** — and the one cell where the corpora diverge
sharply (M2/B, 0/6) is a missing-imports cluster, not a testability effect. With k = 1 this is
noise-dominated. **No RQ5 claim is made from Phase A.** If a migration effect exists in this
data, it will have to show up in the repair effort of Phase B, and even then threat T7 applies:
the corpora differ by parameterized SQL and the absorbed admin page, not only by injection style.

### 5. Threats to validity, as they actually played out

- **T3 (provider routing) is confirmed, not hypothetical.** One model ID, five different
  serving backends within twelve calls, no request-side difference. Any replication of arm M2
  is a replication of a routing lottery.
- **The `max_tokens` cap was a binding constraint, and it was our choice.** 16 000 was
  pre-registered by us; a reasoning-enabled model spent all of it thinking. This is a property
  of the experiment design meeting a model behaviour, and it is reported as such rather than
  charged to the model. A replication should raise the cap — and would then measure something
  different, which is why it is not being done here.
- **T1 (contamination), T2 (N = 6, k = 1), T5 (German identifiers), T6 (date-bound)** stand as
  pre-declared. No significance is claimed anywhere in this document.
- **T4/T8 (single rater; the repairer is an AI agent of the same family as M1)** do not affect
  Phase A at all — Phase A involves no human or agent judgement, only mechanical measurement.
  They become live in Phase B, and every Phase-B number will carry them.

### 6. Cost

| | Calls | Prompt tokens | Completion tokens | EUR |
|---|---|---|---|---|
| M1 `anthropic/claude-sonnet-5` | 12 | 53 456 | 59 831 | 0.6140 |
| M2 `qwen/qwen3-coder-next` | 12 | 30 741 | 44 401 | 0.0341 |
| **Total** | **24** | **84 197** | **104 232** | **0.6482** |

Computed from the price table pinned at freeze, converted at the ECB reference rate published
2026-07-31 (1 EUR = 1.1485 USD), recorded per call. Budget cap €20, never approached.
Note that 32 000 of M1's completion tokens — more than half its total — were the two S1 calls
that produced no answer at all. **Reasoning tokens are billed whether or not they lead to an
answer.**

### 7. Reproduce it

```bash
./mvnw -q -f ai-testgen/harness/pom.xml compile exec:java -Dexec.args="plan"
# generation needs OPENROUTER_API_KEY (see .env.example)
./mvnw -q -f ai-testgen/harness/pom.xml compile exec:java \
  -Dexec.args="generate --corpus A --model anthropic/claude-sonnet-5"
./ai-testgen/measure.sh 2026-07-31 anthropic_claude-sonnet-5 A as-generated
```

Identical output is not expected even at temperature 0 (T3); that is why every raw response is
committed rather than re-derived.
