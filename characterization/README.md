# characterization/ — golden-master tests

Captures of the legacy application's **actual** behaviour. They define functional
equivalence for the whole migration: a migration step is correct when these still
pass — quirks included.

Two suites (Java 25, JUnit 5, no framework beyond JDK http + Jackson + JDBC):

- **`ApiCharacterizationTest`** — every read endpoint vs. `src/test/resources/golden/`.
  Comparison on parsed JSON trees (key order/whitespace irrelevant, values exact);
  the JSP admin page as normalized HTML (date masked). On mismatch the received
  document lands in `target/characterization-received/` for diffing.
- **`DbStateCharacterizationTest`** — DB state transitions of the write paths,
  including the documented quirks: the vehicle-km side effect on order creation
  and the orphaned rows after customer deletion (LEGACY_NOTES B13).

## Determinism

Every suite class resets the database to the committed seed
(`legacy/db/init/02-daten.sql`) in `@BeforeAll` — captures are reproducible on
any machine, any order, any time.

## Run

```bash
docker compose -f legacy/docker-compose.yml up -d   # stand must be running
./mvnw verify -f characterization/pom.xml
```

## Re-capturing goldens

Only when a behaviour change is INTENDED (ADR + playbook entry required).
Reset the stand, then re-run the capture commands documented in the git history
of `golden/` — never hand-edit a golden file.
