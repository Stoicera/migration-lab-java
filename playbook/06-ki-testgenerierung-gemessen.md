# Kapitel 6 — KI-Testgenerierung, gemessen statt geglaubt

*Meilenstein: G6 · Protokoll eingefroren: `ai-testgen-protocol-v1` (2026-07-31)*

> **Status dieses Kapitels:** Der Teil, den Sie unten lesen, ist **vor** der ersten
> Modell-Anfrage geschrieben — das ist der ganze Punkt. Die Ergebnisse (Zahlen,
> Tabellen, Kosten) folgen nach der Durchführung in `ai-testgen/REPORT.md` und
> werden hier zusammengefasst. Sollte das Ergebnis unbequem ausfallen, steht es
> trotzdem hier: Ein Protokoll, das man nach dem Ergebnis ändert, ist Werbung.

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
| Coverage-Gate | Als **Ratsche** scharf gestellt: gepinnt auf das, was die erste Suite tatsächlich erreicht (37,3 % Line, gemessen 2026-07-31), damit Coverage nicht fallen kann. Die 80 % des Standards kommen mit den übernommenen Tests aus dem Experiment — eine Zahl, die nie gemessen wurde, ist Dekoration. |

Merksatz für Ihr Projekt: **Ein Gate, das Sie nicht erreichen können, schalten
Sie nicht „später" scharf — Sie schalten es als Ratsche scharf und ziehen sie
hoch.** Alles andere endet in `-DskipTests`.

## Entscheidungsregeln (Zwischenstand, vor den Ergebnissen)

1. **Kein KI-Test ohne Netz darunter.** Generierte Tests zementieren das
   Verhalten, das sie vorfinden — inklusive Fehler. Ohne Charakterisierungs-
   und E2E-Ebene wissen Sie nicht, ob ein grüner generierter Test etwas
   Richtiges festhält.
2. **Erst migrieren, wo es die Testbarkeit blockiert.** Feldinjektion und
   verkettetes SQL sind keine Stilfragen, sondern Testkosten pro Klasse.
3. **Messen Sie mit Mutation-Score, nicht mit Coverage.** Deshalb liegt der
   Datenhalter als Negativkontrolle im Versuch: Er wird hohe Coverage und
   niedrigen Mutationswert zeigen — genau das Muster, das Coverage-Reports in
   Angeboten so gefährlich macht.
4. **Rechnen Sie mit Reparatur.** Die interessante Zahl ist nicht „schreibt die
   KI Tests?", sondern „wie viel kostet der Weg von generiert zu brauchbar, und
   in welchen Kategorien?".

## Aufwand

| Position | Wert | Art |
|---|---|---|
| Vorbereitung G6 (Protokoll-Finalisierung, zwei Testbed-Module, Harness, Dry-Run, ArchUnit + Testcontainers, Doku) | siehe `docs/worklog.md`, Session 11 | **Messwert** (Agenten-Wall-Time unter Aufsicht) |
| Durchführung + Report | folgt | — |

*Die Etikettierung folgt der Regel aus `playbook/README.md`: Messwerte sind
Agenten-Wall-Time unter Aufsicht, Feldwerte sind gekennzeichnete
Erfahrungsschätzungen. Vermischt wird nichts.*
