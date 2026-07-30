# Kapitel 4 — Boot 3 & 4, Java 25 — und was OpenRewrite wirklich leistet

*Etappe: `stage-4-boot-4x` · Ausgangspunkt: `stage-3-boot-2.7`*

## Ausgangslage

Boot 2.7 (Java 17) ist erreicht. Vor uns liegen zwei Sprünge auf einmal:
**2.7 → 3.5** (der `javax` → `jakarta`-Umstieg) und **3.5 → 4.1** (Jakarta EE
11, Jackson 3), dazu **Java 17 → 25**. Erstmals gibt es dafür Werkzeuge:
OpenRewrite-Rezepte versprechen automatisierte Migration. Dieses Kapitel misst,
was sie halten — die Antwort auf die häufigste Kundenfrage: *«Kann das nicht
ein Tool machen?»*

## Vorgehen

Pro Sprung: Rezept laufen lassen → **Diff Zeile für Zeile prüfen** → bauen →
Netz laufen lassen → Lücken von Hand schließen → dokumentieren.

### Die OpenRewrite-Bilanz (gemessen, nicht geschätzt)

**Was die Rezepte erledigt haben — echter Zeitgewinn bei Mechanik:**

| Rezept | Ergebnis |
|---|---|
| `boot3.UpgradeSpringBoot_3_5` | Parent 2.7.18 → 3.5.16 ✔ |
| `boot4.UpgradeSpringBoot_4_0` | Parent → 4.0.7 ✔ · Starter `web` → `webmvc` ✔ · JSP-Taglib-URI → `jakarta.tags.core` ✔ · Property-Schlüssel `spring.jackson.serialization.*` → `spring.jackson.datatype.datetime.*` ✔ |
| `boot4.SpringBootProperties_4_1` | keine Änderungen nötig |
| **Ein vollständiges `UpgradeSpringBoot_4_1` gibt es (noch) nicht** | 4.1-Sprung von Hand |

**Was die Rezepte übersehen haben — und was es gekostet hat:**

1. **Die JSTL-Falle — der teuerste Fund der Etappe.** Statt `javax.servlet:jstl`
   auf die Jakarta-Artefakte zu migrieren, hat das Boot-3-Rezept die
   **javax-Version von 2009 festgepinnt.** Ergebnis: Build grün, Anwendung
   startet, REST-API funktioniert — und die JSP-Adminseite stirbt zur Laufzeit
   mit `ClassNotFoundException: javax.servlet.jsp.tagext.TagLibraryValidator`.
   **«Kompiliert» heißt nicht «funktioniert».** Gefunden hat es nicht der
   Compiler, sondern der Golden Master der Adminseite.
2. **Jackson 3.** Boot 4 wechselt auf Jackson 3 (`tools.jackson`);
   `Jackson2ObjectMapperBuilderCustomizer` → `JsonMapperBuilderCustomizer` mit
   anderer Builder-API. Kein Rezept fasste unseren Kompatibilitäts-Shim aus
   Kapitel 3 an — der einzige Compile-Bruch des 4er-Sprungs, Handarbeit.
3. **Nichts Strukturelles** — wie erwartet: Rezepte migrieren APIs, sie
   verbessern keine Entwürfe.

Die Entscheidung dazu steht in [ADR-0002](../docs/adr/0002-openrewrite-as-assistant-not-autopilot.md):
**Assistent, nicht Autopilot.**

### Was wir in dieser Etappe bewusst von Hand gemacht haben

- **Konstruktor-Injektion durchgezogen** (alle sechs Controller + Service).
  Migrationszweck: Feldinjektion ist der Grund, warum der Bestand nicht
  testbar war — ohne diesen Schritt läuft das KI-Testgenerierungs-Experiment
  (G6) gegen eine Wand.
- **Die Sicherheitslücke geschlossen** (LEGACY_NOTES B4): Die Kundensuche
  verkettete Benutzereingaben direkt ins SQL. Belegbar, nebeneinander:

  ```
  GET /api/kunden?suche=%' OR '1'='1
  Legacy (Port 8080):  10 Kunden   ← komplette Kundendatei ausgelesen
  Modern (Port 8090):   0 Kunden   ← parametrisiert, Angriff wirkungslos
  Legitime Suche "hofer" auf beiden Ständen identisch  ← Golden Master grün
  ```

  Genau so gehört ein Sicherheitsfund erzählt: reproduzierbar, mit Beweis,
  ohne Verhaltensänderung für legitime Nutzung. Ebenso parametrisiert:
  der Status-Filter der Auftragsliste.

## Stolperfallen

- **Der grüne Build als Trugschluss.** Der gefährlichste Zustand dieser Etappe
  war «alles kompiliert, Anwendung startet» — bei kaputter JSP-Seite. Ohne die
  zweite Netz-Schicht wäre das in Produktion gegangen.
- **Zwei Sprünge gleichzeitig prüfen.** Wir haben 3.5 vollständig verifiziert,
  bevor 4.1 begann. Wer beide Sprünge in einen Schritt legt, kann Ursachen
  nicht mehr trennen.
- **Rezept-Diffs blind committen.** Der JSTL-Pin sah im Diff harmlos aus
  («fügt Dependency-Management hinzu»). Zeile für Zeile lesen.

## Aufwand

| Posten | Stunden |
|---|---:|
| Rezepte 3.5/4.0/4.1 inkl. Diff-Prüfung und Bilanz | 0,25 |
| Manuelle Lücken (Jakarta-JSTL, Jackson 3, 4.1-Bump, Java 25) | 0,3 |
| Konstruktor-Injektion + Sicherheitsfix + Verifikation | 0,25 |
| Doku (Kapitel, ADR-0002, Stage-Log) | 0,2 |
| **Summe Etappe 4 (gemessen, KI-gestützt, Laborbedingungen)** | **≈ 1,0** |

Feldwert: **2–4 Personenwochen** für ein gewachsenes System — der Treiber ist
nicht der Parent-Bump, sondern `javax`→`jakarta` in jeder Bibliothek,
Spring-Security-Konfiguration (bei uns nicht vorhanden!) und die Frage, welche
Abhängigkeit überhaupt eine Jakarta-Version hat.

## Entscheidungsregeln

- **OpenRewrite ja — überwacht.** Rezepte in eigenem Commit, Diff Zeile für
  Zeile prüfen, Netz entscheiden lassen. Jeder Rezept-Schritt, der eine
  Alt-Bibliothek *festpinnt* statt sie zu migrieren, ist ein Defekt.
- **«Build grün» ist kein Migrationsbeweis.** Beweis ist: Laufzeit, API-Vertrag
  und Oberfläche — pro Commit.
- **Refactoring nur mit Migrationszweck.** Konstruktor-Injektion ja (Testbarkeit
  ist das nächste Etappenziel), God-Klasse zerschlagen nein (kein Migrationsbedarf,
  die Anwendung läuft) — sie bleibt bewusst als Studienobjekt für G6 stehen.
- **Sicherheitslücken in der Etappe beheben, in der man den Code ohnehin anfasst**
  — mit Vorher/Nachher-Beleg und grünem Netz für legitimes Verhalten.
