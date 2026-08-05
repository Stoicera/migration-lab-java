# ADR-0012 — PostgreSQL 18 on the modern stand, locale pinned, collation verified by sorting

**Status:** accepted · **Date:** 2026-08-05 · **Milestone:** G7 (stage 6)
**Context:** closes `P2` in [`docs/DEVIATIONS.md`](../DEVIATIONS.md) ("PostgreSQL 9.6 (EOL) …
a G7 decision with its own ADR") **for the modern stand**; bound by the equivalence definition of
[ADR-0004](0004-functional-equivalence-and-sanctioned-divergence.md) and the golden-master
governance of [ADR-0007](0007-golden-master-governance.md).

**A note on form, so it is a decision and not drift:** this ADR and 0013–0015 use the
bold-label header of [ADR-0011](0011-adopting-generated-tests.md) instead of the
`Date: · Status: · Deciders:` line that ADR-0001…0010 carry — two formats exist in this
directory, the newer one is the one that continues, and nobody has to guess whether the
difference means something.

## Context

The modern stand ran PostgreSQL 9.6 from stage 2 through G6 on purpose: an identical
database is one variable fewer while the framework, the JDK and the UI all change
(DEVIATIONS P2). That reason expired with the migration. What replaced it is the reason
the upgrade is not a one-line tag bump: the characterization suite proves equivalence by
comparing two stands, so **anything that changes sort order on one stand turns the
equivalence gate into a comparison of two different databases**. Collation is exactly
such a thing, and it is invisible in every place people usually look.

Measured starting point: the legacy database is PostgreSQL **9.6.24 (Debian)** with
`datcollate`/`datctype` `en_US.utf8`.

## Decision

1. **`modern/` runs `postgres:18`** (measured: **18.4, Debian**), with the locale pinned
   explicitly rather than inherited — `LANG=en_US.utf8` plus
   `POSTGRES_INITDB_ARGS=--locale=en_US.utf8`.
2. **Not `-alpine`.** See the measurement below; this is the headline finding of the
   stage, not a preference.
3. **The collation check is a sort, not a settings read.** New test:
   `WerkstattServiceIntegrationTest.die_datenbank_sortiert_wie_der_legacy_stand_und_sagt_es_nicht_nur`.
4. **The compose volume is renamed** to `modern-werkstatt-db-pg18`.
5. **Legacy stays on 9.6 permanently.** It is the exhibit, not a stand to keep current.

### The measurement that decided it

Probe (run against each image, same statement):
`SELECT x FROM (VALUES ('Huber Transporte GmbH'),('Hubermann'),('de Vries'),('Öhler'),('Ohler'),('Zach'),('van Dijk')) t(x) ORDER BY x`

| image | `datcollate` reports | actually sorts |
|---|---|---|
| `postgres:9.6` (legacy, reference) | `en_US.utf8` | de Vries \| Hubermann \| Huber Transporte GmbH \| Ohler \| Öhler \| van Dijk \| Zach |
| `postgres:18` (Debian) | `en_US.utf8` | **identical to 9.6** |
| `postgres:18` (Debian), locale pinned | `en_US.utf8` | **identical to 9.6** |
| **`postgres:18-alpine`** | **`en_US.utf8`** | Huber Transporte GmbH \| Hubermann \| Ohler \| Zach \| de Vries \| van Dijk \| Öhler |

The alpine image **reports a collation it does not use**: musl accepts the locale name and
then orders as `C`. The obvious review step — read `pg_database.datcollate` on both stands
and compare — **passes**, while every sorted list in the application silently changes
order. Reading the setting is worthless as evidence; only sorting is evidence. That is why
the check in the suite sorts.

## Alternatives rejected

- **Stay on 9.6.** Rejected: the ledger already records it as end of life, and
  `docs/deployment.md` §4.5 carries a section of its own about it. Keeping
  an unsupported database because the tests are green is precisely the reasoning this repo
  argues against.
- **`postgres:18-alpine`** (smaller image, the usual default). Rejected on measurement:
  it reorders every list in the application while all the settings it exposes say it does
  not. It is the cheapest possible way to break the golden masters without a single line
  of application code changing.
- **Bump the tag and keep the existing volume mount** (`- vol:/var/lib/postgresql/data`).
  Rejected on measurement: PG 18 moved both `PGDATA`
  (`/var/lib/postgresql/data` → `/var/lib/postgresql/18/docker`) and the declared `VOLUME`
  (`/var/lib/postgresql/data` → `/var/lib/postgresql`). With the old mount the container
  starts cleanly, initialises a **fresh empty cluster in an anonymous volume**, persists
  nothing — and because Flyway ([ADR-0013](0013-flyway-statt-handgestarteter-sql.md))
  re-migrates on every start, the stand still looks healthy. Data loss behind a green
  healthcheck. The mount was corrected and the volume renamed instead.
- **Leave the locale unpinned and inherit whatever the image ships.** Rejected: it happens
  to be right today (measured identical to 9.6), which is not the same as being decided.
  Two lines of environment turn a coincidence into a contract.

## Consequences

- **The two stands now run different PostgreSQL majors on purpose: 9.6 against 18.** That
  is not a threat to the equivalence gate, it is what makes the gate mean something. A
  comparison of two identical databases holds the database constant and therefore says nothing
  about it; across a 9-major gap the gate finally exercises the layer that actually differs.
  State the result at the size it was measured and no larger: **on this seed, the 47 pinned
  contracts and the `topkunden` response do not change across the version boundary.** That is
  evidence about 47 pinned behaviours and a ten-customer demo dataset — not a proof about the
  application as a whole, and not a statement about any production stack, because no
  deployment exists. The safety net is doing work it was not doing before; it is not doing
  more work than it is doing.
- **Acceptance evidence, measured, exactly as pre-registered:**
  - `characterization` **47/47 green against modern (PG 18)** and **47/47 against legacy (9.6)**.
  - `/api/bericht/topkunden` — the one endpoint whose raw DB types reach JSON through
    `queryForList`, so an int↔decimal drift would break `JsonNode.equals` — returns
    **byte-identical** responses from both stands, including the decimal literal
    `"umsatz":912.00`. This was the specific pre-registered acceptance step, and it passed
    as written.
  - Customer ordering across the two stands: identical.
- `/api/bericht/monat` has different JSON **key order** on the modern stand (alphabetical).
  Measured to be pre-existing since stage 3, not a PG-18 effect, and invisible to the
  order-insensitive `JsonNode.equals` comparison. Recorded here because it was seen while
  looking at something else, not because this ADR caused it.
- The old 9.6 volume stays on disk and is removed by hand (`docs/deployment.md` §4.6). A
  rename is a clean first start; it is not a data migration and is not sold as one.
- **`characterization/README.md` overstates collation stability and must be corrected.**
  Its current wording reads as if collation cannot move the golden masters. What was
  measured is narrower and is the only thing that may be claimed: the orderings are equal
  **on this seed**, on these two images, today. The alpine row above is the counterexample
  that keeps that distinction from being pedantry.

## The honest part

The check anyone would write first — compare `datcollate` on both stands — is the check
that would have let the alpine image through. It was not caught by care or by review; it
was caught because the probe sorted seven names instead of asking the database about
itself. Generalising beyond this repo: a database setting is a claim by the database, and
a claim is not a measurement.
