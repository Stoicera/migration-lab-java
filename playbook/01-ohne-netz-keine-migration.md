# Kapitel 1 — Ohne Netz keine Migration

*Etappe: `stage-1-safety-net` · Ausgangspunkt: `stage-0-legacy`*

## Ausgangslage

WerkstattCRM, Stand 2016: Java 8, Spring Boot 1.5, AngularJS 1.8, eine
JSP-Adminseite, PostgreSQL 9.6. **Keine einzige Testzeile.** Genau so sehen die
Systeme aus, deretwegen Modernisierungen verschoben werden: Jede Änderung kann
unbemerkt etwas kaputt machen, also ändert man lieber nichts.

Die Konsequenz für unser Vorgehen ist nicht verhandelbar: **Bevor auch nur eine
Zeile migriert wird, wird das Ist-Verhalten festgeschrieben.** Nicht das
Soll-Verhalten — das Ist-Verhalten, inklusive der Eigenheiten.

## Vorgehen

Das Sicherheitsnetz hat zwei Schichten, die unterschiedliche Fragen beantworten:

**1. Selenium-E2E-Suite (`e2e/`) — "Funktionieren die Abläufe für den Benutzer?"**

Vier Szenarien decken die Kernabläufe ab: Kunden-Stammdaten (anlegen, ändern,
suchen, löschen), Auftrags-Lebenszyklus (Annahme → Arbeit → fertig → abgeholt),
Rechnung (Erstellung aus fertigem Auftrag, 20 % USt, bezahlt setzen),
Monatsbericht (Kennzahlen). Entscheidend ist die Architektur der Suite:

- **Page Objects adressieren Elemente nur über Absichts-Schlüssel** («das
  Suchfeld der Kundenliste»). Welcher CSS-Selektor dahintersteht, weiß allein
  die Selektor-Landkarte (`selectors/legacy.properties`). Für die neue
  Angular-Oberfläche in Etappe 5 entsteht eine zweite Landkarte — **die
  Szenarien selbst bleiben unverändert.** Dieselbe Suite beweist dann die
  funktionale Gleichwertigkeit von Alt und Neu.
- **Nur explizite Waits.** Implizite Waits stehen auf null; jede
  Synchronisation wartet auf eine konkrete, benannte Bedingung.
- **Determinismus durch Datenhoheit:** Vor jedem Szenario wird die Datenbank
  auf den eingecheckten Seed-Stand zurückgesetzt. Assertions dürfen sich auf
  exakte Werte verlassen; Berichts-Prüfungen verwenden eingefrorene
  Vergangenheitsmonate, Schreibtests schreiben nur in den laufenden Monat.

**2. Charakterisierungs-Tests (`characterization/`) — "Antwortet das System noch identisch?"**

Golden-Master-Aufnahmen jedes Lese-Endpunkts (JSON-Vergleich auf Baumebene,
elf Endpunkte plus die JSP-Adminseite) und der Datenbank-Zustandsübergänge der
Schreibpfade. Auch die Macken sind festgeschrieben: dass die Auftragsannahme
nebenbei den km-Stand des Fahrzeugs ändert; dass das Löschen eines Kunden
verwaiste Fahrzeuge hinterlässt. **Das ist Absicht:** Die Migration muss das
heutige Verhalten reproduzieren, bis eine Etappe es bewusst ändert — mit
Architekturentscheid und Playbook-Eintrag.

**CI-Schranke, ab jetzt für immer:** Jeder Commit baut den Legacy-Stand und
lässt Charakterisierung + E2E gegen den laufenden Docker-Stand laufen. Rot
heißt anhalten und reparieren — nicht «später».

## Stolperfallen (aus diesem Projekt, ehrlich protokolliert)

Unser Flaky-Protokoll aus der Stabilisierung — **drei** echte Funde (zwei beim
ersten lokalen Lauf, der dritte erst in der CI), alle deterministisch behoben,
kein einziger Retry:

1. **Überlappende HTTP-Ladevorgänge.** Die Legacy-Oberfläche bricht laufende
   Requests nicht ab. Wer sofort nach dem Öffnen der Kundenliste sucht, hat
   zwei Antworten im Flug — die spätere gewinnt, die Tabelle rendert doppelt,
   Elementreferenzen veralten (`StaleElementReference`). *Behebung:* Seiten
   gelten erst als «offen», wenn die Erstladung sichtbar abgeschlossen ist.
   Merksatz: **Erst settlen, dann interagieren.**
2. **Navigation auf die bereits aktive Route lädt nichts neu.** AngularJS
   (ngRoute) instanziiert den Controller nicht neu, wenn der Navigationslink
   der aktuellen Route geklickt wird — der Test hing auf einer gefilterten
   Altansicht. *Behebung:* `open()` beginnt mit einem vollständigen Seitenladen
   und navigiert dann per echtem Routenwechsel.

3. **Speichern ohne Rückmeldung — der CI-Fund.** Lokal dreimal grün, in der CI
   dann rot: Beim *Ändern* eines Kunden zeigt die Oberfläche nach dem Speichern
   **keinerlei sichtbare Veränderung** — unser Warte-Kriterium (Überschrift)
   war für diesen Fall wirkungslos, der Test navigierte sofort weiter, und der
   Seitenwechsel brach den noch laufenden PUT-Request ab: **Update verloren,**
   auf schneller Hardware fast nie, auf langsamer CI-Hardware reproduzierbar.
   *Behebung:* explizites Warten darauf, dass AngularJS keine offenen
   HTTP-Requests mehr hat, bevor weiternavigiert wird. *Nebenbefund fürs
   Migrations-Backlog:* fehlendes Speicher-Feedback ist auch für echte
   Benutzer ein Datenverlust-Risiko (schnelles Weiterklicken!).

Alle drei Funde sind Eigenschaften der Legacy-Anwendung, nicht der Testsuite —
und genau deshalb wertvoll: Sie dokumentieren Verhalten, das auch echte
Benutzer trifft, und sie wären mit «Retry bis grün» unsichtbar geblieben.
Fund 3 zeigt außerdem, warum «lokal grün» nicht reicht: Erst die langsamere
CI-Umgebung machte das Zeitfenster groß genug.

Weitere Regeln, die Flakiness strukturell verhindern: Screenshots bei jedem
Fehlschlag (Analyse beginnt mit Beweismaterial), ein einziger Timeout-Standard
für die ganze Suite, keine Testklasse hängt vom Zustand einer anderen ab.
Offen deklarierter Kompromiss: **innerhalb** einer Szenario-Klasse bilden die
Tests bewusst einen geordneten Geschäftsfluss (Anlegen → Ändern → Löschen) —
schlägt Schritt 1 fehl, sind die Folgefehler derselben Klasse Folgeschäden,
keine eigenständigen Befunde. Der Trade-off (realistische Flüsse gegen isolierte
Einzeldiagnose) ist gewollt und hier dokumentiert statt verschwiegen.

## Aufwand

| Posten | Stunden |
|---|---:|
| E2E-Suite (Page Objects, Selektor-Landkarte, 4 Szenarien, 13 Tests) | 0,35 |
| Stabilisierung inkl. Analyse der drei Flaky-Funde | 0,2 |
| Charakterisierung (12 Golden Master, 5 DB-Übergänge) | 0,15 |
| CI-Verdrahtung (Gates, Artefakte bei Fehlschlag) | 0,1 |
| **Summe Etappe 1** | **≈ 0,8** |

**Wie diese Zahl zu lesen ist** (gilt für alle Aufwandstabellen dieses
Playbooks): Die *Summe* ist gemessene Wall-Time — allerdings die eines
**KI-Agenten unter Aufsicht**, nicht die eines Menschenteams (Offenlegung:
README, „How this was built"). Die *Aufteilung* auf die Posten ist eine
nachträgliche Schätzung über die gemessene Summe, keine Einzelmessung — das
stand hier anfangs anders und wurde im Zuge des Reviews korrigiert
(Worklog Session 7/8).

Zum Vergleich: Bei einem realen Kundensystem dieser Größe — konkret: ~1.700
Zeilen Backend, 25 REST-Endpunkte, 10 Views, aber unbekannte Codebasis,
Abstimmung, Zugänge, Testdaten-Klärung — kalkulieren wir für dieselbe Etappe
**3–5 Personentage**. Diese Spanne ist eine **Erfahrungsschätzung aus
Projektgeschäft, nicht in diesem Repo gemessen** — sie ist bewusst anders
etikettiert als die Messwerte darüber. Der Aufwandstreiber im Feld ist nicht
das Schreiben der Tests, sondern das Klären des Ist-Verhaltens.

## Entscheidungsregeln

- **Wie viele E2E-Szenarien reichen?** Die Geschäftsflüsse, deren Ausfall am
  Montagfrüh ein Anruf wäre — nicht mehr. Hier zu Etappe 1: 4 Flüsse, 13 Tests,
  < 1 Minute Laufzeit. Alles Weitere gehört in die billigeren Schichten darunter.
  *Nachtrag (Review Session 7):* Das feindselige Review zeigte, dass unsere
  erste Antwort auf diese Frage zu schmal war — Dashboard, Fahrzeug-Modul,
  Storno- und Validierungspfade fehlten. Die Suite wurde daraufhin auf
  **6 Flüsse / 26 Tests** erweitert (weiterhin < 30 s). Lektion fürs Feld: Die
  «Montagfrüh-Anruf»-Liste macht man zweimal — einmal selbst, einmal von
  jemandem, der das Scheitern sucht.
- **E2E oder API-Charakterisierung?** Beides, mit klarer Arbeitsteilung: E2E
  beweist Benutzer-Flüsse (wenige, teure, aussagekräftige Tests), Golden Master
  beweisen Antwort-Gleichheit in der Breite (viele, billige, exakte Vergleiche).
  Nur E2E → zu grob; nur API → Oberflächen-Regressionen unsichtbar.
- **Was tun mit gefundenen Fehlern im Altsystem?** Festschreiben, nicht fixen.
  Jeder «Bugfix» vor der Migration verändert das Referenzverhalten und macht
  den Gleichwertigkeits-Beweis wertlos. Fehler werden katalogisiert
  (`LEGACY_NOTES.md`) und in einer bewussten Etappe behoben.
- **Wann ist das Netz «fertig»?** Wenn drei aufeinanderfolgende Läufe ohne
  Eingriff grün sind und jeder Fehlschlag der Stabilisierungsphase eine
  dokumentierte Ursache hat. «Läuft meistens» ist kein Sicherheitsnetz.
