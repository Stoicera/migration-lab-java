# SECURITY.md — threat model and how to report a vulnerability

Required by [`docs/ENGINEERING_STANDARDS.md`](docs/ENGINEERING_STANDARDS.md) §4 (*"`SECURITY.md`
mit Threat-Model-Skizze (STRIDE-light)"*). The obligation has been open since the standard was
written; it was registered as **deferred(G7)** in [`docs/DEVIATIONS.md`](docs/DEVIATIONS.md) on
2026-07-31 with the reason *"written when the deployment surface exists to model"*, and this file
is that row being closed on **2026-08-05**.

**Scope, stated up front so nobody goes looking for something that is not here:**

| | Status |
|---|---|
| Threat model over the two local demonstration stands | **this document** |
| How to report a vulnerability, and what response to expect | **[§2](#2-reporting-a-vulnerability)** |
| Protections built in stage 6 — what they cover, measured | **[§5](#5-what-stage-6-protects--and-the-measurement)** |
| Protections that do **not** exist | **[§6](#6-what-is-not-protected)** |
| A deployed system, a public URL, TLS, backups | **none of it exists — [§1.2](#12-what-this-repository-is-not)** |

**Nothing in this repository is deployed anywhere.** There is no server, no domain, no TLS
certificate and no backup. Every measurement quoted below was taken on 2026-08-05 against
containers on one developer machine. Stage 6 is **not complete and not tagged**; what follows
describes what was built and verified, not a finished stage.

---

## 1. Scope

### 1.1 What this repository is

Two applications that do the same thing, ten years apart, running side by side as an exhibit:

| | Legacy stand | Modern stand |
|---|---|---|
| Stack | Java 8 · Spring Boot 1.5.22 · AngularJS 1.8 · WAR | Java 25 · Spring Boot 4.1 · Angular 22 · JAR |
| Database | PostgreSQL **9.6.24** — end of life since 2021-11-11 | PostgreSQL **18.4** (moved in stage 6, ADR-0012) |
| Local ports | app `8080`, db `127.0.0.1:5433` | app `8090`, db `127.0.0.1:5434`, edge overlay `8091` |
| Security posture | **insecure on purpose** — the thing being studied | migrated, and hardened only where a migration reason existed |

The legacy stand's weaknesses are **deliberate, catalogued and not defects**. They are enumerated
in [`legacy/LEGACY_NOTES.md`](legacy/LEGACY_NOTES.md) — B15 "zero security: no authentication, no
authorization, no CSRF protection", B16 the unprotected JSP admin page with a destructive POST,
B4/B3 SQL built by string concatenation, B12 no input validation, B13 deletes without referential
checks, P2 the end-of-life database. A hard rule of this project is that `legacy/` is never
modernised beyond what that file documents. **The exhibit is not retouched.**

### 1.2 What this repository is not

It is not a service. There is no deployment, no hosted instance, no TLS termination, no backup
job and no production data — [`docs/deployment.md` §10](docs/deployment.md#10-production-deployment--not-yet)
says so in the operations document itself rather than leaving the reader to find out. The data in
both stands is a synthetic seed of 10 fictional customers ([`legacy/db/init/02-daten.sql`](legacy/db/init/02-daten.sql)).
**No real personal data exists in this repository.** Where this document talks about personal
data, it talks about what happens when somebody runs this *pattern* with real customers in it —
which is the entire point of a playbook aimed at Austrian SMEs.

### 1.3 What is, and is not, a vulnerability here

| Finding | Treatment |
|---|---|
| A catalogued legacy wart (any entry in `LEGACY_NOTES.md`) | **Not a vulnerability report.** It is the subject matter. Expect a pointer to the catalogue. |
| A weakness in `legacy/` that is **not** in the catalogue | **Wanted.** The catalogue claims completeness; a gap in it is a real defect of this repo. That has happened before: the 2026-07-30 correction in `LEGACY_NOTES.md` records that B4 understated the attack surface, found by review. |
| A weakness in `modern/` | **Wanted**, unless it is a registered sanctioned divergence ([ADR-0004](docs/adr/0004-functional-equivalence-and-sanctioned-divergence.md)) or a row in `DEVIATIONS.md`. |
| A weakness in the edge overlay, the CI workflows or the AI-experiment harness | **Wanted.** |
| Anything that requires a hosted instance | There is none. Run the stands locally. |

---

## 2. Reporting a vulnerability

**Contact the repository owner (Sebastian Kern, Stoicera Software Group) through GitHub.**

**GitHub private vulnerability reporting is not enabled on this repository yet** — checked on
2026-08-05 (`gh api repos/Stoicera/migration-lab-java/private-vulnerability-reporting` →
`{"enabled":false}`), which means the *Security → Report a vulnerability* button does not appear
and pointing you at it would send you to a dead end. Enabling it is a repository-settings change
and is on the owner's checklist in
[`docs/MANUAL_TASKS.md` §E](docs/MANUAL_TASKS.md#e-repository-administration-on-github). When it
is on, that becomes the preferred route and this paragraph goes away.

**This document deliberately publishes no `security@` address**: an address nobody has committed
to monitoring is worse than no address, because it looks like a process. The same reasoning is
why the paragraph above says what is missing instead of describing a route that does not work.

Useful in a report, in this order: which stand (legacy / modern / edge overlay), the commit, what
you did, what happened, what you expected. A `curl` line beats a paragraph.

**What to expect, stated honestly rather than aspirationally.** This is a portfolio repository
maintained by one person alongside client work. There is **no SLA and no bounty**. The target is
an acknowledgement within five working days and a disposition — fix, deviation row, or "that is
the exhibit" — after triage. If a report leads to a change, the change is documented like every
other change in this repo: a dated entry, not a silent commit.

---

## 3. What is worth attacking

| Asset | Where | Why it matters |
|---|---|---|
| Workshop data — customers, vehicles, orders, invoices | both databases | Synthetic here; **customer master data plus service history** in the pattern this demonstrates |
| `POST /admin/bereinigen` | both stands | The only irreversible operation in the application. Deletes cancelled orders older than 90 days **row by row, in no transaction**, takes no body and no parameters — any POST to the path executes it |
| The write endpoints | `POST`/`PUT`/`DELETE` under `/api/kunden`, `/api/fahrzeuge`, `/api/auftraege`, `/api/rechnungen` | Unauthenticated on both stands. `DELETE /api/kunden/{id}` hard-deletes and leaves vehicles and orders behind — the code says so: *"Fahrzeuge und Auftraege bleiben stehen, hat noch nie Probleme gemacht"* (wart B13) |
| The database | `127.0.0.1:5433` / `:5434` | Loopback-bound in both compose files, so not reachable from the network — one of the few things that is right by construction |
| The Docker socket | `modern/docker-compose.edge.yml`, mounted into Traefik | Root-equivalent access to the host ([§4.6](#46-e--elevation-of-privilege)) |
| The one real credential | `OPENROUTER_API_KEY` in `.env` (git-ignored) | The only secret this project has. Everything else in the compose files is a dev-only value in plain sight, on purpose |

Trust boundaries, as they actually stand today:

```
  developer machine
  ├── legacy stand   :8080  ──►  app (no auth, ever)  ──►  PG 9.6   127.0.0.1:5433
  │                                                          EOL, unpatched
  └── modern stand   :8090  ──►  app (no auth)        ──►  PG 18.4  127.0.0.1:5434
                     :8091  ──►  Traefik  ──► app          only with the edge OVERLAY,
                                 auth · headers · rate limit         which is off by default
```

The application ports `8080` and `8090` are published on **all** interfaces, not on loopback
([`docs/deployment.md` §3](docs/deployment.md#3-the-two-stands)). On an untrusted network that is
an anonymous, destructive API. Do not run these stands where strangers can reach them.

---

## 4. STRIDE-light

One pass per category over both stands. Where a threat is mitigated, the mitigation is named with
its measurement; where it is not, it says so.

### 4.1 S — Spoofing

There is **no identity anywhere in either application**. Every request is accepted as legitimate
because there is nothing to be illegitimate against. That is inherited by design: the 2016
application had no authentication (B15), and adding it to the modern stand changes every pinned
contract, which is why it sits behind an owner-scoped decision in `DEVIATIONS.md` rather than
being smuggled in as a side effect.

Stage 6 adds **one** thing: HTTP Basic auth at the edge, in front of `/admin`, `/api/admin` and
`/actuator` ([§5](#5-what-stage-6-protects--and-the-measurement)). That is a **shared secret, not
an identity**. It answers "may this request through", never "who is this". Nothing downstream
learns the user, which is also why [§4.4](#44-r--repudiation) cannot be fixed by it.

### 4.2 T — Tampering

- **Unauthenticated writes.** Every `POST`, `PUT` and `DELETE` listed in [§3](#3-what-is-worth-attacking)
  is reachable without credentials on both bare stands. `DELETE /api/kunden/{id}` is the worst of
  them: it is a hard delete and it orphans the customer's vehicles and orders (B13, no FK
  constraints except on `auftrag_position`).
- **No transactions** (B7): the order insert plus vehicle-km update, and the cleanup delete loop,
  are not atomic. A write interrupted halfway leaves a half-applied state, and that is normal
  behaviour here, not an edge case.
- **SQL injection on the legacy stand — deliberately preserved.** `sucheKunden()` concatenates the
  search term into a `LIKE`, and the `?status=` filter of `getAuftraege()` does the same (B4, plus
  the dated 2026-07-30 correction that the original wart entry understated the surface). This is
  registered as **SD-1** in [ADR-0004](docs/adr/0004-functional-equivalence-and-sanctioned-divergence.md)
  and pinned on both sides by the characterization suite: `%' OR '1'='1` returns **200 with all 10
  customers on legacy and 200 with 0 rows on modern**; a lone `'` returns **500 on legacy with `java.lang.ArrayIndexOutOfBoundsException` in the error
  body's `exception` field** — the driver dies inside the broken SQL and Boot 1.5's default error
  JSON hands the class name to the client — and 200 with 0 rows on modern. The modern sinks are parameterised —
  carrying an injection into the target stack "for bug-for-bug equivalence" would be malpractice.
  What is *pinned* is disclosure and the error leak; **whether the legacy sink is also write-capable
  was not tested**, and this document does not claim either way.
- **No input validation** (B12) on either stand. The modern side inherited it, and it is pinned by
  the characterization suite, so removing it is an ADR-0004 decision rather than a cleanup.

### 4.3 I — Information disclosure

- **The error contract leaks internals on purpose.** `spring.web.error.include-message=always` and
  `include-exception=true` are set in `modern/src/main/resources/application.properties` to
  reproduce Boot 1.5's error body (ADR-0005 wire-format pin, consumed by the frontend). Exception
  class and message therefore reach the client. Controllers additionally `catch (Exception e)` and
  return the raw `e.getMessage()` (B10) — "damit die Telefon-Hotline was sieht", in 2016 terms.
- **The management surface is closed except for two endpoints.** Measured 2026-08-05:
  `/actuator/env`, `/beans`, `/mappings`, `/metrics`, `/loggers` and `/heapdump` all return
  **404**; only `health` and `info` are exposed, with `show-details=never`. Behind the edge
  overlay `/actuator` additionally requires credentials — health is ours to read, not free
  reconnaissance.
- **Logs contain personal data.** This is the item most likely to be waved away, so it has its own
  section: [§7](#7-logs-and-personal-data--the-part-that-must-not-be-softened).
- Legacy-only: the JSP admin page renders a gson dump of the statistics object (B16), and
  `application-prod.properties` carries a plaintext database password (B17 — a fictional demo
  value, and still exactly the pattern that leaks real ones).

### 4.4 R — Repudiation

**There is no audit log.** ENGINEERING_STANDARDS §4 requires one ("wer, was, wann"); it is
registered as **deferred(post-v1.0)** in `DEVIATIONS.md` because no stage owns it and retrofitting
write-path auditing is behaviour-adjacent work behind the ADR-0004 gate. The consequence, plainly:
after a destructive `POST /admin/bereinigen` there is **no record of who triggered it**, on either
stand, with or without the edge. The characterization suite pins *what* writes do, never *who* did
them.

The Traefik overlay writes a JSON access log to the container's stdout. That is an access log, not
an audit log, it is not shipped anywhere, and it is not retained.

### 4.5 D — Denial of service

- Neither bare stand has any rate limiting. The edge overlay does, and it is measured
  ([§5](#5-what-stage-6-protects--and-the-measurement)).
- `POST /admin/bereinigen` is a denial-of-service *and* an integrity problem in one call: the data
  it deletes does not come back, and nothing about the request is authenticated on a bare stand.
- **PostgreSQL 9.6 on the legacy stand receives no security patches of any kind** and never will
  again (final release 2021-11-11). It stays, because it is the exhibit — registered as **P2** in
  `DEVIATIONS.md`. The modern stand moved to 18.4 in stage 6 (ADR-0012). "The legacy stand keeps an
  unpatched database" is a statement about a demonstration container on a laptop; it would be an
  unacceptable statement about anything reachable from a network.
- No CPU or memory limits are set in either compose file.

### 4.6 E — Elevation of privilege

The applications have no privilege levels, so there is nothing to elevate *inside* them. The real
elevation path is the host, and stage 6 introduced it knowingly:

**`/var/run/docker.sock` is mounted into the Traefik container** so that Traefik's Docker provider
can read the routing labels. The mount is `:ro`, and that flag protects the socket *file*, not the
Docker API reachable through it. Any container that can talk to that socket must be treated as
**root-equivalent on the host** — Docker's own documentation states the equivalence for the
`docker` group, and a container with the socket is in the same position. It is the largest single
risk in `modern/docker-compose.edge.yml`, which is why the file says so in a comment instead of
hiding it.

Reduced, not removed: `--providers.docker.exposedbydefault=false` means a new container does not
become publicly routable merely by existing, and `--api=false` means there is no dashboard. A real
deployment would have to do better than that — a filtered socket proxy, or Traefik's file provider
instead of the Docker provider. Neither has been built here.

---

## 5. What stage 6 protects — and the measurement

**Read this line first: the protections below are an opt-in overlay.** A plain
`docker compose -f modern/docker-compose.yml up -d --wait` has **no authentication, no security
headers and no rate limiting**. They exist only when `modern/docker-compose.edge.yml` is layered
on top, which requires `MODERN_ADMIN_AUTH` to be set. That was a deliberate trade: a required
variable in the base file would break the quickstart for everyone.

Authentication sits in the reverse proxy rather than in the application (ADR-0014). The
scope-respecting reason is that `DEVIATIONS.md` already records the requirement as *"the public
demo deployment must protect `/admin` at minimum (reverse-proxy auth counts)"*. The measured reason
is blast radius: a Spring Security matcher scoped to `/admin` breaks exactly **4 tests**, and CSRF
with the Spring default matcher breaks **17 characterization write calls plus every e2e write
scenario** — each one an ADR-0004 sanctioned-divergence decision, i.e. a different stage of work.
The honest counter-argument, recorded rather than avoided: auth that only exists in a compose
overlay can be called theatre. The answers are that Traefik is the same component the target
platform runs, that the boundary is verified by a script rather than asserted, and that on a real
host the application port would not be published at all.

All of the following was produced by [`modern/edge/verify-edge.sh`](modern/edge/verify-edge.sh)
against the running stands on **2026-08-05**, exit 0:

| Check | Measured |
|---|---|
| `GET /admin`, `GET /api/admin/statistik`, `POST /admin/bereinigen`, `GET /actuator/health` — no credentials | **401** |
| `GET /admin`, `GET /api/admin/statistik`, `GET /actuator/health` with correct credentials | **200** — a lock nobody can open is a broken lock, not a secure one |
| `POST /admin/bereinigen` with correct credentials | **not asserted by the script, on purpose** — it deletes data, and a verification step that destroys state every time it runs is a bad trade. Its 401 without credentials is asserted, which is the half that matters for the boundary |
| `GET /admin` with a wrong password | **401** |
| `GET /`, `GET /api/kunden`, `GET /rechnungen` — the public surface | **200** (the edge must not become an outage) |
| Security headers on public responses | `X-Frame-Options: DENY`, `X-Content-Type-Options: nosniff`, `Referrer-Policy: no-referrer`, full CSP |
| Rate limit (average 30/s, burst 60), 200 **concurrent** requests through the edge | **> 0 × 429** is what the script asserts; five consecutive runs on 2026-08-05 produced 103–173. The exact count is machine-dependent and is not a property of the configuration — which is why the assertion is `> 0` and not a number |
| The same burst straight at the application on `:8090` | **0 × 429** — the limit lives in the edge, as designed |

Two configuration choices in that set are deliberate and would be wrong to "fix":

- **HSTS is off** (`stsSeconds: 0`). Switched on over plain HTTP it teaches the browser to refuse
  `http://localhost` for a year. Only a TLS-terminating host sets it — and there is none.
- **`/actuator` is behind the auth**, not public. Health is operationally useful to us and free
  reconnaissance for anybody else.

### 5.1 The CSP finding — a green suite that proved less than it looked like

The first policy was fully strict, including `style-src 'self'`. The E2E suite ran **32 of 32
green through the edge**. The browser console showed the policy being violated and Angular's
runtime-injected component styles being **blocked**.

**The Selenium safety net cannot see a CSP violation.** It asserts behaviour and text, not
appearance. The suite was green while the page was visibly broken, and it was found only by
opening a real browser and reading the console.

The resolution was `style-src 'self' 'unsafe-inline'` and **nothing else relaxed** —
`script-src 'self'` stays strict, and that is the directive that stops injected code from
executing. Re-measured after the change: **0 console errors**. `verify-edge.sh` now asserts
`script-src 'self'` verbatim so that a later "just add `unsafe-inline` until it works" cannot slip
through, and it prints the blind spot in its own output. The clean fix is Angular's `CSP_NONCE`,
which requires `index.html` to be rendered per request instead of served as a static resource —
owner-scoped, and open in `DEVIATIONS.md`.

Also worth knowing when reading test results: the characterization and E2E suites talk to the
application **directly** on `:8090`, deliberately, so the safety net keeps a path that does not
depend on the proxy. A green safety net therefore says nothing about the edge. Only
`verify-edge.sh` does.

---

## 6. What is not protected

Stated as a list rather than left to inference, because the gap between "we have Basic auth" and
"this is secured" is exactly where migration projects get sold something they did not buy.

| Control (ENGINEERING_STANDARDS §4) | Status | Where it is recorded |
|---|---|---|
| OAuth2/OIDC, Keycloak as IdP | **not built** | `DEVIATIONS.md` — owner re-scoping needed; changes every pinned contract |
| Any authentication **inside** the application | **not built** — the app authenticates nobody; the edge is the only gate | inherited by design (B15) |
| Any authentication on the **legacy stand** | **none, and none is planned** | it is the exhibit — `LEGACY_NOTES.md` B15/B16 |
| Per-user identity, roles, authorization | **not built** | there is one shared Basic-auth credential, nothing more |
| CSRF token | **deliberately not added** — see [§8](#8-the-residual-risk-we-accept-basic-auth-and-the-browsers-credential-cache) | the app has no sessions, no cookies, no ambient authority |
| Audit log | **not built** | `DEVIATIONS.md`, deferred(post-v1.0) |
| TLS | **not built** — there is nothing to terminate it on | `docs/deployment.md` §10 |
| Backups | **not built** | `docs/deployment.md` §10 |
| Input validation at the boundary | **not built** on either stand | wart B12, pinned by characterization |
| Container image scanning | **built 2026-08-05, reporting-only** | Trivy scans the built modern image for CRITICAL/HIGH in OS packages and Java dependencies in `modern-ci` — the base-image layer no dependency scanner can see. Runs with `exit-code: 0`, so it reports and does not block; recorded that way in `DEVIATIONS.md` |
| OWASP dependency-check | **not built** | `DEVIATIONS.md` — Dependabot covers the Maven modules, the frontend npm ecosystem and the Actions; `legacy/` is excluded on purpose |
| Secret management beyond `.env` | **not built** | one credential exists (`OPENROUTER_API_KEY`); measured 2026-08-05: the repository has **no GitHub Actions secrets at all** |

---

## 7. Logs and personal data — the part that must not be softened

The application logs **customer names** at `INFO`:

```java
LOG.info("Kunde angelegt: " + kunde.getAnzeigeName() + " (id=" + neueId + ")");   // WerkstattService:155
LOG.info("Loesche Kunde " + id);                                                   // WerkstattService:175
LOG.warn("Bereinigung: " + ids.size() + " stornierte Auftraege geloescht");        // WerkstattService:714
```

and the **raw user search term** at `DEBUG` (`LOG.debug("Kundensuche: " + suchbegriff)`,
`WerkstattService:84`). `DEBUG` does not ship at the default level — it is one `logging.level`
change away from shipping, and that change is exactly what an operator makes while debugging a
search problem. On the legacy stand the same line logs the **fully interpolated SQL string**,
injected input included.

Stage 6 turned the container's log output into structured **ECS 8.11 JSON**
(`LOGGING_STRUCTURED_FORMAT_CONSOLE=ecs`, measured valid, with `trace.id`/`span.id` correlation
working). Structured JSON exists for exactly one purpose: to be collected and forwarded. The
moment a real deployment attaches a log shipper — which is the point — those customer names leave
the machine and land in a searchable store.

**That store then holds personal data.** For an Austrian audience this is not a detail: names of
identified natural persons in a searchable third-party system is a processing activity under the
GDPR, with everything that follows — legal basis, retention, a processor agreement with whoever
runs the log store, and an entry in the record of processing activities.

**And it cannot be cleanly redacted, because of how the log lines are written.** Every statement
above is **string concatenation**, not a parameterised message with MDC fields. The personal data
ends up inside the free-text `message` field of the ECS document. There is no structured field to
drop, mask or exclude — a redaction processor would have to regex free text, which fails silently
the first time a name looks like something else.

What a real deployment would have to do, in the order that actually works:

1. **Decide whether these lines are needed at all.** The cheapest personal data to protect is the
   kind that is never written. `LOG.info("Kunde angelegt (id=" + neueId + ")")` carries the same
   operational information.
2. **Convert the remaining ones to parameterised logging with named fields** (`log.info("Kunde
   angelegt", kv("kundeId", id))`) so that a shipper can drop or hash a *field*, not guess at a
   sentence.
3. **Keep the search-term line off in production** and treat raising the log level as a decision
   with a data-protection consequence, not a routine debugging step.
4. **Set retention on the log store** and put it in the processor contract and the record of
   processing activities.

None of the four has been done in this repository. The demo seed is fictional, so nothing here is
at risk — the exposure belongs to whoever copies this pattern onto real customers, which is
precisely the audience the playbook is written for.

---

## 8. The residual risk we accept: Basic auth and the browser's credential cache

The application has no sessions, no cookies and no ambient authority, so there is nothing for a
classic CSRF token to protect — which is why one was not added, and the reasoning is recorded
rather than the omission.

What Basic auth at the edge **does** introduce is a browser credential cache. Once an operator has
authenticated to `/admin` in their browser, the browser keeps the credentials for that realm and
re-sends them automatically to that origin. A cross-site `POST` — a form on any other page the
operator visits, targeting `/admin/bereinigen` — would therefore arrive **authenticated**, and
`POST /admin/bereinigen` needs no body, no parameters and no token to execute the deletion.

Three things must be said about mitigations so that nobody assumes one is in place:

- **The CSP does not help.** A Content-Security-Policy is enforced against the page that contains
  the form. The attacker's page ships its own policy; ours applies to ours. `form-action 'self'`
  constrains where *our* pages may submit, not where other people's pages may submit *to us*.
- **`SameSite` does not help.** It is a cookie attribute, and there is no cookie — the credentials
  travel in an `Authorization` header the browser attaches on its own.
- **The rate limiter does not help.** One request is enough.

**This risk is accepted for a local demonstration** and it is written here instead of being left
for someone to find. The real fix is the auth stage that does not exist yet: session-based OIDC
plus a CSRF token, which is owner-scoped work with its own ADR (ENGINEERING_STANDARDS §4). Until
then, the operational rule is the boring one that actually works — use a separate browser profile
for `/admin`, and close it afterwards.

---

## Deutsche Kurzfassung

Dieses Dokument ist die **Sicherheitsbetrachtung** des Repos und erfüllt `ENGINEERING_STANDARDS.md`
§4 (STRIDE-light-Threat-Model), offen seit 2026-07-31, geschlossen am 2026-08-05.

**Nichts davon ist deployed.** Es gibt keinen Server, keine Domain, kein TLS, keine Backups. Alle
genannten Messungen stammen von lokalen Containern auf einem Entwicklungsrechner, 2026-08-05.

Die fünf Punkte, auf die es ankommt:

1. **Der Legacy-Stand ist absichtlich unsicher.** Keine Authentifizierung, SQL-Injection im
   Kundensuchfeld, PostgreSQL 9.6 ohne Sicherheitsupdates. Das ist das Ausstellungsstück und wird
   nicht repariert — der Katalog dazu ist `legacy/LEGACY_NOTES.md`.
2. **Beide Stände haben schreibende Endpunkte ohne Anmeldung**, inklusive des endgültig löschenden
   `POST /admin/bereinigen` und eines `DELETE /api/kunden/{id}`, das Fahrzeuge und Aufträge
   verwaist zurücklässt.
3. **Was Etappe 6 schützt, schützt nur mit dem Edge-Overlay** (`modern/docker-compose.edge.yml`):
   Basic-Auth vor `/admin`, `/api/admin` und `/actuator`, Security-Header, Rate Limiting.
   Gemessen mit `modern/edge/verify-edge.sh` ([§5](#5-what-stage-6-protects--and-the-measurement)).
   Ohne Overlay ist nichts davon aktiv.
4. **Was es nicht gibt:** kein OAuth2/OIDC, kein Audit-Log, kein CSRF-Token, keine Authentifizierung
   in der Anwendung selbst, kein TLS, keine Backups ([§6](#6-what-is-not-protected)).
5. **Logs enthalten Kundennamen** (`INFO`) und — bei erhöhtem Level — den rohen Suchbegriff. Seit
   Etappe 6 sind die Container-Logs strukturiertes JSON, also für den Versand an ein Log-System
   gemacht. Sobald das jemand anschließt, verlassen personenbezogene Daten die Maschine, und weil
   die Log-Zeilen zusammengesetzte Strings ohne strukturierte Felder sind, lässt sich dort nichts
   sauber schwärzen. Was ein echter Betrieb dafür tun müsste, steht in
   [§7](#7-logs-and-personal-data--the-part-that-must-not-be-softened).

**Schwachstellen melden:** über den Repository-Eigentümer auf GitHub. Die private
Meldefunktion von GitHub ist derzeit **nicht aktiviert** (geprüft am 2026-08-05) — ein Verweis
darauf würde ins Leere führen. Eine `security@`-Adresse gibt es bewusst nicht —
[§2](#2-reporting-a-vulnerability).
