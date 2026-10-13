# So läuft eine Modernisierung mit uns

*Java · Spring Boot · Angular — Stoicera Software Group (Lugmayr-Kern). Stand 13.10.2026.*

Dieses Dokument beschreibt, wie wir eine bestehende Java-Anwendung modernisieren: in welchen
Etappen, was Sie am Ende jeder Etappe in Händen halten, was wir dafür von Ihnen brauchen, was
wir nicht tun und wie die Übergabe aussieht. Innerhalb der Stoicera Software Group führt die
Marke Lugmayr-Kern Modernisierungen in Java/Spring Boot und Angular aus. Das öffentliche
Referenzprojekt dazu ist dieses Repository — jedes Artefakt, das unten genannt wird, existiert
dort in der Form, in der Sie es bekommen.

Preise und Dauer stehen hier bewusst nicht. Beides ergibt sich aus dem Migrations-Check
(Etappe 0) und wird danach als Angebot formuliert. Die im Repository geloggten Stunden sind die
Wandzeit eines KI-Agenten unter Aufsicht für eine kleine Anwendung und taugen nicht als Maßstab
für Ihr Projekt — das Repository sagt das selbst auf seiner ersten Seite.

## Das Prinzip

Erst das Netz, dann der Sprung. Bevor eine Zeile migriert wird, halten Tests fest, was die
Anwendung heute tut — und eine Etappe ist erst abgeschlossen, wenn dieselben Tests gegen den
neuen Stand grün sind. Jede bewusste Abweichung vom alten Verhalten steht in einem Register mit
Datum, Begründung und einem Test, der beide Seiten festhält. Was nicht im Register steht,
verhält sich gleich.

## Die Etappen im Überblick

| Etappe | Ergebnis | Abgeschlossen, wenn |
|---|---|---|
| 0 · Migrations-Check | Schriftlicher Befund und Etappenplan | Sie entscheiden können, ob und wie es weitergeht |
| 1 · Sicherheitsnetz | Charakterisierungstests und E2E-Suite, grün gegen den Bestand | Kein Commit darf sie mehr brechen |
| 2 · Fundament | Reproduzierbarer Build; neuer Stand läuft als Kopie neben dem alten | Netz grün gegen die Kopie |
| 3 · Sprünge | Framework- und Laufzeitversionen gehoben, Sprung für Sprung | Netz grün nach jedem Sprung |
| 4 · Oberfläche | Frontend migriert, Route für Route | Dieselbe E2E-Suite grün auf beiden Oberflächen |
| 5 · Betrieb | Container, Health-Checks, Härtung, Deployment, Sicherungen mit Rückspielprobe | Verifikation von außen grün; Wiederherstellung einmal durchgeführt |

### Etappe 0 — Migrations-Check

**Ziel:** Wissen, was Sie haben, bevor jemand einen Rahmen nennt.

**Sie bekommen:** einen schriftlichen Befund — Stack-Inventar mit Versionsständen und
Support-Ende, Abhängigkeits-Audit, Schnittstellenkarte (welche Fremdsysteme lesen welche
Antworten), vorhandene Tests und ihr tatsächlicher Wert, katalogisierte Schwächen, eine
Risiko-Einschätzung je Bereich, einen Etappenplan mit Reihenfolge und Abschlusskriterien und die
Liste der Entscheidungen, die vor Etappe 1 bei Ihnen fallen müssen.

**Wir brauchen:** Lesezugriff auf Quellcode und Build-Konfiguration, eine Umgebung, in der die
Anwendung außerhalb der Produktion läuft (oder den Weg dorthin), ein Gespräch mit jemandem, der
die Anwendung fachlich kennt, und eines mit dem Betrieb.

**Abschluss:** Der Befund steht für sich. Sie können damit auch zu jemand anderem gehen.

### Etappe 1 — Sicherheitsnetz

**Ziel:** Das Ist-Verhalten ausführbar festhalten, bevor sich etwas ändert.

**Sie bekommen:** Charakterisierungstests — Golden Master über HTTP-Antworten und über
Datenbankzustände nach Schreibvorgängen, Eigenheiten eingeschlossen (im Referenzprojekt
`characterization/`, 47 Tests, zuletzt grün in der CI am 30.09.2026). Eine Selenium-E2E-Suite
über die Arbeitsabläufe Ihrer Anwender mit Selektor-Map: Die Tests sprechen Absichten an, die
Zuordnung zur Oberfläche steht in einer Datei je Oberfläche, und ein Test prüft, dass beide
Dateien dieselben Schlüssel tragen (`e2e/`, 34 Szenarien je Stand, zuletzt grün am 01.10.2026).
CI-Gates, die beide Suiten bei jedem Commit ausführen und eine rote Suite zum Stopp machen. Das
Abweichungsregister — zu Beginn leer. Kapitel 1 des Playbooks als Lesestoff.

**Wir brauchen:** einen Testdatensatz ohne personenbezogene Daten (wir helfen beim
Anonymisieren), Zugang zu Ihrer CI oder die Freigabe, eine einzurichten.

**Abschluss:** Beide Suiten grün gegen den Bestand; der Ausgangszustand ist als Tag
auscheckbar und startbar.

### Etappe 2 — Fundament

**Ziel:** Ein reproduzierbarer Build, und der neue Stand entsteht als Kopie neben dem alten.
Der alte Stand wird nicht angefasst.

**Sie bekommen:** Build mit Wrapper (kein lokal installiertes Werkzeug nötig), Container-
Definition je Stand, Abhängigkeits-Audit mit Entscheidung je Abhängigkeit (behalten, heben,
ersetzen), vereinheitlichtes Logging, die ersten Entscheidungsprotokolle (ADR, eine Seite je
Entscheidung). Kapitel 2 des Playbooks.

**Abschluss:** Netz grün gegen die Kopie; beide Stände laufen nebeneinander.

### Etappe 3 — Sprünge

**Ziel:** Framework und Laufzeit heben — in dokumentierten Sprüngen, nicht in einem.

**Sie bekommen:** je Sprung einen auscheckbaren Tag, den Bruchkatalog (was brach, wie das Netz
es fing, wie es gelöst wurde), die Bewertung der eingesetzten Werkzeuge (im Referenzprojekt
OpenRewrite: was es übernahm, was nicht — ADR-0002), geschlossene Sicherheitsschwächen als
registrierte Abweichungen mit Test auf beiden Seiten, ADRs. Kapitel 3 und 4 des Playbooks.

**Abschluss:** Netz grün nach jedem Sprung; keine unregistrierte Abweichung.

### Etappe 4 — Oberfläche

**Ziel:** Das Frontend ohne Stichtagsumstellung migrieren: Route für Route, alter und neuer
Teil laufen zeitweise nebeneinander (im Referenzprojekt ADR-0009).

**Sie bekommen:** die migrierte Oberfläche; die E2E-Suite grün auf beiden Oberflächen mit
einer zweiten Selektor-Map — für den Port selbst keine neuen Tests; Lint- und Format-Gates;
registrierte Abweichungen, wo die neue Oberfläche bewusst anders ist (etwa eine korrigierte
Fehlermeldung); ADRs zur Frontend-Strategie. Kapitel 5 des Playbooks.

**Abschluss:** Dieselben Szenarien grün auf alt und neu; die alte Oberfläche ist abschaltbar.

### Etappe 5 — Betrieb

**Ziel:** Die Anwendung läuft dort, wo Sie sie betreiben — und das ist von außen prüfbar.

**Sie bekommen:** Datenbank-Hebung mit Nachweis (Sortierung und Kontrakte gemessen gleich),
Schema-Migrationen als Code, Health-Checks, die die Datenbank tatsächlich sehen, strukturierte
Logs und optionales Tracing, Härtung am Rand — Zugangsschranke für Admin-Flächen,
Security-Header, Rate-Limit, jeweils gemessen —, Image-Scan in der CI, eine
Deployment-Dokumentation, in der jeder Schritt ausgeführt wurde, bevor er aufgeschrieben wurde,
Sicherungen mit durchgeführter Wiederherstellungsprobe, ein Verifikationsskript gegen die
Live-Adressen und die Handarbeitsliste für das, was kein Skript sieht. Kapitel 7 und 8 des
Playbooks.

**Wir brauchen:** die Zielumgebung (Ihre oder eine, die wir in der EU bereitstellen) und die
Entscheidung, wer sie danach betreibt.

**Abschluss:** Verifikationsskript grün von außen; Wiederherstellung einmal durchgeführt, nicht
geplant.

## Voraussetzungen bei Ihnen

- Quellcode und Build sind zugänglich; die Anwendung lässt sich außerhalb der Produktion
  starten.
- Eine fachlich verantwortliche Person mit Entscheidungsbefugnis: Jeder Eintrag im
  Abweichungsregister ist Ihre Entscheidung, nicht unsere.
- Testdaten ohne personenbezogene Daten. Kundendaten verlassen Ihr Haus nicht.
- Git-Hosting und CI — oder die Freigabe, beides einzurichten.
- Die Bereitschaft, Verhalten und Oberfläche während der Migration eingefroren zu lassen.
  Neue Funktionen kommen danach, mit dem Netz.

## Was wir nicht tun

- **Keine Migration ohne Netz.** Wenn Etappe 1 übersprungen werden soll, nehmen wir den
  Auftrag nicht an.
- **Kein Wechsel des Datenbanksystems** im selben Projekt. Eine Versionshebung ja; ein anderes
  System ist ein eigenes Projekt.
- **Keine Zerlegung in Microservices** als Nebeneffekt. Modular bleiben ist in den meisten
  Fällen die richtige Antwort; wenn nicht, ist das eine eigene Entscheidung mit eigenem ADR.
- **Keine neuen Funktionen** während der Migration.
- **Keine Leistungsversprechen.** Im Referenzprojekt war der neue Stand nicht messbar
  schneller (Vergleich vom 02.10.2026). Zehn Jahre Framework-Versionen kaufen Wartbarkeit,
  Sicherheitsupdates und einen Arbeitsmarkt.
- **Keine Bescheinigungen.** Wir benennen den erreichten Sicherheitszustand und seine Lücken
  schriftlich (`SECURITY.md` im Referenzprojekt zeigt die Form). Eine Bescheinigung ist das
  nicht.
- **Keine Aufwände aus fremden Zahlen.** Die Stunden des Referenzprojekts sind kein Maßstab;
  Ihr Rahmen kommt aus Ihrem Migrations-Check.
- **Kein Betrieb des alten Standes** über das Projektende hinaus.

## Übergabe

Alles liegt von Anfang an in Ihrem Repository, nicht erst am Ende. Zum Abschluss übergeben wir:

- **Quellcode:** beide Stände, Tags je Etappe, Git-Historie ohne Umschreibung.
- **Dokumentation:** README, Entscheidungsprotokolle, Abweichungsregister,
  Deployment-Dokumentation, Handarbeitsliste, die Playbook-Kapitel mit Ihren Werten.
- **Tests:** beide Suiten, CI-Konfiguration, Anleitung zum Ausführen gegen jeden Stand.
- **Betrieb:** Container-Definitionen, Verifikationsskript, Sicherungs- und
  Wiederherstellungsanleitung, ein Übergabetermin mit Ihrem Betrieb. Auf Wunsch betreiben wir
  die Anwendung weiter — als eigene Vereinbarung, nicht als Voraussetzung.
- **Zugänge:** liegen bei Ihnen. Zugänge, die wir im Projekt genutzt haben, werden zum
  Projektende zurückgegeben.

## Wie wir arbeiten

Zahlen nur mit Messdatum. Abweichungen registriert statt geglättet. Keine Etappe gilt als
abgeschlossen, die Sie nicht vorgeführt bekommen. Wir arbeiten KI-gestützt unter Anleitung und
Review — im Referenzprojekt auf der ersten Seite offengelegt, bei Ihnen genauso.

## Nächster Schritt

Eine zehnminütige Vorführung beider Stände (`docs/demo.md`), danach der Migrations-Check.
Kontakt: office@stoicera.com.
