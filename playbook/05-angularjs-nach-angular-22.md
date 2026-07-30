# Kapitel 5 — AngularJS → Angular 22: Strangler Fig, Route für Route

*Etappe: `stage-5-angular` · Ausgangspunkt: `stage-4-boot-4x`*

## Ausgangslage

Das Backend steht auf Boot 4.1 / Java 25 — das Frontend ist noch das von 2016:
AngularJS 1.8 (End of Life seit Januar 2022), `$scope`-Controller, dazu die
servergerenderte JSP-Adminseite. Das ist bei echten KMU-Systemen der Normalfall:
das Backend wird gepflegt, das Frontend „läuft ja“. Es ist auch die teuerste
Etappe — AngularJS → Angular ist **kein Upgrade, sondern ein Framework-Wechsel**
(andere Architektur, andere Templates, anderes Tooling).

Der Anspruch dieser Etappe ist das Kernversprechen des ganzen Repos: **dieselben
E2E-Szenarien laufen grün gegen die alte UND die neue Oberfläche** — bewiesene
funktionale Äquivalenz statt „sieht gleich aus“.

## Vorgehen: zwei SPAs auf einem Origin, die URL ist die Naht

Wir haben den Strangler Fig so geschnitten:

1. **Die neue Angular-App übernimmt sofort `/` mit Pfad-Routen** (`/kunden`,
   `/auftraege/17`, …). Die alte AngularJS-App bleibt vollständig funktionsfähig
   unter `/alt.html#!/…` — ihr Hash-Routing macht das gratis möglich. **Das
   URL-Schema selbst ist die Grenze zwischen Alt und Neu.**
2. **Eine Route wandert pro Schritt** (Dashboard → Kunden → Fahrzeuge →
   Aufträge → Rechnungen → Bericht → Admin). Pro Schritt: Angular-Komponente
   bauen, Selektor-Map-Werte der Seite umstellen, **altes View/Controller
   löschen**, Navigation in beiden Shells umhängen, Netz laufen lassen, Commit.
   Der Git-Verlauf IST die Hybrid-Dokumentation.
3. **Übergaben in beide Richtungen sind komplette Seitenwechsel.** Die
   Angular-App leitet unbekannte Pfade auf `/alt.html#!<pfad>` um; die alte App
   führt eine Liste „portierter Routen“ und gibt sie per `window.location` ab.
   Beide Navigationsleisten tragen **byte-identische href-Werte**, damit die
   Selektor-Map auf beiden Shells greift, egal wo ein Ablauf gerade steht.
4. **Flows dürfen die Grenze mitten im Szenario queren.** In der Hybrid-Phase
   erzeugte z. B. das (schon portierte) Angular-Auftragsdetail eine Rechnung
   und landete auf dem (noch alten) AngularJS-Rechnungsblatt — grün, im selben
   Selenium-Szenario. Das ist der eigentliche Beweis, dass die Naht trägt.

**Warum kein ngUpgrade?** `@angular/upgrade` existiert auch für Angular 22
(am 2026-07-31 live geprüft) und kann beide Frameworks **in einer Seite**
mischen. Der Preis: doppelte Change Detection, AngularJS im Angular-Build,
`$injector`-Brücken — Werkzeug für große Apps, deren Komponenten sich eine
Seite teilen müssen. Bei zehn Views mit sauberen Routengrenzen ist die
URL-Naht schlicht billiger und jederzeit rückbaubar. **Entscheidungsregel:
ngUpgrade erst, wenn einzelne Seiten zu groß zum Portieren am Stück sind.**

### Das Sicherheitsnetz bei einem Framework-Wechsel

Die Investition aus Etappe 1 zahlt hier am stärksten aus — **kein einziges
Szenario wurde neu geschrieben**. Konkret bestand die Portierung der Testseite
aus drei Dingen:

- **Selektor-Map v2:** dieselben ~90 Intent-Keys, neue Werte. Die neue UI trägt
  `data-testid`-Anker (entkoppelt von Styling und Struktur) statt der
  positionsbasierten 2016er-Selektoren. Ein neuer Paritäts-Test macht aus
  einem vergessenen Key einen roten Build statt eines rätselhaften
  Szenario-Abbruchs.
- **Wait-Strategie pro Ziel-UI:** Das „keine offenen HTTP-Requests“-Gate der
  Suite pollte bisher `$http.pendingRequests`. Die neue App ist **zoneless** —
  die klassische Angular-Testability hat dort nichts zu beobachten. Statt
  dessen führt die App selbst einen Pending-Request-Zähler (ein
  HTTP-Interceptor, 15 Zeilen) als **Testbarkeits-Vertrag**. In der
  Hybrid-Phase entscheidet eine `hybrid`-Strategie pro Seite, welches Framework
  gerade antwortet; unbekannte Werte werfen — bewusst laut.
- **Ein sanktionierter Erwartungswert pro Stand:** siehe SD-3 unten.

### Formatierung ist Vertrag

`1439,00 €`, `08.07.2026`, exakte Alert-Texte: Die Suite pinnt die sichtbare
Formatierung byte-genau. Deshalb rechnet in der neuen App eine `EuroPipe`
**exakt wie der handgestrickte 2016er-Filter** (Komma, kein Tausenderpunkt,
angehängtes €) — Angulars `CurrencyPipe` hätte anders formatiert und wäre eine
stille Abweichung gewesen. Gleiches gilt für `alert()`/`confirm()`: die neue UI
behält die Dialoge bei. **Äquivalenz zuerst; UX-Modernisierung ist ein eigenes
Projekt mit eigenem Budget** — sonst wird aus der Migration unbemerkt ein
Redesign.

## Sanktionierte Abweichungen dieser Etappe (ADR-0004)

- **SD-2 — Adminseite absorbiert:** Die JSP-Seite ist als SPA-Route `/admin`
  neu entstanden; `GET /api/admin/statistik` liefert die Kennzahlen,
  **`POST /admin/bereinigen` behält Pfad, Status und die exakte deutsche
  Meldung** (der gepinnte Vertrag läuft unverändert auf beiden Ständen). Mit
  der JSP starben `tomcat-jasper`, JSTL und gson — und damit der letzte Grund
  für WAR-Packaging: die App ist jetzt ein normales executable JAR.
- **SD-3 — der „undefined“-Alert:** Die alte UI zeigt bei serverseitig
  abgelehnten Aktionen wörtlich `undefined` an (Kapitel-1-Fund: Boot 1.5
  deklariert den String-Fehlerbody als JSON, AngularJS wirft `$http:baddata`).
  Die neue UI zeigt die echte deutsche Servermeldung. Beide Verhalten sind
  gepinnt — der Erwartungswert steht **pro Stand in der Selektor-Map**, der
  HTTP-Vertrag selbst (identisch auf beiden Ständen) in der
  Charakterisierungs-Suite.

## Stolperfallen

1. **Zoneless Change Detection straft stillen Zustand.** Ein
   `this.kunde = antwort` in einem Subscribe-Callback rendert **nicht** —
   niemand stößt die Change Detection an. Der Fehler war acht Läufe lang grün,
   weil ein paralleles Signal-Update zufällig ein Rendering hinterherschob, und
   flog erst beim neunten Lauf auf (Race verloren, Heading blieb leer).
   **Regel: Zustand, den ein asynchroner Callback ersetzt, gehört in ein
   Signal.** Gefunden hat es — wieder — das Netz, nicht der Compiler.
2. **Der Formatierungs-Vertrag versteckt sich in Filtern.** Wer den
   2016er-`euro`-Filter „idiomatisch“ durch `CurrencyPipe` ersetzt, ändert
   stillschweigend die Anzeige aller Beträge. Erst pinnen, dann portieren.
3. **Lint kommt mit dem Toolchain-Wechsel gratis — und findet sofort etwas.**
   Die Angular-Toolchain brachte eslint/prettier mit; die
   Accessibility-Regel schlug auf allen geerbten `<label>`-Elementen ohne
   `for`-Zuordnung an (2016er-Markup). 19 echte, kleine Funde. Im selben Zug
   wurde google-java-format auf alle Nicht-Exponat-Module gelegt — **eine
   Formatierungs-Story, einmal** (`legacy/` bleibt unangetastet).
4. **Selects sind nicht gleich Selects.** `ng-options` mit `track by` wird in
   Angular zu `@for` + `[ngValue]` — wer hier `value` (String) statt
   `[ngValue]` (Objekt/Zahl) nimmt, bekommt stille Typ-Drift im Payload. Die
   Charakterisierungs-Suite hätte es gefangen; billiger ist, es gar nicht erst
   zu bauen.

## Aufwand (ehrlich)

Sitzungssumme gemessen als Agent-Wandzeit unter Aufsicht (Worklog Session 9);
die Aufteilung ist eine **gekennzeichnete Schätzung** über die gemessene Summe:

| Schritt | Anteil (geschätzt) |
|---|---|
| Shell, Build-Integration (frontend-maven-plugin, Docker), Hybrid-Mechanik, Wait-Strategien | ~30 % |
| Sechs Routen-Slices portieren (Komponente + Map + Rückbau alt, je grün) | ~45 % |
| Adminseite absorbieren (SD-2), Cutover WAR→JAR | ~10 % |
| Lint/Format-Gates + Zoneless-Bugfix | ~15 % |

Feldwert aus Erfahrung: Bei realen Systemen skaliert diese Etappe grob mit der
**Zahl unterschiedlicher View-Muster**, nicht mit der Zahl der Views — die
zehnte Tabelle kostet fast nichts mehr, das erste Inline-Formular kostet.

## Entscheidungsregeln fürs eigene Projekt

- **Niemals Big Bang.** Route für Route, jeder Schritt einzeln grün und einzeln
  rückbaubar. Wenn das Projekt abbricht, hinterlässt es einen funktionierenden
  Mischbetrieb statt einer Baustelle.
- **ngUpgrade nur bei seiteninternem Mischbedarf** — Routengrenzen sind der
  billigere Schnitt (siehe oben).
- **Erst die Testbarkeit der neuen UI bauen** (stabile Testanker, ein
  Idle-Vertrag), dann portieren. Eine neue UI ohne Wait-Kontrakt reißt die
  Flakiness wieder auf, die Etappe 1 mühsam beseitigt hat.
- **Die alte App stirbt mit, nicht danach.** Jeder Slice löscht sein altes
  View/Controller sofort — sonst entsteht der Zombie-Zustand „zwei Wahrheiten
  pro Seite“, den niemand mehr abbaut.
- **Server-Fallbacks nicht vergessen:** Pfad-Routing braucht ein
  Forward-auf-die-Shell für jede SPA-Route (Deep-Links, Reload) — die Liste
  wächst pro Slice mit und ist am Ende die Routentabelle des Servers.
