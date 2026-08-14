# Stages — tag → state → playbook chapter

Git tags are first-class deliverables: every tag is a checkout-and-run state
(`git checkout <tag>` → `docker compose up` → working app).

| Tag | State | Playbook chapter | Status |
|-----|-------|------------------|--------|
| `stage-0-legacy` | WerkstattCRM as found: Java 8, Spring Boot 1.5.22, AngularJS 1.8.2, JSP admin page, PostgreSQL 9.6, **no tests** — wart catalogue in `legacy/LEGACY_NOTES.md` | — (Ausgangslage described in ch. 1) | **done** (2026-07-30) |
| `stage-1-safety-net` | Selenium E2E suite (13 tests, selector-map abstraction) + characterization tests (12 golden masters, 5 DB transitions) green against legacy; CI gates active — **from here on, no commit may break them** | [Kap. 1 — Ohne Netz keine Migration](playbook/01-ohne-netz-keine-migration.md) | **done** (2026-07-30) |
| `stage-2-jdk-build` | `modern/` bootstrapped as faithful copy running side by side (8090/5434); dependency audit; logging unified to Logback (log4j 1.2 retired); JDK deliberately NOT raised — Boot 1.5 caps at Java 8 (raise lands with Boot 2.7) | [Kap. 2 — Fundament: Build & JDK](playbook/02-fundament-build-und-jdk.md) | **done** (2026-07-30) |
| `stage-3-boot-2.7` | Spring Boot 1.5.22 → 2.7.18 + Java 17 in one documented jump; three real breaks (servlet-initializer move, gson pin vs autoconfig, java.sql.Date wire drift — caught by golden master, invisible in UI) | [Kap. 3 — Der weite Sprung](playbook/03-der-weite-sprung-boot-27.md) | **done** (2026-07-30) |
| `stage-4-boot-4x` | Boot 2.7 → 3.5 → 4.1 + Java 25; OpenRewrite used AND evaluated ([ADR-0002](docs/adr/0002-openrewrite-as-assistant-not-autopilot.md)); constructor-injection sweep; SQL-injection (B4) closed — the deliberate hostile-input divergence is registered as SD-1 in [ADR-0004](docs/adr/0004-functional-equivalence-and-sanctioned-divergence.md) and pinned by characterization on both stands | [Kap. 4 — Boot 3/4, Java 25 & OpenRewrite](playbook/04-boot-3-4-java-25-und-openrewrite.md) | **done** (2026-07-30) |
| `stage-5-angular` | AngularJS → **Angular 22.1.0** ([ADR-0003](docs/adr/0003-angular-22-instead-of-angular-20.md); was 20) via Strangler Fig on a URL seam ([ADR-0009](docs/adr/0009-strangler-fig-url-seam-no-ngupgrade.md), no ngUpgrade): route-by-route port with the same E2E scenarios green on the modern stand at every commit (legacy legs re-run at every commit that touched shared suite code, plus the full both-stand matrix at the gate — the risk-based cadence is documented in worklog session 9/10) — incl. one flow crossing the framework seam mid-scenario; JSP admin page absorbed (SD-2), "undefined" alert fixed (SD-3), WAR→JAR; lint/format gates armed (Spotless + angular-eslint) | [Kap. 5 — AngularJS → Angular 22](playbook/05-angularjs-nach-angular-22.md) | **done** (2026-07-31) |
| `stage-6-cloud-ops` | Docker multi-stage, OTel, health checks — and **both stands deployed** on the Stoicera fleet (Hetzner + Dokploy): [migration-lab.stoicera.cyou](https://migration-lab.stoicera.cyou) public with the admin surface gated, [migration-lab-legacy.stoicera.cyou](https://migration-lab-legacy.stoicera.cyou) entirely behind Basic auth (it preserves SQL injection on purpose — [ADR-0016](docs/adr/0016-deployment-dokploy-stoicera-fleet.md)); images from GHCR via CI, TLS via Let's Encrypt, nightly `pg_dump` with an **executed** restore rehearsal | [Kap. 7 — Betrieb & Härtung](playbook/07-betrieb-und-haertung.md) · [Kap. 8 — Der Live-Gang](playbook/08-der-live-gang-und-was-das-heisst.md) | **done** (2026-08-14) — ops half 2026-08-05, deployment half 2026-08-14 |

**Why stage 6's chapter is number 7.** This table promised "Kap. 6" for the ops stage
until 2026-08-05, and that promise was already broken when it was written: chapter 6 is
the AI test-generation chapter, because G6 was a milestone *without* being a stage. The
1:1 mapping between stages and chapters that SPEC assumes ends there. The ops chapter is
therefore **Kapitel 7** and the closing chapter will be **Kapitel 8** — recorded here
rather than renumbered quietly, because a documentation set that silently rewrites its
own history is worth less than one with a visible seam.

**How stage 6 completed, precisely.** The stage landed in two halves nine days apart, and
the distinction is kept because "measured locally" and "running in production" are different
claims. *2026-08-05:* PostgreSQL 18 with pinned collation, Flyway, Actuator health whose
readiness actually tracks the database, OpenTelemetry traces and ECS logs, the Traefik edge
(auth, headers, rate limit), Error Prone, the k6 baseline, the playbook PDF — all measured
locally. *2026-08-14:* the deployment — CI-built images on GHCR, one Dokploy compose service
per stand on the fleet, TLS, nightly `pg_dump` with an executed restore rehearsal, and
`deploy/verify-live.sh` green against the public URLs. The tag `stage-6-cloud-ops` was
created only after that second half was proven, which is why it carries the 2026-08-14 date
while most of the stage's code carries 2026-08-05: a stage tag is a promise that
`git checkout <tag>` gives you a state that runs, and for an ops stage "runs" includes
"is deployed somewhere you can visit" ([`docs/deployment.md` §10](docs/deployment.md#10-production-deployment)).

The AI test-generation experiment (G6) is not a migration stage; its artefacts live
in [`ai-testgen/`](ai-testgen/) and are referenced from the playbook
([Kap. 6](playbook/06-ki-testgenerierung-gemessen.md)). It carries one tag of its own,
which is a **pre-registration marker, not a checkout-and-run state**:

| Tag | Meaning | Status |
|-----|---------|--------|
| `ai-testgen-protocol-v1` | `ai-testgen/PROTOCOL.md` frozen — selection, prompts, models, metrics and threats fixed **before** the first API call; every recorded call carries this file's SHA-256. Harness + both measurement testbeds validated ([ADR-0010](docs/adr/0010-ai-testgen-scope-corpus-b-and-agent-repair.md)) | **frozen** (2026-07-31) |
