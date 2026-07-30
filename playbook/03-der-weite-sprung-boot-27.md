# Kapitel 3 — Der weite Sprung: Spring Boot 1.5 → 2.7 (+ Java 17)

*Etappe: `stage-3-boot-2.7` · Ausgangspunkt: `stage-2-jdk-build`*

## Ausgangslage

Spring Boot 1.5 (EOL 2019) → 2.7 (letzte 2er-Linie) ist der gefürchtete Sprung:
fünf Major-/Minor-Generationen Autokonfigurations-Änderungen auf einmal. Wir
sind ihn **direkt** gegangen, ohne Zwischenversionen — die Entscheidungsregel
dafür steht am Ende des Kapitels. Huckepack dazu: **Java 8 → 17**, denn mit
Boot 2.7 ist die JDK-Decke von Kapitel 2 weg.

## Vorgehen

Version anheben, bauen, Netz laufen lassen, jeden Bruch dokumentieren,
reparieren, wiederholen. Keine Vorab-Spekulation — das Netz findet, was real
bricht. Bei WerkstattCRM waren es **genau drei Brüche:**

**Bruch 1 — Compile: `SpringBootServletInitializer` ist umgezogen.**
`org.springframework.boot.web.support` → `…web.servlet.support` (seit Boot 2.0).
Eine Import-Zeile. Trivial, aber er stoppt jeden WAR-basierten Build sofort.

**Bruch 2 — Start: alte Version-Pins treffen neue Autokonfiguration.**
Der 2015 gepinnte gson 2.3.1 (nur für die JSP-Adminseite!) ließ den Kontext
nicht mehr hochfahren: Boot 2.7s `GsonAutoConfiguration` ruft
`GsonBuilder.setLenient()` — das gibt es in 2.3.1 nicht (`NoSuchMethodError`).
*Lektion:* Jede hart gepinnte Bibliothek ist ein Kandidat für genau diesen
Fehlertyp. *Behebung:* Pin raus, Version ans Boot-BOM übergeben.

**Bruch 3 — der unsichtbare: das API-Format driftet.** Unser einziges
`DATE`-Feld (`pickerlDatum`) kam unter Boot 1.5/Jackson 2.8 als `"2027-04-30"`
über die Leitung, unter Boot 2.7/Jackson 2.13 plötzlich als Epoch-Zahl
`1809043200000` (geändertes `java.sql.Date`-Verhalten seit Jackson 2.9).
**Die Oberfläche verbarg es** — der AngularJS-Datumsfilter schluckt beide
Formate, alle 13 Selenium-Tests grün. **Nur der Golden Master schlug an.**
Genau für diesen Fall gibt es die zweite Netz-Schicht: UI-Tests beweisen
Abläufe, nicht Verträge. *Behebung:* explizite Wire-Format-Kompatibilität
(`configOverride(java.sql.Date.class)`), dokumentiert als bewusste
Vertrags-Entscheidung — nicht stillschweigend Golden Master «nachziehen».

**Was NICHT brach — ebenso ehrlich:** JdbcTemplate-API, JSP/JSTL (Boot 2.7 ist
noch `javax`-Welt), Property-Namen, PostgreSQL-Treiber (BOM-Hebung), die
Java-17-Kompilierung des 2016er-Codes. Und vor allem: Diese Anwendung nutzt
weder Spring Security noch Actuator — **die beiden größten Bruchtreiber dieses
Sprungs kamen hier gar nicht vor.** Reale Systeme mit
`WebSecurityConfigurerAdapter`, Actuator-Endpoints oder tiefem
Hibernate-Custom haben an dieser Stelle die meiste Arbeit; unsere Zahl unten
skaliert dafür nicht linear.

## Stolperfallen

- **«UI-Tests grün» heißt nicht «Verhalten gleich».** Bruch 3 wäre mit reinen
  Selenium-Tests unbemerkt in Produktion gegangen — und hätte jeden
  API-Konsumenten außerhalb der eigenen Oberfläche getroffen.
- **Gepinnte Uralt-Versionen** überleben den Framework-Sprung fast nie. Vor dem
  Sprung: Pin-Inventur (Kapitel 2), jede Position entscheiden.
- **Golden Master nie beiläufig aktualisieren.** Ein abweichender Capture ist
  ein Befund, kein Ärgernis. Erst entscheiden (Vertrag halten oder bewusst
  ändern + ADR), dann handeln.

## Aufwand

| Posten | Stunden |
|---|---:|
| Versionssprung, drei Brüche finden + beheben (Netz-getrieben) | 0,3 |
| Doku (dieses Kapitel, Stage-Log, Worklog) | 0,1 |
| **Summe Etappe 3 (gemessen, KI-gestützt, Laborbedingungen)** | **≈ 0,4** |

Feldwert: Für gewachsene Systeme mit Security/Actuator/Hibernate-Tiefe ist
dieser Sprung der teuerste der ganzen Strecke — **1–3 Personenwochen** sind
realistisch. Der Unterschied zum Blindflug: Mit Netz ist es planbare Arbeit,
ohne Netz ist es Risiko-Roulette.

## Entscheidungsregeln

- **Direktsprung 1.5 → 2.7** ist richtig, wenn: Sicherheitsnetz vorhanden,
  wenig Security-/Actuator-Customizing, Abhängigkeits-Pins inventarisiert.
  **Zwischenschritte** (1.5 → 2.0/2.3 → 2.7) lohnen, wenn tiefes
  Framework-Customizing jede Generation einzeln bricht — sonst bezahlt man
  jede Zwischenstation doppelt.
- **Der API-Vertrag ist Teil der funktionalen Gleichwertigkeit.** Wire-Format
  halten, bis eine Etappe es bewusst ändert (ADR + Golden-Master-Update in
  einem Commit).
- **JDK-Hebung immer huckepack** auf den Framework-Sprung, der sie freigibt —
  nie als eigene Etappe erzwingen (Kapitel 2).
