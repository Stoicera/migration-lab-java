# Worklog — migration-lab

Honest effort log, one entry per session. Format: date · what was done · hours ·
decisions · next. The hours here are part of the product (playbook input) — they
are logged as spent, never smoothed.

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

**Next (G5):** AngularJS → Angular 20 via Strangler Fig; same E2E scenarios
green on old AND new UI via selectors/modern.properties v2 — the headline
result. JSP admin page gets absorbed (gson dies with it).
