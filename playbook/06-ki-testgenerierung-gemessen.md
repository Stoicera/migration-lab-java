# Kapitel 6 — KI-Testgenerierung, gemessen statt geglaubt

*Meilenstein: G6 · Protokoll eingefroren: `ai-testgen-protocol-v1` (2026-07-31)*

> **Status dieses Kapitels: abgeschlossen (2026-08-02).** Der Methodenteil unten ist
> **vor** der ersten Modell-Anfrage geschrieben — das ist der ganze Punkt. Phase A
> (wie generiert) wurde am 2026-07-31 gemessen, Phase B (Nachbesserung) am 2026-08-02.
> Vollständige Zahlen: `ai-testgen/REPORT.md`.
>
> Das Ergebnis ist unbequem ausgefallen, und es steht trotzdem hier: Die Qualitätszahlen
> sind nach der Reparatur **schlechter** als davor, und in einer von 24 Zellen hat nicht
> das Modell versagt, sondern unsere eigene Auswertung. Beides unten, an der Zahl, nicht
> im Kleingedruckten. Ein Protokoll, das man nach dem Ergebnis ändert, ist Werbung.

## Ausgangslage: die teuerste Frage im Migrationsgespräch

„Kann die KI uns nicht einfach die fehlenden Tests schreiben?" — diese Frage
kommt in jedem zweiten Erstgespräch, und sie ist berechtigt. Legacy-Systeme haben
keine Tests (unser Ausstellungsstück hat bewusst null), Tests nachzuziehen ist
teuer, und LLMs schreiben plausibel aussehende Testklassen in Sekunden.

Die ehrliche Antwort lautet nicht „ja" und nicht „nein", sondern: **es kommt
darauf an, und zwar messbar worauf.** Genau das misst dieser Meilenstein.

## Warum Präregistrierung, und was das im Alltag kostet

Wer ein KI-Experiment durchführt und *danach* entscheidet, welche Klassen zählen,
welche Metriken berichtet werden und wann ein Ergebnis „nicht repräsentativ" war,
produziert keine Messung, sondern eine Meinung mit Zahlen. Deshalb:

1. **`PROTOCOL.md` wird vor dem ersten API-Aufruf eingefroren** — Commit + Tag.
   Jede spätere Änderung ist ein datierter Nachtrag und nur für noch nicht
   ausgeführte Schritte erlaubt.
2. **Die Auswahl der Prüfobjekte steht im Code** (`Catalog.java`), nicht im Kopf.
   Ein Test bricht den Build, wenn eine ausgewählte Klasse verschwindet.
3. **Die Prompts stehen im Protokoll** — und ein Test vergleicht sie
   zeichenweise mit den Prompts, die der Harness tatsächlich verschickt.
   Protokoll und Code können nicht auseinanderlaufen.
4. **Fehlschläge bleiben im Repo.** Nicht kompilierender Code, abgebrochene
   Reparaturen, leere Antworten: alles wird eingecheckt und zählt im Nenner.

Kosten dieser Disziplin: rund eine halbe Session Vorarbeit, bevor ein einziges
Ergebnis existiert. Nutzen: das Ergebnis ist zitierfähig — auch das schlechte.

## Der Aufbau in einem Absatz

Sechs Klassen, stratifiziert ausgewählt: die 613-Zeilen-Gottklasse, drei
REST-Controller, die JSP-Adminseite als „unangenehmer Fall" und ein reiner
Datenhalter als **Negativkontrolle** (er soll zeigen, dass hohe Coverage nichts
über Testqualität sagt). Zwei Modelle: ein kommerzielles Spitzenmodell und ein
offenes Gewichtsmodell, identische Prompts, ein Versuch pro Klasse
(`temperature 0`). Gemessen wird in zwei Phasen: **Phase A** so wie generiert,
**Phase B** nach zeitlich gedeckelter Reparatur. Metriken: Kompilierrate,
Pass-Rate, JaCoCo-Coverage, **PIT-Mutation-Score**, Reparaturaufwand,
Token-Kosten in Euro.

## Der Dreh, der aus dem Experiment eine Migrationsaussage macht

Die sechs Klassen werden **zweimal** gemessen: einmal als Legacy-Code von 2016
(Feldinjektion, verkettetes SQL, JSP) und einmal als ihre migrierten
Gegenstücke aus `modern/` (Konstruktorinjektion, parametrisiertes SQL, REST).

Damit beantwortet der Meilenstein nicht nur „Kann die KI Legacy testen?",
sondern die Frage, die eine Geschäftsführung wirklich stellt:

> **Zahlt sich die Modernisierung in Testbarkeit aus — und um wie viel?**

Ein Vorgeschmack aus der Vorbereitung, noch ohne KI: Für den *identischen*
Smoke-Test braucht die Legacy-Variante `@InjectMocks` **plus** Reflection, weil
die Abhängigkeiten in privaten Feldern stecken. Die migrierte Variante braucht
einen Konstruktoraufruf. Das ist kein Schönheitsargument, das ist der Unterschied
zwischen „Testaufbau muss man sich erarbeiten" und „Testaufbau ist eine Zeile".

**Die Ehrlichkeitsschranke dazu** steht als Threat T7 im Protokoll: Die beiden
Korpora unterscheiden sich um mehr als die Injektionsart (Etappe 4 hat auch das
SQL parametrisiert, Etappe 5 die Adminseite absorbiert). Ein Unterschied im
Ergebnis ist ein **Migrations**effekt, kein reiner Injektions­effekt — und wird
so berichtet.

## Was wir bewusst NICHT messen

- **Kein Agenten-Loop.** Ein Modell, das seinen Compiler-Fehler sieht und
  nachbessert, liefert bessere Zahlen — beantwortet aber eine andere Frage
  (und wäre ein eigenes Experiment). Hier: ein Aufruf, ein Ergebnis.
- **Keine Prompt-Optimierung.** Der erste vernünftige Prompt wird eingefroren.
  Wer den Prompt bis zum guten Ergebnis dreht, misst seine eigene Ausdauer.
- **Keine Signifikanz.** N = 6 pro Korpus, k = 1. Das ist eine belastbare
  Illustration mit sauberer Messung, keine Studie. Der Report sagt das an jeder
  Zahl, nicht im Kleingedruckten.

## Die unbequeme Offenlegung

Die Reparatur in Phase B macht in diesem Repo **kein Mensch mit Stoppuhr**,
sondern der ausführende KI-Agent unter Aufsicht des Eigentümers — dasselbe
Ausführungsmodell wie im ganzen Projekt (siehe README „How this was built").
Konsequenz, die wir lieber selbst aussprechen als von einem Gutachter zu hören:

- Die Kennzahl heißt **„Reparaturaufwand in Agenten-Wall-Clock-Minuten unter
  Aufsicht"** und ist **keine** Personenminuten-Schätzung für Ihr Team.
- Der Reparateur (Claude) ist mit einem der beiden Testkandidaten verwandt.
  Dass er dessen Ausgabe schneller versteht, lässt sich nicht ausschließen —
  im Protokoll als Threat T8 registriert, begrenzt durch das 30-Minuten-Limit
  und dadurch, dass alle Rohartefakte im Repo liegen: Wer es mit einem Menschen
  nachmessen will, hat alles, was er dafür braucht.

Rechnen Sie unsere Minuten also **nicht** eins zu eins auf Ihr Budget um. Was Sie
übernehmen können, sind die *Verhältnisse* (Legacy vs. migriert, Modell A vs.
Modell B) und die *Kategorien*, in denen der Aufwand anfällt.

## Nebenprodukt mit eigenem Wert: `modern/` bekommt seine ersten Tests

Drei Punkte des Standards standen bis heute als „aufgeschoben bis G6" im
Abweichungs-Register (`docs/DEVIATIONS.md`) — sie hingen alle daran, dass das
moderne Modul überhaupt eine Testschicht bekommt:

| Punkt | Was jetzt gilt |
|---|---|
| ArchUnit-Regeln | Fünf Regeln, migrationszweckgebunden: keine Feldinjektion mehr (sichert den Konstruktor-Sweep aus Etappe 4 dauerhaft), injizierte Felder final, Service kennt keine Controller, SQL bleibt im Service, Modelle bleiben Spring-frei. |
| Testcontainers-Integrationstest | Startet PostgreSQL 9.6 aus **denselben** Init-Skripten wie der Compose-Stand und fährt die echten SQL-Pfade — ohne laufenden Stand, ohne zweite Schema-Kopie. |
| Coverage-Gate | Als **Ratsche** scharf gestellt: gepinnt auf das, was die Suite tatsächlich erreicht, dann hochgezogen. 2026-07-31: 37,3 % gemessen → Gate 35 %. 2026-08-02: sechs reparierte Testklassen aus dem Experiment übernommen (88 Methoden, ADR-0011) → **81,3 % gemessen → Gate 80 %**. Damit sind die 80 % des Standards zum ersten Mal *erreicht* statt behauptet. |

**Nachtrag zur Übernahme, weil die Auswahlregel die eigentliche Lehre ist:** Übernommen wurde
**eine Klasse pro Prüfobjekt**, nach höchstem Mutation-Score, bei Gleichstand die **mit weniger
Testmethoden**. Genau diese dritte Regel hat die 134-Methoden-Klasse aussortiert, die exakt
dasselbe misst wie ihre 13-Methoden-Konkurrentin. Und sie hat gemischt gewählt — zwei Klassen
vom teuren, vier vom günstigen Modell —, was der beste Hinweis darauf ist, dass hier keine
Vorliebe am Werk war. Was die neue 81 %-Zahl **nicht** heißt: Die Gottklasse ist damit nicht
abgesichert; ihre übernommene Suite hat 44 % Mutation-Score. Das steht im ADR neben der Zahl.

Merksatz für Ihr Projekt: **Ein Gate, das Sie nicht erreichen können, schalten
Sie nicht „später" scharf — Sie schalten es als Ratsche scharf und ziehen sie
hoch.** Alles andere endet in `-DskipTests`.

## Ergebnisse Phase A (wie generiert), 2026-07-31

24 Aufrufe, **€ 0,65 gesamt**. Die Zahlen im Überblick:

| Kennzahl | Wert |
|---|---|
| Kompilierrate | **12 von 24 (50 %)** |
| Pass-Rate der kompilierenden Klassen | 151/154 Testmethoden (98,1 %) |
| Line-/Branch-Coverage auf der Zielklasse | 100 % / 100 % |
| Mutation-Score (PIT, grüne Teilmenge) | 129/130 (99,2 %) |
| Kosten | M1 € 0,614 · M2 € 0,034 |

**Der Befund, der die Intuition umdreht:** Die 55–80 Zeilen langen Controller wurden
mühelos und exzellent getestet. Die **613-Zeilen-Gottklasse — genau die Klasse, für die
man sich die KI-Hilfe wünscht — lieferte in allen vier Zellen nichts Brauchbares.**
Das kommerzielle Modell verbrauchte sein gesamtes Ausgabebudget für internes
„Nachdenken“ und gab **gar keine Antwort** aus (in beiden Korpora), das offene Modell
lieferte nicht kompilierenden Code. *Der Reflex „die KI macht das schon“ trägt dort am
wenigsten, wo der Schmerz am größten ist.*

Zwei weitere Punkte für die Kalkulation:

- **Wo es klappte, war es echte Qualität, keine Coverage-Kosmetik.** 100 % Abdeckung
  *und* 99,2 % Mutation-Score: die Tests fangen eingebaute Fehler wirklich. Genau
  diesen Unterschied zeigt ein Coverage-Report allein nie — deshalb PIT.
- **Billig ist nicht „fast so gut“.** Das offene Modell war 18-mal günstiger und
  kompilierte in 3 von 12 statt 9 von 12 Fällen.

## Ergebnisse Phase B (nach Nachbesserung), 2026-08-02

Elf Zellen mussten repariert werden. Regel dabei: **reparieren ja, dazuschreiben nein** — kein
Testfall, den das Modell nicht selbst geschrieben hat. Nachgeprüft, nicht geglaubt.

| Kennzahl | Phase A (wie generiert) | Phase B (repariert) |
|---|---|---|
| Grüne Zellen | 12 von 24 | **21 von 24** |
| Testmethoden | 154 (3 rot) | **421 (0 rot)** |
| Line-Coverage | 100 % | **90,5 %** |
| Branch-Coverage | 100 % | **78,8 %** |
| Mutation-Score | 99,2 % | **73,2 %** |

**Die Zahlen werden nach der Reparatur schlechter — und das ist das wertvollste Ergebnis
dieses Meilensteins.**

Nicht weil die Reparatur geschadet hätte. Sondern weil die makellosen Phase-A-Werte nur über
die Zellen gerechnet waren, die *zufällig kompiliert hatten* — und das waren ausnahmslos die
kleinen Controller. Die Gottklasse war im Nenner gar nicht enthalten. Erst die Reparatur holt
sie hinein.

> **Merksatz für jedes KI-Werbeversprechen, das Sie künftig lesen:**
> Fragen Sie nicht „Wie gut waren die generierten Tests?“, sondern
> **„Über wie viele der Versuche ist diese Zahl gerechnet?“**
> Eine Erfolgsquote, die nur über die geglückten Läufe gemessen wird, ist keine Messung.
> Das nennt sich Überlebenden-Effekt, und wir sind selbst hineingelaufen — sichtbar nur,
> weil das Protokoll uns zwang, auch die kaputten Zellen zu Ende zu messen.

**Was ein Entscheider daraus mitnimmt:**

1. **Die Gottklasse bleibt die Mauer — auch repariert.** Sie ist die einzige Klasse, die der
   Prüfung nicht standhält: rund **83 % der Zeilen abgedeckt, aber nur ~50 % der eingebauten
   Fehler gefunden**. Ein Coverage-Gate bei 80 % hätte hier „bestanden“ gemeldet. *Genau
   deshalb messen wir mit Mutation-Score.*
2. **Zehnmal so viele Tests, exakt null Mehrwert.** Für denselben Controller schrieb ein
   Modell 13 Testmethoden, das andere 134 — bei identischem Messergebnis (21/21 Zeilen,
   13/13 Mutanten). Die 121 zusätzlichen Methoden finden nichts und müssen für immer gepflegt
   werden. **Kaufen Sie keine Testmengen.**
3. **Kein einziger echter Programmfehler.** 15 Reparaturen betrafen falsche Erwartungen der
   Tests, **keine davon** einen Defekt im Produktivcode. Generierte Tests **zementieren** das
   vorhandene Verhalten, sie prüfen es nicht. Ohne Charakterisierungsnetz darunter wissen Sie
   nicht, ob sie etwas Richtiges festhalten.
4. **Billig kauft Tokens, nicht Ergebnisse.** Das offene Modell war 18-mal günstiger und kam
   nach der Reparatur auf 12 von 12 grünen Zellen — verursachte aber **10 der 11
   Reparaturen**. Die Rechnung verschiebt sich vom Einkauf in die Arbeitszeit.
5. **Frisch migriert heißt kurzfristig schlechter unterstützt.** Das offene Modell benutzte
   `getStatusCodeValue()` — in Spring 4.3 vorhanden, in Spring 7 **entfernt**. Wer auf einen
   sehr neuen Stack migriert, bekommt vorübergehend schlechtere KI-Hilfe, weil das Modellwissen
   dem Framework hinterherhinkt. Planbar, aber real.

**Zur Aufwandszahl sagen wir bewusst weniger, als wir könnten.** Die gemessenen 154 Minuten
über elf Zellen sind als Zeitangabe **unbrauchbar**: Wir haben die Zellen parallel repariert,
und sie haben sich gegenseitig die Maschine weggenommen — eine Zelle meldet 24,7 Minuten
Wanduhr für einen Rechenlauf von 5,6 Sekunden. Wir berichten sie als Obergrenze und benennen
den Fehler als unseren (Protokoll-Nachtrag A4). Belastbar ist stattdessen die **Art und Zahl
der Eingriffe**: 52 insgesamt — 19 Import/Syntax, 17 Mock-Aufbau, 15 falsche Erwartung.
*Der Aufwand hängt nicht an der Zeilenzahl, sondern daran, wie schwer die Abhängigkeiten zu
mocken sind.*

**Und ein Fund, der uns selbst betrifft.** In einer der 24 Zellen hat nicht das Modell versagt,
sondern **unsere Auswertung**: Das Modell schrieb einen Entwurf, verwarf ihn im Klartext
(„Wait, I need to produce the correct final answer…“) und lieferte danach eine korrekte
Testklasse. Unsere vorab festgelegte Regel „nimm den ersten Codeblock“ hat den verworfenen
Entwurf gewertet. Wir haben die Zahl **nicht** nachträglich korrigiert — eine Regel wird nicht
dadurch falsch, dass sie einen Punkt kostet —, aber wir sagen es an der Zahl. *Für Ihre eigenen
KI-Auswertungen: Wie Sie die Antwort aus dem Modelltext herausschneiden, ist eine
Messentscheidung. Bei uns hing ein Zwölftel des Ergebnisses daran.*

**Eine Ehrlichkeitsnotiz zum Ausgabebudget:** Die 16 000-Token-Grenze pro Antwort haben
*wir* im Protokoll festgelegt, nicht das Modell. Dass ein Reasoning-Modell sie mit
Nachdenken füllt, bevor es zur Antwort kommt, ist ein Zusammentreffen von unserem
Design und dem Modellverhalten — und wird als solches berichtet, nicht dem Modell
angelastet. Für Ihr Projekt heißt das trotzdem etwas Praktisches: **Reasoning-Tokens
werden bezahlt, ob sie zu einer Antwort führen oder nicht.** Über die Hälfte der
Ausgabe-Tokens des teuren Modells entfiel auf die zwei Aufrufe, die nie eine Antwort
lieferten.

## Entscheidungsregeln (Stand: beide Phasen gemessen)

1. **Kein KI-Test ohne Netz darunter.** Generierte Tests zementieren das
   Verhalten, das sie vorfinden — inklusive Fehler. Ohne Charakterisierungs-
   und E2E-Ebene wissen Sie nicht, ob ein grüner generierter Test etwas
   Richtiges festhält.
2. **Erst migrieren, wo es die Testbarkeit blockiert.** Feldinjektion und
   verkettetes SQL sind keine Stilfragen, sondern Testkosten pro Klasse.
3. **Messen Sie mit Mutation-Score, nicht mit Coverage.** Dafür liegt der
   Datenhalter als Negativkontrolle im Versuch. Phase A hat die Erwartung nur zur
   Hälfte bestätigt: hohe Coverage ja, aber die Mutanten wurden alle erlegt. Was
   sich zeigte, ist die *Ausbeute pro Zeile* — 12 Mutanten auf 34 abgedeckten
   Zeilen beim Datenhalter gegen 17 Mutanten auf 19 Zeilen beim Controller. Wir
   berichten das wie gemessen, nicht wie erwartet.
4. **Rechnen Sie mit Reparatur.** Die interessante Zahl ist nicht „schreibt die
   KI Tests?", sondern „wie viel kostet der Weg von generiert zu brauchbar, und
   in welchen Kategorien?". Gemessen: 52 Eingriffe über elf Zellen, Schwerpunkt
   Import/Syntax und Mock-Aufbau — der Aufwand hängt an der **Mockbarkeit der
   Abhängigkeiten**, nicht an der Zeilenzahl.
5. **Fragen Sie nach dem Nenner.** Jede KI-Erfolgsquote, die nur über die geglückten
   Läufe gerechnet ist, ist wertlos. Unsere eigenen Phase-A-Zahlen waren genau das,
   und erst die vollständige Messung hat es gezeigt.
6. **Kaufen Sie keine Testmengen.** 134 Testmethoden fanden hier exakt so viele Fehler
   wie 13 — und kosten dauerhaft das Zehnfache an Pflege.

## Aufwand

| Position | Wert | Art |
|---|---|---|
| Vorbereitung G6 (Protokoll-Finalisierung, zwei Testbed-Module, Harness, Dry-Run, ArchUnit + Testcontainers, Doku) | siehe `docs/worklog.md`, Session 11 | **Messwert** (Agenten-Wall-Time unter Aufsicht) |
| Generierung + Phase-A-Messung | siehe `docs/worklog.md`, Session 12 | **Messwert** |
| Phase-B-Reparatur, Messung, Report, Übernahme | siehe `docs/worklog.md`, Session 13 | **Messwert** |
| API-Kosten des gesamten Versuchs | **€ 0,65** | **Messwert** (24 Aufrufe, Preistabelle vorab gepinnt) |
| Reparaturaufwand je Zelle | **nicht belastbar** — siehe oben und Protokoll-Nachtrag A4 | verworfen |

*Die Etikettierung folgt der Regel aus `playbook/README.md`: Messwerte sind
Agenten-Wall-Time unter Aufsicht, Feldwerte sind gekennzeichnete
Erfahrungsschätzungen. Vermischt wird nichts.*
