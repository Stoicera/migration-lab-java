# modern/ — the migrated application

Grows stage by stage from a faithful copy of `legacy/`; final state: Java 25,
Spring Boot 4.1.x, Angular 20, PostgreSQL. Current state: see [`../stages.md`](../stages.md).

## Run it (side by side with the legacy stand)

```bash
docker compose -f modern/docker-compose.yml up -d
```

- App: http://localhost:8090 · JSP admin: http://localhost:8090/admin
- PostgreSQL: localhost:5434 (werkstatt/werkstatt), same committed seed as legacy

## The equivalence gate

The characterization suite that locks the legacy behaviour runs against THIS
stand in `modern-ci` on every commit (`-DbaseUrl=http://localhost:8090`), and
the Selenium suite runs as the `e2e (modern)` matrix leg. Functional
equivalence is proven per commit, not claimed.

## Rules for this directory

- Every change must have a migration purpose — beautification without need is
  scope creep (playbook rule, lived here).
- From `stage-1-safety-net` onward, no commit may break the safety net.
- OpenRewrite recipes are used **and evaluated** (stage 4) — what they caught
  vs. missed is playbook data.

## Stage log

- **Stage 2 (`stage-2-jdk-build`):** baseline copy of legacy + build hygiene —
  logging unified to SLF4J/Logback (log4j 1.2 retired), unused `commons-lang`
  dropped, dead code removed. Deliberately NOT raised: the JDK — Spring Boot
  1.5 caps the runtime at Java 8; the raise lands with Boot 2.7 (stage 3).
  Everything else (field injection, God class, SQL strings, `javax.*`, gson on
  the JSP page) stays until its migration stage — the diff per stage IS the
  playbook material.
