# characterization/ — golden-master tests

Captures of the legacy application's **actual** behaviour. They define
functional equivalence for the whole migration (ADR-0004): a migration step is
correct when these still pass — quirks included.

Three suites (Java 25, JUnit 5, no framework beyond JDK http + Jackson + JDBC):

- **`ApiCharacterizationTest`** — read endpoints (incl. the B4 search endpoint
  `/api/kunden?suche=`) vs. `src/test/resources/golden/`. Comparison on parsed
  JSON trees (key order/whitespace irrelevant, values exact); the admin page
  forks per stand since SD-2 (legacy: JSP golden as normalized HTML, dates
  masked; modern: SPA shell + strict `/api/admin/statistik` pin — exact key
  sets, types, seed values; the modern admin UI FLOW is e2e `AdminTest`).
  Since session 10 it also pins every SPA document route per stand (modern:
  200 + shell; legacy: 404, hash routing has no path documents). On mismatch
  the received document lands in `target/characterization-received/` for
  diffing. Also carries the sanctioned hostile-search divergence (see below).
- **`ErrorContractCharacterizationTest`** — the 4xx/5xx surface the UIs
  consume: 404-without-body for missing resources, the 500-with-German-message
  contract of the write paths (illegal status transition, invoice on a
  non-FERTIG order, duplicate invoice), and the Boot default error JSON where
  no controller catch exists (B12: the DB NOT-NULL constraint is the only
  "validation"). Identical on both stands; the modern stand carries a
  `spring.web.error.*` wire-compat shim for the `exception`/`message` fields
  Boot 2+ hides by default.
- **`DbStateCharacterizationTest`** — DB state transitions of the write paths:
  the complete legal status transition matrix (incl. the FERTIG→IN_ARBEIT
  special case that keeps `fertig_am`, and the `fertig_am`/`abgeholt_am`
  side-effect columns), Auftrag/Rechnung number generation, invoice amounts
  incl. a double-rounding boundary case (B8), the destructive
  `/admin/bereinigen` cleanup, and the documented quirks (vehicle-km side
  effect on order creation, orphaned rows after customer deletion — B13).

## Run

```bash
# against the legacy stand (defaults)
docker compose -f legacy/docker-compose.yml up -d --wait
./mvnw verify -f characterization/pom.xml

# against the modern stand — its own stand, and all THREE flags
docker compose -f modern/docker-compose.yml up -d --wait
./mvnw verify -f characterization/pom.xml \
  -DbaseUrl=http://localhost:8090 \
  -DdbUrl=jdbc:postgresql://localhost:5434/werkstatt \
  -Dstand=modern
```

`--wait` blocks until the healthchecks pass; without it the suite hits a stand
that is not yet accepting connections and fails on timing rather than on
behaviour.

> **This suite has no `-Dtarget`.** That flag belongs to `e2e/` only, where it
> switches base URL, DB URL and selector map together. Here there are three
> separate flags and you need all three — `-DbaseUrl` alone tests the modern app
> against the legacy database, `-Dstand=modern` alone tests modern expectations
> against port 8080. Passing `-Dtarget` used to be silently ignored, which
> produced a **green run against the wrong stand**; since 2026-08-02 it fails
> fast instead.

### The `stand` property — sanctioned divergence (ADR-0004)

Every pin must hold identically on both stands — except where a stage
deliberately changed behaviour and ADR-0004 sanctioned it. There the
expectation is forked on the `stand` system property (`legacy` | `modern`,
default `legacy`, any other value fails fast), and BOTH sides' exact observed
behaviour stays pinned. Currently the only sanctioned divergence is the B4
search endpoint under hostile input (stage 4 replaced string-concatenated SQL
with bind parameters):

| Input to `/api/kunden?suche=` | legacy (pinned)                          | modern (pinned) |
|-------------------------------|------------------------------------------|-----------------|
| `%' OR '1'='1`                | 200, all 10 customers leak               | 200, 0 rows     |
| `'` (lone quote)              | 500, Boot error JSON (broken SQL)        | 200, 0 rows     |

Legitimate search input is golden-mastered and identical on both stands.

## Determinism

- `ApiCharacterizationTest` resets the database to the committed seed
  (`legacy/db/init/02-daten.sql` — read from the repo, never copied) once in
  `@BeforeAll`; its tests only read.
- `DbStateCharacterizationTest` and `ErrorContractCharacterizationTest` reset
  before EVERY test — no ordering dependency, each test stands alone against
  seed rows.
- **Clock coupling is computed, not hard-coded.** The app derives
  Auftrag/Rechnung numbers from the current year (MAX+1 per year); expected
  numbers come from `Seed.java` via `java.time.Year.now()` —
  `A-2026-0017`/`R-2026-0009` while it is 2026, `A-<year>-0001`/`R-<year>-0001`
  in any later year. The `/admin/bereinigen` expectation (STORNIERT orders
  older than 90 days — a moving predicate over fixed seed dates) is computed
  at runtime with the app's own SQL predicate. Bericht endpoints are pinned
  with an explicit `?jahr=2026`, never the implicit current-year default. The
  volatile date on the admin page is masked before comparison.

So: reproducible on any machine, in any test order, on any date — under the
stability assumptions below.

## Stability assumptions (declared, not hidden)

- **Array order** in the goldens is meaningful only because every list
  endpoint has a SQL `ORDER BY`; the goldens inherit exactly that order.
- **Collation:** the ordering of German names comes from the database's
  collation, and a PostgreSQL upgrade is therefore a **golden-impact event**
  (docs/DEVIATIONS.md P2) that follows the ADR-0007 procedure like any contract
  change. Since 2026-08-05 the two stands **no longer run the same PostgreSQL
  major** — legacy stays on 9.6 as the exhibit, modern runs 18 (ADR-0012) — and
  the goldens survived that unchanged. Read that result precisely, because the
  earlier wording here overstated it: the orderings were **measured** equal on
  this seed, with the modern stand's locale pinned to the legacy stand's
  `en_US.utf8`. It is not a property of the data. The same upgrade against
  `postgres:18-alpine` reorders the customer list while still reporting
  `datcollate = en_US.utf8`, so the collation must be pinned and then verified
  by sorting — `WerkstattServiceIntegrationTest` does exactly that, because a
  check that reads the setting would have passed.
- **Timestamps** in the goldens are raw epoch millis — the wire format the
  AngularJS frontend consumes, kept on the modern stand by the wire-compat pin
  (ADR-0005). They encode the seed's fixed dates as interpreted in the stands'
  timezone (both containers run UTC); golden validity is tied to the committed
  seed and that timezone.
- **Error-message prose:** the B12 missing-`nachname` pin asserts the Boot
  error JSON shape exactly but the `message` field by containment — Spring 4.x
  appended "; nested exception is …" prose that Spring 6+ dropped. The
  meaningful content (the PG constraint text) is asserted on both stands.
  Likewise, the charset parameter of `Content-Type` on error responses
  (`application/json;charset=UTF-8` vs `application/json`) is not pinned.

## Re-capturing a golden

Only legitimate when a behaviour change is SANCTIONED — ADR-0007 (golden
governance): the re-captured file, the ADR-0004 register entry (or ADR), and a
conventional commit naming the change travel together in the same commit.
Never hand-edit a golden. CI re-validates every golden against the freshly
built legacy stand, so silent tampering turns the pipeline red.

Procedure, against the pristine seeded legacy stand:

```bash
docker compose -f legacy/docker-compose.yml up -d

# 1. reset to the committed seed (same thing the tests do)
docker exec -i legacy-db-1 psql -U werkstatt -d werkstatt \
  -c "TRUNCATE kunde, fahrzeug, auftrag, auftrag_position, rechnung RESTART IDENTITY CASCADE"
docker exec -i legacy-db-1 psql -U werkstatt -d werkstatt < legacy/db/init/02-daten.sql

# 2. JSON golden: pretty-printed capture of the endpoint
#    (endpoint ↔ file mapping: the @CsvSource table in ApiCharacterizationTest
#     is the authoritative list)
curl -s "http://localhost:8080/api/kunden" \
  | python3 -c "import sys,json;print(json.dumps(json.load(sys.stdin),indent=4,ensure_ascii=False))" \
  > characterization/src/test/resources/golden/kunden.json

# 3. admin page golden: normalized exactly like the test normalizes
#    (ApiCharacterizationTest.normalizeAdminHtml — dd.MM.yyyy dates masked)
curl -s "http://localhost:8080/admin" \
  | sed -E 's/[0-9]{2}\.[0-9]{2}\.[0-9]{4}/XX.XX.XXXX/g' \
  > characterization/src/test/resources/golden/admin.html

# 4. prove it: suite green against BOTH stands, twice each
./mvnw verify -f characterization/pom.xml
./mvnw verify -f characterization/pom.xml \
  -DbaseUrl=http://localhost:8090 -DdbUrl=jdbc:postgresql://localhost:5434/werkstatt -Dstand=modern
```
