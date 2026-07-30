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

**Hours:** 1.5

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

**Hours:** 1.0

**Decisions:**

- Local Java-8 builds always through the Maven/Temurin-8 image (documented in
  `legacy/README.md`) — no local JDK 8 toolchain requirement for contributors.
- Test-data cleanup via SQL instead of volume reset when the stack is running.

**Next (G2 — the most important milestone):** Selenium 4 suite with Page Objects
+ selector map vs. the legacy UI; characterization tests (API + DB states);
CI gates armed for good; playbook chapter 1; tag `stage-1-safety-net`.
