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
| `stage-5-angular` | AngularJS → **Angular 22** ([ADR-0003](docs/adr/0003-angular-22-instead-of-angular-20.md); was 20) via Strangler Fig; same E2E scenarios green on old AND new UI | Kap. 5 — AngularJS → Angular 22 | pending (G5) |
| `stage-6-cloud-ops` | Docker multi-stage, OTel, health checks, deploy of both stands (Hetzner + Dokploy) | Kap. 6 — Betrieb & Cloud + Schlusskapitel | pending (G7) |

The AI test-generation experiment (G6) is not a migration stage; its artefacts live
in [`ai-testgen/`](ai-testgen/) and are referenced from the playbook.
