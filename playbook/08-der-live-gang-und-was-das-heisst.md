# Kapitel 8 — Der Live-Gang, und was das für Ihr Projekt heißt

*Meilenstein: G7 · Etappe `stage-6-cloud-ops` · Schlusskapitel*

> **Status dieses Kapitels:** Beide Stände laufen produktiv auf einem Hetzner-VPS,
> hinter TLS, mit nächtlichem Backup und einer **durchgeführten** Wiederherstellungsprobe.
> Jeder Schritt unten wurde ausgeführt, bevor er hier aufgeschrieben wurde — die Regel aus
> `docs/deployment.md` §10 („keine erfundenen Schritte für Infrastruktur, die es nicht
> gibt") gilt unverändert; sie ist jetzt nur in die andere Richtung wahr.

## Ausgangslage

Kapitel 7 endete mit einer Liste, warum die Etappe nicht abgeschlossen war: kein Server,
kein TLS, kein Backup, keine Secrets, kein Tag. `docs/MANUAL_TASKS.md` §I hielt fest, was
vor dem ersten ehrlichen Deployment-Satz entschieden und beschafft sein musste. Dieses
Kapitel ist das Protokoll dieser Entscheidungen und ihrer Ausführung — einschließlich der
einen Falle, die erst im Betrieb zubiss.

## Die Entscheidungen (ADR-0016, verkürzt)

1. **Plattform: Dokploy auf dem bestehenden Applikations-Host.** `ENGINEERING_STANDARDS.md`
   §7 verlangt „Hetzner-VPS mit Dokploy" seit vor dieser Etappe; der Host hatte gemessen
   5,3 GB RAM und 133 GB Platte frei. Eine andere Plattform wäre eine registrierungs-
   pflichtige Abweichung gewesen, für die kein Argument den Fakten standhielt.
2. **Images aus der CI, nie vom Host gebaut.** Der Host trägt andere Produkte;
   Build-Druck (RAM-Spitzen, OOM) ist dort der dokumentierte Ausfallmodus. Die CI baut
   beide Stände mit ihren unveränderten Dockerfiles und pusht nach GHCR — mit dem
   eingebauten `GITHUB_TOKEN`. Das in `.env.example` reservierte `GHCR_TOKEN` wurde
   **absichtlich nie angelegt**: ein Zugangsdatum, das nicht existiert, kann nicht abfließen.
3. **Ein Traefik, kein Proxy hinter dem Proxy.** Das lokale Edge-Overlay existiert, weil
   ein Entwicklungsrechner keinen Ingress hat. Der Host betreibt dieselbe Komponente
   (Traefik v3.6, Docker-Provider) ohnehin — also wandern die **vermessenen Middlewares**
   aus dem Overlay wortgleich als Labels an den Host-Traefik. Damit verschwindet auch der
   Docker-Socket-Mount, den `SECURITY.md` §4.6 als größtes Einzelrisiko des Overlays
   benannt hatte, und der Rate-Limiter sieht echte Client-Adressen statt der Adresse
   eines Zwischenproxys. Der Anwendungsport ist auf dem Host **nicht veröffentlicht** —
   das war in `SECURITY.md` §5 die Antwort auf den Theater-Einwand, jetzt ist sie Zustand.
4. **Der Legacy-Stand geht gated live, der moderne offen.** Die erhaltene SQL-Injection
   (SD-1), unauthentisierte destruktive Endpunkte und eine End-of-Life-Datenbank öffentlich
   zu betreiben wäre kein Exponat, sondern ein Betrieb einer absichtlich verwundbaren
   Anwendung. Von den drei ehrlichen Optionen aus §I — gar nicht öffentlich, gated,
   öffentlich mit Warnbanner — wurde **Basic-Auth über den ganzen Stand** gewählt: der
   Demo-Effekt („beide nebeneinander") bleibt für geführte Besucher erhalten, Scanner
   sehen 401. Der moderne Stand ist öffentlich, seine Admin-Fläche bleibt hinter der
   ADR-0014-Grenze.
5. **Der Demo-Seed bleibt an.** `.env.example` §4 warnt zu Recht: Produktion muss den
   Seed abwerfen. Dieses Deployment *ist* die öffentliche Demo — ein leeres CRM führt
   nichts vor. Die Warnung gilt dem Muster mit echten Kunden und bleibt unverändert stehen.

## Der Live-Gang, Schritt für Schritt

**CI → GHCR.** Ein neuer Workflow (`deploy.yml`) baut je Stand das Image und pusht zwei
Tags: `master` und ein unveränderliches `sha-`-Tag für Provenienz. Er ist **bewusst kein
Pflicht-Check** — dieselbe Begründung wie beim Playbook-PDF: er erzeugt ein Ergebnis, er
bewacht kein Verhalten, und eine Registry-Störung darf keinen Fix an der Anwendung
blockieren. Solange die Dokploy-Dienste noch nicht existierten, meldete der
Auslöse-Schritt das mit einer Notiz und endete grün; ein gesetztes Repository-Variable
je Stand schaltet ihn scharf, und dann läuft `curl --fail` — ein kaputter Auslöser ist
ein roter Job, nie ein grüner, der nichts deployt hat.

**Dokploy: je Stand ein Compose-Dienst.** Beide zeigen auf dasselbe Repository und je
eine Produktions-Compose-Datei unter `deploy/`. Diese Dateien sind die Übersetzung der
lokalen Stände: gleiche Healthchecks, gleiche Abhängigkeitsreihenfolge, aber Images statt
`build:`, keine veröffentlichten Ports, und die Traefik-Labels tragen Middleware für
Middleware die Werte aus dem lokalen Edge-Overlay.

**Die Falle: ein Schloss, das niemand aufsperren konnte.** Der erste Deploy sah perfekt
aus — Container healthy, 301 auf HTTP, 401 ohne Zugangsdaten. Dann die Gegenprobe, die
`verify-edge.sh` lokal zur Regel gemacht hatte: *mit* korrekten Zugangsdaten … ebenfalls
401. Ein Schloss, das niemand aufsperren kann, ist kein sicheres Schloss, sondern ein
kaputtes. Die Ursache saß zwei Schichten tief: Der htpasswd-Hash enthält `$`-Zeichen;
die Plattform entfernte die schützenden Anführungszeichen aus dem Umgebungswert, und
der Compose-Parser expandierte anschließend `$apr1` und den Salt zu leeren Variablen —
übrig blieb ein Fragment des Hashes als „Passwort". Sichtbar wurde das erst am laufenden
Container-Label. Die Behebung ist unspektakulär (`$$`-Escaping im Umgebungswert), die
Lehre nicht: **Eine Sicherheitsgrenze ist erst geprüft, wenn beide Richtungen geprüft
sind — dass sie Fremde abweist und dass sie Berechtigte einlässt.** Die 401-Hälfte allein
hätte diese Fehlkonfiguration für immer wie Sicherheit aussehen lassen.

**Backups, und zwar mit Rückspielprobe am selben Tag.** Ein nächtlicher Cron zieht per
`pg_dump` beide Datenbanken (kein Datenbank-Port ist veröffentlicht; der Dump läuft über
`docker exec`), prüft jede Datei mit `gzip -t`, behält 14 Tage und löscht älter. Direkt
nach dem ersten Lauf wurde **zurückgespielt**: jede Sicherung in eine Wegwerf-Datenbank
eingelesen und Tabelle für Tabelle gegen die laufende gezählt — Kunden 10/10, Fahrzeuge
13/13, Aufträge 16/16, Rechnungen 8/8, auf beiden Ständen (2026-08-14). Die Auslagerung
auf einen zweiten Standort hängt am selben, noch unkonfigurierten Speicherziel wie beim
Schwesterprojekt auf diesem Host; der Cron ruft dessen Auslagerungsskript bereits mit
auf, das bis zur Konfiguration laut „NOT CONFIGURED" meldet und ordnungsgemäß endet —
derselbe Mechanismus, ein Ziel für alle Produkte des Hosts, und bis dahin wird über
Offsite-Kopien nicht in der Vergangenheitsform geschrieben.

**DNS, TLS, HSTS — in genau dieser Reihenfolge.** Die Domänenwahl trug eine eigene
Eigentümerregel: `stoicera.com` ist die Markendomäne und bleibt Laborprojekten verwehrt;
die Stände laufen unter `migration-lab.stoicera.cyou` und
`migration-lab-legacy.stoicera.cyou`. Diese Zone hat einen Wildcard-Parking-Record —
„der Name löst auf" beweist dort gar nichts, also wurde der **Wert** der beiden
A-Records gegen die Adresse des App-Hosts geprüft, bevor irgendetwas weiterging.
Danach stellte Let's Encrypt die Zertifikate ohne Zutun aus (HTTP-Challenge des
Host-Resolvers; Issuer per `openssl` nachgewiesen, nicht der grünen Browserzeile
geglaubt). Erst **nachdem** das Zertifikat hielt, wurde `MODERN_HSTS_SECONDS=31536000`
gesetzt und neu ausgerollt — HSTS über unverschlüsseltem HTTP wäre ein Jahr Browser-Sperre
gewesen, die Reihenfolge stand seit Kapitel 7 fest. Abschließend `deploy/verify-live.sh`
gegen die öffentlichen URLs: **alle Assertions halten** (14.08.2026) — Issuer, 308-Redirects,
beide Auth-Richtungen auf beiden Ständen, öffentliche Fläche, Header samt HSTS, Rate-Limiter
125 × 429 unter 200 parallelen Requests. Und der eine Check, den kein Skript kann: ein
echter Browser auf der Live-Instanz, Konsole offen — **null CSP-Verletzungen**, eine echte
Suche durch die Oberfläche filtert die Kundenliste korrekt auf den gesuchten Betrieb.

## Die Evidenzleiter dieses Kapitels

| Sprosse | Nachweis | Datum |
|---|---|---|
| Container laufen und sind healthy (Plattform-Probe) | `docker ps`, beide Stände | 2026-08-14 |
| Anwendung antwortet sich selbst | HTTP 200 im Container (legacy), Readiness `UP` (modern) | 2026-08-14 |
| Routing + Auth-Grenze über den Host-Traefik | 301 HTTP→HTTPS · öffentlich 200 · geschützt 401 **und** mit Zugangsdaten 200, beide Stände | 2026-08-14 |
| Fachliche Operation über den öffentlichen Pfad | `/api/kunden` liefert die zehn Seed-Kunden | 2026-08-14 |
| TLS, Zertifikats-Issuer, Redirect von außen | Let's Encrypt auf beiden Hostnamen, HTTP → 308 → HTTPS, HSTS aktiv — `deploy/verify-live.sh`, alle Assertions | 2026-08-14 |
| Echte Browser-Transaktion auf der Live-Instanz | Suche „Huber" durch die Oberfläche filtert auf den einen Betrieb; Konsole ohne CSP-Verletzung; Legacy-Stand durch das Auth-Tor bedient | 2026-08-14 |

## Was das für Ihr Projekt heißt — die Regeln aus acht Kapiteln

1. **Das Netz kommt vor dem Sprung** (Kap. 1). Charakterisierungstests definieren
   „gleiches Verhalten" ausführbar — alles Weitere sind Meinungen.
2. **Migrieren Sie in getaggten Etappen, deren Zustand man auschecken und starten kann**
   (Kap. 2–5). Ein Tag, hinter dem nichts läuft, kostet Vertrauen; ein fehlender Tag ist billig.
3. **Der gefährlichste Fehler kompiliert und sieht gut aus** (Kap. 3). Draht-Kontrakte
   pinnen, nicht Oberflächen ansehen.
4. **Werkzeuge und KI: benutzen ja, glauben nein** (Kap. 4 und 6). OpenRewrite wurde
   bewertet, das KI-Experiment vorregistriert — und das schmeichelhafte Ergebnis als
   Survivorship Bias veröffentlicht. Verlangen Sie von jedem Automatisierungsversprechen
   eine Messung mit Datum.
5. **Dieselben Szenarien gegen beide Oberflächen** (Kap. 5). Funktionale Äquivalenz ist
   eine Messung, die bei jedem Commit wiederholt wird — sonst ist sie ein Werbetext.
6. **Jede grüne Suite hat einen blinden Fleck; benennen Sie ihn** (Kap. 7). Die Suite
   war grün, während der Browser Styles blockierte. Der eine visuelle Check steht
   seither namentlich auf der Handarbeitsliste.
7. **Betrieb ist Teil der Migration, nicht ihr Anhang** (Kap. 7–8). Health-Probes, die
   die Datenbank wirklich sehen; ein Image-Scan, der beim ersten Lauf eine echte CVE in
   der Schicht fing, die kein Manifest-Scanner sieht; Backups erst dann „Backups", wenn
   zurückgespielt wurde.
8. **Sicherheitsgrenzen in beide Richtungen prüfen** (Kap. 8). Abweisen *und* einlassen.
   Die 401-Hälfte allein ist Theater mit Häkchen.
9. **Abweichungen registrieren statt glätten** (alle Kapitel). Das Register erlaubt
   alles außer Schweigen — und genau deshalb kann man dem Rest trauen.
10. **Schließen Sie keine Etappe ab, die Sie nicht vorführen können** (Kap. 7→8). Dieses
    Repository hat seinen eigenen Abschluss so lange verweigert, bis es einen Betrieb
    gab, den man besuchen kann: <https://migration-lab.stoicera.cyou> — und daneben,
    hinter seinem Tor, das Exponat von 2016.

## Aufwand

| Position | Wert | Art |
|---|---|---|
| Deployment-Halbetappe (Session 15, inkl. Basicauth-Falle, Backups mit Probe, Verifikation, Doku) | 2,4 h | Messwert — Agent-Wall-Time unter Aufsicht (Offenlegung: README) |

Aus den Messwerten dieses Repos lässt sich kein menschliches Projektbudget ableiten;
übertragbar sind Reihenfolge, Bruchkatalog und Entscheidungsregeln.
