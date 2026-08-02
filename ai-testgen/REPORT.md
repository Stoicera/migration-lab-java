# REPORT.md — AI-assisted test generation, measured

**Status: COMPLETE.** Phase A (as generated) measured 2026-07-31, Phase B (time-boxed repair)
measured 2026-08-02. Phase A's numbers below are unchanged from the day they were published.

Executed under the pre-registered `PROTOCOL.md` **v1.0**, git tag `ai-testgen-protocol-v1`,
SHA-256 `e7d02d2adfe03ca0cfc690794fbcb7db834a967781eab2aec1f6884c97c41037`. Generation run date
**2026-07-31**. Raw artifacts — prompts, requests, responses, generated code including the
broken files, the repaired files, JaCoCo and PIT reports, the fix log — are in
[`runs/2026-07-31/`](runs/2026-07-31/).

Nothing here was curated after the fact. Four post-freeze amendments, all dated, all in
`PROTOCOL.md`: **A1** improved recording and re-ran nothing; **A2** fixed the four Phase-B method
decisions *before* Phase B ran; **A3** and **A4** are disclosures of two defects found *after*
the fact — one in the extraction rule, one in our own repair-effort measurement — and neither
changes a number. Both are labelled throughout as weaker than the pre-declared §8 threats,
because they are.

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

**Belastbarkeit:** N = 6 pro Korpus, ein Versuch je Zelle. Das ist eine sauber gemessene
Illustration, **keine Studie mit statistischer Aussagekraft**. Alle Zahlen sind modell- und
datumsgebunden.

---

## Phase B — nach der Nachbesserung (2026-08-02)

Elf Zellen waren reparaturbedürftig. Jede wurde von einer **eigenen, für die anderen blinden**
KI-Instanz repariert, 30-Minuten-Deckel, mit fest vorgegebenen Kategorien und der harten Regel:
**reparieren ja, dazuschreiben nein** — kein Testfall, den das Modell nicht selbst geschrieben
hat (Nachtrag A2). Nachgeprüft, nicht geglaubt: die Zahl der `@Test`-Methoden ist in **allen 24**
Zellen unverändert; die einzige Ausnahme ist der eine abgeschnittene Fall, wo die angeschnittene
135. Methode entfernt wurde.

| Kennzahl | Phase A (wie generiert) | Phase B (repariert) |
|---|---|---|
| Grüne Zellen | 12 von 24 | **21 von 24** |
| Testmethoden gesamt | 154 (3 rot) | **421 (0 rot)** |
| Line-Coverage | 222/222 = 100 % | 792/875 = **90,5 %** |
| Branch-Coverage | 30/30 = 100 % | 145/184 = **78,8 %** |
| Mutation-Score | 129/130 = 99,2 % | 306/418 = **73,2 %** |

**Lesen Sie diese Tabelle zweimal. Die Qualitätszahlen sind nach der Reparatur *schlechter* —
und das ist das wichtigste Ergebnis des ganzen Meilensteins.**

Nicht weil die Reparatur etwas verschlechtert hätte. Sondern weil die makellosen Phase-A-Werte
nur über die Zellen gerechnet waren, die **zufällig kompilierten** — und das waren ausnahmslos
die kleinen, leichten Controller. Die schwere Klasse war gar nicht im Nenner. Erst die Reparatur
holt sie hinein, und dann sieht man, was wirklich da ist. **Phase A war ein Überlebenden-Effekt
(Survivorship Bias), kein Qualitätsnachweis** — und genau so entstehen die „KI schreibt perfekte
Tests"-Zahlen, die man in Blogposts liest: gemessen wird, was funktioniert hat.

**Die fünf Befunde für die Kalkulation:**

1. **Die Gottklasse bleibt die Mauer — auch nach der Reparatur.** Repariert kompiliert und läuft
   sie, aber sie ist die einzige Klasse im Versuch, die der Prüfung nicht standhält:
   **84,3 % Line-Coverage bei 55,9 % Mutation-Score** (Korpus A) bzw. **82,5 % bei 44,1 %**
   (Korpus B). Jede andere Klasse: 100 % / 100 %. Übersetzt: *Bei der Gottklasse überlebt fast
   jede zweite eingebaute Fehlerveränderung die Tests.* Ein Coverage-Report hätte hier „über
   80 %, passt" gemeldet.
2. **Mehr Tests sind nicht mehr Wert — hier ist die Quittung.** Für denselben Controller
   erzeugte das eine Modell **13** Testmethoden, das andere **134**. Das Ergebnis ist bis auf die
   Stelle identisch: 21/21 Zeilen, 2/2 Zweige, 13/13 Mutanten. **Das Zehnfache an Testcode,
   exakt null zusätzlicher Fehlerfindung** — und der zehnfache Wartungsaufwand für immer.
3. **Nach der Reparatur dreht sich das Modell-Ranking um.** Das billige offene Modell steht bei
   **12 von 12** grünen Zellen, das teure bei 9 von 12. Aber ehrlich gerechnet: **10 der 11
   Reparaturen entfielen auf das billige Modell.** Es ist 18-mal günstiger im Einkauf und hat
   praktisch den gesamten Nachbesserungsaufwand verursacht. Und die drei verbleibenden
   Fehlstellen des teuren Modells sind keine kaputten Tests, sondern zweimal *„gar keine
   Antwort im Budget"* und einmal ein Fehler **unserer eigenen Auswertung** (siehe unten).
4. **Kein einziger echter Programmfehler gefunden.** 15 Reparaturen betrafen falsche Erwartungen
   der Tests — **keine davon** deckte einen Defekt im Produktivcode auf. Wer KI-Tests als
   Fehlersuche verkauft, hat dafür hier keine Belege: Sie zementieren das vorhandene Verhalten,
   sie prüfen es nicht.
5. **Die Modernisierung kann KI-Hilfe kurzfristig *verschlechtern*.** Der einzige saubere
   Korpus-B-Befund: Das offene Modell schrieb `getStatusCodeValue()` — eine Methode, die es in
   Spring 4.3 (Korpus A) gibt und die Spring 7 (Korpus B) **entfernt** hat. Das Spitzenmodell
   benutzte sie korrekt nur im Legacy-Korpus. *Wer auf einen sehr neuen Stack migriert, wird
   kurzfristig schlechter von LLMs unterstützt, weil deren Wissen dem Framework hinterherhinkt* —
   ein realer Übergangseffekt, den man einplanen sollte.

**Was wir zum Aufwand ehrlich NICHT sagen können.** Der Reparaturaufwand sollte in Minuten
berichtet werden. Die gemessenen 154 Minuten über 11 Zellen sind **unbrauchbar als Zeitangabe**:
Weil die Zellen parallel liefen (unsere Entscheidung, Nachtrag A2.1), haben sie sich gegenseitig
die Maschine weggenommen — eine Zelle meldet 24,7 Minuten Wanduhr für einen Maven-Lauf, der
5,6 Sekunden gearbeitet hat. Wir berichten die Minuten trotzdem, als **Obergrenze und als
gemessen**, und sagen dazu, dass die belastbare Größe die **Zahl und Art der Eingriffe** ist:
52 Eingriffe, davon 19 Import/Syntax, 17 Mock-Aufbau, 15 falsche Erwartung, 1 strukturell.
Details und die Selbstkritik dazu: Nachtrag A4 im Protokoll.

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
| Malformed import | 1 | M1 on S4 corpus A — **this row was wrong about the model and is corrected below (amendment A3)** |

> **Correction, 2026-08-02 — the "malformed import" cell was our failure, not the model's.**
> Found while repairing, and reported here because the original row would otherwise stand as an
> unearned point against arm M1.
>
> That cell's reply contains **two** ```` ```java ```` blocks. The first is an empty class body
> carrying the malformed `org.assertj.org.assertj.core.api` import. Between them the model writes,
> verbatim: *"Wait, I need to produce the correct final answer without mistakes. Let me redo it
> properly."* The second block is a complete class with **26 test methods and the import spelled
> correctly**. `finish_reason` is `stop` — the model finished, and its final answer was the good
> one. Our pre-registered §5 rule takes the **first** fenced block, so the pipeline recorded the
> model's self-rejected draft and discarded its actual answer.
>
> **The 12/24 compile rate stands as measured and the cell was not re-extracted.** Re-reading an
> artifact more favourably *after seeing that it cost a number* is exactly the fraud
> pre-registration prevents; the rule does not become wrong because it was expensive. What
> changes is the *description*: this is a **prompt-compliance failure** (the system prompt says
> "exactly one … single ```java code block") plus a naive extraction rule — not broken code.
>
> **Scope, checked rather than assumed:** all 24 responses were scanned. **One** cell is
> affected; 21 contain exactly one block and 2 contain none (the budget-exhausted cells).
> Counterfactual, stated as a counterfactual: under a last-block rule the Phase-A compile rate
> would have been 13/24 rather than 12/24 — about four percentage points of the headline number,
> decided by a line of code nobody would have thought to argue about.

The corrected reading of the taxonomy: **9 of the 12 Phase-A failures are missing or malformed
imports**, i.e. genuinely cheap; 3 are "no answer within the pinned output budget"; and 1 of the
9 is really an artifact of our own extraction rule. Whether "cheap" survives contact with the
measurement is what Phase B answers below — and for the God class, it does not.

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

### 7. Phase B — results after time-boxed repair (2026-08-02)

Eleven cells needed work. Each was repaired by its own agent instance, blind to every other cell,
30-minute cap, fixed categories, and the hard rule that **repair may not add a test the model did
not write** (amendment A2). Compliance was verified mechanically, not asserted: `@Test` counts are
identical in all 24 cells before and after, the single exception being the truncated cell where
the cut-off 135th method was dropped under the A2.4 salvage rule.

The 13 cells that did not need work are carried through unchanged, so
`measurements-repaired.csv` covers all 24 and is directly comparable with the Phase-A file.

| | Phase A | Phase B |
|---|---|---|
| Cells compiling **and** running green | 12 / 24 | **21 / 24** |
| Test methods (failures + errors) | 154 (3) | **421 (0)** |
| Line coverage, target classes | 222/222 = 100 % | 792/875 = **90.5 %** |
| Branch coverage, target classes | 30/30 = 100 % | 145/184 = **78.8 %** |
| PIT mutation score | 129/130 = 99.2 % | 306/418 = **73.2 %** |

Per cell, green-cell counts: M1/A 4→4 · M1/B 5→5 · M2/A 3→**6** · M2/B 0→**6**.

**The quality metrics got worse, and that is the central result.** Not because repair damaged
anything — it added no tests and removed one truncated stub — but because Phase A's perfect
figures were computed over the cells that *happened to compile*, which were exclusively the small
controllers. The God class was absent from the denominator. Repair puts it in, and the aggregate
immediately tells a different story. **Phase A was survivorship bias, not a quality result.** Any
published "LLMs write excellent tests" number computed only over runs that succeeded has the same
defect.

**Per-unit, after repair.** Every class reaches 100 % line and 100 % mutation — except one:

| Cell | Tests | Line | Branch | Mutation |
|---|---|---|---|---|
| M2/A `WerkstattService` (God class) | 36 | 177/210 = 84.3 % | 51/70 = 72.9 % | **62/111 = 55.9 %** |
| M2/B `WerkstattService` (God class) | 40 | 236/286 = 82.5 % | 54/74 = 73.0 % | **49/111 = 44.1 %** |
| every other green cell | 6–134 | 100 % | 100 % | 100 % (one at 94.1 %) |

The God class is the only unit where coverage and mutation score disagree sharply, and the
disagreement is enormous: **~83 % of its lines are covered while ~50 % of injected faults
survive.** A coverage gate at 80 % would have passed this class. That gap is the entire argument
for mutation testing, and it appears exactly where the milestone predicted it would.

**Test count is not test value, quantified.** For `AuftragController` on corpus B, M1 produced
13 test methods and M2 produced 134 — a 10× difference. Their measured value is identical to the
digit: 21/21 lines, 2/2 branches, 13/13 mutants killed. The 121 extra methods find nothing and
must be maintained forever.

### 8. Repair effort — reported, and honestly discounted

| Category | Fixes | What it actually was |
|---|---|---|
| `IMPORT/SYNTAX` | 19 | missing import of the class under test; ambiguous `Date` across two wildcard imports; test-method names containing literal spaces; a removed Spring API |
| `MOCKING-SETUP` | 17 | almost entirely `JdbcTemplate` overload/varargs mismatches — `any()` matching a single vararg, matchers mixed with raw arguments, stubs shadowing each other |
| `WRONG-EXPECTATION` | 15 | year-derived invoice prefixes hard-coded to 2025; umlaut handling in the search term; asserting a re-read entity carries a status the code writes via SQL; a state transition the state machine forbids |
| `STRUCTURAL` | 1 | the A2.4 truncation salvage |
| `BUG-FOUND` | **0** | — |
| `ABANDONED` | 2 | the two cells whose recorded output was the literal `null` |

**Zero `BUG-FOUND` across 15 wrong-expectation repairs.** Not one generated test caught a real
defect in the code under test; every mismatch was the model misreading the code. Generated tests
here **pin existing behaviour**, they do not audit it — which is why the protocol requires a
characterization and E2E layer underneath them (§2).

**The minutes, and why you should not use them.** Total wall clock: **154 minutes over 11 cells**,
range 1.4 – 28.1. Two cells hit the 30-minute cap region: the God class (28.1 min, 14 fixes) and
the 134-method truncated controller (25.3 min, 6 fixes).

These numbers are contaminated, by our own design decision, and amendment **A4** records it in
full. A2.1 ran the cells in parallel to remove learning transfer; up to six agents therefore shared
an 8-core machine while all of them repeatedly invoked Maven. One repairer reported it precisely:
**24.7 wall-clock minutes waiting on a Maven build that itself did 5.6 seconds of work.** The
fix-log timestamps cannot rescue the figure either — several repairers batched their log writes at
the end instead of logging live as §6 requires, so the span is 0.0 minutes in six of eleven cells.

So: the minutes are reported **as measured and as an upper bound**, and the transferable numbers
are the **fix counts and categories** above, which no scheduler can distort. The *ranking* the
minutes imply survives independently — the God class and the 134-method class needed 14 and 6
fixes, a missing import needed 1.

Read together with T8 (the repairer is an AI agent of the same family as arm M1): none of these
figures is a person-minutes estimate for a human team, and A2.1 makes them **per-cell cold-start**
effort — nobody gets faster on the ninth identical missing import, because there is no ninth
repairer who saw the first eight.

### 9. The research questions, answered

- **RQ1 — how much compiles and passes with no human help?** 12 of 24 cells (50 %), 151 of 154
  test methods in the compiling subset. Of the 12 failures, 9 are import-level, 3 are "no answer
  within the pinned budget", and 1 of the 9 is an artifact of our extraction rule (A3).
- **RQ2 — real quality, before and after repair?** As generated, on the surviving subset: 100 %
  coverage, 99.2 % mutation. After repair, on nearly the whole matrix: 90.5 % line, 78.8 % branch,
  **73.2 % mutation**. The honest headline is the second row, and the difference between the two is
  survivorship, not improvement.
- **RQ3 — what does it cost, and where?** €0.65 of tokens for everything; repair concentrated in
  `IMPORT/SYNTAX` (19) and `MOCKING-SETUP` (17), the latter almost entirely `JdbcTemplate` overload
  and varargs mismatches — i.e. **the cost is driven by how hard the dependency is to mock**, not
  by the number of lines. Minutes: see §8 and A4.
- **RQ4 — frontier vs open-weight?** As generated, the frontier model wins clearly (9/12 vs 3/12
  compiling). After repair the open-weight model reaches **12/12** and the frontier model stays at
  9/12 — but **10 of the 11 repairs were spent on the open-weight model**, and the frontier model's
  three gaps are two non-answers and one extraction artifact, not broken code. Cheap buys you
  tokens at 1/18th the price and hands you essentially the entire repair bill.
- **RQ5 — does migrating pay off in testability?** **No effect demonstrated, and we will not
  manufacture one.** The God class scored *lower* on the migrated corpus (44.1 % vs 55.9 %
  mutation) but was *cheaper* to repair there (9.4 vs 28.1 minutes) — contradictory, k = 1,
  noise-dominated, and T7 says any difference would be a migration effect rather than an injection
  effect anyway. The one clean corpus-B finding runs the *other* way: the open-weight model emitted
  `ResponseEntity.getStatusCodeValue()`, which exists in corpus A's Spring 4.3 and was **removed in
  corpus B's Spring 7**. The frontier model used that API only on corpus A. **Migrating to a very
  new stack can make LLM assistance temporarily worse**, because model knowledge lags the
  framework. That is a real, plannable transition cost, and it is the opposite of what the
  milestone hoped to show.

### 10. Reproduce it

```bash
./mvnw -q -f ai-testgen/harness/pom.xml compile exec:java -Dexec.args="plan"
# generation needs OPENROUTER_API_KEY (see .env.example)
./mvnw -q -f ai-testgen/harness/pom.xml compile exec:java \
  -Dexec.args="generate --corpus A --model anthropic/claude-sonnet-5"
./ai-testgen/measure.sh 2026-07-31 anthropic_claude-sonnet-5 A as-generated
./ai-testgen/measure.sh 2026-07-31 anthropic_claude-sonnet-5 A repaired
```

Identical output is not expected even at temperature 0 (T3); that is why every raw response is
committed rather than re-derived. If you replicate Phase B, run the cells **serially** and record
CPU time as well as wall clock — A4 explains what we got wrong.
