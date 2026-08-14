# Kapitel 7 — Betrieb und Härtung: was ein grünes Netz nicht sieht

*Meilenstein: G7 · Etappe `stage-6-cloud-ops`: **begonnen, nicht abgeschlossen** — es gibt keinen Tag, die Begründung steht am Ende dieses Kapitels*

> *Nachtrag 2026-08-14: Die Etappe ist inzwischen abgeschlossen — das Deployment ist erfolgt
> und getaggt. Der Statusblock unten bleibt unverändert stehen, weil er die Lage zum
> Zeitpunkt seiner Niederschrift korrekt beschreibt; die Fortsetzung ist
> [Kapitel 8](08-der-live-gang-und-was-das-heisst.md).*

> **Status dieses Kapitels: die Härtung ist gebaut und gemessen — das Deployment ist es
> nicht.** Alles, was unten steht, läuft lokal in Docker Compose und wurde dort am
> 2026-08-05 gemessen. Es gibt **keinen Server, keine Domain, kein TLS, kein Backup**
> und keinen einzigen deployten Endpunkt. Deshalb ist die Etappe nicht getaggt und es
> gibt keine Version 1.0.0. Ein Betriebskapitel, das einen Betrieb behauptet, den es
> nicht gibt, wäre exakt die Glättung, gegen die dieses Repo argumentiert.

## Vorab: warum dieses Kapitel „7“ heißt und nicht „6“

`stages.md` hat das Betriebskapitel als „Kap. 6 — Betrieb & Cloud + Schlusskapitel“
angekündigt. Kapitel 6 ist aber längst vergeben — an die KI-Testgenerierung. Der Grund
ist strukturell: **G6 war ein Meilenstein ohne Etappe.** Die Regel aus `docs/SPEC.md`,
dass jedes Kapitel genau einer Etappe entspricht, ist dort gebrochen, wo ein Meilenstein
ein Kapitel verdient hat, ohne ein Migrationsschritt zu sein.

Aufgelöst wird das offen: Betrieb ist **Kapitel 7**, das Schlusskapitel wird Kapitel 8;
`stages.md` und `playbook/README.md` sagen das jetzt auch. Die billigere Variante wäre
gewesen, still umzunummerieren — bemerkt hätte es niemand. Genau deshalb steht es hier:
Wer die eigene Planungsspur nachträglich glättet, gewöhnt sich das Glätten an.

## Ausgangslage

Nach Kapitel 5 ist der moderne Stand **funktional** fertig: Boot 4.1, Java 25,
Angular 22, das Netz grün auf beiden Ständen (Charakterisierung 47/47, E2E 34/34).
Betriebsfähig ist er damit nicht. Er läuft auf **derselben PostgreSQL 9.6** wie das
Ausstellungsstück, sein Schema kommt aus Init-Skripten, die es zweimal gibt, sein
Healthcheck ist eine TCP-Probe, und alles ist offen: keine Authentisierung, keine
Sicherheits-Header, kein Rate-Limit. Das ist der Normalfall, in dem Migrationen ein
zweites Mal bestraft werden: **Die Anwendung ist migriert, die Betriebsumgebung nicht.**

Dieses Kapitel bearbeitet vier Baustellen — Datenbank, Startbereitschaft,
Schemaverwaltung, Absicherung — und misst zum Schluss, was die Modernisierung an
Geschwindigkeit gebracht hat. Die Antwort auf die letzte Frage ist unbequem und steht
trotzdem drin.

## PostgreSQL 9.6 → 18: zwei Fallen, eine davon lautlos

Der Legacy-Stand bleibt für immer auf 9.6 — er ist das Exponat. Bewegt wurde nur
`modern/`, auf **PostgreSQL 18.4 (Debian)**, Locale explizit gepinnt über
`LANG=en_US.utf8` und `POSTGRES_INITDB_ARGS=--locale=en_US.utf8` (ADR-0012).

### Falle 1 — das Image, das über seine eigene Sortierung die Unwahrheit sagt

Ein Sprung von 9.6 auf 18 bedroht vor allem eines: die **Sortierreihenfolge**. Sie
steckt in jeder Kundenliste, jedem Bericht, jeder Auswahlliste. Die naheliegende
Prüfung lautet: `pg_database.datcollate` auf beiden Ständen auslesen und vergleichen.
Wir haben stattdessen sortieren lassen — dieselben sieben Namen, drei Images:

| Image | meldet als `datcollate` | sortiert tatsächlich |
|---|---|---|
| `postgres:9.6` (Legacy, Referenz) | en_US.utf8 | de Vries \| Hubermann \| Huber Transporte GmbH \| Ohler \| Öhler \| van Dijk \| Zach |
| `postgres:18` (Debian) | en_US.utf8 | **identisch zu 9.6** |
| `postgres:18` (Debian), Locale gepinnt | en_US.utf8 | **identisch zu 9.6** |
| **`postgres:18-alpine`** | **en_US.utf8** | **Huber Transporte GmbH \| Hubermann \| Ohler \| Zach \| de Vries \| van Dijk \| Öhler** |

Das Alpine-Image **meldet eine Sortierung, die es nicht verwendet**: musl nimmt den
Locale-Namen entgegen und ignoriert ihn, sortiert also nach Bytes. Eine Review-Position
„Collation auf beiden Ständen vergleichen“ wäre grün durchgelaufen — während sich jede
sortierte Liste der Anwendung still umordnet. `de Vries` hinter `Zach`, `Öhler` ans
Ende: für einen Sachbearbeiter, der eine alphabetische Liste abarbeitet, ist das kein
Schönheitsfehler.

> **Merksatz, der weit über Datenbanken hinaus gilt:**
> **Ein Prüfschritt, der eine Einstellung *liest*, ist keine Prüfung.**
> Beweiskraft hat nur das Verhalten. Fragen Sie Ihre Checkliste bei jeder Zeile:
> *Lese ich hier eine Absichtserklärung — oder ein Ergebnis?*

Konsequenz im Repo: Der moderne Stand fährt `postgres:18`, ausdrücklich **nicht**
`-alpine`, und es gibt jetzt einen Test, der sortiert statt fragt —
`WerkstattServiceIntegrationTest.die_datenbank_sortiert_wie_der_legacy_stand_und_sagt_es_nicht_nur`.
Der Name ist absichtlich so lang.

### Falle 2 — der Tag-Bump, der still aufhört zu speichern

Zwischen 9.6 und 18 sind sowohl `PGDATA` als auch das deklarierte `VOLUME` des Images
umgezogen:

- `postgres:9.6` → `PGDATA=/var/lib/postgresql/data`, VOLUME `/var/lib/postgresql/data`
- `postgres:18` → `PGDATA=/var/lib/postgresql/18/docker`, VOLUME `/var/lib/postgresql`

Wer nur die Version im Compose-File hochzieht und den Mount
`- vol:/var/lib/postgresql/data` stehen lässt, bekommt: sauberen Start, einen
**frischen leeren Cluster in einem anonymen Volume** — und keinerlei Persistenz. In
diesem Repo fällt das zusätzlich nicht auf, weil Flyway bei jedem Start neu migriert:
Der Stand sieht nach jedem Neustart korrekt aus. **Datenverlust bei grünem Healthcheck.**

Behandelt wurde das nicht mit einer Notiz, sondern mit einem Namen: Das Compose-Volume
heißt jetzt `modern-werkstatt-db-pg18`, der erste Start ist damit sauber statt kryptisch
(„database files are incompatible with server“); das alte 9.6-Volume bleibt liegen und
wird von Hand entfernt. **Ein Versionssprung, der das Datenverzeichnis verschiebt,
verdient einen neuen Volume-Namen — keinen Kommentar.**

### Was der Sprung das Sicherheitsnetz gekostet hat: nichts

- **Charakterisierung 47/47 grün gegen den modernen Stand (PG 18)** und 47/47 gegen den
  Legacy-Stand (9.6); Kundensortierung über beide Stände identisch.
- `/api/bericht/topkunden` — der einzige Endpunkt, dessen rohe DB-Typen über
  `queryForList` bis ins JSON durchschlagen, also der empfindlichste Kandidat für eine
  Zahlentyp-Drift — liefert auf beiden Ständen **bytegleiche** Antworten, inklusive des
  Dezimalliterals `"umsatz":912.00`. Das war der vorab festgelegte Abnahmeschritt.
- `/api/bericht/monat`: Werte identisch, aber die **Schlüsselreihenfolge** im JSON
  unterscheidet sich (modern alphabetisch). Das besteht seit Etappe 3, ist kein
  PG-18-Effekt und für den reihenfolgeunabhängigen `JsonNode`-Vergleich unsichtbar.
  Wir schreiben es hin, weil „ist uns bekannt“ nur zählt, wenn es irgendwo steht.

Und eine Korrektur an uns selbst: `characterization/README.md` behauptete zu viel über
die Stabilität der Sortierung. Der Text sagt jetzt, was gilt — die Reihenfolgen wurden
**auf diesem Seed gleich gemessen**, nicht: Collation könne nicht schaden. Das
Alpine-Ergebnis oben ist der Gegenbeweis zum eigenen alten Satz.

## Startbereitschaft: der Healthcheck, der zwei Minuten lang gelogen hat

Ein Healthcheck ist die Schnittstelle zwischen Ihrer Anwendung und jedem
Orchestrator — Compose, Dokploy, Kubernetes. Ist er falsch, sind alle darauf
aufbauenden Automatismen falsch. Drei Funde, alle gemessen (ADR-0015):

1. **Die Standard-Readiness-Gruppe von Spring Boot enthält die Datenbank nicht.**
   Gemessen: Bei gestopptem `modern-db-1` antwortete `/actuator/health/readiness` mit
   `200 {"status":"UP"}`, während `/actuator/health` bereits `503` lieferte. Eine
   Anwendung, die keine einzige Anfrage beantworten kann, meldet sich als bereit — und
   der Loadbalancer schickt ihr Verkehr. Behoben mit
   `management.endpoint.health.group.readiness.include=readinessState,db`. Danach
   gemessen: DB weg → Readiness `503 {"status":"DOWN"}`.
2. **Liveness darf die Datenbank ausdrücklich NICHT enthalten.** Sie bleibt bei
   `livenessState` allein. Wer die DB in die Liveness-Probe legt, lässt bei einem
   Datenbankausfall reihum alle Anwendungscontainer neu starten — aus einer Störung
   wird ein Ausfall. Gemessen nach dem Fix: DB weg → Liveness `200 {"status":"UP"}`.
   **Readiness beantwortet „darf ich Verkehr?“, Liveness beantwortet „bin ich noch ich
   selbst?“. Das sind zwei Fragen.**
3. **Das Wiederholungs-Budget war ebenfalls falsch — die teuerste Kleinigkeit der
   Etappe.** Der alte Check hatte `retries: 24`, weil dieselbe Zahl auch den langsamen
   Start abdecken musste. Gemessen heißt das: Die Anwendung antwortete bereits mit 503,
   und der Container galt **zwei Minuten lang weiter als `healthy`**. Jetzt `retries: 3`
   plus `start_period: 90s` — Fehlschläge während der Startphase zählen nicht gegen die
   Retries, die beiden Aufgaben sind getrennt. End-to-End gemessen: Der Container wird
   **25 s** nach dem Tod der Datenbank `unhealthy` und **6 s** nach ihrer Rückkehr
   wieder `healthy`. Die alte TCP-Probe hat den Ausfall nie bemerkt: Ein offener Port
   ist kein Lebenszeichen.

Eine praktische Randnotiz: Der Healthcheck ist `bash` plus `/dev/tcp`, weil das
Laufzeit-Image `eclipse-temurin:25-jre` **weder curl noch wget** enthält — und weil
`CMD-SHELL` unter dash läuft, das kein `/dev/tcp` kennt. Beim Aufschreiben fiel
nebenbei auf, dass der Kommentar in `legacy/docker-compose.yml` dasselbe über
`eclipse-temurin:8-jre` behauptet und **falsch** ist: Das Legacy-Image hat beides.

### Was sonst noch beobachtbar wurde

- **Actuator: nur `health` und `info` freigegeben.** Gemessen: `/actuator/env`,
  `/beans`, `/mappings`, `/metrics`, `/loggers`, `/heapdump` liefern **404**. Ein
  offener Actuator ist kein Monitoring, sondern eine Konfigurations- und
  Speicherauskunft für Fremde.
- **Strukturierte Logs (ECS 8.11 JSON) nur im Compose-Umfeld** — ein blankes
  `java -jar` bleibt menschenlesbar: Das Logformat gehört der Umgebung, nicht dem
  Artefakt. Bei der Trace-Korrelation lauert eine Kleinigkeit: Micrometer schreibt
  `traceId`/`spanId` in den MDC, das sind **nicht** die ECS-Feldnamen. Erst
  `logging.structured.json.rename.traceId=trace.id` (und dasselbe für `spanId`) macht
  aus zwei Systemen eines. Gemessen danach: `"trace.id":"0e5f…"`, `"span.id":"9c85…"`.
- **Tracing ist standardmäßig aus** — ohne Collector protokolliert der OTLP-Exporter bei
  jedem Batch einen Verbindungsfehler, und Instrumentierung, die den Log flutet, wird
  abgeschaltet statt gelesen. Eingeschaltet wird sie mit dem Beobachtungs-Profil
  (`grafana/otel-lgtm`); gemessen liegen dann Traces mit
  `rootServiceName: werkstatt-crm-modern` in Tempo.

## Flyway: eine Schema-Wahrheit — und eine Boot-4-Falle ohne Fehlermeldung

Bis hierher gab es das Schema zweimal: als Init-Skripte für Compose und als Kopie für
den Testcontainers-Test (Wart B18). Zwei Kopien driften, das ist keine Prognose,
sondern Erfahrung. Jetzt gilt (ADR-0013): `modern/db/init/*.sql` ist **gelöscht**,
Schema ist `V1__baseline_schema.sql`, Demodaten sind `V2__demo_seed.sql`, und der
Testcontainers-Test kopiert nichts mehr, sondern lässt Flyway bauen — er beweist damit
zusätzlich, dass die Migrationen laufen. Produktiv wird die zweite Location per
`SPRING_FLYWAY_LOCATIONS=classpath:db/migration` weggelassen; Demodaten sind eine
Umgebungsentscheidung, kein Build-Artefakt. Gemessen: zwei Migrationen angewandt,
`flyway_schema_history` zeigt `1 baseline schema` und `2 demo seed`, beide erfolgreich
(Flyway 12.4.0).

**Die Falle: `flyway-core` allein tut in Boot 4 nichts.** Boot 4 hat die
Auto-Konfiguration in Module pro Technologie zerlegt. Mit nur `flyway-core` im
Classpath startet die Anwendung fehlerfrei, migriert **nichts** und stirbt später an
`relation "kunde" does not exist`. Bei uns war das eine echte, gemessene Störung — 6
von 7 Integrationstests fehlerhaft —, bis `spring-boot-starter-flyway` ergänzt war
(`flyway-database-postgresql` ist seit Flyway 10 ein zweites Pflicht-Artefakt). Das ist
der unangenehme Typ Fehlkonfiguration: **still, nicht laut.** Wer auf Boot 4 hebt,
prüft für jedes Feature, das „einfach da war“, ob es inzwischen ein eigenes
Starter-Modul hat.

Zwei Details, die man erst im zweiten Anlauf richtig macht:

- `spring.flyway.baseline-on-migrate=false` ist **Absicht**. Mit `true` würde ein schon
  befülltes Volume V1 überspringen und anschließend den Seed ein zweites Mal einspielen.
  Bequemlichkeit beim ersten Start, doppelte Daten beim zweiten.
- Der **CI-Driftwächter wurde neu geschrieben**. Er verglich früher die beiden
  Init-Skript-Paare per `diff -q`. Jetzt ruft `legacy-ci.yml` das Skript
  `scripts/check-schema-drift.sh`, das Kommentare und Leerzeilen entfernt und das SQL
  vergleicht, das der Server tatsächlich ausführt — und das **laut fehlschlägt, wenn
  eine der Dateien fehlt**, damit das Löschen einer Datei den Wächter nicht grün macht.
  Verifiziert mit einer Negativkontrolle: `Franz` → `Franzl` im Seed lässt das Skript
  mit Exit-Code 1 und Diff abbrechen. **Ein Wächter, den man nicht scheitern gesehen
  hat, ist kein Wächter.**

## Absicherung am Rand statt in der Anwendung

Authentisierung, Sicherheits-Header und Rate-Limit sitzen in einem **Traefik-Reverse-
Proxy**, nicht in der Anwendung (ADR-0014). Die Gründe, nach Gewicht:

1. `docs/DEVIATIONS.md` hält die Anforderung als *„die öffentliche Demo muss mindestens
   `/admin` schützen — **Reverse-Proxy-Auth zählt**“* fest; vollständiges OAuth2/OIDC
   ist dort als eigentümergebundene Arbeit mit eigenem ADR eingetragen.
2. Spring Security in der Anwendung würde gepinnte Verträge verändern. Der Radius ist
   nicht geschätzt, sondern aus der Netz-Landkarte gemessen: Ein auf `/admin`
   beschränkter Matcher bricht **genau 4 Tests**, CSRF mit dem Spring-Standard-Matcher
   bricht **17 Charakterisierungs-Schreibaufrufe** plus jedes schreibende E2E-Szenario.
   Jeder dieser Brüche wäre eine sanktionierte Abweichung nach ADR-0004 — eine bewusste
   Entscheidung, keine Nebenwirkung.
3. Selenium kann Chromes nativen Basic-Auth-Dialog nicht wegklicken; der CDP-Hook
   `HasAuthentication` ist versionsabhängig fragil, und diese Suite hat eine erklärte
   Null-Toleranz für Flakiness.
4. Dokploy betreibt Traefik. Der lokale Rand ist damit die **Generalprobe** der
   Zielplattform, kein Platzhalter.

**Der ehrliche Gegeneinwand, den wir lieber selbst formulieren:** Authentisierung, die
nur in einem Compose-Overlay existiert, kann man Theater nennen. Die Antwort hat drei
Teile — es ist dieselbe Komponente, die die Zielplattform betreibt; sie ist durch ein
Skript automatisch verifiziert statt behauptet; und der Anwendungsport wird auf einem
echten Host **nicht veröffentlicht** (`docs/deployment.md` §12). Was davon nicht
bewiesen ist, bleibt unbewiesen: Es gibt keinen echten Host. Siehe unten.

Technisch ist der Rand ein **Overlay**, kein Profil (`modern/docker-compose.edge.yml`):
Er braucht `MODERN_ADMIN_AUTH`, und eine Pflichtvariable in der Basisdatei würde den
Schnellstart für alle kaputt machen. Der Rand hört auf :8091, die Anwendung bleibt
lokal auf :8090 — das Sicherheitsnetz behält seinen direkten Weg.

**Gemessen, vollständig über `modern/edge/verify-edge.sh` (Exit 0):**

- ohne Anmeldung: `/admin`, `/api/admin/statistik`, `POST /admin/bereinigen`,
  `/actuator/health` → je **401**
- mit Anmeldung: dieselben vier → **200**; falsches Passwort → **401**
- öffentliche Fläche unverändert: `/` 200, `/api/kunden` 200, `/rechnungen` 200
- Header auf öffentlichen Antworten: `X-Frame-Options: DENY`,
  `X-Content-Type-Options: nosniff`, `Referrer-Policy: no-referrer`, vollständige CSP
- Rate-Limit (Mittel 30/s, Burst 60): 80 schnelle Anfragen über den Rand →
  **103–173 × 429** über fünf Läufe; derselbe Burst direkt auf die Anwendung → **0 × 429**. Auch ein
  Rate-Limit prüft man, indem man es auslöst.
- **HSTS ist standardmäßig aus** (`stsSeconds: 0`), und das ist Absicht: Über reines
  HTTP eingeschaltet, bringt es dem Browser für ein Jahr bei, `http://localhost` zu
  verweigern. Den Header setzt der TLS-Host, nicht die Vorlage.

**CSRF wurde bewusst nicht ergänzt**, mit Begründung: Die Anwendung hat keine Sessions,
keine Cookies, keine Umgebungsautorität — es gibt nichts, was ein klassisches
CSRF-Token schützen könnte. Was Basic-Auth am Rand allerdings *einführt*, sind im
Browser zwischengespeicherte Zugangsdaten, auf denen ein seitenübergreifendes POST
mitreiten könnte. Dieses Restrisiko ist für eine Demo akzeptiert und steht in
`SECURITY.md`; die echte Lösung (sessionbasiertes OIDC plus Token) gehört in die
Auth-Etappe.

## Der CSP-Befund: die Suite grün, während der Browser Styles blockierte

Das ist der wichtigste Absatz dieses Kapitels.

Die erste Content-Security-Policy war streng, wie es sich gehört: `style-src 'self'`.
Danach liefen **32 der 34 Szenarien durch den Rand hindurch grün**. Die beiden
`AdminTest`-Szenarien laufen durch den Rand grundsätzlich nicht — `/admin` liegt hinter
Basic Auth, und Selenium kann den nativen Anmeldedialog des Browsers nicht bedienen; sie
laufen weiterhin direkt gegen den Anwendungsport und sind dort Teil der 34/34-Schranke.
Abgeschaltet wurde nichts.

Ein Blick in die Browserkonsole zeigte gleichzeitig, dass die Richtlinie **verletzt und
Styles blockiert wurden**: Angular fügt Komponenten-Styles zur Laufzeit als
`<style>`-Elemente ein. Nach der Korrektur meldete dieselbe Konsole **null Fehler**.

Nüchtern gesagt: Der Browser hat einen Teil der Darstellung verworfen, und das
Selenium-Sicherheitsnetz — die teuerste, sorgfältigste Investition dieses Projekts —
hat es **nicht bemerkt und konnte es nicht bemerken.** Es prüft Verhalten und Texte,
nicht Aussehen. Die Schaltflächen waren da, die Klicks kamen an, die Assertions
stimmten.

Gemessen ist genau das: CSP-Verstöße in der Konsole, blockierte zur Laufzeit injizierte
Komponenten-Styles, danach null Fehler. **Wie kaputt die Seite dabei aussah, haben wir
nicht festgehalten** — kein Screenshot, keine Bewertung. Die aus Dateien geladenen
Stylesheets waren von `style-src 'self'` nicht betroffen, die Seite war also nicht
gänzlich unformatiert. Wir schreiben es hier so ungenau, wie wir es gemessen haben.

> **Merksatz für jede Testsuite, nicht nur für unsere:**
> Eine Suite beweist, was sie prüft — und **über alles andere beweist ein grüner Lauf
> gar nichts**. Die produktive Frage lautet deshalb nicht „ist die Suite grün?“,
> sondern: **„Welche Fehlerklasse kann diese Suite grundsätzlich nicht sehen?“**
> Bei Selenium ist die Antwort: alles, was der Browser *meldet*, statt es anzuzeigen —
> CSP-Verstöße, Konsolenfehler, fehlgeschlagene Asset-Requests, gebrochenes Layout.

Gefunden wurde es nicht von einem Test, sondern **weil jemand hingeschaut hat**: echter
Browser, Konsole offen. Das ist keine Anekdote, sondern eine Arbeitsanweisung — jede
Etappe braucht mindestens einen Schritt, der die Automatisierung verlässt.

Aufgelöst wurde es minimal: `style-src 'self' 'unsafe-inline'` — und **sonst nichts**.
`script-src 'self'` bleibt streng, und das ist die Direktive, die eingeschleusten Code
am Ausführen hindert. Nach der Änderung neu gemessen: **0 Konsolenfehler.** Die saubere
Lösung wäre Angulars `CSP_NONCE`; die verlangt, dass `index.html` pro Anfrage gerendert
statt als statische Ressource ausgeliefert wird — eigentümergebundener Umfang, also
Eintrag in `DEVIATIONS.md` statt stiller Ausbau.

Und weil ein einmal verstandenes Loch beim nächsten Mal wieder unsichtbar ist:
`verify-edge.sh` prüft `script-src 'self'` **wörtlich**, damit ein späteres „dann
schreiben wir halt auch dort `unsafe-inline` rein, dann geht's“ nicht durchrutscht —
und das Skript benennt die blinde Stelle in seiner eigenen Ausgabe. **Das Loch ist
damit nicht geschlossen, sondern beschriftet.** Das ist ehrlicher als ein Test, der so
tut, als sähe er etwas.

### Ein zweiter blinder Fleck, gefunden beim Nachschauen

`e2e/.../config/TestConfig.java` hatte kein Gegenstück zum `rejectTargetFlag` der
Charakterisierungs-Suite: `-Dstand=modern` an die E2E-Suite übergeben wurde
**stillschweigend ignoriert**, der Lauf ging grün — gegen den **Legacy**-Stand. Wieder
ein grüner Lauf, der die falsche Frage beantwortet, und exakt derselbe Fehlertyp, den
Session 13 auf der Charakterisierungs-Seite behoben hat. Lehre: **Eine Absicherung auf
einer Seite eines symmetrischen Fehlers ist eine halbe Absicherung** — wer einen
Bedienfehler abfängt, sucht im selben Zug seine Zwillinge. Jetzt wird `-Dstand`
abgelehnt und `-Dtarget` sofort validiert; gemessen bricht `-Dstand=modern` den Lauf ab.

## Der Lasttest: die Modernisierung ist nicht messbar schneller

Ein Lesepfad (Auftragsliste → Kundenliste → Suche → Kundendetail → Fahrzeuge →
Monatsbericht), 5 virtuelle Benutzer, 45 Sekunden, k6, beide Stände:

| Kennzahl | modern (Boot 4.1 / Java 25 / PG 18) | legacy (Boot 1.5 / Java 8 / PG 9.6) |
|---|---|---|
| `http_req_duration` Mittel | 0,87 ms | 0,77 ms |
| `http_req_duration` p(95) | 1,60 ms | 1,56 ms |
| `http_req_duration` max | 4,13 ms | 5,68 ms |
| Berichts-Endpunkt p(95) | 1,15 ms | 1,21 ms |
| Anfragen | 1.146 | 1.146 |
| Fehlerquote | 0 % | 0 % |
| Checks | 1.528 bestanden, 0 fehlgeschlagen | 1.528 bestanden, 0 fehlgeschlagen |

**Die unschmeichelhafte, korrekte Lesart: Die Modernisierung ist auf dieser Last nicht
messbar schneller.** Zehn Jahre Framework- und JDK-Versionen haben Wartbarkeit,
Sicherheit und einen Arbeitsmarkt gekauft — keine Geschwindigkeit. Das ist kein
Betriebsunfall. **Erfahrungswert, hier ausdrücklich nicht gemessen:** Nach unserer
Erfahrung gilt das für viele KMU-Systeme, weil die Antwortzeit an Datenmenge, Abfragen
und Netzwerk hängt und nicht an der Spring-Version. Gemessen haben wir ein Szenario auf
einem Rechner — daraus folgt kein Satz über eine Grundgesamtheit.
**Wer eine Migration mit Performance verkauft, muss vorher messen — und verkauft sie
danach in der Regel anders.** Die belastbaren Argumente stehen in Kapitel 4
(Sicherheitslücken, gepflegte Abhängigkeiten) und Kapitel 6 (Testbarkeit).

Die Einschränkung gehört untrennbar zu diesen Zahlen: Lastgenerator, Anwendung und
Datenbank teilen sich **einen Laptop**, und der Datenbestand ist ein Demo-Seed mit zehn
Kunden. Das ist eine **Vergleichsbasis zwischen zwei Ständen**, keine Kapazitätsaussage.
Wer daraus „hält 1.146 Anfragen“ liest, liest etwas hinein, das nicht gemessen wurde.

## Was offen ist — und warum es offen bleibt

Diese Liste ist der eigentliche Grund, warum dieses Kapitel kein Etappenabschluss ist:

- **Es hat kein Deployment stattgefunden.** Kein Hetzner-VPS, kein Dokploy, kein TLS,
  kein `pg_dump`-Cron, kein Push nach GHCR. `docs/deployment.md` §10 gilt weiter: keine
  erfundenen Schritte für Infrastruktur, die es nicht gibt. Das Repo hat außerdem
  **überhaupt keine GitHub-Secrets** (`gh secret list` ist leer) — ein Workflow, der
  `DOKPLOY_TOKEN` oder `GHCR_TOKEN` referenziert, wäre nicht „vorbereitet“, sondern
  kaputt.
- **Deshalb existiert der Tag `stage-6-cloud-ops` nicht und v1.0.0 ist nicht
  veröffentlicht.** Die Etappe abzuschließen wäre genau die Beschönigung, gegen die
  dieses Repo argumentiert — und die Stelle, an der man ein Migrationsprojekt am
  leichtesten belügt, ist der Abschlussbericht.
- **Kein OAuth2/OIDC, kein Keycloak, kein Audit-Log.** Alles drei ist echte Arbeit mit
  echten Vertragsänderungen (siehe die gemessenen 4 bzw. 17 Testbrüche oben) und steht
  als eigentümergebundener Umfang in `docs/DEVIATIONS.md`. Ein „das machen wir schnell
  mit“ wäre entweder oberflächlich oder würde das Netz brechen.
- **Der Legacy-Stand bleibt unverändert** — unauthentisiert, PostgreSQL 9.6. Er ist das
  Exponat. Ihn zu härten hieße, das Vergleichsobjekt zu verändern.

## Entscheidungsregeln fürs eigene Projekt

- **Prüfen Sie Verhalten, nicht Konfiguration.** Sortierung sortieren lassen, Rate-Limit
  auslösen, Healthcheck durch echtes Abschalten der Datenbank testen, Driftwächter
  einmal absichtlich scheitern lassen. Jede Prüfung, die nur eine Einstellung ausliest,
  ist eine Absichtserklärung mit Häkchen daneben.
- **Datenbank-Hauptversionssprünge sind Migrationsetappen, keine Tag-Bumps.** Sortierung
  und Datenverzeichnis sind die beiden Stellen, an denen es lautlos schiefgeht — beide
  vorher messen, das Volume bekommt einen neuen Namen.
- **Readiness und Liveness sind zwei verschiedene Fragen.** Abhängigkeiten gehören in
  die Readiness, niemals in die Liveness. Und das Wiederholungsbudget für Ausfälle ist
  nicht dasselbe wie die Geduld beim Start — dafür gibt es `start_period`.
- **Fragen Sie bei jeder grünen Suite nach ihrem blinden Fleck** und schreiben Sie ihn
  auf. Ein benanntes Loch ist beherrschbar; ein unbekanntes Loch ist der Grund, warum
  „bei uns war alles grün“ nach einem Vorfall so oft stimmt.
- **Sicherheit am Rand, wenn die Zielplattform ohnehin einen Rand betreibt** — aber
  sagen Sie dazu, was sie nicht leistet: keine Anwendungsautorisierung, kein Audit-Log.
- **Messen Sie den Leistungsgewinn, bevor Sie ihn versprechen.** Er kann null sein; bei
  uns war er es.
- **Schließen Sie keine Etappe, deren Ergebnis Sie nicht vorführen können.** Ein
  fehlender Tag ist billig; ein Tag, hinter dem nichts läuft, kostet Vertrauen.

## Aufwand

| Position | Wert | Art |
|---|---|---|
| PostgreSQL 18 inkl. Collation-Untersuchung, Flyway-Umstellung, Health-Checks und Beobachtbarkeit, Edge-Härtung, Lasttest, Doku | siehe `docs/worklog.md`, Session 14 | **Messwert** (Agenten-Wall-Time unter Aufsicht) |
| Deployment auf einen echten Server, TLS, Backups | **nicht gemessen — hat nicht stattgefunden** | — |
| Reparaturaufwand des CSP-Befundes | im Messwert der Session enthalten, nicht getrennt erfasst | **Messwert** (nicht aufgeschlüsselt) |

*Die Etikettierung folgt der Regel aus `playbook/README.md`: Messwerte sind
Agenten-Wall-Time unter Aufsicht, Feldwerte sind gekennzeichnete
Erfahrungsschätzungen. Vermischt wird nichts. Für diese Etappe gibt es bewusst
**keinen** Feldwert in Personentagen: Der teuerste Teil einer echten Betriebsübernahme
— Zugänge, Netzwerk, Datenschutz, Betriebsvereinbarung, Rufbereitschaft — ist hier
nicht einmal berührt worden, und eine Schätzung ohne diesen Teil wäre irreführend.*
