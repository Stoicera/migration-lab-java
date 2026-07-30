# migration-lab

[![legacy-ci](https://github.com/Stoicera/migration-lab-java/actions/workflows/legacy-ci.yml/badge.svg)](https://github.com/Stoicera/migration-lab-java/actions/workflows/legacy-ci.yml)
[![modern-ci](https://github.com/Stoicera/migration-lab-java/actions/workflows/modern-ci.yml/badge.svg)](https://github.com/Stoicera/migration-lab-java/actions/workflows/modern-ci.yml)
[![e2e](https://github.com/Stoicera/migration-lab-java/actions/workflows/e2e.yml/badge.svg)](https://github.com/Stoicera/migration-lab-java/actions/workflows/e2e.yml)

**A public, reproducible legacy modernization:**
Java 8 / Spring Boot 1.5 / AngularJS 1.8 → Java 25 / Spring Boot 4.1 / Angular 20 —
safety net first, honest numbers, reusable German migration playbook.

> **Status: G1 done — `stage-0-legacy` is runnable.** Next: G2, the safety net.
> Follow progress in [`stages.md`](stages.md) and [`docs/worklog.md`](docs/worklog.md).

## Why this exists

Companies and institutes sit on Java-8/Spring-Boot-1.x/AngularJS applications
(AngularJS EOL since January 2022, Spring Boot 1.x EOL since 2019). Migrations get
postponed because legacy systems have no tests, the risk feels incalculable, and
vendors demand blind trust. This repository shows — publicly, step by step, with
real effort numbers — how a senior team de-risks exactly this migration:

1. **Safety net before anything else:** a Selenium E2E suite and characterization
   tests define functional equivalence *before* the first migration commit — and
   must stay green through every stage.
2. **Reproducible stages:** every stage is a git tag; checkout → `docker compose up`
   → working application. See [`stages.md`](stages.md).
3. **Measured AI-assisted test generation:** LLM-generated unit tests for legacy
   code, evaluated with JaCoCo coverage **and PIT mutation scores** under a
   pre-registered protocol. Failures stay in the repo.
4. **A German migration playbook** ([`playbook/`](playbook/)) with honest effort
   figures and decision rules, reusable for real projects.

## Repository layout

| Directory | Content |
|-----------|---------|
| [`legacy/`](legacy/) | WerkstattCRM as found (2016-era, deliberately untested) — the exhibit |
| [`modern/`](modern/) | The migrated application, growing stage by stage |
| [`e2e/`](e2e/) | Selenium 4 suite — same scenarios vs. both UIs via selector maps |
| [`characterization/`](characterization/) | Golden-master tests = the definition of functional equivalence |
| [`ai-testgen/`](ai-testgen/) | Pre-registered AI test-generation experiment + PIT reports |
| [`playbook/`](playbook/) | German playbook, one chapter per stage |
| [`docs/`](docs/) | PRD, SPEC, milestones, ADRs, worklog, glossary |

## Honest limits

- The legacy application is **synthetic but pattern-faithful** — built from real
  legacy smells, transparently catalogued (`legacy/LEGACY_NOTES.md`), because no
  suitable genuinely abandoned, permissively licensed OSS application exists
  (research documented in [ADR-0001](docs/adr/0001-synthetic-legacy-application.md)).
- One small domain: numbers do not scale linearly to 500k-LOC systems; the playbook
  explains the scaling deltas.
- AI results are model- and date-specific; the report pins models and dates.

## Quickstart

```bash
docker compose -f legacy/docker-compose.yml up -d
# SPA: http://localhost:8080 · JSP admin: http://localhost:8080/admin
```

---

## Deutsche Kurzfassung

**migration-lab** ist eine öffentlich nachvollziehbare Legacy-Modernisierung:
Java 8 / Spring Boot 1.5 / AngularJS → Java 25 / Spring Boot 4.1 / Angular 20.

Der professionell entscheidende Schritt kommt zuerst: ein **Sicherheitsnetz** aus
Selenium-E2E-Suite und Charakterisierungs-Tests, das während der gesamten Migration
grün bleiben muss. Jede Etappe ist ein auscheckbarer Git-Tag mit lauffähigem
Docker-Compose-Stand. Aufwände werden ehrlich in Stunden geloggt
([`docs/worklog.md`](docs/worklog.md)), Sackgassen dokumentiert statt gelöscht, und
das KI-Testgenerierungs-Experiment folgt einem **vorab festgeschriebenen Protokoll**
mit Mutation-Testing-Auswertung (PIT).

Das Ergebnis für Entscheider: ein **deutschsprachiges Migrations-Playbook**
([`playbook/`](playbook/)) mit Vorgehen, Stolperfallen, ehrlichen Aufwänden und
Entscheidungsregeln — wiederverwendbar für reale Modernisierungsprojekte.

Ein Projekt von [Stoicera Software Group](https://stoicera.com) ·
[Lugmayr-Kern](https://lugmayrkern.at), Oberösterreich.

## License

[Apache-2.0](LICENSE)
