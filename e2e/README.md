# e2e/ — Selenium safety net

Selenium 4 + JUnit 5 + Java 25. **The same scenarios run against both UIs** —
page objects address elements by intent key only; the mapping to concrete CSS
selectors lives in `src/test/resources/selectors/<target>.properties`.
Stage 5 proved the design: porting the suite to the Angular UI meant new
`modern.properties` values, two `wait.strategy` implementations and ONE
sanctioned per-stand expectation (SD-3, below) — zero new scenarios. A
`SelectorMapParityTest` guards that both maps carry identical key sets.

## Scenarios (9 classes, 27 tests + 1 map-parity guard)

| Class | Flow |
|---|---|
| `DashboardTest` (2) | start-page KPIs and both workshop tables show the exact seed state |
| `KundenCrudTest` (4) | create → edit → search (hits the legacy search path) → delete |
| `KundeDetailTest` (1) | seed customer: full master data + vehicle list (the view shows **no** orders — that is the 2016 UI, not a test gap) |
| `FahrzeugeTest` (3) | global list from seed; create and delete a vehicle via the owning customer (the only flow the legacy UI has) with list deltas |
| `AuftragLebenszyklusTest` (4) | accept → work + position → finish → pick up, list states |
| `AuftragSonderfaelleTest` (3) | ANGENOMMEN→STORNIERT, FERTIG→"Zurück in Arbeit", add/remove position with net-sum recalculation |
| `RechnungTest` (4) | invoice from finished seed order (exact number, 20% USt math), mark paid, list→detail navigation with position lines, duplicate-invoice rejection |
| `BerichtTest` (4) | monthly report numbers of the frozen seed months, top customer, real year-switch round trip |
| `ValidierungTest` (2) | required-field alerts on kunde-neu and auftrag-neu — nothing saved, exact messages pinned |

## Coverage philosophy and its limits

The suite covers the **Monday-morning-call flows**: everything the workshop
does at the counter, once each, through the real UI, within a runtime budget
of well under 60 s per stand (currently ~23 s including Maven).

Deliberately **not** covered at the UI level — these permutations are held by
the `characterization/` layer, where they are cheaper and more precise:

- the full status-transition matrix and its timestamp semantics (DB-state characterization),
- invoice rounding edge cases (characterization),
- search with hostile input (single quote, injection patterns — characterization + ADR-0004),
- the orphaned-vehicles side effect of deleting a customer (DB-state characterization),
- the admin JSP page and the 90-day cleanup job (golden master + DB-state characterization),
- the 4xx/5xx error-contract surface across all endpoints (error-contract characterization).

Known UI-only gaps, accepted with reason: the order status filter buttons, the
"nur unbezahlte" checkbox and the print button are trivial client-side
conveniences; covering them buys almost no migration safety against the
runtime budget. E2E proves each UI mechanism once — exhaustiveness lives one
layer down.

## Zero-flake rules (binding)

1. **Explicit waits only** (`support/Waits`, 10 s / 200 ms poll). Implicit waits
   are set to ZERO in the driver — mixing wait styles is the classic flakiness
   source. No sleeps, no retries, ever.
2. **Deterministic data**: every scenario class resets the DB to the committed
   seed in `@BeforeAll` (`support/DbReset`: TRUNCATE + canonical
   `legacy/db/init/02-daten.sql`). Assertions may rely on exact seed values.
   Tests write only into the current month; report assertions use frozen past
   months. The seed splitter fails loudly on anything its line-based `;`-split
   cannot handle (dollar quotes, semicolons inside literals, unterminated
   statements) instead of corrupting statements silently.
3. **Settled views** — the suite works around **three documented AngularJS
   races** found during stabilisation and review (see playbook ch. 1):
   1. lists have no request cancellation — two in-flight loads re-render
      last-response-wins, so `open()` settles the initial load before any
      interaction;
   2. ngRoute does not re-instantiate the controller when the current route's
      nav link is clicked again — `open()` therefore starts from a full page
      load;
   3. templates render before their async data arrives and writes give no
      visible feedback — `kunde-detail` is gated on a populated form
      (`KundeDetailPage.waitLoaded`), async-filled dropdowns are gated on the
      expected option (`AuftragNeuPage`), and every write that produces no DOM
      change is gated on `Waits.idle()` (navigating away too early aborts the
      in-flight request and silently loses the update).
4. A red test is analysed with evidence (screenshot in `target/screenshots/`),
   fixed deterministically, and the finding is logged. Never retried-until-green,
   never `@Disabled`, zero skips.

## Wait strategy (per target)

`Waits.idle()` — the "no pending HTTP work" gate — dispatches on the
`wait.strategy` key of the selector map. Three strategies exist:

- `angularjs` (legacy map): polls `$http.pendingRequests`.
- `angular` (modern map since the stage-5 cutover): polls the app-maintained
  counter `window.werkstattOffeneRequests` (an HTTP interceptor in
  `modern/frontend`) — the app is **zoneless**, so the classic Testability
  `isStable` probe observes nothing; the counter is the app's testability
  contract, same semantic as `$http.pendingRequests`.
- `hybrid` (used during the stage-5 route-by-route port, kept as a documented
  strategy): dispatches per CURRENT document on whichever framework marker is
  present — one flow could legally cross from an Angular page to an AngularJS
  page mid-scenario; neither marker present counts as "not idle".

Unknown values **throw** — fail-loud by design: a wrong strategy times out on
every save, and silently skipping the gate would reintroduce the lost-update
race (#3 above). Weakening the wait is not an option.

## Year handling

The backend derives document numbers from the **wall-clock year**
(`A-<year>-<max+1>` / `R-<year>-<max+1>`, MAX per year). The seed contains
orders/invoices only in 2026, so the next numbers are `A-2026-0017` /
`R-2026-0009` while the clock is in 2026 and `…-0001` in any later year.
Tests never hard-code these — `support/Seed` computes them from
`java.time.Year.now()`.

The report tests select the seed year **2026 explicitly**. This keeps working
on any date because the legacy year dropdown is built from the current year
down to 2016, so 2026 never disappears. Two deterministic paths in
`BerichtPage.jahr(jahr, erwarteteGesamtAuftraege)`:

- **already selected** (wall clock still 2026): selecting the selected option
  fires no change event — a documented no-op; the displayed data already
  belongs to that year.
- **real change** (wall clock ≥ 2027, and always exercised by
  `BerichtTest.jahresWechselLaedtNeu` via the empty year 2025): waiting on the
  12-row count would be vacuous — the API always returns 12 rows and ng-repeat
  re-renders in place — so the gate is `idle()` plus the exact text of the
  "Gesamt/Aufträge" cell, which provably differs (0 for empty years vs 16
  for 2026 on a fresh seed).

## Ordered flows inside a class — deliberate trade-off

One scenario class = one ordered user flow (`@TestInstance(PER_CLASS)` +
`@Order`) in one browser session against one DB reset. Later steps depend on
earlier ones (create → edit → delete), so a failing early step fails the rest
of the class. We accept that: the flows mirror how the workshop actually
works, isolation lives at class level (fresh DB + fresh browser per class),
and the alternative — full isolation per test — would triple the runtime for
no additional migration safety. Mirrored in playbook ch. 1.

## Formatting contract (pinned on purpose)

Assertions like `"1439,00 €"`, `"08.07.2026"` and the exact alert texts pin
the **UI formatting contract** of the legacy app (hand-rolled `euro` filter,
`dd.MM.yyyy` dates, controller alerts). The stage-5 Angular UI must reproduce
these byte-for-byte or the change is sanctioned via ADR-0004 — silent
formatting drift is a migration defect, not a cosmetic detail.

That includes a pinned **defect** — now with a per-stand expectation (SD-3,
ADR-0004): on server-rejected actions (e.g. creating a second invoice for an
order) the LEGACY UI shows an alert reading literally `undefined`. Spring Boot
1.5 content-negotiates the plain-string 500 body to
`Content-Type: application/json` for XHR Accept headers, AngularJS 1.8's
response transform throws `[$http:baddata]` on the non-JSON body, and the
error callback alerts `fehler.data` of an `Error` object. Pinned as-is on
legacy (honesty rule). The stage-5 Angular UI surfaces the backend's German
message instead — the expected alert text lives in the selector maps
(`alert.rechnungDuplikat`), so ONE assertion pins both behaviours per stand.
The *server-side* message contract (identical on both stands) is held by the
error-contract characterization tests.

## Selector discipline

Raw selectors never live in page objects — every element goes through
`SelectorMap` intent keys (`SelectorMap.css`), non-selector per-target values
through `SelectorMap.value`. The 2016 markup offers no ids for table cells, so
the legacy map's cell access is positional (`nth-child` in the map or index in
the page object); wherever that is unavoidable, the tests **pin the column
headers** (or section headings on the dashboard) first, so a silent
column/section reorder fails loudly instead of asserting the wrong cell. The
stage-5 Angular UI carries explicit `data-testid` anchors instead — the modern
map addresses those, and the key sets of both maps are guarded byte-identical
by `SelectorMapParityTest`.

## Run

```bash
docker compose -f legacy/docker-compose.yml up -d
./mvnw verify -f e2e/pom.xml -Dtarget=legacy
docker compose -f modern/docker-compose.yml up -d
./mvnw verify -f e2e/pom.xml -Dtarget=modern
```

Requires Chrome/Chromium (Selenium Manager resolves the driver; CI uses the
preinstalled Chrome on ubuntu runners).
