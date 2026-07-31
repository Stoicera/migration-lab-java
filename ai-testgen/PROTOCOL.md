# PROTOCOL.md — AI-assisted test generation, pre-registered

**Status: FROZEN — v1.0, 2026-07-31, tag `ai-testgen-protocol-v1`.**

Drafted 2026-07-30 (v0.1), reviewed and accepted by the owner, finalized and frozen on
2026-07-31 after the freeze checklist in §9 was ticked with evidence. From the freeze
commit on, changes are allowed **only** as dated entries in the *Amendments* appendix,
**only** for steps that have not yet run, and never retroactively. `REPORT.md` must cite
the git hash of the frozen protocol it was executed under; every recorded call carries the
SHA-256 of this file (`usage.json` → `protocolSha256`).

Everything in §1–§8 was written **before** a single generation call was made. No API call
had happened at freeze time; the harness dry-run of §5 uses hand-written tests only.

**Two dimensions, deliberately different words for them:**

- **Model arms M1 / M2** — the two models compared (§3).
- **Corpora A / B** — the *code* under test: A = `legacy/` (Java 8, field injection),
  B = `modern/` (Java 25, constructor injection). Corpus B is **IN** (§4).

---

## 1. Research questions

- **RQ1** — What fraction of LLM-generated unit tests for genuinely legacy code
  (field injection, God class, JdbcTemplate + inline SQL, no seams) compiles and
  passes without any human help?
- **RQ2** — What is their real quality — JaCoCo line/branch coverage and **PIT
  mutation score** — as generated vs. after time-boxed repair?
- **RQ3** — What does it cost: API tokens/EUR and repair minutes, and in which
  categories does the repair effort concentrate?
- **RQ4** — How does a frontier commercial model compare to an open-weight model
  under identical prompts and procedure?
- **RQ5** (corpus B) — Does the migration itself change AI test-gen effectiveness? The same
  six classes, the same prompts, once as 2016 legacy and once as their migrated
  counterparts. This is the question the stage-4 constructor-injection sweep was a
  precondition for, and the one a decision-maker actually asks: *does modernizing pay off
  in testability, measurably?*

Results are published in `REPORT.md` (German summary + English detail). Failed
generations stay in the repo. Nothing is curated afterwards.

## 2. Code under test

`legacy/` at the protocol-freeze commit (frozen by repo rule since `stage-0-legacy`) and
`modern/` at the same commit. Unit level only, **no Spring context, no real database, no
network** — dependencies are mocked. Production code is never modified; if a generated
test exposes a genuine bug, the test is aligned to actual behaviour and the finding is
tagged `BUG-FOUND` (characterization mindset — legacy stays legacy).

### Stratified selection (fixed at freeze)

Deviation from SPEC §5, recorded honestly: SPEC names a "mappers" stratum, but the
legacy app has **no standalone mapper classes** — all row mapping lives as anonymous
`RowMapper`s inside the God class. The strata below reflect the code as it actually is.

| Stratum | Class (FQN `at.werkstatt.crm.…`) | LOC A / B | Why selected |
|---|---|---|---|
| S1 God service | `service.WerkstattService` | 613 / 728 | The headline target: JdbcTemplate, inline RowMappers, status-flow validation, VAT math, report aggregation — "untestable" legacy in one class |
| S2 REST controller | `controller.KundenController` | 78 / 77 | CRUD + search path (the former B4 injection-shaped query) |
| S2 REST controller | `controller.AuftragController` | 80 / 79 | Status lifecycle endpoint (business-rule rejection paths) |
| S2 REST controller | `controller.RechnungController` | 56 / 55 | Invoice creation + duplicate rejection |
| S3 Mixed tech | `controller.AdminController` | 58 / 50 | JSP + gson servlet-era shape — the awkward case |
| S4 Negative control | `model.Rechnung` | 113 / 111 | Pure data holder. Expectation: trivially high coverage, low mutation value — detects assert-getter test generation and keeps the metrics honest |

LOC are whole-file line counts (`wc -l`), measured at freeze. Corpus B's larger S1 count is
formatting (google-java-format, stage 5), not added behaviour.

Excluded (reason logged, no other selection happens): `FahrzeugController`,
`BerichtController` (shape-redundant with S2 picks), remaining models (redundant with
S4), `WerkstattCrmApplication` (bootstrap), `config.JacksonWireCompatConfig` and
`controller.SpaForwardController` (corpus B only — a class without a corpus-A twin cannot
carry an A/B comparison). **N = 6 units per corpus, 12 unit×corpus cells, 24 calls at k = 1.**

The selection lives in code as well (`harness/…/Catalog.java`); `CatalogTest` fails the
build if a selected class is missing, if the two corpora stop holding the same six classes,
or if `legacy/src/test` ever appears.

## 3. Model arms

Both arms get identical prompts, procedure, and evaluation. One generation call per
unit per model (k = 1), `temperature 0`, `top_p 1`, `max_tokens 16000`; one retry only on
transport error (HTTP 429/5xx, timeout, connection failure), never on unsatisfying content.

| Arm | OpenRouter ID | Price (in/out per M tokens) | Verified live |
|---|---|---|---|
| M1 commercial frontier | `anthropic/claude-sonnet-5` | $2.00 / $10.00 | 2026-07-31 (freeze day) |
| M2 open-weight | `qwen/qwen3-coder-next` | $0.12 / $0.80 | 2026-07-31 (freeze day) |

M2 was confirmed at freeze (alternatives considered and dropped: `deepseek/deepseek-v4-pro`,
`z-ai/glm-5.1`, `openai/gpt-oss-120b`). Prices were re-read from the live OpenRouter model
list on freeze day and are pinned in `harness/…/Pricing.java`; the harness refuses any model
outside this table. Costs are always computed from the pinned table, never from a later price.

OpenRouter may route one model ID to different backend providers (quantization may
differ) — the actual serving provider from each response is recorded per call and listed
in the report (threat T3).

Budget cap: hard abort at **€20** total API spend, enforced in the harness against the sum
of all recorded `usage.json` files (global, not per invocation). Expected spend is far
below: 24 calls ≈ €1.20–1.50, dominated by M1.

## 4. Prompt template (v1, part of the pre-registration)

One prompt per unit, built mechanically by the harness — no manual per-class editing.
Every rendered prompt instance is committed under
`runs/<date>/<model>/<corpus>/<unit>/prompt.md`.

**System prompt:**

````text
You are generating JUnit 5 unit tests for a <STACK> codebase.
Constraints:
- JUnit 5.14.x, Mockito (with mockito-junit-jupiter), AssertJ. From spring-test, only
  org.springframework.test.util.ReflectionTestUtils is available. No other libraries.
- Unit tests only: no Spring context, no MockMvc, no real database, no network, no file I/O.
- Mock all dependencies of the class under test and instantiate it as it is — the class under
  test must not be modified.
- Output exactly one complete, compilable test class in a single ```java code block,
  package <TEST_PACKAGE>, class name <TEST_CLASS>.
- Aim for behavioural coverage: happy paths, edge cases, error/rejection paths.
- Do not invent methods that do not exist in the provided source.
````

**User prompt (per unit):**

````text
Class under test (full source):
<SOURCE>

Direct dependency types visible to the class (signatures only):
<DEPENDENCY_SIGNATURES>

Database schema excerpt referenced by the SQL in this class (DDL, if any):
<DDL_EXCERPT>

Write the test class now.
````

Placeholders, all filled mechanically (`PromptRenderer`, `SourceFacts`):

- `<STACK>` — `legacy Java 8 / Spring Boot 1.5` (corpus A) or `Java 25 / Spring Boot 4.1`
  (corpus B). **This is the only text that differs between the corpora** — a prompt that
  claimed the wrong stack would be a bug, and any further wording difference would confound
  RQ5. `PromptRendererTest` pins that the two system prompts are otherwise byte-identical.
- `<TEST_PACKAGE>` — `at.werkstatt.crm.gen`, deliberately *not* the package of the class
  under test, so JaCoCo and PIT can exclude the generated tests from measurement.
- `<TEST_CLASS>` — `<SimpleName>GeneratedTest`.
- `<SOURCE>` — the class file verbatim.
- `<DEPENDENCY_SIGNATURES>` — public declaration lines of every project-internal type the
  class references, bodies stripped, sorted by FQN.
- `<DDL_EXCERPT>` — the `CREATE TABLE` statements of every table named in the class's SQL,
  in schema order; the literal text `(none — this class contains no SQL)` otherwise.

`PromptTemplateDriftTest` compares the two blocks above, character by character, with the
constants the harness actually sends. Protocol and code cannot drift apart silently.

**`spring-test` on the classpath (decided at freeze, deviation from draft v0.1):** the draft
allowed only JUnit/Mockito/AssertJ. Withholding `ReflectionTestUtils` would have handicapped
corpus A against what every real Spring shop has on its classpath, and would have made the
A/B result look better than reality. It is offered to both corpora identically; MockMvc and
any Spring context stay forbidden by the prompt, so "unit test, no container" still holds.
A generated test that boots a context or uses MockMvc is a `STRUCTURAL` repair case, not a
tolerated variant.

Explicitly **out of scope** (future work, not this experiment): re-prompting the generating
model, multi-turn generation, self-repair loops driven by the *generating* model, and
prompt-optimization. Phase B repair (§6) is a separate, time-boxed, categorized activity
performed by a different actor and is never fed back into a second generation call.

**Corpus B: IN** (owner decision at freeze; draft default was OUT). Reasons recorded so the
scope change is auditable: it answers RQ5, which is the question this project's audience
actually has; it costs one additional model call per unit (≈ €0.60 total); and it is the
only path by which `modern/` gets a unit-test suite at all — three `deferred(G6)` entries in
`docs/DEVIATIONS.md` (coverage gate, ArchUnit, Testcontainers) hang on this milestone. The
adoption of repaired corpus-B tests into `modern/src/test` is a **separate step after the
experiment**, and the run artifacts under `runs/` stay immutable when it happens.

## 5. Harness and measurement environment

- `ai-testgen/testbed/legacy/` and `ai-testgen/testbed/modern/`: two Maven modules that
  compile the corpus's `src/main/java` as an additional source root (corpus A at
  `--release 8`, corpus B at `--release 25`) and host the generated tests under
  `src/test/java`. `legacy/pom.xml` and `modern/pom.xml` are never touched, and **no test
  ever lands in `legacy/`** — the exhibit stays test-free (hard repo rule). Each testbed
  copies its corpus's dependency block verbatim; `PomDriftGuardTest` fails the run if the
  copy and the original diverge.
- Pinned tooling, all re-verified live against Maven Central on freeze day (2026-07-31):
  **JUnit 5.14.4** (ADR-0006; 6.1.2 exists and is deliberately not used — the safety net
  stays boring) · **Mockito 5.23.0** (+ `mockito-junit-jupiter`, attached as a `-javaagent`
  rather than self-attaching, which JDK 26 still allows but warns about) ·
  **AssertJ 3.27.7** (4.0.0-M1 is a milestone, not used) · **spring-test** at the corpus's
  own Spring version (BOM-managed: 4.3.x for A, 7.x for B) · **JaCoCo 0.8.15** ·
  **pitest-maven 1.25.8** + **pitest-junit5-plugin 1.2.3**, **DEFAULTS** mutator set,
  `targetClasses` scoped to the class under test per run, `skipFailingTests=true` (below).
  The build plugins are pinned too (**maven-compiler-plugin 3.15.0**, **maven-surefire-plugin
  3.5.6**): a measurement environment that inherits its compiler and test runner from whatever
  Maven the replicator happens to have is not reproducible.
- **PIT and red suites (decided at freeze, forced by the self-test below):** PIT refuses to
  analyse a suite that has failing tests at all — and Phase-A suites are expected to be red.
  With `skipFailingTests=true` the mutation score is therefore the score of the **green
  subset** of the generated tests. Failing tests stay fully counted in the pass rate, and
  every Phase-A mutation number in the report names the subset it was computed on. The
  alternative — "not measurable" whenever one test fails — would discard the most
  interesting cases.
- `ai-testgen/measure.sh`: step 4 of §6 as a script — places one unit's class in the testbed,
  runs compile → test → JaCoCo → PIT in a single Maven invocation, copies every report back
  next to the run artifacts, and appends one row per unit to `measurements-<phase>.csv`.
- `ai-testgen/harness/`: renders prompts, calls OpenRouter through an `LlmClient` seam,
  records raw request/response/usage verbatim, and extracts code mechanically — take the
  **first** ```` ```java ```` fenced block; if there is none or it is never closed, the file
  `EXTRACTION-FAILED.txt` is written with the raw text, that unit×model counts as
  non-compiling, and it is **not** re-prompted. The harness never edits generated code.
- **Pipeline validation before freeze (harness dry-run) — done, evidence below.** One
  hand-written smoke test per stratum shape per corpus, committed under
  `ai-testgen/harness-validation/`, clearly excluded from every result, run through the
  full pipeline on 2026-07-31:

  | Corpus | compile | tests | JaCoCo XML | PIT |
  |---|---|---|---|---|
  | A (legacy, `--release 8` on JDK 26) | green | 8/8 green | written | 326 mutations, 10 killed, XML+HTML written |
  | B (modern, `--release 25`) | green | 7/7 green | written | 328 mutations, 10 killed, XML+HTML written |

  The low kill rate is expected and correct: these are trivial smoke tests, and their only
  job is to prove that every measurement step actually produces a number. Reproduce with
  `./mvnw -Pvalidation -f ai-testgen/testbed/<corpus>/pom.xml test org.pitest:pitest-maven:mutationCoverage`.

  **Self-test of the measurement step**, committed under `runs/pipeline-selftest/` (synthetic
  inputs, **not** an experiment run, excluded from every result): one hand-written class that
  compiles with one passing and one deliberately failing test, and one file that is deliberately
  not valid Java. Result: the broken unit is recorded as `COMPILE_FAILED` with zeros across the
  board, the other as 2 tests / 1 failure, coverage counted on the class under test only
  (6 lines covered, 29 missed) and 12 mutations / 1 killed. Two defects in the measurement
  script were found and fixed by exactly this self-test before the freeze: stale `target/`
  content made a compile-failed unit inherit the previous unit's coverage and mutation score,
  and PIT invoked as a separate Maven call saw no compiled code at all. A measurement
  pipeline that has never been shown a failing input has not been validated.

## 6. Procedure (per unit × model, fixed order S1→S4, M1 before M2, corpus A before B)

1. Render prompt, commit instance.
2. Call model once; record raw request/response, token usage, serving provider, latency,
   date, and the SHA-256 of this protocol.
3. Mechanical code extraction (rule above).
4. **Phase A — as generated:** place the file in the testbed → `mvn test-compile` → run
   tests → JaCoCo → PIT. Record everything, including total failure. Non-compiling output
   is measured as 0 across coverage/mutation — never excluded, never touched.
5. **Phase B — repair:** time-boxed **30 min cap per unit×model**, live log
   (`fix-log.csv`: timestamp, category, description). Categories (fixed):
   `IMPORT/SYNTAX` · `MOCKING-SETUP` · `WRONG-EXPECTATION` (test asserts behaviour the
   code does not have → align test to actual behaviour) · `BUG-FOUND` (test is right, code
   is wrong → align test to actual behaviour + tag, production code stays untouched) ·
   `STRUCTURAL` (test approach unusable, e.g. boots a context or hits a real DB) ·
   `ABANDONED` (cap reached; state recorded as-is).
6. Re-measure Phase A metrics on the repaired suite.
7. Commit all artifacts: `as-generated/` (broken files included), `repaired/`, JaCoCo XML,
   PIT XML+HTML, fix log, run metadata.

**Who performs Phase B — disclosed, because it changes what the number means.** The repair
is done by the **Claude Code agent that executes this repository's sessions, under the
owner's supervision** — the same execution model as every other session here (README "How
this was built", worklog header). It is *not* a human developer with a stopwatch. Therefore:

- the metric is **repair effort in agent wall-clock minutes under supervision**, capped at
  30 per cell, and is labelled that way everywhere it appears;
- it must **not** be read as a person-minutes estimate for a human team, and the report
  says so at the point of the number, not in a footnote;
- the repairing agent works only with compiler/test feedback and the fixed categories above;
  it never calls the generating model, and no repair is fed back into generation;
- every `as-generated/` artifact stays in the repo, so a third party can re-run Phase B with
  a human and compare.

This is a real limitation, not a formality — see T8.

## 7. Metrics — exact definitions

| Metric | Definition | Measured |
|---|---|---|
| Compile rate | compiling generated test classes ÷ generated test classes (extraction failures count as non-compiling) | per model, per stratum, per corpus |
| Pass rate | passing test methods ÷ test methods in compiling classes, run against unmodified code under test | Phase A and B |
| Coverage | JaCoCo line % and branch % **on the target class only** | Phase A (compiling subset) and B |
| Mutation score | PIT killed mutants ÷ all mutants generated on the target class (DEFAULTS mutators, `skipFailingTests=true` → the score of the green subset, §5); surviving mutants of S1 additionally analysed qualitatively | Phase A and B |
| Repair effort | **agent wall-clock minutes under supervision** (see §6), cap 30 per cell, category breakdown from the fix log | Phase B |
| Cost | tokens in/out from OpenRouter usage × pinned price table; EUR at the run-day ECB reference rate, rate and its publication date recorded per call | per call, aggregated |

Aggregation: per stratum, per model, per corpus, overall. Denominators always include
failures. With N = 6 and k = 1, **no statistical significance is claimed anywhere** — the
report states this limitation explicitly and reports raw numbers, medians and ranges only.

## 8. Threats to validity (pre-declared)

- **T1 Contamination:** this repo is public since 2026-07; models may have seen the
  code. Repo publication date vs. model knowledge cutoffs is documented; the risk
  cannot be excluded and is stated in the report.
- **T2 Tiny N:** 6 units per corpus, 1 generation each — an illustration with honest
  measurement, not a study with statistical power.
- **T3 Provider routing:** OpenRouter may serve differing backends/quantizations;
  actual provider recorded per call. `temperature 0` reduces but does not guarantee
  determinism, which is why raw responses are recorded rather than re-derived.
- **T4 Single rater:** one actor performs all repairs and timing; cap and live logging
  mitigate, bias remains (see also T8).
- **T5 Language:** German identifiers/domain terms may affect models unevenly.
- **T6 Date-specificity:** all results are model-version- and date-bound; everything
  is pinned and dated.
- **T7 The corpora differ in more than injection style.** Corpus B is not "corpus A with
  constructor injection". Stage 4 also parameterized every SQL statement (SD-1, ADR-0004),
  stage 5 absorbed the JSP admin page so S3 compares a JSP+gson `@Controller` (A) with a
  JSON `@RestController` (B), and stage 5 reformatted the code (google-java-format). An
  RQ5 delta is therefore a **migration effect**, not an injection effect, and the report
  must not claim otherwise. S3 numbers carry the additional caveat that the two classes are
  not shape-equivalent.
- **T8 The repairer is an AI agent of the same family as M1.** Phase B is executed by
  Claude (§6), while arm M1 is `anthropic/claude-sonnet-5`. A same-family repairer may
  understand M1's output faster than M2's, which would flatter M1 exactly in the repair-cost
  metric. Nothing in this design removes that risk; it is bounded by the 30-minute cap and
  the fixed categories, made auditable by the committed `as-generated/` artifacts and fix
  logs, and stated next to every repair-effort number in the report.

## 9. Freeze checklist (all ticked 2026-07-31, with evidence)

- [x] Strata and the six-class list confirmed — §2, encoded in `Catalog.java`, guarded by `CatalogTest`
- [x] M2 choice confirmed (`qwen/qwen3-coder-next`); both OpenRouter IDs + prices re-read from the live model list on freeze day — §3
- [x] Mockito / AssertJ / JaCoCo / PIT / JUnit versions verified live on Maven Central and pinned — §5
- [x] Harness dry-run green: smoke tests flow through compile → run → JaCoCo → PIT on **both** corpora — §5 table
- [x] Corpus B (modern comparison) decided: **IN** — §4, with the reasons recorded
- [x] `git diff stage-0-legacy..HEAD -- legacy/src` re-checked: empty
- [x] Commit + tag `ai-testgen-protocol-v1` → **FROZEN**

## Amendments

*(empty — permitted only after freeze, dated, only for steps not yet executed)*
