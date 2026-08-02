# legacy/ — WerkstattCRM (as found)

The legacy application in its deliberate 2016-era state: **Java 8, Spring Boot
1.5.22, AngularJS 1.8.2, JSP admin page, Maven, PostgreSQL 9.6.** Synthetic but
pattern-faithful (ADR-0001); every deliberate wart is catalogued in
[`LEGACY_NOTES.md`](LEGACY_NOTES.md) — that file is the contract for this module.

## Run it

```bash
docker compose -f legacy/docker-compose.yml up -d --wait
```

- SPA: http://localhost:8080 — customers, vehicles, orders, invoices, monthly report
- JSP admin page: http://localhost:8080/admin (deliberately unprotected — see LEGACY_NOTES B15/B16)
- PostgreSQL: 127.0.0.1:5433 (werkstatt/werkstatt), seeded from `db/init/`

### Building it outside Docker — and why that fails

`./mvnw verify -f legacy/pom.xml` on a modern local JDK **fails**, but not for the
reason people assume. Measured on JDK 26 (2026-08-02), the build gets further than
expected:

```
[INFO] --- compiler:3.1:compile  ---   OK   ← javac still accepts source/target 8
[INFO] --- surefire:2.18.1:test  ---   OK   ← there are no tests, by design
[INFO] --- war:2.6:war           ---   FAILS
[ERROR] Failed to execute goal org.apache.maven.plugins:maven-war-plugin:2.6:war
        … due to an API incompatibility
[ERROR] ExceptionInInitializerError: Unable to make field private final
        java.util.Comparator java.util.TreeMap.comparator accessible:
        module java.base does not "opens java.util" to unnamed module
```

**It is not the Java 8 source level — it is the 2016 build plugin.**
`maven-war-plugin` 2.6, pinned by Spring Boot 1.5's dependency management,
reflects into `java.util` internals. JDK 16 made that strong encapsulation the
default, so the plugin cannot initialise at all. No compiler flag fixes this;
the plugin predates the module system.

That is precisely why the Dockerfile builds this module inside
`maven:3.9-eclipse-temurin-8` — and it is a nice miniature of the whole project:
the thing that blocks the upgrade is rarely your own code.

If you really need a local build, point one command at a JDK 8 without changing
anything globally:

```bash
JAVA_HOME=/usr/lib/jvm/java-8-openjdk ./mvnw verify -f legacy/pom.xml
```

Full operations reference: [`docs/deployment.md`](../docs/deployment.md).

## Rules for this directory (root `CLAUDE.md`)

- Code keeps its 2016-era style — it is the exhibit, not the product.
- No modernization beyond what `LEGACY_NOTES.md` documents.
- Deliberately **no tests** at stage 0 — that is the starting condition the
  safety net (stage 1) exists to fix.

Domain walkthrough: a car arrives → order (`ANGENOMMEN`) → work (`IN_ARBEIT`) →
done (`FERTIG`) → invoice (20% USt) → pickup (`ABGEHOLT`). Cancelled orders
(`STORNIERT`) can be purged permanently from the admin page.
