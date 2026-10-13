# Regiebuch — migration-lab in zehn Minuten

*Vorführung vor IT-Leitung und Geschäftsführung: Universität, öffentliche Stelle, Softwarehaus,
Industrie. Stand 13.10.2026. Zahlen aus der Faktenbasis der Case Study, jede mit Messdatum.*

Diese Demo zeigt nicht „wir können migrieren", sondern **wie man migriert, ohne blind zu
fliegen** — und dass die Zahlen dazu öffentlich sind, einschließlich der unbequemen. Alles, was
gezeigt wird, ist im Repository nachlesbar. Was hier nicht steht, wird nicht behauptet.

## Vorbereitung — zehn Minuten vor Beginn

1. **Zugang zum Legacy-Stand aus 1Password holen.** Der 2016er-Stand liegt vollständig hinter
   einer Zugangsschranke; die Zugangsdaten liegen ausschließlich in 1Password (Eintrag zum
   Legacy-Stand von migration-lab). Sie stehen nie in Folien, Chats oder Mails und werden nicht
   an Zuhörer weitergegeben — wer den Stand selbst sehen will, bekommt auf Anfrage einen eigenen
   Zugang.
2. **Zwei Browserfenster nebeneinander, gleich breit.** Links
   <https://migration-lab-legacy.stoicera.cyou> (anmelden), rechts
   <https://migration-lab.stoicera.cyou>. Beide auf die Kundenliste stellen (`/#!/kunden`
   links, `/kunden` rechts). Beide Stände einmal durchklicken: Kunden, Aufträge, Rechnungen,
   Monatsbericht. Antwortet eine Adresse nicht, zwei Minuten warten und erneut laden.
3. **Repository im dritten Fenster:** <https://github.com/Stoicera/migration-lab-java>. Vorab
   geöffnet: `e2e/src/test/resources/selectors/`, `characterization/src/test/resources/golden/`,
   `docs/DEVIATIONS.md`, `ai-testgen/REPORT.md` (Abschnitt „Deutsche Zusammenfassung"),
   `playbook/README.md`.
4. **Das Playbook-PDF** liegt als Datei bereit (Link in Szene 7), falls das Netz ausfällt.
5. **Browserkonsole** im rechten Fenster kurz öffnen: keine CSP-Verletzung. Das ist der eine
   Check, den kein Skript sehen kann — Szene 5 erzählt, warum.
6. **Nicht zeigen:** Terminals mit Hostnamen oder Pfaden, das Deployment-Panel, Zugangsdaten,
   `docs/VERMARKTUNG.md`. Die erhaltene SQL-Injection des Legacy-Standes wird benannt, nicht
   vorgeführt — keine Eingaben, keine Beispiele.

## Ablauf — sieben Szenen

| Szene | Minute | Bild | Kernaussage |
|---|---|---|---|
| 1 | 0:00–1:00 | Startseite des Legacy-Standes | So sieht der Bestand aus |
| 2 | 1:00–3:00 | Beide Stände nebeneinander: Kunden, Aufträge | Gleiches Verhalten ist das Produkt |
| 3 | 3:00–5:00 | `e2e/` — Szenarien und Selektor-Map | Dieselbe Suite, zwei Oberflächen |
| 4 | 5:00–6:30 | `characterization/golden/` | Der Golden Master fängt, was das Auge nicht sieht |
| 5 | 6:30–7:30 | `docs/DEVIATIONS.md`, ADR-0004 | Das Register erlaubt alles außer Schweigen |
| 6 | 7:30–9:30 | `ai-testgen/REPORT.md` | Das KI-Experiment, mit den unbequemen Zahlen |
| 7 | 9:30–10:00 | `playbook/` | Die Mitgabe |

### Szene 1 — Der Bestand (0:00–1:00)

**Bild:** Startseite des Legacy-Standes. Auf die Fußzeile zeigen: „WerkstattCRM 1.4.2 · intern ·
bei Problemen: Hr. B. anrufen".

**Sprechtext:** „Das ist eine Anwendung, wie sie in vielen Häusern läuft: Java 8, Spring Boot 1.5,
AngularJS, PostgreSQL 9.6 — seit Jahren ohne Sicherheitsupdates, ohne einen einzigen Test, und
der Mann, der sie kennt, steht in der Fußzeile. Sie ist synthetisch, aber nach echten Mustern
gebaut; der Katalog ihrer Schwächen liegt offen im Repository. Die Frage, die jede Modernisierung
entscheidet, lautet nicht ‚Können wir das neu bauen?', sondern ‚Woher wissen wir, dass das Neue
dasselbe tut?'"

### Szene 2 — Beide Stände nebeneinander (1:00–3:00)

**Bild:** Links der Stand von 2016, rechts der moderne Stand — Kundenliste. In beiden Fenstern
auf „Aufträge" klicken, in beiden den Filter „Fertig" setzen, dann denselben Auftrag öffnen.

**Sprechtext:** „Links 2016, rechts 2026: Java 25, Spring Boot 4.1, Angular 22, PostgreSQL 18.
Sie sehen fast dasselbe — und das ist kein Mangel, das ist die Botschaft. Funktionale Äquivalenz
ist das Produkt. Eine Modernisierung, die nebenbei die Oberfläche ‚verbessert', verändert
unbemerkt Verhalten, auf das sich Mitarbeiter, Schnittstellen und Fremdsysteme verlassen. Die
Oberfläche können Sie danach jederzeit ändern — mit demselben Netz."

**Hinweis für den Vorführenden:** Die Daten im rechten Stand sind ein Demo-Datensatz, den
Besucher verändern können; die nächtliche Sicherung ist zugleich der Rücksetzpfad. Passt eine
Zahl nicht zum linken Stand, genau das sagen.

### Szene 3 — Dieselbe Selenium-Suite (3:00–5:00)

**Bild:** `e2e/src/test/java/at/stoicera/migrationlab/e2e/tests/` — zwölf Szenarioklassen,
von `KundenCrudTest` bis `AdminTest`. Dann `selectors/legacy.properties` und
`selectors/modern.properties` nebeneinander: dieselbe Zeile `nav.kunden=` in beiden, einmal
`a[href='#!/kunden']`, einmal `a[href='/kunden']`. Zuletzt `SelectorMapParityTest.java`.

**Sprechtext:** „34 Selenium-Szenarien je Stand — Kunde anlegen, Auftrag durch alle Status
führen, Rechnung erzeugen, Monatsbericht prüfen — laufen gegen beide Oberflächen. Nicht zwei
Suiten: dieselbe. Der Mechanismus ist unspektakulär: Die Tests kennen keine CSS-Selektoren, nur
Absichten — ‚der Kunden-Link', ‚die Auftragstabelle'. Welcher Selektor dahintersteht, steht in
einer Datei pro Oberfläche, und ein Test prüft, dass beide Dateien dieselben Schlüssel haben. Die
Suite auf die neue Oberfläche zu portieren hieß: eine neue Datei, keine neuen Tests. In der CI
am 1. Oktober 2026 gemessen und am 13. Oktober 2026 erneut grün: 34 von 34 auf beiden Ständen."

### Szene 4 — Charakterisierungstests (5:00–6:30)

**Bild:** `characterization/src/test/resources/golden/` — `kunden.json`, `auftrag-5.json`,
`bericht-monat-2026.json` öffnen. Dann die `README.md` des Moduls, Abschnitt „sanctioned
divergence".

**Sprechtext:** „Bevor die erste Zeile migriert wurde, haben wir aufgezeichnet, was die alte
Anwendung tatsächlich antwortet — HTTP-Antworten und Datenbankzustände nach Schreibvorgängen,
Eigenheiten eingeschlossen. 47 solcher Tests, grün gegen beide Stände — in der CI am
30. September 2026 gemessen, am 13. Oktober 2026 erneut. Beim Sprung von Spring Boot 1.5 auf
2.7 änderte sich die JSON-Darstellung eines Datumstyps. Die Oberfläche sah unverändert aus. Der Draht-Kontrakt war anders — jedes
Fremdsystem, das dieses Feld liest, wäre irgendwann gebrochen, lange nach dem ‚erfolgreichen'
Go-live. Gefunden hat es dieser Golden Master, nicht ein Mensch."

### Szene 5 — Das Abweichungsregister (6:30–7:30)

**Bild:** `docs/DEVIATIONS.md`, die Tabelle oben. Dann ADR-0004 mit den Zeilen SD-1 bis SD-3.

**Sprechtext:** „Drei Stellen verhalten sich absichtlich anders: Die Suche ist gegen
SQL-Injection abgedichtet — den Fehler Zeile für Zeile zu übernehmen wäre fahrlässig. Die alte
JSP-Adminseite ist in die Anwendung aufgenommen. Und eine Fehlermeldung, die 2016 wörtlich
‚undefined' anzeigte, zeigt jetzt den Text des Servers. Jede dieser Abweichungen steht in einem
Register mit Datum, Begründung und einem Test, der beide Seiten festhält. Dasselbe Register führt
auch, wo wir unsere eigenen Standards nicht erfüllen — mit Datum und Entscheidung. Und es führt
die eine Lücke, die kein Skript sieht: Die Suite lief 32 von 34 grün, während der Browser die
Styles blockierte. Das Register erlaubt alles außer Schweigen."

### Szene 6 — Das KI-Experiment (7:30–9:30)

**Bild:** `ai-testgen/REPORT.md`, „Deutsche Zusammenfassung" und die Tabelle Phase A / Phase B.
Optional die Grafik „Die 80-%-Falle" aus der Case Study.

**Sprechtext:** „Wir haben gemessen, was KI-generierte Unit-Tests für solchen Legacy-Code
leisten — Protokoll vor dem ersten Aufruf eingefroren, zwei Modelle, sechs Klassen, zwei
Codebasen. Kosten: 65 Cent. Das erste Ergebnis sah aus wie ein Prospekt: 100 % Abdeckung,
99,2 % Mutation Score. Der Haken: Nur 12 von 24 generierten Testklassen kompilierten überhaupt,
und die Zahlen galten nur für die Überlebenden. Die 613-Zeilen-Klasse, an der beide Modelle
scheiterten, war nicht im Nenner. Nach einem zeitbegrenzten Reparaturdurchgang standen 421
Testmethoden grün — und der Mutation Score fiel auf 73,2 %. Diese eine große Klasse: 84 %
Abdeckung, 56 % Mutation Score. Ein übliches 80-%-Gate hätte sie durchgewunken. Und: Von 421
generierten Testmethoden fand keine einen echten Fehler. Generierte Tests konservieren Verhalten
— für eine Migration genau richtig. Ein Audit sind sie nicht. Das ist ein Experiment mit sechs
Klassen, kein Beweis — und wir haben es genau so veröffentlicht."

### Szene 7 — Die Mitgabe (9:30–10:00)

**Bild:** `playbook/README.md`, die Kapitelübersicht; dann das PDF.

**Sprechtext:** „Alles, was Sie gesehen haben, steht in acht Kapiteln auf Deutsch: Vorgehen,
Stolperfallen, Entscheidungsregeln, Aufwände — gemessene und geschätzte sauber getrennt. Das
Playbook ist frei verfügbar. Was wir anbieten, ist nicht das Dokument, sondern die Durchführung
bei Ihnen — mit demselben Netz. Wie das abläuft, steht in `docs/enterprise.md`."

Link zum Mitgeben:
<https://github.com/Stoicera/migration-lab-java/releases/download/v1.0.0/migration-playbook.pdf>

## Drei Kernsätze

1. „Die teuersten Migrationsfehler kompilieren, starten und sehen gut aus." — Szene 4.
2. „Unser erstes Ergebnis war zu gut, um wahr zu sein. Wir haben es veröffentlicht — zusammen
   mit dem Grund, warum es zu gut war." — Szene 6.
3. „Generierte Tests konservieren Verhalten. Sie finden keine Fehler. Für eine Migration ist das
   erste genau richtig — solange niemand das zweite verspricht." — Szene 6.

## Zahlen-Spickzettel — nur mit Datum zitieren

| Zahl | Wert | Gemessen |
|---|---|---|
| Charakterisierungstests | 47, grün gegen beide Stände | 30.09.2026 (CI) |
| Selenium-E2E-Szenarien | 34 je Stand, 0 Fehler | 01.10.2026 (CI) |
| Umfang des Legacy-Standes | ~1.700 Zeilen Backend, 25 REST-Endpunkte, 10 Views + 1 JSP | 01.10.2026 |
| Entscheidungsprotokolle (ADR) | 16 | 01.10.2026 |
| KI-Experiment: Aufrufe / Kosten | 24 / 0,65 € | Lauf 31.07.2026, nachgerechnet 01.10.2026 |
| Phase A: kompilierende Testklassen | 12 von 24 (50 %) | 31.07.2026, reproduziert 01.10.2026 |
| Phase A: Mutation Score (nur kompilierende) | 99,2 % | 31.07.2026, reproduziert 01.10.2026 |
| Phase B: nach Reparatur | 21 von 24 grün, 421 Testmethoden, 90,5 % Zeilen, 73,2 % Mutation Score | 02.08.2026, reproduziert 01./02.10.2026 |
| God-Klasse (613 Zeilen) nach Reparatur | 84,3 % Zeilenabdeckung, 55,9 % Mutation Score | 02.08.2026, reproduziert 01.10.2026 |
| Echte Fehler durch generierte Tests | 0 (52 Reparaturen, davon 15 wegen falscher Erwartung) | 02.08.2026, ausgezählt 01.10.2026 |
| Übernommene generierte Tests | 88 Testmethoden; Modulabdeckung 37,3 % → 81,3 % | 01./02.10.2026 |
| Lastvergleich lokal, Lesepfad, beide Stände auf einem Rechner | p95: alt 3,99 ms, neu 2,34 ms · Median: alt 1,17 ms, neu 1,28 ms — kein durchgängiger Vorteil | 02.10.2026 |
| Geloggte Stunden | 16,05 h — Wandzeit eines KI-Agenten unter Aufsicht, kein Teambudget | 01.10.2026 |
| Release | v1.0.0, beide Stände live | 14.08.2026 |

Die Stundenzahl wird nur genannt, wenn danach gefragt wird — und dann mit der Einordnung.
Die KI-Zahlen tragen immer „in unserem Experiment" und das Datum: sechs Klassen, ein Versuch
je Zelle, keine Statistik.

## Akzente je Zielgruppe

- **Universität, Institut:** Szene 6 verlängern — vorregistriertes Protokoll mit Hash, Rohdaten
  und PIT-Berichte im Repository, Anschluss an Forschung zu LLM-gestützter Testgenerierung.
  Szene 3 kürzen.
- **Öffentliche Stelle:** Szenen 4 und 5 — Nachvollziehbarkeit, Register, jede Entscheidung
  datiert. Dazu die Übergabe aus `docs/enterprise.md` und der Betrieb in der EU. Keine Aussagen
  zu Vergabeverfahren.
- **Softwarehaus:** Szenen 3 und 4 technisch tiefer — Wait-Strategien je Oberfläche
  (`e2e/README.md`), Golden-Master-Governance (ADR-0007), OpenRewrite benutzt und bewertet
  (ADR-0002).
- **Industrie:** Szene 2 auf Schnittstellen und Fremdsysteme zuspitzen; Betrieb aus Kapitel 7
  des Playbooks: Health-Checks, die die Datenbank sehen, Image-Scan, Sicherungen mit
  durchgeführter Wiederherstellungsprobe.

## Zehn Einwände und die Antworten

1. **„Wie lange dauert das bei uns?"** — Das sagt erst der Migrations-Check: Umfang,
   Abhängigkeiten, Schnittstellen, vorhandene Tests. Aus den Zahlen dieses Repositorys lässt
   sich kein Projektbudget ableiten; was überträgt, sind Reihenfolge, Bruchkatalog und
   Entscheidungsregeln. Wir nennen einen Rahmen nach dem Check, nicht davor.

2. **„Das Repository nennt 16 Stunden — dann kostet das ja fast nichts."** — Die 16,05 Stunden
   (Stand 01.10.2026) sind die Wandzeit eines KI-Agenten unter Aufsicht für eine Anwendung mit
   1.700 Zeilen, keine Personentage eines Teams. Das Repository verbietet selbst, daraus ein
   Budget abzuleiten. Eine Migration „in X Stunden" versprechen wir deshalb nicht.

3. **„KI macht das doch inzwischen automatisch."** — Gemessen: Die Hälfte der generierten
   Testklassen kompilierte nicht, und von 421 reparierten Testmethoden fand keine einen Fehler
   (Experiment vom 31.07. und 02.08.2026, sechs Klassen). KI beschleunigt das Festhalten von
   Verhalten; das Urteil, was richtig ist, bleibt Handarbeit. Auch OpenRewrite wurde benutzt und
   bewertet — Assistent, kein Autopilot (ADR-0002).

4. **„Unsere Anwendung ist zwanzigmal größer."** — Richtig, und die Zahlen skalieren nicht
   linear; jedes Playbook-Kapitel sagt, was überträgt und was nicht. Die Methode — Netz zuerst,
   Etappen als auscheckbare Zustände, Register — hängt nicht von der Größe ab. Der
   Migrations-Check ermittelt, wie groß das Netz bei Ihnen sein muss.

5. **„Wir haben keine Tests — bei uns geht das nicht."** — Das ist der Normalfall und der
   Ausgangspunkt dieses Projekts: Der Legacy-Stand hat keinen einzigen Test.
   Charakterisierungstests brauchen kein Verständnis des Codes; sie zeichnen auf, was die
   Anwendung tut. Deshalb kommen sie zuerst.

6. **„Warum nicht gleich neu schreiben?"** — Weil niemand vollständig weiß, was die alte
   Anwendung tut, und ein Neubau genau das verliert. Hier wurde Route für Route ersetzt
   (ADR-0009), bei jedem Schritt mit denselben Szenarien grün. Ein Neubau bleibt danach
   möglich — mit dem Netz.

7. **„Wird die Anwendung danach schneller?"** — Im Referenzprojekt nicht nachweisbar. Beim
   Vergleich am 02.10.2026 lag der Median beim alten Stand vorn (1,17 ms gegen 1,28 ms), das
   95. Perzentil beim neuen (2,34 ms gegen 3,99 ms) — Bruchteile von Millisekunden, ein Lauf,
   ein Rechner. Zehn Jahre Framework-Versionen kaufen Wartbarkeit, Sicherheitsupdates und einen
   Arbeitsmarkt, nicht Geschwindigkeit. Leistungsgewinne versprechen wir erst nach einer Messung
   an Ihrer Anwendung.

8. **„Ist der neue Stand dann sicher?"** — Sicherer als der alte, nicht „sicher": Die Suche ist
   gegen SQL-Injection abgedichtet, der Admin-Bereich liegt hinter einer Zugangsschranke,
   Security-Header und Rate-Limit sind gemessen, ein Image-Scan läuft bei jedem Build. Derselbe
   Scan meldet seit 30.09.2026 wieder Funde im modernen Image; sie sind offen und noch nicht
   behoben. Was fehlt, steht in `SECURITY.md`: keine Benutzerverwaltung, kein Audit-Log. Wir
   benennen den Zustand, wir bescheinigen keinen.

9. **„Wir haben Oracle und wollen eigentlich Microservices."** — Beides ist hier bewusst nicht
   Teil des Vorgehens: Die Datenbank bleibt (ein Versionssprung ja, ein Wechsel des Systems
   nein), und modular bleiben ist in den meisten Fällen die richtige Antwort. Wenn Sie beides
   wollen, ist das ein eigenes Projekt mit eigener Begründung — nach der Migration, mit dem Netz.

10. **„Das haben Sie mit KI gebaut — zählt das?"** — Ja, und es steht auf der ersten Seite des
    Repositorys: Ein KI-Agent hat unter Anleitung und Review gearbeitet. Was zählt, ist nicht,
    wer getippt hat, sondern was prüfbar ist: 47 Charakterisierungstests, 34 Szenarien je
    Stand, 16 Entscheidungsprotokolle, ein Register — alles öffentlich. Bei Ihrem Projekt gilt
    dieselbe Arbeitsweise und dieselbe Prüfbarkeit.

## Was in dieser Demo nicht vorkommt

- Keine Zugangsdaten, keine Hostnamen, keine Ansichten des Deployment-Panels.
- Keine Vorführung der SQL-Injection — sie wird als katalogisiertes Exponat benannt.
- Keine Stundenzahl als Preis, keine Preise. Ein Angebot folgt dem Migrations-Check.
- Keine Verallgemeinerung aus sechs Klassen.
- Kein „0 Funde" im Image-Scan — das war der Stand vom 05.08.2026, nicht der aktuelle.
- Sicherungen samt durchgeführter Wiederherstellungsprobe: ja. Ein Sicherungsstandort außerhalb
  des Hosting-Kontos: noch nicht — das wird so gesagt und nicht anders.
