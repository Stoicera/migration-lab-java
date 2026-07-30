# Kapitel 2 — Fundament: Build, Abhängigkeiten und die JDK-Frage

*Etappe: `stage-2-jdk-build` · Ausgangspunkt: `stage-1-safety-net`*

## Ausgangslage

Das Sicherheitsnetz steht. Jetzt — und erst jetzt — beginnt die eigentliche
Migration. Der moderne Stand (`modern/`) startet als **originalgetreue Kopie**
des Altsystems und läuft parallel dazu (Ports 8090/5434 statt 8080/5433).
Das Altsystem bleibt eingefroren als Referenz; jeder Migrationsschritt ist
damit jederzeit gegen das Original vergleichbar.

## Vorgehen

**1. Abhängigkeits-Audit — erst wissen, dann ändern.** Der Kassasturz des
Bestands, mit Entscheid pro Position:

| Abhängigkeit | Befund | Entscheid |
|---|---|---|
| Spring Boot 1.5.22 | EOL seit 08/2019, keine Security-Fixes | Migrationspfad 2.7 → 3.x → 4.1 (Etappen 3–4) |
| log4j 1.2.17 | **EOL seit 2015**, bekannte CVE-Klassen | **Jetzt raus** → SLF4J/Logback (Boot-Standard) |
| commons-lang 2.6 | deklariert, **nirgends verwendet** | **Jetzt raus** — toter Ballast |
| gson 2.3.1 | nur von der JSP-Adminseite verwendet | Bleibt bis Etappe 5 — stirbt mit der JSP-Seite |
| PostgreSQL-Treiber (BOM-verwaltet) | uralt | Hebt sich automatisch mit den Boot-Sprüngen |
| AngularJS 1.8.2 | EOL seit 01/2022 | Etappe 5 (Strangler Fig) |

**2. Logging-Vereinheitlichung.** log4j-1.2-API und -Konfiguration entfernt,
`System.out`-Reste auf den Logger umgezogen, Boot-Standard-Logging aktiviert.
Kleiner Schritt, aber er beseitigt die älteste Zeitbombe im Stack.

**3. Die JDK-Frage — und die Antwort, die viele überrascht.** Der Reflex lautet
«zuerst auf ein aktuelles Java heben». **Geht hier nicht:** Spring Boot 1.5
läuft nicht auf Java 9+ (interne JDK-APIs, altes CGLIB/ASM). Die Decke bestimmt
das Framework, nicht der Wunsch. Also: **Framework vor JDK** — die Java-Hebung
landet in Etappe 3, huckepack auf Boot 2.7.

**4. Der Äquivalenz-Beweis.** Dieselben Golden Master (17 Prüfungen) und
dieselbe Selenium-Suite (13 Tests) laufen ab jetzt **pro Commit auch gegen den
modernen Stand** — in dieser Etappe: alles grün, die Bereinigung hat das
beobachtbare Verhalten nachweislich nicht verändert. Genau dafür wurde das
Netz in Etappe 1 gebaut.

## Stolperfallen

- **Die Beautification-Falle.** Wer den Code beim Kopieren «gleich ein bisschen
  aufräumt», verbrennt Budget und verwässert den Äquivalenz-Beweis. Field
  Injection, God-Klasse, SQL-Verkettung — alles bleibt bewusst stehen, bis
  seine Etappe kommt. Entfernt wurde nur, was null beobachtbare Wirkung hat
  (ungenutzte Abhängigkeit, toter Code) oder direktes Etappenziel war (Logging).
- **Parallelbetrieb braucht saubere Trennung:** eigene Ports, eigenes
  DB-Volume, eigener Compose-Stack. Sonst testet man versehentlich gegen den
  falschen Stand.
- **JDK-Reihenfolge falsch geplant** = Wochen Verlust. Erst die
  Framework-Kompatibilitätsmatrix lesen, dann den Fahrplan schreiben.

## Aufwand

| Posten | Stunden |
|---|---:|
| Kopie, Ports/Volumes, Compose-Parallelbetrieb | 0,1 |
| Audit + Logging-Vereinheitlichung + Aufräumen | 0,1 |
| CI-Äquivalenz-Gate (Charakterisierung + E2E gegen 8090) | 0,1 |
| **Summe Etappe 2** | **≈ 0,3** |

Lesart der Zahlen: gemessene Agent-Wall-Time, Aufteilung geschätzt — siehe die
verbindliche Erklärung in Kapitel 1 („Wie diese Zahl zu lesen ist") und README
(„How this was built"). Feldwert für ein reales System dieser Größe (~1.700
Zeilen Backend, 25 Endpunkte — hier ist gemeint: *plus* gewachsene, unbekannte
Abhängigkeitsbäume): **1–2 Personentage** — eine **Erfahrungsschätzung, nicht
hier gemessen**; der Treiber ist das Audit (Lizenz- und CVE-Recherche).

## Entscheidungsregeln

- **Framework vor JDK.** Die Boot-Version bestimmt die Java-Decke. Fahrplan
  immer von der Kompatibilitätsmatrix ableiten, nie vom Wunschdenken.
- **Nur entfernen, was beweisbar folgenlos ist** (ungenutzt, tot) oder direktes
  Etappenziel. Alles andere: katalogisieren, stehen lassen, Etappe zuweisen.
- **Der alte Stand bleibt lauffähig stehen.** Parallelbetrieb macht jeden
  Vergleich zur Ein-Kommando-Übung und nimmt der Migration die Angst.
