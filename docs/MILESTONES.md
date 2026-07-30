# Milestones — migration-lab

Empfohlene Reihenfolge: **nach einvoice-at starten** (oder frühestens nach dessen M3, wenn Parallelität nötig). Geschätzter Gesamtaufwand: ~5 Wochen fokussiert.

Regel wie überall: Ein Milestone = demonstrierbares Inkrement + Definition of Done (ENGINEERING_STANDARDS.md) + grüne CI. Zusätzlich hier: **jede Etappe endet mit Git-Tag + Playbook-Kapitel.**

---

## G0 — Kickoff-Gate + Skeleton (½–1 Tag)
Entscheidung dokumentieren (ADR-0001): reale abandonnierte OSS-App gefunden (Lizenz prüfen!) oder synthetisches WerkstattCRM (Default). Monorepo-Skeleton, Workflows-Gerüst, README, LICENSE, `stages.md`.
**Abnahme:** ADR-0001 + leeres, grünes CI-Gerüst.

## G1 — Legacy-App (Stage 0) (3–4 Tage)
WerkstattCRM als 2016er-Zustand bauen (Java 8, Boot 1.5, AngularJS 1.8, Postgres, JSP-Adminseite), Legacy-Muster katalogisiert in `LEGACY_NOTES.md`, Docker Compose, Seed-Daten. Tag `stage-0-legacy`.
**Abnahme:** App läuft per Compose; LEGACY_NOTES vollständig; bewusst KEINE Tests (das ist der Punkt).

## G2 — Sicherheitsnetz (Stage 1) (4–5 Tage) — der wichtigste Milestone
Selenium-Suite mit Page Objects + Selektor-Abstraktion (läuft gegen Legacy), Charakterisierungs-/Approval-Tests (API + DB-Zustände), CI-Gates aktiv (legacy + e2e müssen grün sein, für immer). Playbook-Kapitel 1 ("Ohne Netz keine Migration"). Tag `stage-1-safety-net`.
**Abnahme:** E2E stabil (3 aufeinanderfolgende grüne Läufe), Flaky-Strategie dokumentiert.

## G3 — Build/JDK + Boot 2.7 (Stages 2–3) (4–5 Tage)
Dependency-Audit, JDK-Hebung, Boot 1.5→2.7 mit dokumentierten Brüchen; Sicherheitsnetz durchgehend grün. Playbook-Kapitel 2–3, Tags `stage-2-jdk-build`, `stage-3-boot-2.7`.
**Abnahme:** Identische Selenium-Suite grün; Aufwände ehrlich geloggt.

## G4 — Boot 3.x→4.1 + Java 25 (Stage 4) (4–5 Tage)
jakarta-Umstieg, Security-Rewrite, Constructor Injection, OpenRewrite-Einsatz inkl. Bewertung (was fingen die Rezepte, was nicht). Playbook-Kapitel 4, ADRs für echte Entscheidungen, Tag `stage-4-boot-4x`.
**Abnahme:** Modern-Backend auf Boot 4.1/Java 25, E2E grün, OpenRewrite-Bilanz im Playbook.

## G5 — AngularJS→Angular 22 (Stage 5) (5–6 Tage)
Strangler Fig: Angular-Shell, Route-für-Route-Portierung, Hybrid-Phase dokumentiert, JSP-Adminseite absorbiert, Selektor-Map v2 — **dieselben E2E-Szenarien grün auf alter UND neuer UI** (das Headline-Ergebnis). Playbook-Kapitel 5, Tag `stage-5-angular`.
**Abnahme:** E2E-Matrix (legacy|modern) beidseitig grün; funktionale Äquivalenz belegt.

## G6 — KI-Testgenerierung, gemessen (3–4 Tage)
`PROTOCOL.md` VOR Durchführung festschreiben; Generierung (2 Modelle), Auswertung JaCoCo + PIT-Mutation-Score, Nachbesserungsaufwand geloggt, `REPORT.md` (DE-Zusammenfassung + EN-Detail) inkl. Kosten. Fehlschläge bleiben im Repo.
**Abnahme:** Report vollständig, reproduzierbar (Harness + Prompts versioniert), keine Schönfärbung.

## G7 — Cloud/Betrieb + Launch (Stage 6) (2–3 Tage)
OTel, Health, Deployment beider Stände (Hetzner+Dokploy, side-by-side Demo), Playbook-Schlusskapitel + PDF-Export in CI, README final mit Stages-Tabelle und Badges, Release v1.0.0. Tag `stage-6-cloud-ops`.
**Abnahme:** Beide Demos live; Playbook-PDF als CI-Artefakt; Quickstart < 5 min je Stand.

---

### Prinzipien für Claude Code
1. **Das Sicherheitsnetz (G2) ist heilig:** Ab `stage-1-safety-net` darf kein Commit die Legacy-E2E-Suite brechen. Rot = stoppen und reparieren, nie "später fixen".
2. Refactoring nur mit Migrationszweck — Schönheitsreparaturen ohne Not sind Scope Creep (Playbook-Regel, im Repo vorleben).
3. Aufwands-Logging je Session in `docs/worklog.md` — die ehrlichen Stunden sind Teil des Produkts.
4. G6 strikt nach Protokoll — Ergebnisse werden nicht nachträglich kuratiert.
