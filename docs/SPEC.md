# Technical Specification — migration-lab

A public, reproducible legacy modernization: Java 8 / Spring Boot 1.5 / AngularJS → Java 25 / Spring Boot 4 / Angular 20, safety net first, with measured AI-assisted test generation and a reusable migration playbook.

Status: v1.0 · 2026-07-23 · Repo language: English; playbook German (decision-maker audience)

---

## 1. Repository layout (monorepo)

```
migration-lab/
├── legacy/                  # WerkstattCRM as found: Java 8, Spring Boot 1.5.x, AngularJS 1.8, Maven, Postgres
│   └── (intentionally realistic warts: field injection, God services, SQL strings, no tests, mixed concerns)
├── modern/                  # grows stage by stage; final: Java 25, Spring Boot 4.1, Angular 20
├── e2e/                     # Selenium WebDriver suite — runs against BOTH apps via config (base URL + selector map)
├── characterization/        # approval/golden-master tests captured from legacy behaviour (API + DB state)
├── ai-testgen/              # experiment harness: prompts, generated tests, PIT mutation reports, evaluation notebook/report
├── playbook/                # German markdown chapters, one per stage; exportable
├── docs/                    # PRD, SPEC, MILESTONES, ADRs, glossary, worklog
└── .github/workflows/       # legacy-ci.yml (must stay green), modern-ci.yml, e2e.yml (matrix: legacy|modern)
```

**Git tags are first-class deliverables:** `stage-0-legacy`, `stage-1-safety-net`, `stage-2-jdk-build`, `stage-3-boot-2.7`, `stage-4-boot-3x-4x`, `stage-5-angular`, `stage-6-cloud-ops`. Every tag: checkout → `docker compose up` → working app. A `stages.md` table maps tag → state → playbook chapter.

## 2. The legacy application (stage 0)

Domain "WerkstattCRM" (car-workshop CRM, KMU-realistic): customers, vehicles, repair orders (status flow), invoicing light, a monthly report page. Small enough to migrate honestly (~4–6k LOC backend, ~15 AngularJS controllers/views), large enough to exhibit real problems.

Deliberate legacy patterns (each catalogued in `legacy/LEGACY_NOTES.md` with the real-world smell it represents): Spring Boot 1.5 + Java 8 + `javax.*`, field injection everywhere, one `WerkstattService` God class, JdbcTemplate string concatenation (incl. one intentional SQL-injection-shaped smell — flagged, fixed in migration, told as a security story), AngularJS 1.8 with `$scope` soup, server-rendered JSP admin page (mixed UI tech — very real), no tests, properties files with duplicated config, Log4j 1.x-style logging via bridge.

**Honesty rule:** README and playbook state clearly the legacy app is synthetic but pattern-faithful (unless the kickoff gate selects a real abandoned OSS project — then licence + provenance documented instead).

## 3. Safety net (stage 1 — the professionally decisive stage)

- **Selenium suite** (`e2e/`): Java 25 + JUnit 5 + Selenium 4, Page-Object pattern, selector abstraction layer so the same scenarios run against AngularJS UI and later Angular UI (per-app selector maps). Scenarios: customer CRUD, order lifecycle, invoice creation, report values. Explicit waits only; zero flaky tolerance (retry-analyse-fix, documented).
- **Characterization tests:** golden-master captures of legacy REST/JSP responses and DB state transitions (ApprovalTests style) — these define "functional equivalence".
- CI: `legacy-ci` + `e2e (legacy)` must be green before any migration commit. This gate never lifts.

## 4. Migration stages (each = tag + playbook chapter + ADR where a real decision was made)

- **Stage 2 — Build & JDK:** Maven hygiene, dependency audit (OWASP), Java 8→17/21 compile fixes under Boot-compatible ceiling, logging unification.
- **Stage 3 — Boot 1.5→2.7:** the documented long jump (config property migration, actuator changes, `WebSecurityConfigurerAdapter` era), refactor God class only as far as migration requires (playbook: "migrate first, refactor with purpose").
- **Stage 4 — Boot 2.7→3.x→4.1 + Java 25:** `javax`→`jakarta`, Spring Security rewrite, constructor injection sweep, records/pattern-matching where it pays, OpenRewrite recipes used **and evaluated** (what the recipes caught vs. missed — playbook data).
- **Stage 5 — AngularJS→Angular 20:** Strangler Fig: Angular shell + route-by-route port, hybrid period documented, selector map v2; JSP admin page absorbed. E2E stays green throughout — that's the headline.
- **Stage 6 — Cloud & ops:** Docker multi-stage, OTel, health checks, GitHub Actions deploy to Hetzner+Dokploy, both stands (legacy demo + modern) deployed side by side for the demo effect.

## 5. AI-assisted test generation experiment (`ai-testgen/`) — the research-grade artefact

Method (pre-registered in `ai-testgen/PROTOCOL.md` before running — Plösch-style empirical honesty):
1. Select N legacy classes (stratified: God service, mappers, controllers).
2. Generate unit tests via `LlmClient` (OpenRouter; 2 models compared, e.g. Claude Sonnet vs. one open-weight model; prompts versioned).
3. Evaluate: compile rate → pass-against-legacy rate → line/branch coverage (JaCoCo) → **mutation score (PIT)** → human-fix effort (minutes, logged).
4. Report `ai-testgen/REPORT.md` (DE summary + EN full): tables, costs in tokens/EUR, where AI helped, where it failed. No cherry-picking; failed generations stay in the repo.

## 6. CI/CD

Three workflows: legacy (build+characterization), modern (build+unit+integration with Testcontainers), e2e matrix (legacy|modern, nightly + on main). Deployment job (modern) → GHCR → Dokploy. Badges per workflow in README. Same Engineering Standards as all Labs repos (formatting, security scans, conventional commits).

## 7. Playbook (`playbook/`, German)

Chapters mirror stages: Ausgangslage → Vorgehen → Stolperfallen → Aufwand (ehrliche Stunden) → Entscheidungsregeln ("Wann 1.5→2.7 direkt, wann Zwischenschritte?"). Final chapter: "Was das für Ihr Projekt heißt" (the sales bridge). Exportable as PDF via pandoc in CI (artifact) — usable as Direktvergabe attachment.

## 8. Honest limits (README)

Synthetic legacy (if gate says so) — patterns real, history not · single small domain, numbers don't linearly scale to 500k-LOC systems (playbook explains the scaling deltas) · AI results are model- and date-specific (report pins models + dates) · no DB engine migration in scope.
