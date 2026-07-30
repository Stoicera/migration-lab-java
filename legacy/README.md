# legacy/ — WerkstattCRM (as found)

This module will contain the legacy application in its deliberate 2016-era state:
Java 8, Spring Boot 1.5.x, AngularJS 1.8, JSP admin page, Maven, PostgreSQL.

It is built in **G1 (stage 0)** and tagged `stage-0-legacy`. Every intentional legacy
pattern is catalogued in `LEGACY_NOTES.md` (created with the module).

**Rules for this directory** (see root `CLAUDE.md`):

- Code here keeps its 2016-era style — it is the exhibit, not the product.
- No modernization beyond what `LEGACY_NOTES.md` documents.
- Deliberately **no tests** at stage 0 — that is the starting condition the
  safety net (stage 1) exists to fix.

Status: **empty — work starts in G1.** See [`../stages.md`](../stages.md).
