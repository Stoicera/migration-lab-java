# migration-lab — Agent Context


## Company (Stoicera Software Group)

Two founders (Sebastian Kern, Raphael Lugmayr), Upper Austria. Brands: Stoicera (B2B web/AI/EU cloud, modern stack) and Lugmayr-Kern (.NET/Java contract work, B2C). Goal until 7.7.2027: 10,000 € monthly result, half recurring — every task names the goal it serves; results first, ship the 80 % version, measure, iterate. Founders orchestrate; agents build, test, deploy, operate. Truth before effect: no invented facts, no superlatives, name assumptions. Simple over complete. Decide and report in three lines (done, open, blocked). Customer contact answered substantively within 4 hours. Never: customer data or secrets in repo/prompts; dark patterns; religious or warrior vocabulary in anything public; Hostinger/Vercel/Railway. German (AT) for customers, code in English, commits in German.

## What migration-lab is

See `docs/00_ssot.md`. One sentence here: public, reproducible Java legacy modernisation with measured results, for Austrian SMEs and universities.
Non-goals (`docs/PRD.md` §5 Out of Scope): no database switch — PostgreSQL stays, Oracle→Postgres only as a playbook excursus · no microservice decomposition — staying modular is the deliberate answer (ADR) · no operation of the legacy stand beyond the project end.
Active PRD: `docs/PRD.md` — read before building.

## How we work here

- Work = GitHub issue. Questions as issue comments, not chat.
- Fresh git worktree per task from `origin/master` (never build on master), PR against `master` with `Closes #NN` and the template Intent · Gherkin · Evidence · Debt taken · Open. Small PRs. CI green before PR. Full loop: `ai/prompts/factory-feature.md`.
- There is no root pom; every Maven call needs `-f <module>/pom.xml`. Commands per `docs/deployment.md` §5–§7:
- Build: `./mvnw -B verify -f modern/pom.xml` (also runs `npm ci` + Angular build, `ng lint`, `prettier --check`, Spotless, JaCoCo; needs Docker) · legacy: `./mvnw -B verify -f legacy/pom.xml` (JDK 8)
- Run a stand: `docker compose -f modern/docker-compose.yml up -d --wait` (legacy: `legacy/docker-compose.yml`)
- Test: `./mvnw verify -f e2e/pom.xml -Dtarget=legacy|modern` · characterization: `./mvnw verify -f characterization/pom.xml` (legacy; modern needs `-DbaseUrl=… -DdbUrl=… -Dstand=modern`) · both need a running stand
- Lint/Typecheck: part of the modern `verify` above · frontend alone: `npm run lint` in `modern/frontend`
- Migrate: Flyway runs on application start (`spring-boot-starter-flyway`); no separate command.
- Deploy: push to `master` → `.github/workflows/deploy.yml` builds both images to GHCR → triggers the Dokploy compose services `legacy-stand` / `modern-stand` → check with `deploy/verify-live.sh` (`docs/deployment.md` §10). Rollback: `git revert --no-edit <sha> && git push origin master` — `deploy.yml` rebuilds both images under the `master` tag and redeploys both Dokploy services (`pull_policy: always`, `docs/deployment.md` §10.7); then `deploy/verify-live.sh`. Every image also carries an immutable `sha-<12>` tag for pinning.
- No per-PR preview: `deploy.yml` runs on `master` only.
- Before you request review: check the result against the intent, fix deviations yourself. Copilot review runs automatically on every PR; a second model reviews security.
- Decisions with reach → `docs/decisions/` (ADR, one page). Cycle memo → `docs/cycles/`.
- After any production change: append one line to `ops/runlog.md` (date · agent · what · rollback).

## Conventions

- Stack: legacy stand Java 8, Spring Boot 1.5, PostgreSQL 9.6 (preserved on purpose) · modern stand Java 25, Spring Boot 4.1, Flyway, PostgreSQL 18, Angular 22 frontend · Maven Wrapper, Docker Compose.
- Money in cents (integer), time zone Europe/Vienna, tenant id on every table.
- No new dependency without one sentence of justification in the PR. No speculative abstractions.
- Tests: unit for logic, integration for API, one E2E per critical path. Synthetic data only.
- Accessibility and Lighthouse ≥ 90 on public pages are merge gates.

## Never

- Personal or customer data in repo, fixtures, logs or prompts.
- Secrets in files; use 1Password (`op run`) or Coolify secrets.
- Destructive operations (drop, force-push, rm -rf on servers) without a fresh backup and a run-log line.
- Silent scope changes: if the PRD is wrong, say so in the issue, then build.
