# PROTOCOL.md — AI-assisted test generation, pre-registered

**Status: DRAFT v0.1 — NOT FROZEN. No generation, no API call, no harness run happens
before the owner freezes this protocol (commit + tag `ai-testgen-protocol-v1`).**

Drafted 2026-07-30. Freeze = owner review → edits → commit + tag. After the freeze,
changes are allowed only as dated entries in the *Amendments* appendix, only for steps
that have not yet run, never retroactively. `REPORT.md` must cite the git hash of the
frozen protocol it was executed under.

---

## 1. Research questions

- **RQ1** — What fraction of LLM-generated unit tests for genuinely legacy code
  (field injection, God class, JdbcTemplate + inline SQL, no seams) compiles and
  passes without any human help?
- **RQ2** — What is their real quality — JaCoCo line/branch coverage and **PIT
  mutation score** — as generated vs. after time-boxed human repair?
- **RQ3** — What does it cost: API tokens/EUR and human-fix minutes, and in which
  categories does the human effort concentrate?
- **RQ4** — How does a frontier commercial model compare to an open-weight model
  under identical prompts and procedure?

Results are published in `REPORT.md` (German summary + English detail). Failed
generations stay in the repo. Nothing is curated afterwards.

## 2. Code under test

`legacy/` at the protocol-freeze commit (legacy is frozen by repo rule since
`stage-0-legacy`; the freeze checklist re-verifies `git diff stage-0-legacy..HEAD -- legacy/src`
is empty apart from documented items). Unit level only, **no Spring context, no real
database, no network** — dependencies are mocked. Production code is never modified;
if a generated test exposes a genuine bug, the test is aligned to actual behaviour and
the finding is tagged `BUG-FOUND` (characterization mindset — legacy stays legacy).

### Stratified selection (fixed at freeze)

Deviation from SPEC §5, recorded honestly: SPEC names a "mappers" stratum, but the
legacy app has **no standalone mapper classes** — all row mapping lives as anonymous
`RowMapper`s inside the God class. The strata below reflect the code as it actually is.

| Stratum | Class (FQN `at.werkstatt.crm.…`) | LOC | Why selected |
|---|---|---|---|
| S1 God service | `service.WerkstattService` | 613 | The headline target: 60 members, JdbcTemplate, inline RowMappers, status-flow validation, VAT math, report aggregation — "untestable" legacy in one class |
| S2 REST controller | `controller.KundenController` | 78 | CRUD + search path (the former B4 injection-shaped query) |
| S2 REST controller | `controller.AuftragController` | 80 | Status lifecycle endpoint (business-rule rejection paths) |
| S2 REST controller | `controller.RechnungController` | 56 | Invoice creation + duplicate rejection |
| S3 Mixed tech | `controller.AdminController` | 58 | JSP + gson servlet-era shape — the awkward case |
| S4 Negative control | `model.Rechnung` | 113 | Pure data holder. Expectation: trivially high coverage, low mutation value — detects assert-getter test generation and keeps the metrics honest |

Excluded (reason logged, no other selection happens): `FahrzeugController`,
`BerichtController` (shape-redundant with S2 picks), remaining models (redundant with
S4), `WerkstattCrmApplication` (bootstrap). **N = 6 units.**

## 3. Arms (models)

Both arms get identical prompts, procedure, and evaluation. One generation call per
unit per model (k = 1), `temperature 0`, `top_p 1`; one retry only on transport error
(HTTP/timeout), never on unsatisfying content.

| Arm | OpenRouter ID | Price (in/out per M tokens) | Verified live |
|---|---|---|---|
| M1 commercial frontier | `anthropic/claude-sonnet-5` | $2.00 / $10.00 | 2026-07-30 |
| M2 open-weight (proposal) | `qwen/qwen3-coder-next` | $0.12 / $0.80 | 2026-07-30 |

M2 alternates if the owner prefers at freeze: `deepseek/deepseek-v4-pro`,
`z-ai/glm-5.1`, `openai/gpt-oss-120b` (all verified live 2026-07-30). IDs and prices
are **re-verified live on freeze day** and pinned in the amendment-free table above.
OpenRouter may route one model ID to different backend providers (quantization may
differ) — the actual serving provider from each response's metadata is recorded per
call and listed in the report (threat to validity T3).

Budget cap: hard abort at **€20** API spend total (expected spend is far below: ~6
units × 2 models × ≈(12k in + 8k out) tokens ≈ €1.50, dominated by M1).

## 4. Prompt template (v1, part of the pre-registration)

One prompt per unit, built mechanically by the harness — no manual per-class editing.
Every rendered prompt instance is committed under `runs/<date>/<model>/<class>/prompt.md`.

**System prompt (identical for both arms):**

```text
You are generating JUnit 5 unit tests for a legacy Java 8 Spring Boot 1.5 codebase.
Constraints:
- JUnit 5.14.x, Mockito (with mockito-junit-jupiter), AssertJ. No other libraries.
- Unit tests only: no Spring context, no real database, no network, no file I/O.
- Mock all dependencies (fields are @Autowired field-injected — handle this without
  modifying the class under test).
- Output exactly one complete, compilable test class in a single ```java code block,
  package <TEST_PACKAGE>, class name <CLASS_UNDER_TEST>GeneratedTest.
- Aim for behavioural coverage: happy paths, edge cases, error/rejection paths.
- Do not invent methods that do not exist in the provided source.
```

**User prompt (per unit):**

```text
Class under test (full source):
<SOURCE>

Direct dependency types visible to the class (signatures only):
<DEPENDENCY_SIGNATURES>

Database schema excerpt referenced by the SQL in this class (DDL, if any):
<DDL_EXCERPT>

Write the test class now.
```

Template placeholders are filled by the harness: `<SOURCE>` verbatim,
`<DEPENDENCY_SIGNATURES>` = public signatures of project-internal types the class
references (models etc.), `<DDL_EXCERPT>` = relevant `CREATE TABLE` statements from
`legacy/db/init` for classes containing SQL, empty otherwise. Template changes after
freeze = amendment (allowed only before any generation ran).

Explicitly **out of scope** (future work, not this experiment): multi-turn/agentic
self-repair with compiler feedback, prompt-optimization loops, test generation for
`modern/`. — *Optional Arm B (owner decision at freeze): repeat the identical
procedure against the constructor-injected `modern/` counterparts to measure how
migration state changes AI test-gen effectiveness (this is what the stage-4
constructor-injection sweep was a precondition for). Default: OUT for G6 scope.*

## 5. Harness and measurement environment

- `ai-testgen/testbed/`: a dedicated Maven module that compiles `legacy/src/main/java`
  as an additional source root at `--release 8` and hosts generated tests under
  `src/test/java`. `legacy/pom.xml` is never touched; no tests land in `legacy/`.
  Unit tests run without Spring context, so Boot 1.5 classes on a modern JVM are safe.
- Pinned tooling (re-verified live on freeze day): JUnit 5.14.4 · Mockito + AssertJ
  (exact versions pinned at freeze) · JaCoCo (exact version pinned at freeze) ·
  pitest-maven 1.25.8 + pitest-junit5-plugin 1.2.3 (verified 2026-07-30) with the
  **DEFAULTS** mutator set, `targetClasses` scoped to the class under test.
- `harness/`: scripts that render prompts, call OpenRouter (`LlmClient` abstraction),
  record raw request/response/usage JSON verbatim, and extract code mechanically:
  take the **first** ```java fenced block; if none or unparseable, record
  `EXTRACTION-FAILED` — that unit×model counts as non-compiling, it is not re-prompted.
- **Pipeline validation before freeze (harness dry-run):** one trivial human-written
  smoke test per stratum shape must flow through compile → run → JaCoCo → PIT on the
  testbed. Committed under `harness-validation/`, clearly excluded from results. The
  protocol is only freezable once this pipeline demonstrably works — otherwise metric
  definitions might be bent to tooling limits mid-experiment.

## 6. Procedure (per unit × model, in fixed order S1→S4, M1 before M2)

1. Render prompt, commit instance.
2. Call model once; record raw response, token usage, serving provider, latency, date.
3. Mechanical code extraction (rule above).
4. **Phase A — as generated:** place file in testbed → `mvn test-compile` → run tests
   → JaCoCo → PIT. Record everything, including total failure. Non-compiling output
   is measured as 0 across coverage/mutation — never excluded, never touched.
5. **Phase B — human repair:** time-boxed **30 min cap per unit×model**, stopwatch,
   live log (`fix-log.csv`: timestamp, category, description). Categories (fixed):
   `IMPORT/SYNTAX` · `MOCKING-SETUP` · `WRONG-EXPECTATION` (test asserts behaviour the
   legacy code does not have → align test to actual behaviour) · `BUG-FOUND` (test is
   right, code is wrong → align test to actual behaviour + tag, production code stays
   untouched) · `STRUCTURAL` (test approach unusable, e.g. tries to hit a real DB) ·
   `ABANDONED` (cap reached; state recorded as-is).
6. Re-measure Phase A metrics on the repaired suite.
7. Commit all artifacts: `as-generated/` (broken files included), `repaired/`,
   JaCoCo XML, PIT XML+HTML, fix log, run metadata.

## 7. Metrics — exact definitions

| Metric | Definition | Measured |
|---|---|---|
| Compile rate | compiling generated test classes ÷ generated test classes (extraction failures count as non-compiling) | per model, per stratum |
| Pass rate | passing test methods ÷ test methods in compiling classes, run against unmodified legacy code | Phase A and B |
| Coverage | JaCoCo line % and branch % **on the target class only** | Phase A (compiling subset) and B |
| Mutation score | PIT killed mutants ÷ all mutants generated on the target class (DEFAULTS mutators); surviving mutants of S1 additionally analysed qualitatively | Phase A and B |
| Human-fix minutes | wall-clock per unit×model, cap 30, category breakdown from fix-log | Phase B |
| Cost | tokens in/out from OpenRouter usage × pinned price table; EUR conversion at run-day ECB rate, recorded | per call, aggregated |

Aggregation: per stratum, per model, overall. Denominators always include failures.
With N = 6 and k = 1, **no statistical significance is claimed anywhere** — the report
states this limitation explicitly and reports raw numbers, medians and ranges only.

## 8. Threats to validity (pre-declared)

- **T1 Contamination:** this repo is public since 2026-07; models may have seen the
  code. Repo publication date vs. model knowledge cutoffs is documented; the risk
  cannot be excluded and is stated in the report.
- **T2 Tiny N:** 6 units, 1 generation each — an illustration with honest measurement,
  not a study with statistical power.
- **T3 Provider routing:** OpenRouter may serve differing backends/quantizations;
  actual provider recorded per call.
- **T4 Single rater:** one human (the owner) does all repairs and timing; cap and live
  logging mitigate, bias remains.
- **T5 Language:** German identifiers/domain terms may affect models unevenly.
- **T6 Date-specificity:** all results are model-version- and date-bound; everything
  is pinned and dated.

## 9. Freeze checklist (owner — nothing runs before every box is ticked)

- [ ] Strata and the 6-class list confirmed (or edited — edits before freeze are free)
- [ ] M2 choice confirmed; both OpenRouter IDs + prices re-verified live on freeze day
- [ ] Mockito / AssertJ / JaCoCo exact versions verified live and pinned in §5
- [ ] Harness dry-run green: smoke tests flow through compile → JaCoCo → PIT (§5)
- [ ] Optional Arm B (modern/ comparison) decided IN or OUT
- [ ] `git diff stage-0-legacy..HEAD -- legacy/src` re-checked empty/documented
- [ ] Commit + tag `ai-testgen-protocol-v1` → **FROZEN**

## Amendments

*(empty — permitted only after freeze, dated, only for steps not yet executed)*
