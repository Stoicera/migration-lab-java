# playbook/ — Migrations-Playbook (Deutsch)

Das wiederverwendbare Kern-Artefakt für Entscheider: je Migrationsetappe ein Kapitel
mit **Ausgangslage → Vorgehen → Stolperfallen → Aufwand (ehrliche Stunden) →
Entscheidungsregeln**. Schlusskapitel: „Was das für Ihr Projekt heißt."

Regeln:

- Sprache: Deutsch (Zielgruppe: IT-Leitung und Geschäftsführung österreichischer KMU,
  Universitäten, öffentliche Hand).
- Kapitel entstehen **milestone-begleitend**, nicht am Ende (Rohmaterial:
  `docs/worklog.md`).
- Aufwände, Sackgassen und Fehlschläge werden dokumentiert, nicht geglättet.
- PDF-Export via pandoc: `playbook/build-pdf.sh` (läuft im Container, damit derselbe
  Befehl lokal und in CI funktioniert — GitHubs Runner haben pandoc, aber keine
  TeX-Engine). Der Workflow `playbook` legt das PDF als Artefakt ab; er ist bewusst
  **kein Pflicht-Check**, weil ein fehlgeschlagener PDF-Bau keine Korrektur an der
  Anwendung blockieren darf.

- Aufwands-Etikettierung (verbindlich seit Review Session 7): **Messwerte** sind
  Agent-Wall-Time unter Aufsicht (Offenlegung: README „How this was built");
  **Feldwerte/Personentage** sind Erfahrungsschätzungen und werden immer als
  solche gekennzeichnet. Die beiden werden nie vermischt.

## Kapitel

| Kapitel | Inhalt | Stand |
|---|---|---|
| 1 — Ohne Netz keine Migration | Sicherheitsnetz vor der ersten Migrationszeile | fertig (Etappe 1) |
| 2 — Fundament: Build & JDK | Abhängigkeits-Audit, Build, JDK | fertig (Etappe 2) |
| 3 — Der weite Sprung | Spring Boot 1.5 → 2.7 | fertig (Etappe 3) |
| 4 — Boot 3/4, Java 25 & OpenRewrite | jakarta-Umstieg, OpenRewrite bewertet | fertig (Etappe 4) |
| 5 — AngularJS → Angular 22 | Strangler Fig, dieselben E2E-Szenarien auf beiden UIs | fertig (Etappe 5) |
| 6 — KI-Testgenerierung, gemessen | Präregistriertes Experiment, beide Phasen | **abgeschlossen 2026-08-02** (kein Etappen-Kapitel: G6 war ein Milestone ohne Etappe) |
| 7 — Betrieb & Härtung | Datenbank-Hebung, Health, Beobachtbarkeit, Härtung am Rand, Lasttest | **neu 2026-08-05**; Etappe 6 war damit noch **nicht** abgeschlossen — eingelöst am 14.08.2026, siehe Kapitel 8 |
| 8 — Der Live-Gang, und was das für Ihr Projekt heißt | Deployment auf die Stoicera-Flotte (ausgeführt, mit Evidenz), Backups mit Rückspielprobe, die Regeln aus acht Kapiteln | **abgeschlossen 2026-08-14** — mit dem tatsächlichen Deployment, wie angekündigt |

**Warum die Nummerierung ab hier von den Etappen abweicht:** Kapitel und Etappe waren bis
G5 dasselbe. Mit Kapitel 6 ist das gebrochen, weil G6 ein Milestone ohne Etappe war. Das
Betriebskapitel ist deshalb Kapitel **7**, nicht Kapitel 6, obwohl es zu Etappe 6 gehört.
Nachträglich umnummeriert wird nicht — eine Dokumentation, die ihre eigene Historie still
korrigiert, ist weniger wert als eine mit einer sichtbaren Naht.

Status (Stand 14.08.2026): **Kapitel 1–8 veröffentlicht, Etappe 6 abgeschlossen.** Beide
Stände laufen auf der Stoicera-Flotte — <https://migration-lab.stoicera.cyou> (moderner
Stand, öffentlich; `/admin`, `/api/admin` und `/actuator` hinter Basic-Auth) und
<https://migration-lab-legacy.stoicera.cyou> (das Exponat, vollständig hinter Basic-Auth)
—, mit Let's-Encrypt-Zertifikaten, nächtlichen Datenbank-Sicherungen und einer tatsächlich
durchgeführten Rückspielprobe. Tag `stage-6-cloud-ops` und Release v1.0.0 existieren, die
Entscheidungen stehen in ADR-0016, den Vollzug berichtet Kapitel 8. **Offen bleibt bewusst**
die Auslagerung der Sicherungen an einen zweiten Standort: Es gibt sie noch nicht.

*An dieser Stelle stand bis zum 14.08.2026 — geschrieben am 05.08.2026 und bis dahin
zutreffend:* „Kapitel 1–7 veröffentlicht. Etappe 6 ist begonnen, aber nicht fertig: Betrieb
und Härtung sind gebaut und gemessen, das Deployment selbst gibt es nicht — kein Server,
kein TLS, keine Sicherungen, folglich auch kein Tag `stage-6-cloud-ops` und kein Release
v1.0.0. Kapitel 8 wird geschrieben, wenn es etwas zu berichten gibt." Das war korrekt,
solange es zutraf, und wird deshalb datiert statt still entfernt.
