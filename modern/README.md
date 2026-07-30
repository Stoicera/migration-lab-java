# modern/ — the migrated application

Grows stage by stage from a faithful copy of `legacy/`; final state: Java 25,
Spring Boot 4.1.x, Angular 22, PostgreSQL. Current state: see [`../stages.md`](../stages.md).

## Run it (side by side with the legacy stand)

```bash
docker compose -f modern/docker-compose.yml up -d
```

- App: http://localhost:8090 · JSP admin: http://localhost:8090/admin
- PostgreSQL: localhost:5434 (werkstatt/werkstatt), same committed seed as legacy

## The equivalence gate

The characterization suite that locks the legacy behaviour runs against THIS
stand in `modern-ci` on every commit (`-DbaseUrl=http://localhost:8090`), and
the Selenium suite runs as the `e2e (modern)` matrix leg. Functional
equivalence is proven per commit, not claimed.

## Rules for this directory

- Every change must have a migration purpose — beautification without need is
  scope creep (playbook rule, lived here).
- From `stage-1-safety-net` onward, no commit may break the safety net.
- OpenRewrite recipes are used **and evaluated** (stage 4) — what they caught
  vs. missed is playbook data.

## Wart ledger — what survives here, deliberately

The legacy wart catalogue (`legacy/LEGACY_NOTES.md`) applies to the exhibit;
THIS table is the disposition of each wart on the migrating stand, so
"deliberate" and "forgotten" stay distinguishable (review finding 8/14):

| Wart | Status in modern/ |
|---|---|
| B1 God class | **survives** until G6 — it is the study object of the AI test-gen experiment |
| B2 field injection | removed (stage 4 constructor-injection sweep, complete) |
| B3/B4 SQL concatenation | removed — string sinks in stage 4, remaining typed-ID sites in the review remediation; `modern/` is concatenation-free |
| B7 no transactions | **survives** — pinned by characterization (km side effect); fix would be behaviour-relevant → needs ADR-0004 sanctioning first |
| B8 money as double | **survives** — disposition in `docs/DEVIATIONS.md`; rounding behaviour pinned by characterization |
| B9 static SimpleDateFormat | **survives** — single-threaded usage today; candidate for the G6 test target list |
| B10 raw error messages (500 + `e.getMessage()`) | **survives** — the error contract is pinned by characterization; sanitising it is a contract change (ADR-0004 gate) |
| B11–B13, B19 | **survive** — pinned where observable; same ADR-0004 gate for any change |
| B14 debug leftovers / dead code | removed (stage 2, documented) |
| B16 JSP admin page incl. destructive POST | **survives** until stage 5 absorbs it (gson dies with it) |
| B17 config duplication w/ plaintext password | removed (review remediation) — `application-prod.properties` deleted; real deployment config arrives with G7 via environment, see `.env.example` |
| B18 hand-run SQL schema | **survives** — Flyway disposition in `docs/DEVIATIONS.md` (G7) |

## Stage log

- **Stage 4 (`stage-4-boot-4x`):** Boot 2.7 → 3.5.16 → 4.1.0, Java 17 → 25,
  `javax` → `jakarta`. OpenRewrite used **and evaluated** ([ADR-0002](../docs/adr/0002-openrewrite-as-assistant-not-autopilot.md)):
  recipes handled the parent bumps, the `web` → `webmvc` starter rename, the
  taglib URI and a property key — but pinned javax JSTL instead of migrating it
  (green build, dead JSP page at runtime) and ignored the Jackson 3 move.
  Hand work with migration purpose: constructor-injection sweep (testability is
  the next milestone's precondition) and the B4 SQL-injection fix, with
  before/after proof in playbook ch. 4. God class deliberately left standing —
  it is the study object for the AI test-generation experiment.

- **Stage 3 (`stage-3-boot-2.7`):** Boot 1.5.22 → 2.7.18 + Java 17 in one jump.
  Three real breaks, all net-caught: `SpringBootServletInitializer` package
  move (compile); pinned gson 2.3.1 vs `GsonAutoConfiguration` (startup);
  `java.sql.Date` wire-format drift Jackson 2.8→2.13 (API contract — invisible
  in the UI, caught only by the golden masters; pinned back via
  `JacksonWireCompatConfig`). Details: playbook Kap. 3.

- **Stage 2 (`stage-2-jdk-build`):** baseline copy of legacy + build hygiene —
  logging unified to SLF4J/Logback (log4j 1.2 retired), unused `commons-lang`
  dropped, dead code removed. Deliberately NOT raised: the JDK — Spring Boot
  1.5 caps the runtime at Java 8; the raise lands with Boot 2.7 (stage 3).
  Everything else (field injection, God class, SQL strings, `javax.*`, gson on
  the JSP page) stays until its migration stage — the diff per stage IS the
  playbook material.
