# Stages — tag → state → playbook chapter

Git tags are first-class deliverables: every tag is a checkout-and-run state
(`git checkout <tag>` → `docker compose up` → working app).

| Tag | State | Playbook chapter | Status |
|-----|-------|------------------|--------|
| `stage-0-legacy` | WerkstattCRM as found: Java 8, Spring Boot 1.5, AngularJS 1.8, JSP admin page, PostgreSQL, **no tests** | — (Ausgangslage described in ch. 1) | pending (G1) |
| `stage-1-safety-net` | Selenium E2E suite + characterization tests green against legacy; CI gates active — **from here on, no commit may break them** | Kap. 1 — Ohne Netz keine Migration | pending (G2) |
| `stage-2-jdk-build` | Maven hygiene, dependency audit, JDK raised under Boot-compatible ceiling, logging unified | Kap. 2 — Fundament: Build & JDK | pending (G3) |
| `stage-3-boot-2.7` | Spring Boot 1.5 → 2.7 (the documented long jump) | Kap. 3 — Der weite Sprung: Boot 1.5 → 2.7 | pending (G3) |
| `stage-4-boot-4x` | Boot 2.7 → 3.x → 4.1, Java 25, `javax`→`jakarta`, security rewrite, OpenRewrite used & evaluated | Kap. 4 — Boot 3/4 & Java 25 | pending (G4) |
| `stage-5-angular` | AngularJS → Angular 20 via Strangler Fig; same E2E scenarios green on old AND new UI | Kap. 5 — AngularJS → Angular 20 | pending (G5) |
| `stage-6-cloud-ops` | Docker multi-stage, OTel, health checks, deploy of both stands (Hetzner + Dokploy) | Kap. 6 — Betrieb & Cloud + Schlusskapitel | pending (G7) |

The AI test-generation experiment (G6) is not a migration stage; its artefacts live
in [`ai-testgen/`](ai-testgen/) and are referenced from the playbook.
