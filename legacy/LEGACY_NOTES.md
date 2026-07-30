# LEGACY_NOTES — the deliberate wart catalogue

WerkstattCRM is **synthetic but pattern-faithful** (ADR-0001). Every legacy pattern
below is deliberate, mapped to the real-world smell it represents, and stays put
until a migration stage removes it *for a migration reason*. This file is the
completeness criterion for stage 0: if a wart is in the code, it is in this list.

Rule from `CLAUDE.md`: code in `legacy/` keeps its 2016-era style. Never modernise
beyond what this file documents.

## Platform & dependencies

| # | Wart | Real-world smell it represents |
|---|------|-------------------------------|
| P1 | Spring Boot **1.5.22** (EOL 2019), Java 8, `javax.*` | The starting point of most Austrian SME migrations |
| P2 | PostgreSQL **9.6** (EOL 2021), driver version managed by ancient BOM | Databases nobody dares to touch |
| P3 | **log4j 1.2.17** (EOL 2015) via `slf4j-log4j12`, `log4j.properties` from "the old Tomcat", log file written to the working directory | Logging setups inherited across three server generations |
| P4 | **gson 2.3.1** next to Jackson — used only in `AdminController` ("copied from a forum") | Two libraries for one job, nobody consolidated |
| P5 | **commons-lang 2.6** declared in the pom, used nowhere | Dependencies accumulate, none are ever removed |
| P6 | Frontend libraries **vendored** into `webapp/lib/` (AngularJS 1.8.2 — EOL 2022, Bootstrap 3.3.7), no package manager | Pre-npm frontend asset management |
| P7 | WAR packaging with embedded Tomcat + **JSP** support | The "we deployed to Tomcat once" heritage |

## Architecture & backend code

| # | Wart | Where | Real-world smell |
|---|------|-------|------------------|
| B1 | **God class**: one `@Service` holds customers, vehicles, orders, invoices, reports, admin stats (~750 lines) | `WerkstattService` | The grown service layer every legacy app has; primary target for the AI test-gen experiment (G6) |
| B2 | **Field injection** everywhere (`@Autowired` on fields) | all controllers + service | Untestable-by-design wiring; constructor-injection sweep happens in stage 4 |
| B3 | SQL built by **string concatenation** for ids and status values | most reads in `WerkstattService` | JdbcTemplate era-typical; blocks parameterised-query hygiene |
| B4 | **SQL-injection-shaped smell (flagged!)**: user input concatenated into `LIKE` | `WerkstattService.sucheKunden()` | THE security finding of the migration story — found by review, fixed in a migration stage, told in the playbook. Deliberately the only user-input injection point |
| B5 | Row mapping **duplicated inline** three times for Kunde; only Fahrzeug got an extracted mapper ("2018, Hr. F.") | `WerkstattService` | Inconsistent evolution: refactorings that stopped halfway |
| B6 | Order/invoice numbers via **`MAX+1`** with no UNIQUE constraint | `neuerAuftrag()`, `erstelleRechnung()`, schema | Race condition that "never happened because there is only one desk" |
| B7 | **No transactions**: order insert + vehicle km update non-atomic; cleanup deletes row-by-row | `neuerAuftrag()`, `bereinigeStornierte()` | Data integrity by luck |
| B8 | **Money as `double`** + manual rounding, VAT as int property, invoice stores netto/ust/brutto redundantly | models, `erstelleRechnung()` | The classic; BigDecimal conversion is a migration chapter with real numbers |
| B9 | `static SimpleDateFormat` shared across threads | `WerkstattService.DATUM` | Thread-safety bug class of the `java.util.Date` era |
| B10 | Business errors as `RuntimeException`; controllers `catch (Exception e)` → HTTP 500 with raw `e.getMessage()` | all controllers | No error contract; internals leak to the client "to help phone support" |
| B11 | DB entities serialized **directly as JSON**, display fields (`kundeName`) glued onto domain objects, computed getters serialized | models | No DTO boundary; API shape is an accident of the schema |
| B12 | **No input validation** ("das Frontend schickt schon das Richtige") | all controllers | Trust-the-client, 2016 edition |
| B13 | Deletes without referential checks; **no FK constraints** except `auftrag_position` (added 2018 "after a data problem") | schema + `loescheKunde()` | Orphaned rows as a known, tolerated condition — the data-quality surprise of every real migration |
| B14 | `System.out.println` debug leftovers; **commented-out method** kept "as reference"; dated TODO comments naming staff | `WerkstattService` | Version control by comment |
| B15 | **Zero security**: no authentication, no authorization, no CSRF protection | whole app | "Runs only in the workshop LAN" |
| B16 | JSP admin page: second UI technology, unprotected, with a **destructive POST** (permanent cleanup) and a gson debug dump | `AdminController`, `admin.jsp` | The forgotten server-rendered page every SPA-era app still drags along; absorbed in stage 5 |
| B17 | Config duplication with drift: `application-prod.properties` disagrees on version, misses the UID, contains a plaintext DB password (fictional demo value) | `src/main/resources` | Copy-paste environments; "please also update the other file" comments |
| B18 | Schema = hand-run SQL files, no migration tool, no history | `db/init/` | "The schema lives on the server"; Flyway arrives with a migration stage |
| B19 | Mixed German/English identifiers, German REST paths (`/api/kunden`), German status strings as `static final String` (no enum) | everywhere | Absolutely faithful to Austrian in-house software |

## Frontend

| # | Wart | Where | Real-world smell |
|---|------|-------|------------------|
| F1 | All controllers in **one `controllers.js`** — except `bericht-controller.js` ("the intern, summer 2018") | `js/` | File organisation by archaeology |
| F2 | `$scope` soup, logic in controllers, no components | all controllers | The AngularJS style that makes route-by-route Strangler-Fig porting (stage 5) the right strategy |
| F3 | `Api` factory exists but **half the controllers call `$http` directly** | `services.js` vs. `controllers.js` | Abstractions introduced, never enforced |
| F4 | `alert()` / `confirm()` for errors and destructive confirmations | controllers | UX of the intranet era |
| F5 | Custom `euro` filter because the `currency` filter shows `$` ("locale issue, never solved"); dates travel as epoch timestamps | `app.js`, API | Internationalisation debt |
| F6 | Inline styles sprinkled in views; status colours coupled to status strings via CSS class names | views, `werkstatt.css` | Styling by accretion |
| F7 | Report endpoint returns **snake_case keys** (raw `queryForList`) while everything else is camelCase | `topkunden` API + `bericht.html` | Inconsistent API contract consumed as-is |

## What is intact on purpose

- The app **works**: all flows (customer/vehicle CRUD, order lifecycle with status
  rules, invoicing with correct 20% VAT math, monthly report) behave correctly.
  Legacy ≠ broken — that is exactly why migrations are worth doing carefully.
- **No tests of any kind.** That is the stage-0 condition the safety net (stage 1)
  exists to fix. Do not add tests inside `legacy/`.
- Seed data (`db/init/02-daten.sql`): 10 customers, 13 vehicles, 16 orders across
  2026, 8 invoices — enough for every view and the report to show real numbers.
