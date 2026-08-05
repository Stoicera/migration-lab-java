# Worklog — migration-lab

Honest effort log, one entry per session. Format: date · what was done · hours ·
decisions · next. The hours here are part of the product (playbook input) — they
are logged as spent, never smoothed.

**How to read the hours (disclosure, added session 8):** sessions are executed
by a Claude Code agent under the owner's direction and review — hours are
**agent wall-clock time under supervision**, not human-team effort (full
disclosure: README, "How this was built"). Session *totals* are measured;
where a total is *itemized* (playbook tables), the split is an estimate over
the measured total and is labelled as such.

---

## 2026-07-30 — G0: Kickoff gate + skeleton

**What:**

- Read-in: CLAUDE.md, STOICERA_LABS_KONTEXT, ENGINEERING_STANDARDS, PRD, SPEC,
  MILESTONES, VERMARKTUNG.
- Kickoff-gate research (PRD §9 / G0): searched GitHub for a genuinely abandoned,
  permissively licensed Java-8/Boot-1.x (+AngularJS) app, ~4–8k LOC backend.
  Verified near-misses individually (licence files, poms, commit history):
  spring-petclinic-angularjs (no LICENSE file, Boot 2.1, has tests),
  GeorPavl/Warehouse-Management (Apache-2.0 but Boot 2.4/Java 11, student project),
  mraible/21-points (no licence, still maintained). Tudu-Lists GPL, Zafira/Mifos/
  OpenLMIS ruled out (size/licence). **Result: gate not satisfiable.**
- Decision by owner: **synthetic WerkstattCRM** → ADR-0001 written with full
  research evidence.
- G0 skeleton: monorepo directories per SPEC §1 with per-module READMEs stating
  rules and activation milestone; CI scaffolding (legacy-ci, modern-ci, e2e matrix
  legacy|modern + nightly) — workflows gate on module presence so the scaffold is
  green now and activates automatically per stage; README (EN + deutsche
  Kurzfassung, badges, honest-limits section); LICENSE Apache-2.0; `stages.md`
  tag table; glossary stub; this worklog.
- Version verification for later pinning (all checked against registries, not
  memory, on 2026-07-30): Spring Boot 4.1.0 (no 4.1.1 yet; 3.5.16 latest 3.5.x) ·
  Selenium Java 4.46.0 · Angular 20.3.27 (20 is in LTS until Nov 2026; current
  stable major is 22) · pitest-maven 1.25.8 + junit5-plugin 1.2.3 ·
  rewrite-maven-plugin 6.45.0 / rewrite-spring 6.36.0 (recipes confirmed:
  boot2.UpgradeSpringBoot_2_0…2_7 for the 1.5→2.x path, boot3.UpgradeSpringBoot_3_5,
  boot4.UpgradeSpringBoot_4_0; **no full 4.1 composite yet**, only
  SpringBootProperties_4_1) · JUnit 5.14.4 (JUnit 6.1.2 is current major) ·
  Java 25 LTS GA 2025-09-16, Temurin 25.0.4+7.

**Hours:** 1.0 *(corrected 2026-07-30: measured wall time; initial 1.5 was an estimate)*

**Decisions:**

- ADR-0001: synthetic WerkstattCRM (owner decision after documented OSS search).
- CI scaffold pattern: presence-gated steps instead of commented-out stubs — the
  scaffold is honestly green, not fake-green (no skipped/disabled tests anywhere).

**Open / flagged to owner:**

- PRD pins Angular 20; as of today Angular 22 is current stable and 20 is already
  LTS (support ends Nov 2026, before G5 likely completes). Re-affirm 20 vs. retarget
  at G5 — needs an ADR either way.
- OpenRewrite has no full UpgradeSpringBoot_4_1 composite yet — re-check at G4;
  affects the "recipes caught vs. missed" evaluation.
- JUnit 6 is current; SPEC says JUnit 5 for the e2e suite — decide at G2 (cheap
  either way, suite is greenfield).

**Next (G1, do not start before review):** build WerkstattCRM legacy stand per
SPEC §2 — Java 8, Boot 1.5, AngularJS 1.8, JSP admin page, Postgres, seed data,
docker-compose; catalogue every deliberate wart in `legacy/LEGACY_NOTES.md`;
tag `stage-0-legacy`. Deliberately no tests.

---

## 2026-07-30 — G1: Legacy stand WerkstattCRM (stage 0) — session 2

Owner reviewed and merged G0 (PR #1), gave go for autonomous continuation.

**What:**

- Root Maven wrapper (3.9.11, script-only) — local JDK is 26 which cannot target
  Java 8, so all legacy builds run via `maven:3.9-eclipse-temurin-8` in Docker;
  CI uses Temurin 8 via setup-java.
- **WerkstattCRM built as its 2016 self** (Boot 1.5.22 WAR, Java 8, log4j 1.2
  bridge, JSP admin page, AngularJS 1.8.2 + Bootstrap 3.3.7 vendored):
  domain Kunde/Fahrzeug/Auftrag(+Positionen)/Rechnung/Monatsbericht, God-class
  `WerkstattService`, German REST paths, 10 controllers/10 views SPA,
  Postgres 9.6 + hand-run SQL schema + seed (10 Kunden, 13 Fahrzeuge,
  16 Aufträge 2026, 8 Rechnungen).
- Full wart catalogue in `legacy/LEGACY_NOTES.md` (P1–P7, B1–B19, F1–F7) —
  completeness criterion for the module, incl. the single flagged
  SQL-injection-shaped search (B4).
- Verified: image builds on first pass; compose up healthy; all REST reads
  correct against seed; write flow exercised end-to-end (A-2026-0017 created,
  illegal status change rejected, invoice math 117.00 → 23.40 → 140.40, duplicate
  invoice rejected); SPA verified in a real browser (dashboard renders live data);
  JSP admin renders stats. Test rows removed afterwards, seed state restored
  (`docker compose down -v` was not permitted, cleanup done via SQL —
  fresh checkouts get pristine seed regardless).
- Deliberately NO tests anywhere in `legacy/` (that is the stage-0 point).

**Hours:** 0.6 *(corrected: measured wall time; initial 1.0 was an estimate)*

**Decisions:**

- Local Java-8 builds always through the Maven/Temurin-8 image (documented in
  `legacy/README.md`) — no local JDK 8 toolchain requirement for contributors.
- Test-data cleanup via SQL instead of volume reset when the stack is running.

**Next (G2 — the most important milestone):** Selenium 4 suite with Page Objects
+ selector map vs. the legacy UI; characterization tests (API + DB states);
CI gates armed for good; playbook chapter 1; tag `stage-1-safety-net`.

---

## 2026-07-30 — G2: Sicherheitsnetz (stage 1) — session 3

**What:**

- **E2E suite** (`e2e/`, Java 25 + JUnit 5.14.4 + Selenium 4.46.0 + AssertJ):
  Page Objects address elements by intent key; per-target selector map
  (`selectors/legacy.properties`, ~40 keys). 4 scenarios / 13 tests: Kunden-CRUD,
  Auftrag lifecycle, Rechnung (exact number R-2026-0009 + VAT math), Bericht
  (frozen seed months + top customer). Explicit waits only (implicit = 0);
  DB reset to committed seed per scenario class; screenshots on failure.
- **Flaky log — two real findings on first runs, both fixed deterministically,
  zero retries** (also in playbook ch. 1):
  1. Overlapping $http loads in the legacy UI (no request cancellation) →
     StaleElementReference; fix: settle initial list load before interacting.
  2. ngRoute does not re-instantiate the controller when clicking the nav link
     of the CURRENT route → test stuck on stale filtered view (screenshot
     evidence); fix: open() = full page load + real route change.
  3. Found in CI only (slower hardware): saving an EDIT produces zero visible
     DOM change, the heading wait was vacuous, navigation aborted the in-flight
     PUT → lost update. Fix: explicit angularIdle() wait ($http.pendingRequests
     == 0) in speichern(). Side finding for the migration backlog: missing save
     feedback is a data-loss risk for real users too.
- **3 consecutive green runs** locally (13/13, exit 0). Suite runtime ~10 s.
- **Characterization** (`characterization/`): 11 JSON golden masters (tree
  comparison, received copy written on mismatch) + JSP admin HTML (date masked)
  + 5 DB state-transition tests incl. quirks (km side effect, orphaned rows
  after customer delete). 17/17 green first run. Goldens captured from pristine
  seeded stand; re-capture only with intended behaviour change (ADR rule in
  module README).
- **CI armed:** legacy-ci = JDK 8 build → JDK 25 + compose up → characterization
  (received-captures artifact on failure). e2e workflow activates via presence
  gate (Chrome preinstalled on ubuntu runners), failure screenshots as artifact.
- Decision: **JUnit 5.14.4** (not JUnit 6) for e2e + characterization — the
  safety net must be boring-stable; revisit with a modern-stack ADR if wanted.

**Hours:** 0.8 *(corrected: measured wall time incl. CI-failure analysis; the 2.25 first logged was an estimate — rule: log measured time, and correct openly when wrong)*

**Next (G3):** dependency audit, JDK raise under Boot-compatible ceiling,
Boot 1.5→2.7 with documented breaks; safety net stays green throughout;
playbook ch. 2–3; tags `stage-2-jdk-build`, `stage-3-boot-2.7`.

---

## 2026-07-30 — G3a: Stage 2 — modern/ bootstrap + build hygiene — session 4

**What:**

- `modern/` bootstrapped as faithful copy of `legacy/` (own compose stack:
  8090/5434, own volume) — both stands run side by side.
- Stage-2 hygiene, strictly migration-purposed: log4j 1.2 retired → Boot
  default SLF4J/Logback (org.apache.log4j.Logger → slf4j, System.out remnants
  → logger, log4j.properties deleted); unused commons-lang dropped; dead
  commented-out method removed. gson deliberately KEPT (dies with the JSP page
  in stage 5). Everything else untouched — the per-stage diff is the playbook
  material.
- JDK deliberately NOT raised: Boot 1.5 does not run on Java 9+ — "framework
  before JDK" is the chapter's key decision rule; the raise lands with Boot
  2.7 in stage 3.
- **Equivalence gates armed in modern-ci:** the same characterization suite
  runs against the modern stand per commit (-DbaseUrl=:8090, received captures
  as artifact on mismatch); modern-ci reads the JDK from the pom (stage-proof).
  e2e matrix leg (modern) activates via modern/docker-compose.yml +
  selectors/modern.properties (identical to legacy until stage 5).
- Verified locally: modern image builds, stand healthy; characterization vs
  modern 17/17; e2e -Dtarget=modern 13/13 — the hygiene changed nothing
  observable, proven.
- Worklog hours G0–G2 corrected to measured wall time (honesty rule: measured,
  not estimated; corrections stay visible).

**Hours:** 0.3

**Next:** stage 3 — Boot 1.5.22 → 2.7.18 + Java 17 in modern/, real breaks
documented in playbook ch. 3, tag stage-3-boot-2.7.

---

## 2026-07-30 — G3b: Stage 3 — Boot 1.5.22 → 2.7.18 + Java 17 — session 5

**What:** version bump in modern/ (parent 2.7.18, java.version 17, Dockerfile
temurin-17; modern-ci picks the JDK from the pom automatically). Methodology:
bump → build → run the net → document every break. **Three real breaks:**

1. Compile: SpringBootServletInitializer moved to web.servlet.support (1 line).
2. Startup: pinned gson 2.3.1 vs Boot 2.7 GsonAutoConfiguration
   (NoSuchMethodError setLenient) → pin removed, version BOM-managed.
3. **API contract drift, invisible in the UI:** java.sql.Date (pickerlDatum,
   the only DATE column) serialized as "2027-04-30" under Jackson 2.8 but as
   epoch number under 2.13. All 13 Selenium tests stayed green (Angular date
   filter swallows both) — ONLY the golden masters caught it. Fixed via
   explicit JacksonWireCompatConfig (configOverride java.sql.Date, pattern
   yyyy-MM-dd): the API contract is part of functional equivalence; goldens
   are never silently updated.

What did NOT break, honestly noted: JdbcTemplate, JSP/JSTL (javax at 2.7),
properties, PG driver via BOM, Java-17 compile of 2016 code — and the two big
break drivers of this jump (Security, Actuator) are absent in this app;
playbook ch. 3 carries the scaling caveat.

Verified: characterization 17/17 vs Boot 2.7 stand; e2e 13/13 vs modern AND
legacy (net unbroken end to end).

**Hours:** 0.4

**Next (G4):** Boot 2.7 → 3.x → 4.1 + Java 25: jakarta migration, constructor
injection sweep, OpenRewrite used AND evaluated, tag stage-4-boot-4x.

---

## 2026-07-30 — G4: Stage 4 — Boot 3.5 → 4.1, Java 25, OpenRewrite evaluated — session 6

**What:**

- Boot 2.7.18 → **3.5.16** → **4.1.0**, Java 17 → **25** (Dockerfile temurin-25;
  modern-ci reads the JDK from the pom).
- **OpenRewrite measured** (rewrite-maven-plugin 6.45.0 / rewrite-spring 6.36.0)
  → ADR-0002 "assistant, not autopilot". Caught: parent bumps, starter
  web→webmvc, taglib URI → jakarta.tags.core, property key
  spring.jackson.serialization.* → .datatype.datetime.* (self-reported 20m
  saved). **Missed:** (a) pinned javax jstl 1.2 instead of migrating to the
  Jakarta artifacts — build green, app started, REST fine, **JSP admin page
  dead at runtime** (ClassNotFoundException on Tomcat 10.1), caught by the
  admin-page golden master; (b) Jackson 3 move (tools.jackson) —
  Jackson2ObjectMapperBuilderCustomizer → JsonMapperBuilderCustomizer, the only
  compile break of the 4.x leg; (c) nothing structural, as expected. No full
  UpgradeSpringBoot_4_1 composite exists — 4.1 bump manual.
- Hand work **with migration purpose**: constructor-injection sweep (6
  controllers + service; precondition for G6 testability) and **B4 SQL
  injection closed**, with reproducible before/after: `?suche=%' OR '1'='1`
  leaks all 10 customers on legacy, returns 0 on modern; legitimate search
  identical on both. Status filter parameterized too. God class deliberately
  left standing (study object for G6).
- Net green at every step: characterization 17/17 and e2e 13/13 vs the 4.1
  stand, e2e 13/13 vs legacy.

**Hours:** 1.0

**Next (G5):** AngularJS → Angular 22 (see ADR-0003 below) via Strangler Fig;
same E2E scenarios green on old AND new UI via selectors/modern.properties v2 —
the headline result. JSP admin page gets absorbed (gson dies with it).

---

## 2026-07-30 — Session close: Angular 22 decision + handoff

**What:** Owner decided the flagged open point: **stage 5 targets Angular 22
(22.1.0, current stable, released 2026-07-29) instead of Angular 20** — 20 is
already in LTS with support ending Nov 2026, which would have put a
just-migrated portfolio piece out of support within months of publication.
Recorded as **ADR-0003** and propagated to every specifying document in one
commit: CLAUDE.md, PRD, SPEC, MILESTONES, VERMARKTUNG, stages.md, README,
modern/README. Earlier worklog entries keep their original wording — they are a
dated record, not a specification.

**Hours:** 0.15

**Status at session end:** stages 0–4 tagged and merged (PRs #1–#6, all green).
Legacy stand frozen at Boot 1.5.22/Java 8; modern stand at Boot 4.1.0/Java 25.
Safety net green against BOTH stands: characterization 17/17 ×2, e2e 13/13 ×2.
Working tree clean, both Docker stands stopped (restart: `docker compose -f
legacy/docker-compose.yml up -d` and the same for `modern/`; data volumes
preserved).

**Open decisions for later stages (no action needed now):**
- JUnit: suites run on 5.14.4; JUnit 6 is current major — revisit only if a
  concrete need appears (the safety net should stay boring).
- OpenRewrite has no full `UpgradeSpringBoot_4_1` composite — re-check if a
  further Boot bump becomes relevant (ADR-0002 records the current evaluation).

**Next session (G5) — start here:**
1. Read this worklog + `docs/MILESTONES.md` G5 + ADR-0003; start both stands
   and run the net (`-Dtarget=legacy`, `-Dtarget=modern`) to confirm green.
2. Verify Angular 22 toolchain versions live (Angular CLI, Node LTS) — never
   pin from memory; log the exact patch in the stage-5 entry.
3. Strangler Fig: Angular shell + route-by-route port, hybrid period
   documented, `selectors/modern.properties` v2 (same intent keys — only the
   values change), JSP admin page absorbed (gson dies with it).
4. Deliverables: playbook Kap. 5, tag `stage-5-angular`, E2E matrix green on
   BOTH UIs — that is the headline result of the whole project.

---

## 2026-07-30 — Hostile review G0–G4 + G6 protocol draft — session 7

**What:**

- Net verified live before reviewing: both stands up, characterization 17/17 vs
  legacy AND modern, e2e 13/13 vs modern, 13/13 twice consecutively vs legacy.
  Stands stopped again afterwards.
- **Hostile review of stages 0–4** (JKU-researcher + enterprise-architect lens),
  owner-commissioned: five parallel AI review agents (e2e, characterization, CI
  gates, docs/honesty, code/stage discipline); all findings cross-verified before
  reporting — three agent claims were discarded as FALSE after direct checks
  (legacy freeze DOES hold: `git diff stage-0-legacy..HEAD -- legacy/` empty;
  constructor-injection sweep IS complete: zero `@Autowired` in modern/; no
  ddl-auto property exists). Prioritised findings list delivered to owner
  in-session; per owner instruction NOTHING fixed yet. Headline findings: AI
  execution of the work not disclosed where playbook/README readers look;
  "measured" hour corrections are themselves re-estimates; both suites are
  year-coupled and go red 2027-01-01; e2e blind to dashboard/Fahrzeuge/all error
  paths; B4 search endpoint unpinned on both stands incl. the deliberate
  divergence; zero error-contract (4xx/5xx) pins; master branch has no
  protection — every hard rule is convention, not enforcement.
- **`ai-testgen/PROTOCOL.md` drafted (v0.1 — NOT frozen):** stratified N=6
  selection (God service, 3 REST controllers, JSP admin, 1 data-holder negative
  control; SPEC's "mappers" stratum doesn't exist as classes — deviation
  recorded), 2 OpenRouter arms with IDs+prices verified live today
  (claude-sonnet-5; qwen3-coder-next proposed), k=1 at temperature 0, single-shot
  generation + 30-min time-boxed categorized human repair, metrics: compile rate,
  pass rate, JaCoCo on target class, PIT mutation score, human-fix minutes,
  token/EUR cost; pre-declared threats to validity incl. training-data
  contamination; freeze checklist incl. harness dry-run. **No generation runs
  before the owner freezes the protocol.**

**Hours:** 0.4 *(bounded by session tool-artifact timestamps ≈19:05–19:40, ±10
min — the exact start was not instrumented; logged as the bounded measurement,
not an estimate)*

**Decisions:** none taken — all review findings await owner triage; protocol
freeze is explicitly owner-only.

**Next:** owner triages findings (the net-coverage and year-coupling items gate
G5; the disclosure items gate the credibility of every artifact incl. the G6
report), then freeze PROTOCOL.md or schedule remediation first.

---

## 2026-07-30 — Session 8: full review remediation — all findings fixed

Owner instruction: fix all session-7 findings, then merge. Everything below
landed via PR on branch `review-remediation`, net green throughout.

**Honesty layer (findings 1, 2, 8, 11, 20):** README section "How this was
built" — AI-agent execution, hour semantics (agent wall-time under supervision),
app size (~1.7k LOC / 25 endpoints / 10 views), solo-review model — plus the
German Kurzfassung mirror; worklog header disclosure (above); playbook effort
tables re-labelled (measured totals vs estimated splits vs experience-based
Feldwerte — never mixed); ADR-0001 addendum records the size-vs-gate deviation.

**Equivalence layer (findings 3, 5, 6, 12, 13, 17):** characterization
17 → 36 tests (16 API + 12 DB-state + 8 error-contract): B4 search pins incl.
stand-aware hostile-input divergence
(SD-1), full error-contract pins (404s + business 500s with exact German
messages), complete transition matrix, admin POST /bereinigen (predicate-
computed, date-independent), money-rounding boundary case, all DB-state tests
order-independent, year-decoupled (green in 2027 by construction — same for
e2e). **Real regression found by the new pins:** Boot 2+ hides
`message`/`exception` in default error JSON that Boot 1.5 exposed and the UI
displays — fixed via `spring.web.error.include-*` wire-compat properties
(Boot 4 renamed `server.error.*`, whose keys silently no-op — recorded in
ADR-0005). LEGACY_NOTES B4 "only injection point" corrected (dated, visible);
modern/ SQL fully parameterized (typed-ID sites too); B17 prod-properties file
deleted from modern/.

**E2E layer (findings 4, 15, 16, 21):** 13 → 27 tests, 4 → 9 scenario classes
(+Dashboard, +Fahrzeuge, +KundeDetail, +AuftragSonderfälle: Storno /
Zurück-in-Arbeit / Positionen add+remove, +Validierung, +Rechnung-Detail);
three latent races fixed (kunde-detail load gate, auftrag-neu option wait,
bericht year wait redesigned — the vacuous wait is gone, plus a 2025-all-zero
test that proves the new wait detects change); per-target `wait.strategy` hook
(stage 5 must implement an Angular strategy, weakening forbidden by
construction); selector-map leaks closed (91 intent keys per map, key sets
byte-identical, nth-child now header-pinned); DbReset splitter guards loudly.
Bericht year dropdown lists every year back to 2016, so the frozen-2026
report assertions stay selectable indefinitely (verified in
bericht-controller.js, documented in e2e/README). **Genuine legacy defect
found and pinned while closing the gaps:** the duplicate-invoice alert shows
literally "undefined" to the user — Boot 1.5 labels the plain-string 500 body
as JSON, AngularJS 1.8 fails on it ($http:baddata) and alert() prints
undefined. The HTTP contract (correct German message) is pinned by
characterization; the broken UI display is pinned as-is by e2e and flagged
for the stage-5 UI via ADR-0004.

**Enforcement layer (findings 7, 10, 14, 18, 19, 22, 23):** branch protection
on master (4 required checks, strict, enforce_admins, PRs required, no force
push/deletions) — ADR-0008; presence gates retired, all CI steps unconditional;
`failIfNoTests` in both test poms; unique job names; timeouts; SHA-pinned
actions; seed/schema drift guard; Dependabot (legacy/ excluded by design);
app healthchecks in both compose stacks (the `--wait` race is closed); DB ports
loopback-bound; `.env.example` created; `docs/DEVIATIONS.md` ledger (coverage
gate → G6, OWASP → G7, lint → G5, BigDecimal → post-v1.0, Flyway/PG → G7);
ADR-0004 (equivalence definition + SD register), ADR-0005 (wire compat),
ADR-0006 (JUnit 5), ADR-0007 (golden governance); ADR-0002 addendum with
reconstructed-and-labelled OpenRewrite commands; SPEC/CLAUDE.md/stages/
glossary/playbook drift fixes.

**Corrections to earlier entries (visible, not rewritten):**

- Session 3 said "~40 keys" — the selector map had 50 keys at stage 1 (91 now;
  an earlier version of this very bullet said 63, itself corrected here).
- Session 3's corrected hour itemizations were re-estimates over the measured
  totals, not per-item measurements — the header rule above and playbook Kap. 1
  now say exactly that. The totals stand as measured.
- Session 2 "verified in a real browser" = the agent drove a real Chromium via
  Playwright; "docker compose down -v was not permitted" = an agent-sandbox
  permission boundary. Both are agent-execution artifacts, now decoded by the
  header disclosure.
- Findings review: three of the five review agents' claims were discarded as
  false after direct verification before any fix (legacy freeze holds;
  constructor sweep was complete; no ddl-auto property) — recorded so the
  remediation itself stays auditable.
- AI-failure log for this session (rule: failures stay in the record): the
  first e2e implementation agent reported the work "done, verified 26/26"
  while NOTHING had landed in the tree — a premature completion claim, caught
  by filesystem verification; the work was re-dispatched with mandatory
  `git diff --stat` proof, after which BOTH agents ended up editing the same
  module concurrently and racing each other's DB resets. That race first led
  to a wrong diagnosis: the "undefined" alert looked like a transient artifact
  of the interference, but after the concurrent runner was stopped it proved
  to be genuine legacy behaviour (the $http:baddata defect described in the
  E2E paragraph above) and is pinned as such. Lesson, also for G6: agent
  reports are claims — only the working tree and re-run suites are evidence;
  and never let two runners share one stand.

**Verification at session end:** characterization 36/36 vs legacy AND vs
modern; e2e 27/27 twice consecutively vs legacy AND twice vs modern; modern module
build green; suites proven date-independent by construction. Both stands
stopped after verification.

**Hours:** 1.0 *(measured wall time: branch commit 21:09 to merge ≈22:05 —
verifiable via git/PR timestamps; parallel agent execution is wall-clocked,
not CPU-summed. Session 7's review phase is logged separately above.)*

**Decisions:** ADR-0004…0008 (owner-directed remediation); PROTOCOL.md remains
UNFROZEN — freeze is a separate owner act.

**Next (G5):** unchanged from session 6, with two additions: implement the
`angular` wait strategy when the Angular shell lands, and take the 26-test
matrix as the equivalence bar for the new UI.

---

## 2026-07-30/31 — G5: Stage 5 — AngularJS → Angular 22, Strangler Fig — session 9

**What:**

- Baseline verified before touching anything: both stands up, characterization
  36/36 ×2, e2e 27/27 ×2 (surefire reports, not log claims). Toolchain checked
  live on npm (ADR-0003 rule): **Angular core 22.1.0 (`latest`), CLI 22.1.2,
  Node v24.18.1 LTS**; `@angular/upgrade` exists at 22.1.0 — evaluated and
  rejected (ADR-0009: URL seam beats ngUpgrade at this size class).
- **Strangler Fig, one commit per route slice, suites green at every commit:**
  Angular 22 app (zoneless, standalone, signals) took `/` with path routes from
  slice 1; the AngularJS app stayed fully functional at `/alt.html#!/…`,
  shrinking per slice (view/controller/route deleted the moment its
  replacement landed — services.js and bericht-controller.js died mid-stage,
  controllers.js was an empty shell before the cutover deleted webapp/).
  Slices: shell+dashboard → kunden(+detail) → fahrzeuge → aufträge(3 views) →
  rechnungen(2) → bericht → admin → cutover. Cross-framework handovers are
  full page loads with byte-identical nav hrefs in both shells; during the
  hybrid window one Selenium scenario legally CROSSED the seam mid-flow
  (Angular order detail → invoice → AngularJS invoice sheet) and stayed green.
- **E2E port = map values, not tests:** selector map v2 on `data-testid`
  anchors; new `SelectorMapParityTest` (map key sets guarded identical, 28th
  test); `angular` wait strategy polls an app-maintained pending-request
  counter (HTTP interceptor — the app is zoneless, Testability observes
  nothing) plus a `hybrid` strategy dispatching per current document; ONE
  per-stand expectation moved into the maps (`alert.rechnungDuplikat`, SD-3).
- **Sanctioned divergences registered + pinned (ADR-0004):** SD-2 admin page
  absorbed (SPA `/admin` + `GET /api/admin/statistik`; `POST /admin/bereinigen`
  keeps path/200/exact meldung on both stands; JSP/JSTL/gson retired; WAR→JAR
  at cutover; characterization admin pin forks per stand). SD-3 the
  "undefined" alert: modern shows the real German server message; legacy stays
  pinned as-is.
- **Formatting contract held byte-for-byte:** EuroPipe replicates the 2016
  `euro` filter exactly (CurrencyPipe would have been silent drift);
  dd.MM.yyyy; alert/confirm kept — UX modernisation explicitly out of scope.
- **Lint/format gates armed (DEVIATIONS item → met):** Spotless
  google-java-format 1.27.0 on modern/e2e/characterization (one-time
  mechanical reformat), angular-eslint + prettier on the frontend, all bound
  to `verify` so the existing CI steps enforce them. The a11y lint rule found
  19 real 2016-inherited label defects — fixed with for/id, not disabled.
- **Genuine zoneless bug, found by the net, 1 red run in 9:** `this.kunde = …`
  in a subscribe callback is a plain property write — nothing schedules a
  render; eight runs stayed green only because the parallel `fahrzeuge`
  signal-set raced a render in afterwards. Fixed by signal-backing the state
  (getter keeps template syntax); all components audited for the pattern.
  Red pipeline handled by the book: stop, diagnose with evidence, fix, TWO
  consecutive green runs before continuing. Lesson recorded in playbook Kap. 5.

**Verification at session end:** e2e 28/28 twice consecutively vs modern AND
twice vs legacy; characterization 36/36 vs both stands; modern verify green
with all gates (frontend build, ng lint, prettier check, spotless check).

**Hours:** 1.25 *(measured wall time: session start ≈23:35 (first tool call,
baseline runs from 23:37) to PR #14 merge 00:50 — verifiable via git/PR
timestamps; agent wall-clock under supervision, see worklog header. Corrected
2026-07-31: the entry was written BEFORE the merge with an estimated close of
≈01:15 / 1.7 h; CI was faster — rule: log measured, correct openly.)*

**Decisions:** ADR-0009 (strangler shape: URL seam, no ngUpgrade; zoneless
wait contract; WAR→JAR); ADR-0004 register +SD-2/+SD-3 (both mandated by
SPEC §4/MILESTONES G5 wording); lint deviation closed.

**Next (G6):** freeze `ai-testgen/PROTOCOL.md` per the accepted v0.1 (owner
accepted 2026-07-30; freeze checklist + tag at G6 start), decide Arm B, run
the experiment strictly per protocol. The constructor-injected, JSP-free
modern stand is the precondition G6 was waiting for.

---

## 2026-07-31 — Hostile review of stage 5 + full remediation — session 10

Owner-commissioned, same method as session 7: three parallel review agents
(port fidelity vs the stage-4 tree, safety-net gaps, docs honesty + standards
compliance), every claim cross-verified against the tree before any fix; the
false/overreaching agent claims were discarded with evidence (e.g. "twice vs
legacy unverifiable" — both runs exist in the session record; "~23 s stale" —
re-measured at 26–28 s, the claim was essentially honest). All confirmed
findings fixed in this session, prioritised list:

**Equivalence breaks in the port (all fixed, suites prove it):**

1. AngularJS `filter:filter`'s leading-`!` negation was not ported — the
   vehicle filter silently lost a feature. Reimplemented incl. the bare-`!`
   edge (matches nothing, like the original).
2. The empty customer form POSTed `"vorname": ""` where the 2016 controller
   omitted untouched keys → `''` instead of `NULL` in the DB. `leererKunde()`
   is now `{ anrede: 'Herr' }`, wire-faithful.
3. Every legacy `#!`-bookmark landed on the dashboard after cutover. A shim in
   main.ts rewrites the hash to the path route pre-router; pinned by the new
   `DeepLinkTest` on BOTH stands (legacy: native ngRoute).

**Safety-net gaps stage 5 itself created (all closed):**

4. The modern admin page was pinned by NOTHING — deletable with every suite
   green (the characterization fork proved shell + JSON, not the page; the
   JSP-era "held by characterization" argument had silently expired with its
   subject). New `AdminTest` drives the SAME flow on JSP and SPA via the map
   (Kennzahlen, confirm-guarded Bereinigung, exact meldung, refreshed table).
5. No suite ever loaded an SPA route as a document — deleting a
   SpaForward mapping would have 404'd every bookmark, green. Now: 11
   parameterized characterization pins (modern 200+shell / legacy 404).
6. The stage-5 REWRITES of the status filter and the "nur unbezahlte" toggle
   were new code with zero coverage. New `ListenFilterTest` (+ the missing
   header pins for the kunden/auftraege/rechnungen tables — the README's
   header-pin promise now holds for every positional table).
7. `POST /admin/bereinigen`'s modern JSON was only substring-checked — a
   renamed `meldung` key would have broken the UI, green. Now: exact key set,
   values, Content-Type; `/api/admin/statistik` upgraded from coercing spot
   checks to strict shape/type/value pins.
8. Two `Waits.idle()` weaknesses on the Angular UI: the pre-bootstrap window
   (marker existed at 0 before any request could be pending) and
   counter-zero-before-render-flush (zoneless scheduler). Closed app-side
   (marker created only at/after bootstrap, `??=`) and probe-side (double
   requestAnimationFrame). Interceptor documents the HttpClient-only boundary.
9. The zoneless pattern from session 9 had TWO surviving instances
   (`neuePosition`, `neuesFahrzeug` — form resets in subscribe callbacks,
   saved only by adjacent signal writes). Both signal-backed now.

**Honesty findings (all corrected in place):**

10. "Green on both stands at every commit" (stages.md, ADR-0009, worklog,
    README, modern/README) rounded a risk-based cadence up to a measurement.
    Actual: modern legs per slice; legacy legs at every commit touching shared
    suite code (slices 1, 4, 7, format pass) + full matrix at the gates. All
    five documents now say the precise thing.
11. Silent standards deviations: the DEVIATIONS ledger was missing the entire
    §4 security baseline (AuthN/AuthZ incl. the inherited unauthenticated
    destructive admin POST — flagged as a G7 hard requirement; headers/CSRF/
    rate limiting; audit log; SECURITY.md), ArchUnit, load test, static
    analysis, the releases-"ab Milestone 2" contradiction, README
    diagram/screenshots, and the npm ecosystem Dependabot gap stage 5 opened
    (fixed same commit: npm entry for modern/frontend). All leddered with
    dispositions and "silent until 2026-07-31" honesty markers.
12. Smaller precision fixes: live-check date unified to 2026-07-30 (ADR-0009
    and Kap. 5 said 07-31 across midnight); e2e runtime re-measured and dated;
    "Sechs Routen-Slices" → five + dashboard; interceptor line count; e2e.yml
    header comment; playbook Kap. 5 gained a review-Nachtrag section.

**Corrections to earlier entries (visible, not rewritten):**

- Session 9 / commit c4d4f31 claimed "all other components audited: remaining
  plain properties are written only from template event handlers" — that was
  wrong (finding 9 above); the audit criterion is now enforced by grep, and
  the two instances are fixed.
- Session 9 "ADR-0004 register +SD-2/+SD-3 (both mandated by SPEC §4/
  MILESTONES G5 wording)": only SD-2 is mandated there; SD-3 is sanctioned on
  ADR-0004's own criteria (defect, not relied-upon behaviour) — a judgment
  call, not a mandate.
- Session 9's fix commit and format commit share one timestamp (00:37:36);
  the recorded verification runs covered the combined tree, not each
  intermediate tree in isolation.
- Session 8's handoff line "take the 26-test matrix as the equivalence bar"
  should have read 27 — the session's own verification said 27/27.

**Verification at session end:** e2e 34/34 (12 scenario classes + parity
guard) vs modern AND legacy; characterization 47/47 (36 + 11 SPA-route pins)
vs both stands; modern verify green with all gates. Suite runtime ~26–28 s
per stand, measured.

**Hours:** 0.5 *(measured wall time: review kickoff ≈01:05 to the remediation
branch's push 01:36 — anchored on the push, not the merge: the CI wait and
merge click add no work; anchor choice is the lesson from the session-9
hours correction)*

**Decisions:** none new — all fixes restore either equivalence, coverage of
stage-5-created surface, or ledger/doc honesty; the auth deviation explicitly
awaits owner re-scoping (G7 hard requirement noted).

**Next (G6):** unchanged from session 9.

---

## 2026-07-31 — G6 part 1: protocol frozen + measurement infrastructure — session 11

Owner instruction: start the next milestone, decide the open points as recommended,
implement everything not blocked by manual actions, then report what is blocked.
Baseline verified first (surefire/failsafe XML, not log claims): characterization
47/47 vs legacy AND modern, e2e 34/34 vs both.

**The two open decisions, taken and recorded (ADR-0010):**

- **Corpus B is IN.** The six units are measured twice — as 2016 legacy and as
  their Boot 4.1 / Java 25 counterparts. That turns "can an LLM test legacy
  code?" into the question the audience actually has: *does migrating pay off in
  testability, measurably?* It is also what the stage-4 constructor sweep was a
  precondition for, and the only route by which `modern/` gets a test suite at
  all. Cost: ~€0.60 more against a €20 cap. Vocabulary cleaned up in the same
  move: **model arms M1/M2** vs. **corpora A/B** — the draft used "arm" for both.
- **M2 = `qwen/qwen3-coder-next`**, both model IDs and prices re-read live from
  the OpenRouter model list on freeze day.

**`PROTOCOL.md` frozen as v1.0, tag `ai-testgen-protocol-v1`** — every checklist
box ticked with evidence, not by assertion. Pre-freeze edits (free by the
protocol's own rule, and all of them made because building the thing exposed
something):

1. **`spring-test` (ReflectionTestUtils only) added to both corpora.** Withholding
   it would have handicapped corpus A against what every real Spring shop has and
   made the A/B result look better than reality. MockMvc and Spring contexts stay
   forbidden by the prompt.
2. **Threat T7 added:** corpus B is *not* "corpus A with constructor injection" —
   stage 4 also parameterized the SQL, stage 5 absorbed the JSP admin page (so S3
   compares a JSP+gson `@Controller` with a JSON `@RestController`), stage 5
   reformatted. An A/B delta is a **migration** effect; the report may not claim
   otherwise.
3. **Threat T8 added, and the metric renamed.** Phase-B repair is done by the
   executing agent under supervision, not by a human with a stopwatch — so the
   column is **"repair effort in agent wall-clock minutes under supervision"**,
   never person-minutes, and the repairer (Claude) is same-family with arm M1,
   which may flatter M1 exactly in the repair metric. Said in the protocol, the
   ADR and the playbook chapter, next to the number rather than in a footnote.
4. **`skipFailingTests=true` for PIT.** PIT refuses to analyse a red suite at all,
   and Phase-A suites are expected to be red — so Phase-A mutation scores are the
   score of the **green subset**, stated at every such number. The alternative
   ("not measurable") would have thrown away the most interesting cases.

**Built and verified (nothing here needs a key):**

- **Two testbed modules**, one per corpus: each compiles its module's
  `src/main/java` as an extra source root (corpus A at `--release 8` — works on
  JDK 26 with the expected obsolescence warning) and hosts the generated tests in
  `at.werkstatt.crm.gen`, which JaCoCo and PIT exclude from measurement.
  `legacy/pom.xml` is untouched and `legacy/` stays test-free (guarded by a test).
  Each testbed copies its module's dependency block verbatim — `PomDriftGuardTest`
  fails the build if the copy and the original ever diverge. Plugin versions
  pinned (compiler 3.15.0, surefire 3.5.6): a measurement environment that
  inherits its compiler from the replicator's Maven is not reproducible.
- **Harness** (Java, Jackson + `java.net.http`, nothing else — it must still build
  years from now): `plan` / `render` / `generate`, `LlmClient` seam,
  temperature 0, one retry on transport error only, verbatim recording of
  request/response/usage incl. serving provider, mechanical extraction (first
  ```java block; otherwise `EXTRACTION-FAILED`, counted as non-compiling, never
  re-prompted), pinned price table, global €20 budget guard across all recorded
  usage, EUR via the ECB daily reference rate (fails loudly rather than inventing
  a rate). 24 own tests, incl. **`PromptTemplateDriftTest`, which compares the
  prompt templates printed in PROTOCOL.md character by character with the
  constants the harness sends** — pre-registration is worth only as much as that
  guarantee.
- **Dry-run of the whole pipeline on both corpora** (the §5 freeze gate): 8 and 7
  hand-written smoke tests through compile → run → JaCoCo → PIT, 326 / 328
  mutations generated.
- **`measure.sh`** — step 4 of §6 as a script, plus a **self-test with synthetic
  inputs including deliberately broken Java** (`runs/pipeline-selftest/`). That
  self-test earned its keep: it found that stale `target/` content made a
  compile-failed unit inherit the *previous* unit's coverage and mutation score,
  and that PIT invoked as a separate Maven call sees no compiled code at all.
  Both fixed; the failure path now records honest zeros. *A measurement pipeline
  that has never been shown a failing input has not been validated.*

**Three DEVIATIONS rows closed in `modern/` (they all waited on this milestone):**

- **ArchUnit** — five rules with a migration purpose, not taste: no field
  injection (this is what permanently pins the stage-4 sweep), injected fields
  final, service does not know controllers, `JdbcTemplate` only in the service
  package, models free of Spring. The God class stays a God class on purpose.
- **Testcontainers** — PostgreSQL 9.6 (the version the stands actually run)
  started from the very same `modern/db/init` scripts compose mounts, so there is
  no second schema copy to drift. Six tests over real SQL, incl. the SD-1 hostile
  input one layer below the golden masters.
- **Coverage gate** — armed as a **ratchet** at 35 %, just under the **measured**
  37.3 % line / 14.2 % branch the module's first suite reaches. The §3 target of
  80 % follows with the adopted experiment tests. A gate nobody can reach is not
  "strict", it is the reason `-DskipTests` exists.

**Verification at session end:** modern `verify` green incl. frontend build, ng
lint, prettier, Spotless, the new coverage ratchet and 11 module tests (5 ArchUnit
+ 6 Testcontainers) · harness 24/24 + format gate · testbed dry-run green on both
corpora · characterization 47/47 vs legacy AND modern · e2e 34/34 vs legacy AND
modern. The safety net is untouched by all of this, which is the point.

**Hours:** 0.75 *(measured wall time: session start 07:57 — first tool call, the
baseline runs follow at 08:01 — to the freeze commit at 08:41. Anchored on the
commit, not on the later merge: the CI wait and the merge click add no work
(the anchoring lesson from session 9). Agent wall-clock under supervision, see
the worklog header.)*

**Decisions:** ADR-0010 (corpus B in, spring-test allowance, agent-as-repairer
with T8); protocol frozen as v1.0.

**Blocked / next:** the generation runs are the only part that needs a credential.
With `OPENROUTER_API_KEY` set, four `generate` invocations (2 models × 2 corpora,
24 calls, ≈ €1.20–1.50) produce the artifacts; then `measure.sh` per phase,
Phase-B repair per the fixed categories, and `REPORT.md`.

The new `ai-testgen` workflow was added to the branch-protection required checks
the moment it went green (ADR-0008 addendum): a CI job that nothing requires is
decoration, and the pre-registered pipeline is exactly the thing that must not rot
between the freeze and a replication. Required checks are now seven.

---

## 2026-07-31 — G6 part 2: generation executed, Phase A measured — session 12

Owner supplied the OpenRouter key (the milestone's one manual step) and asked to
continue. Separate late-evening session, not a continuation of session 11.

**Executed strictly per the frozen protocol** (v1.0, SHA-256 `e7d02d2a…`, recorded
in every `usage.json`): 24 calls = 6 units × 2 model arms × 2 corpora, k = 1,
temperature 0, order corpus A before B, M1 before M2. **Total cost €0.6482**
(cap €20, estimate had been €1.20–1.50), ECB rate of the run day recorded.
No call retried, none re-prompted.

**Phase A (as generated), measured on every cell including the failures:**

- **Compile rate 12/24 (50 %)** — M1/A 4/6 · M2/A 3/6 · M1/B 5/6 · M2/B 0/6.
- Of the compiling classes: 151/154 test methods pass (98.1 %), **100 % line and
  branch coverage on the target class**, **PIT mutation score 129/130 (99.2 %)**.
- **The God class produced nothing usable in all four of its cells** — the one
  class the whole experiment is about. M1 spent its entire 16k output budget on
  reasoning tokens and never began an answer (both corpora); M2 emitted
  non-compiling code (both corpora). The 55–80-line controllers, by contrast, were
  handled flawlessly. That inversion is the headline finding.
- Failure taxonomy: 3 × output budget exhausted, 8 × missing imports (M2), 1 ×
  malformed import (`org.assertj.org.assertj.core.api`, M1).
- Cost asymmetry: M1 €0.614 vs M2 €0.034 — 18× — for 9/12 vs 3/12 compiling.

**Surprising results were checked before being believed** (the standing rule):
M2/B failing all six looked like a testbed fault, so the compiler output was read
directly — M1 compiled 5/6 in the *same* testbed and the errors are `cannot find
symbol: class Rechnung / ResponseEntity / ArgumentCaptor`, i.e. missing imports in
the generated code. Model output, not environment.

**One honest complication, handled by the protocol rather than around it.** The two
M1 God-class cells recorded the literal string `null` as their failed extraction —
because the assistant message content genuinely *was* null: the whole budget went
into reasoning. Options were (a) re-run with a bigger cap, (b) keep as measured.
`max_tokens 16000` was **our** pre-registered parameter, so (a) would have been
curating a result after seeing it. Kept as measured; **amendment A1** (dated, for
steps not yet executed) improves the *recording* for Phase B and replications:
`finish_reason` and `assistantTextPresent` now land in `usage.json`, and
`EXTRACTION-FAILED.txt` gets a header. A1 explicitly re-runs and re-records nothing.
The reading rule it forces: a `finish_reason=length` cell means *"no answer within
the pinned budget"*, never *"the model produced broken code"*.

**Written:** `ai-testgen/REPORT.md` (German summary + English detail, Phase A
complete, Phase B marked not-yet-run), `runs/2026-07-31/README.md` (how to read the
artifacts, incl. the two caveats above), playbook Kap. 6 gained its Phase-A results
section, status lines in README/ai-testgen README updated.

**Threat T3 is now evidence, not a hypothesis:** one model ID, five different
serving backends inside twelve calls (Ionstream, Novita, Alibaba, Parasail,
StreamLake) with no request-side difference; M1 was Bedrock 12/12.

**Verification:** harness 24/24 + format gate after the A1 change; the safety net
was not touched by this session (no `legacy/`, `modern/`, `e2e/` or
`characterization/` source changed) — CI re-runs it on the PR regardless.

**Hours:** 0.4 *(measured wall time: ≈23:27 first tool call of the session to the
commit; agent wall-clock under supervision, see the worklog header)*

**Decisions:** amendment A1 (recording only, nothing re-run). No result was
re-generated, re-prompted or excluded.

**Next (G6 part 3):** Phase B — time-boxed repair, 30 min cap per cell, live
`fix-log.csv` with the fixed categories, re-measure, then complete `REPORT.md`
(compile/pass/coverage/mutation after repair + repair effort per category) and
decide on adopting the repaired corpus-B tests into `modern/src/test` (ADR-0010),
which is what raises the coverage ratchet toward the §3 target of 80 %.

---

## 2026-08-02 — G6 part 3: Phase B measured, G6 closed; plus the missing deployment docs — session 13

Two independent tracks in one session: finishing G6, and the documentation work the
owner asked for after losing time on manual steps.

### Track A — G6 Phase B

**Method decisions written down BEFORE the repair, as amendment A2** (the protocol
only allows amendments for steps not yet executed, and this was the last moment):
mutually blind parallel repairers one per cell; **repair may not add a test the model
did not write**; a cell whose recorded output is the literal `null` is `ABANDONED` at
zero minutes and must not be averaged in; truncated output is salvaged, never completed.

**Executed:** 11 repairable cells, one isolated agent each, own worktree, own clock,
live fix log, 30-minute cap. 13 cells needed nothing. **A2.2 compliance was verified
mechanically, not asserted** — `@Test` counts are identical in all 24 cells before and
after, the sole exception being the A2.4 salvage that dropped one cut-off method.

**Result — and it is the unflattering one:**

| | Phase A | Phase B |
|---|---|---|
| green cells | 12/24 | **21/24** |
| test methods | 154 (3 red) | **421 (0 red)** |
| line / branch coverage | 100 % / 100 % | **90.5 % / 78.8 %** |
| mutation score | 99.2 % | **73.2 %** |

The quality metrics got **worse**, because Phase A's perfect figures were computed only
over the cells that happened to compile — exclusively the small controllers. The God
class was not in the denominator. **Phase A was survivorship bias**, and only the
protocol's insistence on measuring every failed cell to the end made that visible.
Repaired, the God class reaches ~83 % line coverage at a **44–56 % mutation score**:
roughly half the injected faults survive the class the whole experiment is about.

Three further findings: one model wrote **134 test methods** where the other wrote 13,
with byte-identical measured value (21/21 lines, 13/13 mutants); across 15
wrong-expectation repairs there were **zero** real defects found in the production code;
and the open-weight model emitted `getStatusCodeValue()`, present in corpus A's Spring 4.3
and **removed** in corpus B's Spring 7 — migrating to a very new stack makes LLM support
temporarily worse.

**Two defects found in our own work, both disclosed rather than quietly fixed:**

- **A3 — one cell in 24 was decided by our extraction rule, not by the model.** Arm M1 /
  corpus A / `Rechnung`: the model wrote a draft, rejected it in plain text (*"Wait, I need
  to produce the correct final answer without mistakes"*), then wrote a correct 26-test
  class. Our pre-registered "first fenced block" rule kept the draft. **The cell was not
  re-extracted and the 12/24 rate stands** — a rule does not become wrong because it cost a
  point. All 24 responses were scanned; exactly one is affected. The REPORT's failure
  taxonomy is corrected: that row described the artifact correctly and the model wrongly.
- **A4 — the repair-effort clock is contaminated by our own design.** A2.1's parallelism
  had up to six agents sharing an 8-core machine, all invoking Maven; one cell recorded
  24.7 wall-clock minutes for a build that worked 5.6 seconds. The fix-log timestamps
  cannot rescue it either — several repairers batched their writes instead of logging live
  as §6 requires. Minutes are published as a measured **upper bound**; the transferable
  figures are the 52 fixes and their categories (19 IMPORT/SYNTAX, 17 MOCKING-SETUP,
  15 WRONG-EXPECTATION, 1 STRUCTURAL, **0 BUG-FOUND**). A2.1 traded a known confound for
  an unexamined one — that is written down as ours, in the protocol.

**Adoption (ADR-0011):** one class per unit, highest mutation score, ties to **fewer test
methods** — the rule that rejects the 134-method class. It selected a mixed set (2 frontier,
4 open-weight), which is the sign it is not a preference in disguise. 88 methods adopted,
**99 module tests green**, coverage **37.3 % → 81.3 % line**, ratchet 0.35 → **0.80**. §3's
80 % target is reached rather than declared for the first time. Stated next to it: branch
coverage still lags 23 points and the adopted God-class suite has a 44.1 % mutation score.

### Track B — the deployment documentation

`ENGINEERING_STANDARDS.md` §7 has always required `docs/deployment.md`. **It did not exist,
and the gap was not in `DEVIATIONS.md` either** — found by a doc audit, not by the ledger,
which is the more serious half. Written now, every command executed before being written
down, plus `docs/MANUAL_TASKS.md` as the by-hand checklist. Production deployment gets **no
invented steps**: §10 states what does not exist and enumerates the open items.

Beginner-fatal defects fixed: **`--wait` was missing from every human quickstart** while all
three CI workflows always used it (the single highest-frequency way to lose an hour here);
**characterization silently ignored `-Dtarget`**, so `-Dtarget=modern` went green against the
*legacy* stand — an equivalence proof that proved nothing, now a fail-fast; modern `verify`
needs a Docker daemon and nothing said so. Also corrected: the `legacy/` build failure is
**not** the Java 8 source level — measured on JDK 26, `compile` and `surefire` succeed and it
dies in `maven-war-plugin:2.6` reflecting into `java.util` internals JDK 16 sealed.

**Dead end, logged rather than deleted:** the first repair fan-out was launched without
worktree isolation — 11 agents would have collided in one testbed. Aborted after ~1 minute,
before any repair ran; the started clocks were discarded and the run restarted clean. No
measurement was contaminated.

**Verification:** modern `verify` green with 99 tests and the 0.80 ratchet · harness 24/24 ·
characterization green vs legacy and vs modern, and the new `-Dtarget` guard verified to fire ·
e2e green vs both stands · all 24 cells re-measured through the unmodified `measure.sh`.

**Hours:** 2.5 *(measured wall time 10:42 first tool call → 13:0x commit; agent wall-clock
under supervision, see the header. Includes both tracks and the two workflow fan-outs.)*

**Decisions:** amendments A2 (pre-Phase-B method), A3 and A4 (post-hoc disclosures);
ADR-0011 (adoption rule + ratchet to 0.80); `docs/deployment.md` and `docs/MANUAL_TASKS.md`
created; two DEVIATIONS rows added, one closed.

**Next: G7 — stage 6 (cloud/ops + launch).** OTel, Actuator health instead of the TCP probes,
deployment of both stands (Hetzner + Dokploy), playbook closing chapter + PDF export in CI,
README final, release v1.0.0, tag `stage-6-cloud-ops`. Two hard preconditions already
registered: neither stand has authentication (including the destructive
`POST /admin/bereinigen`), and PostgreSQL 9.6 is end-of-life. `docs/deployment.md` §10 is the
work list.

---

## 2026-08-05 — G7 part 1: stage 6's ops half, built and measured; the deployment half untouched — session 14

The owner had no time for manual infrastructure work, so this session took the part of
G7 that needs no server and left the part that does entirely alone. That split is not a
compromise, it is the honest boundary: everything below runs on a laptop, and nothing
below pretends a host exists.

**Baseline first, so any red would be ours:** characterization 47/47 and E2E 34/34 green
against both stands before the first edit.

### What was built

**PostgreSQL 9.6 → 18 on the modern stand only** (ADR-0012; DEVIATIONS P2 closed for
modern). Legacy keeps 9.6 for ever — an end-of-life database is part of what the exhibit
demonstrates. The upgrade produced the two findings worth the session:

- **`postgres:18-alpine` reports a collation it does not use.** It answers
  `datcollate = en_US.utf8` and then sorts in C order, because musl accepts the locale
  name and ignores it. Measured against the same probe: legacy 9.6 and Debian
  `postgres:18` agree exactly; alpine reorders the list. **A review step that reads the
  setting and compares it passes.** Only sorting is evidence — so the integration test
  now sorts instead of asking.
- **`PGDATA` and the declared `VOLUME` both moved** (`/var/lib/postgresql/data` →
  `/var/lib/postgresql/18/docker`, volume at `/var/lib/postgresql`). Keep the old mount
  and the container starts clean, creates an empty cluster in an anonymous volume and
  persists **nothing** — and because Flyway re-migrates on every start, it still looks
  healthy. Silent data loss behind a green health check.

The equivalence gate survived: **47/47 on both stands across a nine-major database gap**,
and `/api/bericht/topkunden` — the one endpoint whose raw DB types reach JSON through
`queryForList` — byte-identical, decimal literals included. That was the pre-registered
acceptance step, and it is evidence about 47 pinned contracts on a ten-customer seed, not
a proof about the application.

**Flyway** (ADR-0013, wart B18 closed for modern). `modern/db/init/` is gone; one schema
source. `baseline-on-migrate` stays **false** on purpose — with `true`, a populated volume
skips V1 and applies the seed twice. **`flyway-core` alone does nothing in Boot 4:** the
module split means the *starter* is what wires it, and without it the app starts, migrates
nothing, and dies later on `relation "kunde" does not exist`. Measured as a real failure
(6 of 7 integration tests errored) before the starter was added — a silent
misconfiguration, which is the expensive kind. CI's schema-drift guard was rewritten to
compare the SQL the server executes and to fail loudly when a file is missing, and was
verified with a negative control.

**Observability** (ADR-0015). Actuator health, ECS logs, OTLP traces, an observability
compose profile. **Two defects of our own, found by measuring rather than by reading:**
Boot's default readiness group does **not** include the database — with the DB stopped it
answered `200 UP` while `/actuator/health` answered 503 — and the healthcheck's
`retries: 24`, inherited from the TCP probe where it covered slow startup, meant a 503
application counted as healthy for two minutes. Both fixed and re-measured: unhealthy
**25 s** after the database dies, healthy again **6 s** after it returns. Liveness
deliberately excludes the database, so an outage does not restart a healthy application.
Also corrected: `trace.id`/`span.id` are renamed to the ECS field names, because a format
called ECS should be ECS.

**Security at the edge** (ADR-0014). Basic auth, security headers and rate limiting in a
Traefik overlay rather than in the application — the option the ledger had already named
("reverse-proxy auth counts"), the one Dokploy actually runs, and the one that does not
turn 4 safety-net tests (or, with CSRF, 17 write calls plus every E2E write scenario) into
ADR-0004 divergence decisions. Verified by `modern/edge/verify-edge.sh`, not asserted.
The counter-argument that a compose overlay is theatre is written into the ADR together
with the answer, including the obligation that a real host must not publish the app port.

### The finding worth more than the features

The first CSP was fully strict. **32 of the suite's 34 scenarios ran green through the
edge while the browser was blocking Angular's runtime styles.** The two `AdminTest`
scenarios cannot run through Basic auth at all and kept running against the application
port; nothing was disabled. The point is the other 32: **the Selenium safety net cannot
see a CSP violation.** It asserts behaviour and text, never appearance. Found by opening a
real browser and reading the console — not by any test we own.

Resolution: `style-src 'self' 'unsafe-inline'`, nothing else relaxed, `script-src 'self'`
still strict, 0 console errors afterwards. What is *measured* is the console output; **we
did not photograph the broken page and make no claim about how bad it looked.** The
limitation is now a DEVIATIONS row, `verify-edge.sh` asserts the policy verbatim, and the
visual check is on the human checklist, because that is where it honestly belongs.

### Also

Static analysis (Error Prone, ERROR tier only) cost ten `-J--add-exports` flags to run on
a current JDK at all — the recurring tax of living on the newest JDK. First run: **0
ERROR-severity findings, 17 warnings**, one of which is a real inherited concurrency
hazard — a `static final SimpleDateFormat` shared by all threads, under a 2016 comment
saying it has always worked. **Not fixed here**: the ops stage's job is the deployment
surface, not the God class that is G6's study object. Ledgered as wart **B20** for an
owner decision. We did not reproduce a corrupted response under load and do not claim to.

One k6 read-path scenario on both stands. **The modernisation is not measurably faster**
(modern p95 1.60 ms vs legacy 1.56 ms). Ten years of framework and JDK bought
supportability, security and a labour market, not speed on this workload.

A **safety-net gap that predates G7 was closed**: the E2E suite silently ignored
`-Dstand`, the mirror image of the bug session 13 fixed on the characterization side. A
guard on one side of a symmetric mistake is half a guard.

Playbook PDF now builds through a container so CI and laptop run the same command
(ubuntu-latest has pandoc but no TeX engine). The two glyphs Latin Modern lacks are mapped
in a header file, and the build fails if a new one appears.

### Dead ends and things we got wrong, kept rather than deleted

- The compose comment claiming the new probe would notice a dead database was written
  **before** it was true; measuring proved it false and the config was fixed to match the
  comment, not the comment to match the config.
- `newunicodechar.sty` is not in the pandoc image — the PDF build died on it; replaced by
  a catcode mapping rather than by pulling a TeX distribution into CI.
- A first `stages.md` draft claimed static analysis and image scanning were "measured in
  CI". They are configuration until a runner executes them; the hostile review caught it
  and the sentence now separates *measured locally*, *added to CI but never run*, and
  *not built*.
- The legacy compose comment "curl/wget are not in the Temurin image" was simply wrong for
  `eclipse-temurin:8-jre`. Corrected in place, with the correction dated.
- **`verify-edge.sh` produced a false red — and the false red was ours, not Traefik's.**
  Run straight after `up -d --wait` on a cold edge it reported **17 failed assertions**
  against a configuration that was completely correct. Cause: Traefik's Docker provider
  discovers containers asynchronously, so for a second or two the process answers and
  every path 404s, while `--wait` had already returned because the container was
  *running*. The whole session had been verifying an edge that happened to be warm. Two
  fixes, because one was not enough: the edge now has a compose healthcheck that checks
  **routing** rather than liveness (a liveness check would have gone green through exactly
  that window), and the script waits for the edge to route before asserting, so it is also
  correct when run by hand. Verified by recreating the edge and verifying immediately,
  three times: 20/20 assertions, exit 0, each time. Worth stating plainly: this is the
  same failure shape the repo's `--wait` rule exists for, and we walked into it anyway —
  one level further down, where "the container is up" and "the system works" came apart
  again. **A false red costs exactly as much trust as a false green.**

### What CI caught that local verification did not

The first push went **red on two checks**, and both were real — worth recording because the
whole session had been green locally right up to that point.

- **`harness` failed:** the G6 prompt renderer reads the schema from
  `modern/db/init/01-schema.sql`, which Flyway deleted. It refuses to render a prompt from a
  missing schema instead of quietly emitting one without DDL, which is the only reason this
  was a red check rather than a silently different experiment. Fixed by resolving corpus B's
  DDL to the Flyway file — and then **proved harmless the only way that counts: all 24
  recorded prompts in `runs/2026-07-31/` re-render byte-identically.** Only `CREATE TABLE`
  blocks reach the prompt, and the drift guard holds the two stands' DDL equal.
- **`testbed-validation (modern)` failed:** `PomDriftGuardTest` forbids the G6 testbed from
  compiling against a different classpath than the module under test, and stage 6 had just
  given that module four new dependencies. The guard worked exactly as designed.
- **`modern-build` then failed on the second push — in the teardown, not the work.** The new
  edge-verification step passed on the runner (20/20 assertions, routing after 1 s), and the
  `Stop modern stand` step died because Compose interpolates the edge overlay's labels even
  for `down`, while the credential only existed inside the previous step's shell. A local
  run could not have caught it: the variable is exported in the developer's shell for the
  whole session. **This is the entire justification for the "added to CI but not yet
  executed" category in `stages.md`** — it was written before this failure, and the failure
  is what it was written about.

Both are the same lesson from opposite ends: **corpus B is not a copy of the modern module,
it *is* the modern module**, so an ops stage moves the measurement environment of a finished
experiment. Recorded as **amendment A5**, which changes no recorded result but states the
cost plainly — a corpus-B replication after today compiles against a wider classpath than the
2026-07-31 run did, so mixing runs from before and after compares environments, not models.
The honest general point is in the amendment: an experiment whose subject is a living module
inherits that module's future, and the alternative (freezing a copy) would have made corpus B
stop being what its name claims.

**Verification:** characterization 47/47 vs legacy and 47/47 vs modern · E2E 34/34 vs
legacy and 34/34 vs modern · modern `verify` green, **100 module tests**, coverage 81.3 %
line / 58.3 % branch, 0.80 ratchet met · schema-drift guard green plus negative control ·
`verify-edge.sh` 20/20 assertions on three consecutive cold starts · playbook PDF builds (7 chapters) · both stands rebuilt from an empty
volume, Flyway applying 2 migrations · G6 infrastructure green again after the CI findings
(harness 24/24, testbed-validation 8/8 legacy and 7/7 modern) and all 24 frozen prompts
re-rendered byte-identically. **A third CI-only failure, and this one was a flaky assertion of ours.** The rate-limit
check fired 80 requests through a shell loop — one `curl` process each. On a loaded runner
those 80 process spawns took longer than the token-bucket window, so the burst never
exceeded 30/s and the check failed against a limiter that was working perfectly. A
timing-dependent assertion in a suite whose stated policy is zero flaky tolerance is a
defect, not bad luck. Replaced by a single `curl --parallel` firing 200 concurrent
requests, which is a burst regardless of how slow the machine is: five consecutive runs
produced 103–173 × 429. The assertion stays `> 0` rather than a count, because the count
is a property of the machine and not of the configuration.

**On the runner, all eight checks green** on the third push: the edge-verification step
20/20 with routing after 1 s (the rate limiter produced **6** × 429 there against 5 here —
which is exactly why `SECURITY.md` asserts *> 0* and quotes 5 as one observation rather
than as a property).

**And the image scan paid for itself on its first execution.** One HIGH: **CVE-2026-54291**
in `org.postgresql:postgresql` 42.7.11 — a man-in-the-middle protection bypass through a
SCRAM-SHA-256-PLUS downgrade, which is the authentication mechanism PostgreSQL 18 uses and
which this very stage had just switched the modern stand to. Pinned to 42.7.12, the minimum
fixed version rather than the newest, because this is a security fix and not a version bump.
Nothing else in the repository would have caught it: Dependabot watches manifests, and the
driver's version was not in one — it came from Boot's dependency management.

After the pin, the scan is **clean: 0 findings** across the Ubuntu base layer, the
application jar and the Go binary in the image. Every one of the eight required checks is
green.

Fixing it uncovered a quieter hole. Neither `modern/pom.xml` nor the G6 testbed declared a
version for the driver, and a property override travels through Boot's `<parent>` but **not**
through the testbed's imported BOM — so the first fix moved the application to 42.7.12 while
the testbed stayed on 42.7.11, and `PomDriftGuardTest` stayed **green**, because it compares
*declared* coordinates and neither declared one. Both now pin the version literally. The
guard was never wrong; it was answering a narrower question than it appeared to, which is
the same shape as the CSP finding above.

**Hours:** 1.4 *(measured wall time 15:34 first tool call → 16:55, including the CI-red repair; agent wall-clock under
supervision, see the header)*

**Decisions:** ADR-0012 (PostgreSQL 18 + pinned collation), ADR-0013 (Flyway),
ADR-0014 (authentication at the edge), ADR-0015 (observability); **PROTOCOL amendment A5**
(the experiment's subject module moved; no recorded result altered); `SECURITY.md` created;
playbook Kapitel 7; wart B20 recorded; ops chapter numbered 7 with the stage↔chapter break
stated openly rather than renumbered; **no stage tag and no v1.0.0 release**.

**Late catch, before the merge:** `.env.example` documented none of the seven environment
variables stage 6 introduced. A hard rule ("`.env.example` complete") broken inside the very
session that added the variables, and found by grepping the compose files against the
template rather than by the ledger. Fixed and grouped by purpose; the two entries whose
*defaults* carry weight are spelled out — HSTS ships at 0 deliberately, and production must
drop the demo-seed Flyway location or it inserts ten fictional customers into an empty
database.

**Next: G7 part 2 — the deployment, which is owner-blocked.** `docs/MANUAL_TASKS.md` §I
lists what only the owner can decide or procure (host, domains, whether the legacy stand
goes public at all given its preserved SQL injection, and the three repository secrets
that do not exist yet). Nothing further can be honestly written until those exist. When
they do: image push to GHCR, Dokploy per stand, TLS plus switching HSTS on, `pg_dump` cron
**with a restore rehearsal**, then the tag `stage-6-cloud-ops`, playbook Kapitel 8 and
v1.0.0.
