# migration-lab

[![legacy-ci](https://github.com/Stoicera/migration-lab-java/actions/workflows/legacy-ci.yml/badge.svg)](https://github.com/Stoicera/migration-lab-java/actions/workflows/legacy-ci.yml)
[![modern-ci](https://github.com/Stoicera/migration-lab-java/actions/workflows/modern-ci.yml/badge.svg)](https://github.com/Stoicera/migration-lab-java/actions/workflows/modern-ci.yml)
[![e2e](https://github.com/Stoicera/migration-lab-java/actions/workflows/e2e.yml/badge.svg)](https://github.com/Stoicera/migration-lab-java/actions/workflows/e2e.yml)

**A public, reproducible legacy modernization:**
Java 8 / Spring Boot 1.5 / AngularJS 1.8 → Java 25 / Spring Boot 4.1 / Angular 22 —
safety net first, honest numbers, reusable German migration playbook.

> **Status: stage 5 done — the modern stand runs Spring Boot 4.1.0 / Java 25
> with an Angular 22 UI**, migrated route by route via Strangler Fig, the same
> Selenium scenarios green on the old AND the new UI (per-slice on modern,
> full matrix at the gates; cadence in the worklog) —
> functionally equivalent to the frozen 2016 stand for all legitimate inputs
> (the deliberate divergences — security fix, absorbed admin page, fixed
> "undefined" alert — are registered and pinned per stand in
> [ADR-0004](docs/adr/0004-functional-equivalence-and-sanctioned-divergence.md)).
> Next: measured AI test generation (G6) — protocol **frozen before anything ran**
> (tag `ai-testgen-protocol-v1`); 24 generation calls executed 2026-07-31 for €0.65,
> Phase A measured in [`ai-testgen/REPORT.md`](ai-testgen/REPORT.md). Phase B (repair
> effort) follows.
> Progress: [`stages.md`](stages.md) · [`docs/worklog.md`](docs/worklog.md).

## Why this exists

Companies and institutes sit on Java-8/Spring-Boot-1.x/AngularJS applications
(AngularJS EOL since January 2022, Spring Boot 1.x EOL since 2019). Migrations get
postponed because legacy systems have no tests, the risk feels incalculable, and
vendors demand blind trust. This repository shows — publicly, step by step, with
measured effort numbers — how such a migration is de-risked:

1. **Safety net before anything else:** a Selenium E2E suite and characterization
   tests define functional equivalence *before* the first migration commit — and
   must stay green through every stage.
2. **Reproducible stages:** every stage is a git tag; checkout → `docker compose up`
   → working application. See [`stages.md`](stages.md).
3. **Measured AI-assisted test generation (G6, in progress):** LLM-generated unit tests
   for the same six classes **twice** — once as 2016 legacy, once as their migrated
   counterparts — evaluated with JaCoCo coverage **and PIT mutation scores** under a
   protocol that was frozen before the first API call
   ([`ai-testgen/PROTOCOL.md`](ai-testgen/PROTOCOL.md), tag `ai-testgen-protocol-v1`).
   That second corpus turns the experiment into a migration statement: *does
   modernizing pay off in testability, measurably?* Failures stay in the repo.
4. **A German migration playbook** ([`playbook/`](playbook/)) with honest effort
   figures and decision rules, reusable for real projects.

## How this was built — read this before reusing any number

**The execution was AI-assisted: a Claude Code agent performed the work, directed
and reviewed by the owner.** This is disclosed here, at the front door, because it
changes how the numbers transfer:

- **The logged hours are agent wall-clock time under supervision** — the five
  backend stages took ~4 wall-clock hours on 2026-07-30, against a human-team
  plan estimate of ~5 focused weeks ([`docs/MILESTONES.md`](docs/MILESTONES.md)).
  Do **not** price a human migration from these hours; price the *method* (stage
  order, safety-net-first, break catalogue) and see the playbook's separately
  labelled experience-based estimates.
- **What does transfer:** the migration path, the breaks the net caught and how,
  the decision rules, the tooling evaluations (e.g. OpenRewrite's catch/miss
  list) — those are properties of the stacks, not of who typed.
- **Review model:** solo maintainer; "owner reviewed" means author-is-reviewer,
  hardened by commissioned adversarial reviews whose findings are public
  (worklog session 7) and were remediated in the open
  ([ADR-0008](docs/adr/0008-ci-enforcement-and-solo-review.md)).
- The app is small on purpose: **~1.7k LOC backend, 25 REST endpoints, 10 views**
  — big enough to exhibit real breaks, small enough to stay fully honest.
  Scaling caveats are in every playbook chapter.

## Repository layout

| Directory | Content |
|-----------|---------|
| [`legacy/`](legacy/) | WerkstattCRM as found (2016-era, deliberately untested) — the exhibit |
| [`modern/`](modern/) | The migrated application, growing stage by stage |
| [`e2e/`](e2e/) | Selenium 4 suite — same scenarios vs. both UIs via selector maps |
| [`characterization/`](characterization/) | Golden-master tests = the definition of functional equivalence |
| [`ai-testgen/`](ai-testgen/) | Pre-registered AI test-generation experiment (G6; protocol frozen, harness + testbeds validated) |
| [`playbook/`](playbook/) | German playbook, one chapter per stage |
| [`docs/`](docs/) | PRD, SPEC, milestones, ADRs, worklog, deviations ledger, glossary |

## Honest limits

- The legacy application is **synthetic but pattern-faithful** — built from real
  legacy smells, transparently catalogued (`legacy/LEGACY_NOTES.md`), because no
  suitable genuinely abandoned, permissively licensed OSS application exists
  (research documented in [ADR-0001](docs/adr/0001-synthetic-legacy-application.md)).
  The catalogue includes deliberate security warts — among them a flagged
  SQL-injection-shaped search (B4), fixed in the modern stand in stage 4 and told
  as the playbook's security story.
- **Small scale, disclosed exactly:** ~1.7k LOC backend / 25 endpoints / 10 views.
  Numbers do not scale linearly to 500k-LOC systems; each playbook chapter states
  what does and does not generalize.
- Effort figures are AI-agent wall-clock time (see *How this was built*); the
  playbook labels measured values and experience-based estimates separately.
- AI-experiment results will be model- and date-specific; the protocol pins both.
- Known standards deviations are ledgered in [`docs/DEVIATIONS.md`](docs/DEVIATIONS.md)
  — nothing is silently waived.

## Quickstart

Needs only Docker with the Compose plugin — the applications build inside Docker.

```bash
docker compose -f legacy/docker-compose.yml up -d --wait
# SPA: http://localhost:8080 · JSP admin: http://localhost:8080/admin
docker compose -f modern/docker-compose.yml up -d --wait
# modern stand: http://localhost:8090
```

`--wait` blocks until the healthchecks pass. Without it the containers are merely *running* —
PostgreSQL is not yet accepting connections, and the next test run fails on timing.

Everything else — prerequisites per module, running the suites, resetting the database,
troubleshooting — is in [`docs/deployment.md`](docs/deployment.md). The by-hand steps are
checklisted in [`docs/MANUAL_TASKS.md`](docs/MANUAL_TASKS.md).

---

## Deutsche Kurzfassung

**migration-lab** ist eine öffentlich nachvollziehbare Legacy-Modernisierung:
Java 8 / Spring Boot 1.5 / AngularJS → Java 25 / Spring Boot 4.1 / Angular 22.

Der professionell entscheidende Schritt kommt zuerst: ein **Sicherheitsnetz** aus
Selenium-E2E-Suite und Charakterisierungs-Tests, das während der gesamten Migration
grün bleiben muss. Jede Etappe ist ein auscheckbarer Git-Tag mit lauffähigem
Docker-Compose-Stand. Sackgassen werden dokumentiert statt gelöscht, und das
KI-Testgenerierungs-Experiment folgt einem **vorab festgeschriebenen Protokoll**
mit Mutation-Testing-Auswertung (PIT).

**Transparenz zur Entstehung:** Die Umsetzung erfolgte KI-gestützt — ein
Claude-Code-Agent hat unter Anleitung und Review des Inhabers gearbeitet. Die
geloggten Stunden ([`docs/worklog.md`](docs/worklog.md)) sind **Agent-Wall-Time
unter Aufsicht**, keine Personentage eines Teams; was auf reale Projekte übertragbar
ist (Methode, Stolperfallen, Entscheidungsregeln) und was nicht (die Stundenzahlen),
steht oben unter *How this was built* und in jedem Playbook-Kapitel.

Das Ergebnis für Entscheider: ein **deutschsprachiges Migrations-Playbook**
([`playbook/`](playbook/)) mit Vorgehen, Stolperfallen, transparent gekennzeichneten
Aufwänden und Entscheidungsregeln — wiederverwendbar für reale
Modernisierungsprojekte.

Ein Projekt von [Stoicera Software Group](https://stoicera.com) ·
[Lugmayr-Kern](https://lugmayrkern.at), Oberösterreich.

## License

[Apache-2.0](LICENSE)
