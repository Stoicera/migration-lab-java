# dev.to — article outline (EN)

> **Status: DRAFT OUTLINE (2026-10-15). Not published.** Target 700–1200 words. About the problem
> and one design decision, not a feature list. If the piece also runs on stoicera.com, set the
> canonical URL to our page (`canonical_url` in the dev.to front matter).

## Working title (pick one)

1. `The most expensive migration bugs compile, start and look fine` — recommended
2. `One Selenium suite, two UIs ten years apart: how we tested a Spring Boot 1.5 → 4.1 migration`

Tags (dev.to allows four): `java`, `springboot`, `testing`, `angular`.

## Structure

### 1. The problem (≈150 words)

- Applications from around 2016 still run real businesses: Spring Boot 1.x (end of life 2019),
  AngularJS (end of life January 2022), PostgreSQL 9.6 (no security updates since November 2021).
- The migration fails at the same point every time: the old system is its own specification.
  Nobody knows completely what it does, so nobody notices when the new one does it differently.
- A green build proves too little. The dangerous bug compiles, starts, looks right, and changed a
  date format in the JSON.

### 2. The decision: net before jump (≈250 words)

- Stage 1, before the first migration commit: characterization tests (golden masters over HTTP
  responses and database state) and a Selenium suite. From tag `stage-1-safety-net` on, no
  commit may break them.
- The design choice that made one suite work against two UIs: page objects know intent keys
  only; one properties file per UI maps them to selectors. Runnable excerpt, verbatim from the
  repo:

  ```properties
  # e2e/src/test/resources/selectors/legacy.properties (AngularJS 1.8)
  nav.kunden=a[href='#!/kunden']
  kunden.searchField=#suchfeld
  kunden.searchButton=#btn-suchen
  kunden.rows=#kunden-tabelle tr[ng-repeat]

  # e2e/src/test/resources/selectors/modern.properties (Angular 22)
  nav.kunden=a[href='/kunden']
  kunden.searchField=[data-testid='suchfeld']
  kunden.searchButton=[data-testid='btn-suchen']
  kunden.rows=tr[data-testid='kunde-zeile']
  ```

  Run it (needs Docker, JDK 25 and a local Chrome for Selenium — check `e2e/README.md`; both stands up first):

  ```bash
  git clone https://github.com/Stoicera/migration-lab-java && cd migration-lab-java
  docker compose -f legacy/docker-compose.yml up -d --wait
  docker compose -f modern/docker-compose.yml up -d --wait
  ./mvnw verify -f e2e/pom.xml -Dtarget=legacy
  ./mvnw verify -f e2e/pom.xml -Dtarget=modern
  ```

  *Before publishing: run these five commands on a clean machine and note the time; we have not
  timed them for this draft.*
- Numbers with date: 47 characterization tests, 34 Selenium scenarios, green against both stands
  in CI on 2026-10-15.
- Deliberate differences are registered (ADR-0004: closed SQL injection, absorbed JSP admin
  page, fixed `undefined` alert) and pinned per stand by a test. No exploit inputs in the article.

### 3. Proof stories (≈450 words — pick three or four, each with its date)

From case study 18 §7. Each is in the repository and can be linked.

1. **The golden master caught what the browser could not see** (2026-07-30). Boot 1.5 → 2.7
   changed the JSON serialisation of `java.sql.Date`. The UI looked right; the wire contract had
   changed. The characterization test failed, the old contract was restored (playbook ch. 3).
   Same family, 2026-08-05: default readiness did not include the database (`200 UP` with the DB
   stopped); after the fix the container goes unhealthy 25 s after the DB dies and healthy 6 s
   after it returns. And `postgres:18-alpine` reports `en_US.utf8` but sorts in C order.
2. **32 of 34 green while the browser blocked the styles** (2026-08-05). With a strict
   `style-src 'self'` the suite stayed almost green while Angular's runtime styles were blocked.
   Found by opening a real browser and reading the console. `verify-edge.sh` now pins the
   directive and prints its own blind spot. Follow-up of the same kind, 2026-08-17: the modern
   stand's tables had rendered without styling since go-live (no `<tbody>`), found by putting two
   screenshots side by side, not by a test (PR #44).
3. **A flaky assertion treated as a defect** (2026-08-05). The rate-limit check fired 80
   requests in a shell loop; on a slow runner that was no longer a burst. Replaced by 200
   concurrent requests and an assertion of `> 0` rejections, because the exact count is a
   property of the machine.
4. **The image scan paid off on its first run** (2026-08-05). Trivy found CVE-2026-54291 in the
   PostgreSQL JDBC driver, a version that appeared in no manifest. Pinned to the minimum fixed
   version. **Current state must be stated next to it:** on 2026-10-15 the same scan reports 11
   findings, 3 CRITICAL in embedded Tomcat; it reports, it does not fail the build.
5. **The perfect result was survivorship bias** (2026-07-31 / 2026-08-02). As generated, 12 of 24
   LLM-written test classes compiled; over those: 100 % line coverage, 99.2 % mutation score.
   After a time-boxed repair (21/24 green, 421 methods): 90.5 % line, 73.2 % mutation. The largest
   class: 84.3 % line, 55.9 % mutation. Zero real bugs found. **Six classes per codebase, one
   attempt each: "in our experiment", never a general claim.**
6. **A register that allows everything except silence.** `docs/DEVIATIONS.md` lists every unmet
   standard with date and reason, including entries that count against the project.

Recommendation: stories 1, 2 and 5 carry the article; 3 and 4 as one short paragraph each.

### 4. What this does not show (≈150 words)

- The app is small on purpose (about 1.7k lines of backend, 25 endpoints, 10 views, counted
  2026-10-01). Numbers do not scale linearly to large systems.
- It is not faster. Local read-path load test, two runs: 2026-08-05 p95 1.60 ms modern vs
  1.56 ms legacy; 2026-10-02 p95 2.34 vs 3.99 ms, median 1.28 vs 1.17 ms. Quote both, never the
  p95 alone.
- Selenium asserts behaviour and text, not appearance (story 2).
- No user identity: Basic auth at the edge is a lock, not an identity system.

### 5. Close (≈100 words)

- One sentence: the method transfers (stage order, net first, the break catalogue); effort
  figures from this repository do not.
- Links: repository, `stages.md`, `ai-testgen/REPORT.md`, the German playbook (PDF on release
  v1.0.0), case study https://stoicera.com/werk/migration-lab-java/.
- Question to readers: what broke in your Boot 1.x or AngularJS migration that a suite like this
  would not have caught?

## Do not

- Effort hours, prices, "migration in X days".
- Legacy credentials, server names, exploit inputs, the JSP admin page screenshot.
- "AI-built" framing. If the method comes up, link the README section "How this was built".
