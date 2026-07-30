# legacy/ — WerkstattCRM (as found)

The legacy application in its deliberate 2016-era state: **Java 8, Spring Boot
1.5.22, AngularJS 1.8.2, JSP admin page, Maven, PostgreSQL 9.6.** Synthetic but
pattern-faithful (ADR-0001); every deliberate wart is catalogued in
[`LEGACY_NOTES.md`](LEGACY_NOTES.md) — that file is the contract for this module.

## Run it

```bash
docker compose -f legacy/docker-compose.yml up -d
```

- SPA: http://localhost:8080 — customers, vehicles, orders, invoices, monthly report
- JSP admin page: http://localhost:8080/admin (deliberately unprotected — see LEGACY_NOTES B15/B16)
- PostgreSQL: localhost:5433 (werkstatt/werkstatt), seeded from `db/init/`

Build without Docker requires JDK 8 (`./mvnw verify -f legacy/pom.xml` from repo
root). On a modern local JDK, build via the image instead — the Dockerfile uses
`maven:3.9-eclipse-temurin-8`.

## Rules for this directory (root `CLAUDE.md`)

- Code keeps its 2016-era style — it is the exhibit, not the product.
- No modernization beyond what `LEGACY_NOTES.md` documents.
- Deliberately **no tests** at stage 0 — that is the starting condition the
  safety net (stage 1) exists to fix.

Domain walkthrough: a car arrives → order (`ANGENOMMEN`) → work (`IN_ARBEIT`) →
done (`FERTIG`) → invoice (20% USt) → pickup (`ABGEHOLT`). Cancelled orders
(`STORNIERT`) can be purged permanently from the admin page.
