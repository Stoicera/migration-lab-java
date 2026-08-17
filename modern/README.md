# modern/ — the migrated application

Grows stage by stage from a faithful copy of `legacy/`; since stage 5: Java 25,
Spring Boot 4.1.x, **Angular 22** (executable JAR, frontend built into the boot
artifact). Current state: see [`../stages.md`](../stages.md).

## Run it (side by side with the legacy stand)

```bash
docker compose -f modern/docker-compose.yml up -d --wait
```

- App: http://localhost:8090 · admin (SPA route since stage 5): http://localhost:8090/admin
- PostgreSQL: 127.0.0.1:5434 (werkstatt/werkstatt), same committed seed as legacy

**`./mvnw verify -f modern/pom.xml` requires a running Docker daemon** — the module
tests include a Testcontainers integration test that starts a real `postgres:18`
with the stand's pinned `en_US.utf8` collation — image and collation both matter,
because collation decides `ORDER BY` and the golden masters are sensitive to it
([ADR-0012](../docs/adr/0012-postgresql-18-und-fixierte-collation.md)) — and lets
**Flyway** build the schema from the same locations the compose stand migrates on
start (`src/main/resources/db/migration` for the schema, `db/demo` for the seed;
production drops the second), so there is one schema source and not two copies
([ADR-0013](../docs/adr/0013-flyway-statt-handgestarteter-sql.md)). Until stage 6
this was `postgres:9.6` seeded from mounted `db/init` scripts. Full operations
reference: [`docs/deployment.md`](../docs/deployment.md).

## Frontend build (stage 5)

The Angular app lives in [`frontend/`](frontend/) and is built INTO the Boot
artifact by `frontend-maven-plugin` during `./mvnw verify -f modern/pom.xml`
(or the Docker image build): the plugin installs its own pinned Node
(v24.18.1 LTS) and runs `npm ci` against the committed lockfile — no local
Node toolchain required, same pattern as the Dockerized JDK-8 build of
`legacy/`. Frontend dev loop: `cd modern/frontend && npm start` (proxy to a
running stand not configured on purpose — the compose stand is the reference).
Lint/format gates (`ng lint`, `prettier --check`, Spotless google-java-format)
run at `verify` (see `docs/DEVIATIONS.md`, item met at G5).

The E2E wait contract of the UI: the app maintains
`window.werkstattOffeneRequests` (an HTTP-interceptor counter, see
`frontend/src/app/offene-requests.interceptor.ts`) because the app is zoneless
— this is a testability contract, do not remove it (e2e/README, wait strategy).

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
| B1 God class | **survives** — it was G6's study object (completed 2026-08-02) and stayed after it, deliberately: 728 lines at v1.0.0, not split. Sanctioned in `docs/DEVIATIONS.md` (ArchUnit row: a God class on purpose, not an architecture violation) |
| B2 field injection | removed (stage 4 constructor-injection sweep, complete) |
| B3/B4 SQL concatenation | removed — string sinks in stage 4, remaining typed-ID sites in the review remediation; `modern/` is concatenation-free |
| B7 no transactions | **survives** — pinned by characterization (km side effect); fix would be behaviour-relevant → needs ADR-0004 sanctioning first |
| B8 money as double | **survives** — disposition in `docs/DEVIATIONS.md`; rounding behaviour pinned by characterization |
| B9 static SimpleDateFormat | **survives** — as written (2026-07-30): "single-threaded usage today; candidate for the G6 test target list". Both halves have since been overtaken, kept here rather than rewritten: `WerkstattService` *was* the headline G6 target (`ai-testgen/PROTOCOL.md`, slice S1) and the wart outlived the experiment; and on 2026-08-05 the stage-6 static analysis re-found the same field independently, filed as `LEGACY_NOTES` B20 and ledgered `deferred(post-v1.0)` in `docs/DEVIATIONS.md`. Since the stand went live (2026-08-14, Kap. 8) the disposition rests on that ledger entry, not on the "single-threaded usage" premise |
| B10 raw error messages (500 + `e.getMessage()`) | **survives** — the error contract is pinned by characterization; sanitising it is a contract change (ADR-0004 gate) |
| B11–B13, B19 | **survive** — pinned where observable; same ADR-0004 gate for any change |
| B14 debug leftovers / dead code | removed (stage 2, documented) |
| B16 JSP admin page incl. destructive POST | removed (stage 5, SD-2) — absorbed as SPA route `/admin` + `GET /api/admin/statistik`; `POST /admin/bereinigen` keeps path/status/meldung (pinned); JSP/JSTL/gson retired, WAR→JAR |
| B17 config duplication w/ plaintext password | removed (review remediation) — `application-prod.properties` deleted; deployment config comes from the environment (`.env.example`). *As written this was still pending G7; redeemed on 2026-08-14 (`stage-6-cloud-ops`, v1.0.0) — the live stands read their secrets from Dokploy's per-service env store, never from this repository ([ADR-0016](../docs/adr/0016-deployment-dokploy-stoicera-fleet.md))* |
| B18 hand-run SQL schema | removed (stage 6 / G7, 2026-08-05) — schema and demo data are Flyway migrations under `src/main/resources/db/{migration,demo}`, `modern/db/init/` is gone ([ADR-0013](../docs/adr/0013-flyway-statt-handgestarteter-sql.md)); `legacy/` keeps its hand-run scripts — there, the wart is the exhibit |

## Stage log

- **Stage 6 (`stage-6-cloud-ops`, done 2026-08-14):** ops half on 2026-08-05 — PostgreSQL
  9.6 → 18 with pinned collation ([ADR-0012](../docs/adr/0012-postgresql-18-und-fixierte-collation.md)),
  schema via Flyway instead of mounted init scripts ([ADR-0013](../docs/adr/0013-flyway-statt-handgestarteter-sql.md)),
  Actuator-based health checks whose readiness actually tracks the database, OpenTelemetry
  and structured logs ([ADR-0015](../docs/adr/0015-observability-actuator-otel-strukturierte-logs.md)),
  Error Prone, and the reverse-proxy edge as an auth boundary
  ([ADR-0014](../docs/adr/0014-authentifizierung-am-edge.md); overlay `docker-compose.edge.yml`,
  check `edge/verify-edge.sh`). Deployment half on 2026-08-14 — CI-built GHCR images, one
  Dokploy compose service per stand on the Stoicera fleet, TLS from Let's Encrypt, nightly
  `pg_dump` with an **executed** restore rehearsal
  ([ADR-0016](../docs/adr/0016-deployment-dokploy-stoicera-fleet.md)); the modern stand is
  public at <https://migration-lab.stoicera.cyou> with its admin surface gated. Off-site
  copies of those dumps do **not** exist yet — deliberately deferred, and the nightly job
  logs that fact.

- **Stage 5 (`stage-5-angular`):** AngularJS 1.8 → Angular 22.1.0 via Strangler
  Fig on a URL seam ([ADR-0009](../docs/adr/0009-strangler-fig-url-seam-no-ngupgrade.md)):
  the Angular app took `/` with path routes from the first slice, the old app
  stayed fully functional at `/alt.html#!/…` until each route ported over —
  one commit per route group — each verified green against the modern stand,
  legacy legs re-run when shared suite code changed, full both-stand matrix at
  the gate (cadence recorded in the worklog) — cross-framework handovers as
  full page loads. JSP admin page absorbed (SD-2), the legacy
  "undefined" error alert replaced by the real server message (SD-3),
  packaging WAR→JAR, `src/main/webapp/` deleted at cutover. Formatting parity
  is enforced (EuroPipe replicates the 2016 `euro` filter byte-for-byte;
  alert/confirm kept) — UX modernisation is deliberately out of scope.
  Zoneless lesson (found by the net): async-written component state must be
  signal-tracked. Details: playbook Kap. 5.

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
