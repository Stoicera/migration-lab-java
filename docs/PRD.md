# PRD — migration-lab (Arbeitstitel)

**Eine öffentliche, nachvollziehbare Legacy-Modernisierung: Java 8 / Spring Boot 1.5 / AngularJS → Java 25 / Spring Boot 4 / Angular 22 — mit Charakterisierungs-Tests, Selenium, KI-gestützter Testgenerierung und einem wiederverwendbaren Migrations-Playbook.**

Version 1.0 · 23.07.2026 · Owner: Sebastian Kern · Umsetzung: Claude Code, milestone-basiert

---

## 1. Warum dieses Projekt (Selektion)

Kandidaten aus dem Brainstorming waren: NIS2-Microtools, Barrierefreiheits-Tool (Barri), Google-Ads-Plattform, EEG Connect, Applied-AI-Plattform, Stoicera Forge, BookHit-Orchestrator. Auswahlkriterien: **größte Nachfrage + größter Nutzen für österreichische KMU und Unis**, direkter Werkvertrags-Bezug, keine Überschneidung mit Bestehendem.

**Entscheidung: Legacy-Modernisierung als öffentliches Referenzprojekt.** Begründung:

1. Das Brainstorming selbst stellt fest: *"Die meisten Unternehmen wollen entweder veraltete Software modernisieren ODER in die Cloud wechseln."* Modernisierung ist der größte adressierbare Werkvertrags-Markt — jede Firma und jede Uni hat Legacy.
2. Die **JKU-Ausschreibung (Plösch) ist wörtlich eine Angular/Spring-Boot-Migration + Selenium-Testautomatisierung.** Kein anderes Projekt beweist die beworbene Leistung so direkt. Zusätzlich deckt es sich mit Plöschs Forschung (KI-gestützte Migration, LLMs für Testgenerierung) — wir liefern ein empirisches Anschauungsobjekt, keine Behauptung.
3. Regionale Ausschreibungen nennen ausdrücklich Wartung, Weiterentwicklung, bestehende Unternehmenssoftware — genau das.
4. Keine Kollisionen: Barri ist laut stoicera.com bereits extern in Entwicklung; EEG ist mit der Clearing Engine abgedeckt; Ads/Applied-AI sind Produktwetten, keine Vertrauens-Anker.
5. Es ist das perfekte Komplement zu einvoice-at: dort *Neubau nach Standards*, hier *Modernisierung von Bestand* — zusammen decken die beiden Repos die zwei Kaufmotive des Zielmarkts vollständig ab.

## 2. Problem

Unternehmen und Institute sitzen auf Java-8/Spring-Boot-1.x/AngularJS-Anwendungen (AngularJS: EOL seit Jänner 2022; Spring Boot 1.x: EOL seit 2019). Migration wird verschoben, weil (a) keine Tests existieren, (b) das Risiko unkalkulierbar wirkt, (c) Anbieter Vertrauensvorschuss verlangen. Es gibt kaum öffentlich nachvollziehbare, ehrliche End-to-End-Migrationsbeispiele mit Zahlen.

## 3. Lösung

Ein Repo (Monorepo mit zwei Application-Ständen + Playbook), das eine realistische Legacy-Anwendung **öffentlich und schrittweise** modernisiert:

- **`legacy/`** — "WerkstattCRM": eine bewusst realistische Alt-Anwendung (Kundenverwaltung/Auftragsannahme einer Autowerkstatt — KMU-nah): Java 8, Spring Boot 1.5, AngularJS 1.x, Field Injection, God-Classes, keine Tests, SQL-Strings — dokumentiert als synthetisch, aber nach echten Legacy-Mustern gebaut. *(Entscheidungs-Gate beim Kickoff: Falls sich ein geeignetes, real abandonnenes Open-Source-Projekt mit passender Lizenz findet, wird stattdessen dieses migriert — höhere Glaubwürdigkeit; sonst synthetisch mit transparenter Kennzeichnung.)*
- **Migrationsstrecke in nachvollziehbaren Etappen** (je Etappe ein Git-Tag + Playbook-Kapitel): Sicherheitsnetz → Build/JDK → Spring Boot 1.5→2.7→3.x→4.x → AngularJS→Angular 22 (Strangler-Fig, komponentenweise) → Betrieb/Cloud (Docker, CI/CD, OTel, Hetzner).
- **Sicherheitsnetz zuerst:** Charakterisierungs-Tests (Approval-Tests der bestehenden Läufe), **Selenium-E2E-Suite** auf der Legacy-UI, die während der gesamten Migration grün bleiben muss — das ist die Kernbotschaft an Auftraggeber.
- **KI-gestützte Testgenerierung, empirisch gemessen:** LLM-generierte Unit-Tests für untestbare Legacy-Klassen; Qualität bewertet mit Coverage **und Mutation Testing (PIT)** — ehrliche Zahlen (wie viele generierte Tests töten Mutanten? was musste ein Mensch nachbessern?). Genau Plöschs empirischer Stil: "wo hilft KI wirklich, wo nicht".
- **Migrations-Playbook** (`playbook/`): je Etappe Vorgehen, Stolperfallen, Aufwand, Entscheidungslogik — das wiederverwendbare Sales-Asset für jedes Kundengespräch.

## 4. Zielgruppen

- **IT-Leiter / Geschäftsführer österreichischer KMU** mit Altsystem: liest Playbook + Zahlen, versteht Risiko-Beherrschung.
- **Universitäten/Institute (JKU!):** sieht exakt die ausgeschriebene Leistung öffentlich vorgeführt, inkl. Forschungsanschluss (LLM-Testgenerierung mit Mutation-Score).
- **Entwickler bei Kunden/Vermittlern:** prüft Commits, Tags, Testsuiten.

## 5. Scope

**In Scope (MVP):** Legacy-App lauffähig (Docker) · Selenium-Suite + Charakterisierungs-Tests · Migrationsetappen bis Spring Boot 4 + Angular 22 · KI-Testgen-Experiment mit PIT-Auswertung und Kurzbericht · Playbook · CI mit parallelen Pipelines (legacy bleibt grün, modern wächst) · Deployment beider Stände als Demo.

**Out of Scope (dokumentiert):** Datenbank-Wechsel (bleibt PostgreSQL; Oracle→Postgres nur als Playbook-Exkurs) · Microservice-Zerlegung (bewusst: Modular bleiben ist meist die richtige Antwort — ADR) · Betrieb der Legacy-Version über Projektende hinaus.

## 6. User Stories (Auszug)

1. Als Interessent öffne ich das Playbook und sehe je Etappe: Ausgangslage, Schritte, Ergebnis, Aufwand ehrlich in Stunden.
2. Als Entwickler checke ich Tag `stage-0-legacy` aus und starte die Alt-App in Docker; Tag `stage-5-angular` zeigt denselben Funktionsumfang im neuen Stack, `stage-6-cloud-ops` den Endzustand inklusive Betrieb — dieselbe Selenium-Suite läuft gegen Alt- und Neu-Stand. *(Der bei Abfassung geplante Tag-Name `stage-6-modern` wurde nie vergeben; die tatsächliche Tag-Liste steht in [`../stages.md`](../stages.md).)*
3. Als Prüfer lese ich den KI-Testgen-Bericht: Anzahl generierter Tests, Mutation Score vor/nach menschlicher Nachbesserung, Kostenaufstellung.
4. Als JKU-Gesprächspartner sehe ich, dass die Migrationsstrecke der Ausschreibung (Angular/Spring Boot + Selenium) bereits einmal vollständig öffentlich durchlaufen wurde.

## 7. Nicht-funktionale Anforderungen

- Jede Etappe ist ein auschecke- und startbarer Zustand (Tag + Docker Compose) — Reproduzierbarkeit ist das Produkt.
- Selenium-Suite: < 10 min Laufzeit, stabil (keine Flaky-Toleranz; Warte-Strategien dokumentiert).
- Playbook auf Deutsch (Zielgruppe Entscheider), technische Doku Englisch.
- Ehrlichkeit als NFR: Aufwände, Sackgassen und KI-Fehlschläge werden dokumentiert, nicht geglättet.

## 8. Erfolgs-Metriken

- Selenium-Suite grün auf Legacy- UND Modern-Stand (funktionale Äquivalenz belegt).
- Mutation-Testing-Bericht veröffentlicht (das differenzierende Artefakt).
- Playbook als eigenständiges PDF/Website-Kapitel exportierbar; ≥ 1 Gespräch mit Verweis darauf innerhalb von 3 Monaten.

## 9. Offene Punkte an den Owner

> **Stand bei Abfassung — die folgenden Punkte bleiben als historischer Beleg stehen und werden nicht gelöscht.** Inzwischen erledigt: Das Kickoff-Gate ist am 30.07.2026 zugunsten des synthetischen WerkstattCRM entschieden ([ADR-0001](adr/0001-synthetic-legacy-application.md)); der Name `migration-lab` ist mit dem Release v1.0.0 vom 14.08.2026 fixiert; die Reihenfolgefrage zu einvoice-at hat sich mit dem vollständig durchlaufenen Projekt (G0–G7, Tag `stage-6-cloud-ops`, siehe [Kapitel 8](../playbook/08-der-live-gang-und-was-das-heisst.md)) erledigt. Aus dieser Liste offen bleibt allein die Serie auf austrianbusiness.at (siehe [VERMARKTUNG.md](VERMARKTUNG.md)).

- Name (Arbeitstitel migration-lab) & ob das Playbook zusätzlich auf austrianbusiness.at als Serie erscheint.
- Kickoff-Gate: reale abandonnierte OSS-App vs. synthetisches WerkstattCRM (Default: synthetisch, transparent gekennzeichnet).
- Reihenfolge zu einvoice-at: parallel (nicht empfohlen) oder danach (empfohlen).
