# LinkedIn — Entwürfe (DE, Sie)

> **Status: ENTWURF (15.10.2026). Nicht gepostet.** Jeder Gründer postet selbst von seinem Profil.
> Regeln: erste Zeile trägt eine Zahl oder einen Befund; der Link steht im **ersten Kommentar**,
> nicht im Post; höchstens drei Hashtags. Zahlen am Posting-Tag gegen
> [checklist.md](checklist.md) prüfen.

## Post 1 — Sebastian Kern (Launch-Tag)

> 47 Tests, zwei Anwendungen, zehn Jahre dazwischen — und bei jedem Commit dasselbe Ergebnis.
>
> Viele Betriebe arbeiten mit Web-Anwendungen aus der Zeit um 2016: Java 8, Spring Boot 1.x,
> AngularJS. Die Modernisierung wird verschoben, weil niemand vollständig weiß, was die alte
> Anwendung tut — und deshalb auch niemand merken würde, wenn die neue es anders tut.
>
> Wir haben gezeigt, wie man das absichert, und zwar öffentlich: migration-lab ist eine
> im Stil von 2016 nachgebaute Anwendung, in sechs Etappen auf Java 25, Spring Boot 4.1 und Angular 22
> gehoben. Der alte und der neue Stand laufen nebeneinander. Vor der ersten Migrationszeile
> entstand das Sicherheitsnetz: 47 Charakterisierungstests, die Antworten und Datenbankzustand
> der alten Anwendung festhalten, und 34 Selenium-Szenarien. Dieselben Suiten laufen gegen beide
> Stände; die letzten Läufe vom 15.10.2026 sind auf beiden grün.
>
> Was das Netz gefunden hat, hätte eine Oberfläche nie gezeigt: Beim Sprung auf Spring Boot 2.7
> änderte sich das Format eines Datumsfelds in der Schnittstelle. Die Masken sahen richtig aus,
> ein Fremdsystem wäre gebrochen.
>
> Was das Projekt nicht ist: kein Werkzeug, das man auf den eigenen Code ansetzt, und keine
> Abkürzung. Die Anwendung ist bewusst klein. Schneller wurde sie auch nicht — das steht mit den
> Messwerten im Repository.
>
> Wenn Sie eine solche Anwendung betreiben: Welche Frage müsste ein Migrationsplan für Sie
> beantworten, bevor Sie ihm trauen?
>
> #Java #SpringBoot #Softwaremodernisierung

**Erster Kommentar:**

> Repository (Apache-2.0), mit Playbook auf Deutsch:
> https://github.com/Stoicera/migration-lab-java
> Fallstudie: https://stoicera.com/werk/migration-lab-java/

**Bild:** das Kernbildpaar `docs/screenshots/legacy-kunden-live.png` und
`modern-kunden-live.png` (17.08.2026, beide mit Adressleiste), nebeneinander, gleich groß.
Bildunterschrift: „Fast gleich — genau das ist der Beweis.“

## Post 2 — Raphael Lugmayr (am selben Tag, Nachmittag)

Raphael hat am Projekt nicht gearbeitet (Fallstudie 18 §3: verantwortlich Sebastian Kern). Der
Post teilt deshalb und sagt, warum es ihn als Java-Entwickler interessiert, ohne Urheberschaft
zu beanspruchen.

> 84 % Testabdeckung, 56 % Mutation Score — dieselbe Klasse.
>
> Mein Mitgründer Sebastian hat in unserem Migrationsprojekt KI-generierte Unit-Tests gemessen,
> nach einem Protokoll, das vor dem ersten Aufruf eingefroren wurde. Das Ergebnis, das mich als
> Java-Entwickler am meisten beschäftigt: Die größte Klasse erreichte nach der Reparatur 84,3 %
> Zeilenabdeckung, aber nur 55,9 % Mutation Score (gemessen 02.08.2026). Ein übliches 80-%-Gate
> hätte sie durchgewunken, obwohl fast jede zweite eingebaute Verhaltensänderung von keinem Test
> bemerkt wurde.
>
> Abdeckung misst, was ausgeführt wird. Nicht, was geprüft wird.
>
> Wichtig zur Einordnung: sechs Klassen je Codebasis, ein Versuch je Zelle — eine sauber
> gemessene Illustration, keine Studie. Protokoll, Rohdaten und Berichte liegen offen.
>
> Messen Sie in Ihren Projekten Mutation Score, oder reicht die Abdeckung als Gate?
>
> #Java #Softwaretests #Softwarequalität

**Erster Kommentar:**

> Bericht mit allen Zahlen:
> https://github.com/Stoicera/migration-lab-java/blob/master/ai-testgen/REPORT.md

**Bild:** `pit-07-80-prozent-falle.png` aus dem SSOT
(`assets/case-study-migration-lab/screenshots/`), Fallstudie 18 §10.

## Nicht verwenden

- Zugangsdaten des Legacy-Standes, Servernamen, Ausnutzungs-Beispiele der erhaltenen
  SQL-Injection.
- Den Screenshot der JSP-Adminseite.
- Stundenangaben oder „Migration in X Tagen“.
- Prozentwerte aus dem KI-Experiment ohne „in unserem Experiment“ und Datum.
- „KI-gebaut“ als Rahmung.
