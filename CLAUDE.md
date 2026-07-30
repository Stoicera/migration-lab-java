# CLAUDE.md — migration-lab

You are working on **migration-lab**: a public, reproducible legacy modernization (Java 8 / Spring Boot 1.5 / AngularJS → Java 25 / Spring Boot 4 / Angular 22) with a Selenium safety net, measured AI-assisted test generation, and a German migration playbook. This is a portfolio piece of the Stoicera Software Group aimed at Austrian SMEs and universities (JKU) — it must demonstrate how a senior team de-risks migrations. Honesty and reproducibility are the product.

## Read first (in this order)
1. `docs/STOICERA_LABS_KONTEXT.md` — who we are, who this is for
2. `docs/ENGINEERING_STANDARDS.md` — binding Definition of Done
3. `docs/PRD.md` — requirements and selection rationale (German)
4. `docs/SPEC.md` — repo layout, stages, safety net, AI experiment protocol
5. `docs/MILESTONES.md` — work strictly milestone by milestone (G0…G7)

## Hard rules
- **The safety net is sacred.** From tag `stage-1-safety-net` onward, no commit may break the Selenium E2E suite or the characterization tests. Red pipeline = stop and fix, never "fix later", never skip/disable a test to get green.
- **Stage discipline:** every stage ends with a git tag, a working `docker compose up` state, and a playbook chapter. Never blend stages in one commit series.
- **Legacy stays legacy:** code in `legacy/` keeps its 2016-era style (that's the exhibit). Never modernise `legacy/` beyond what `LEGACY_NOTES.md` documents. All modernization happens in `modern/`.
- **Honesty rules:** effort hours logged per session in `docs/worklog.md`; dead ends and AI failures are documented, not deleted; the AI test-gen experiment follows `ai-testgen/PROTOCOL.md` written BEFORE execution — results are never curated afterwards.
- **Refactor only with migration purpose.** Beautification without need is scope creep — the playbook preaches this, the repo must live it.
- **E2E quality:** Selenium 4, Page Objects, selector-map abstraction (same scenarios vs. AngularJS and Angular UIs), explicit waits only, zero flaky tolerance.
- **Stack of the modern side:** Java 25, Spring Boot 4.1.x, Angular 22 (ADR-0003; was 20), PostgreSQL, Testcontainers, OpenRewrite (used AND evaluated). No other frameworks without ADR + approval.
- **No secrets in the repo.** `.env.example` complete.
- Language: code + technical docs English; `playbook/` German (decision-maker
  audience); the German strategy docs (PRD, MILESTONES, VERMARKTUNG,
  STOICERA_LABS_KONTEXT, ENGINEERING_STANDARDS) stay German by design;
  `docs/glossary.md` bridges the terms.
- Standards deviations are never silent: ledger in `docs/DEVIATIONS.md`.

## Working style
- One milestone at a time; start each session reading `docs/worklog.md` + current milestone and running the relevant suites; end with green CI + worklog entry (date, what, hours, decisions, next).
- Ambiguity: PRD → SPEC → ask the owner (Sebastian). Never silently invent scope.
- ADRs for every real migration decision (jump path, OpenRewrite usage, hybrid strategy).
- Conventional Commits; tags and `stages.md` updated in the same PR as the stage completion.

## Commands (keep current as the repo grows)
```bash
docker compose -f legacy/docker-compose.yml up -d    # legacy stand (8080, db 127.0.0.1:5433)
docker compose -f modern/docker-compose.yml up -d    # modern stand (8090, db 127.0.0.1:5434; exists since stage 2)

# suites (stands must be up; both suites fail loudly if zero tests are discovered)
./mvnw verify -f characterization/pom.xml            # characterization vs legacy
./mvnw verify -f characterization/pom.xml \
  -DbaseUrl=http://localhost:8090 -DdbUrl=jdbc:postgresql://localhost:5434/werkstatt -Dstand=modern
./mvnw verify -f e2e/pom.xml -Dtarget=legacy         # E2E (also: -Dtarget=modern)

# builds: the legacy WAR needs Java 8 and builds INSIDE Docker (compose build) —
# a bare `./mvnw -f legacy/pom.xml` fails on a modern local JDK (legacy/README.md).
# modern verify builds the Angular frontend too (frontend-maven-plugin installs its
# own Node, npm ci) and runs the lint/format gates (Spotless, ng lint, prettier).
./mvnw verify -f modern/pom.xml                      # modern build (module tests arrive with G6)
```
