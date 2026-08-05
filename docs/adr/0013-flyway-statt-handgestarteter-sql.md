# ADR-0013 — Flyway migrations instead of hand-run SQL files (wart B18, modern stand only)

**Status:** accepted · **Date:** 2026-08-05 · **Milestone:** G7 (stage 6) · **Deciders:** Sebastian Kern (owner)
**Context:** closes wart **B18 for the modern stand** (`legacy/LEGACY_NOTES.md`, deferred to G7 in
[`docs/DEVIATIONS.md`](../DEVIATIONS.md)); required by `ENGINEERING_STANDARDS.md` §7
("Migrationen automatisch (Flyway …) beim Start oder als Job"); the drift guard it touches
exists because of [ADR-0007](0007-golden-master-governance.md).

## Context

Until stage 6 the modern schema lived in `modern/db/init/01-schema.sql` and
`modern/db/init/02-daten.sql`, mounted into the Postgres container's
`docker-entrypoint-initdb.d`. That is the 2016 practice the migration is supposed to
retire, kept alive on the modern side purely because it was convenient: the files were
byte-copies of the legacy ones, so `legacy-ci.yml` could guard the two stands against
drift with a `diff -q`.

Three things are wrong with it, and only the first is cosmetic. The scripts run **only on
an empty volume** — an existing database never sees a change. There is **no version
history**, so nothing can answer "which schema is this database at". And the second copy
of the SQL exists only so the mount has something to mount, which means the guard is
protecting a duplicate that should not exist.

## Decision

1. **`modern/db/init/` is deleted.** The schema becomes
   `modern/src/main/resources/db/migration/V1__baseline_schema.sql` and the demo data
   `modern/src/main/resources/db/demo/V2__demo_seed.sql`.
2. **Two locations, one of which is dropped in production:**
   `spring.flyway.locations=classpath:db/migration,classpath:db/demo`; a production stand
   sets `SPRING_FLYWAY_LOCATIONS=classpath:db/migration`. Demo data is a property of the
   demo, not of the schema.
3. **`spring.flyway.baseline-on-migrate=false`, on purpose** — see the rejected
   alternatives; this is the one setting most tutorials get wrong for this shape of repo.
4. **Two Maven artifacts, both load-bearing:** `spring-boot-starter-flyway` and
   `flyway-database-postgresql`. Resolved version: **Flyway 12.4.0**.
5. **The Testcontainers integration test stops copying init scripts** and lets Flyway build
   the schema, so the test now also proves that the migrations run.
6. **The CI drift guard is rewritten** as
   [`scripts/check-schema-drift.sh`](../../scripts/check-schema-drift.sh).

## Alternatives rejected

- **Keep the hand-run init scripts.** Rejected: they cannot change an existing database,
  carry no version, and the standard names automatic migrations explicitly. Keeping them
  would also mean shipping a modernisation whose own schema management is the wart the
  playbook tells readers to remove.
- **`baseline-on-migrate=true`** (the setting every "Flyway on an existing database" answer
  recommends). Rejected with a concrete failure: on an already populated volume Flyway
  would baseline at V1 — skipping it — and then apply V2, **seeding the demo data a second
  time**. A migration configuration that silently duplicates rows on restart is worse than
  none, because the first symptom is a wrong report, not an error. A stand starts on an
  empty volume (`docs/deployment.md` §4.1); that is the contract this setting depends on
  and it is written down there.
- **Keep `modern/db/init/` as a second copy for the compose mount** and add Flyway
  alongside. Rejected: it re-creates the exact problem — two files that must agree, with
  nothing forcing them to. One schema source, or the guard is guarding a copy.

## Consequences

- **The wart is closed on one side only, and stays open on the other for ever.** `legacy/`
  keeps its hand-run `legacy/db/init/*.sql` and the `docker-entrypoint-initdb.d` mount,
  because "legacy stays legacy" is a hard rule and B18 is one of the things the exhibit is
  there to show. Anyone reading "B18 closed" should read it as *closed for the modern stand*;
  hand-run SQL has not left this repository and is not meant to.
- **`flyway-core` alone does nothing in Spring Boot 4, and it fails silently.** Boot 4 split
  auto-configuration into per-technology modules, so the *starter* is what wires Flyway
  into the context. **We got this wrong first and fixed it after measuring:** with only
  `flyway-core` on the classpath the application started happily, migrated nothing, and
  died later on `relation "kunde" does not exist` — **6 of 7 integration tests errored**
  before `spring-boot-starter-flyway` was added. Recorded because the failure mode is the
  dangerous kind: a dependency that is present, resolves, and does nothing.
  `flyway-database-postgresql` is a second separate artifact since Flyway 10; without it
  Flyway starts and fails on an unsupported database — that one at least fails loudly.
- **Measured on the fresh PG 18 stand** ([ADR-0012](0012-postgresql-18-und-fixierte-collation.md)):
  Flyway applied **2 migrations**; `flyway_schema_history` holds `1 baseline schema` and
  `2 demo seed`, both success.
- **One schema source, and the guard now proves it rather than a duplicate.**
  `check-schema-drift.sh` strips comments and blank lines and compares the SQL the server
  actually executes — the two files legitimately differ in their headers now, so a
  `diff -q` would have had to be deleted or weakened. It also **fails loudly if either file
  is missing**, so deleting one side cannot turn the guard green. Verified with a negative
  control: changing `'Franz'` to `'Franzl'` in the seed makes it exit 1 with a diff.
- The integration test gained a second job for free: it no longer only exercises the
  service's SQL, it also fails if a migration is broken, before anything is built.
- The demo seed is now reachable from the classpath, which is what makes dropping it in
  production a one-variable decision instead of a different set of files.
