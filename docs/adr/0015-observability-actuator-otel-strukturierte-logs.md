# ADR-0015 — Observability: Actuator probes, OpenTelemetry via the starter, ECS logs — and what stays off

**Status:** accepted · **Date:** 2026-08-05 · **Milestone:** G7 (stage 6) · **Deciders:** Sebastian Kern (owner)
**Context:** `ENGINEERING_STANDARDS.md` §5 (OpenTelemetry from the start, structured JSON
logs, health/readiness endpoints, a small `observability` compose profile); the probe this
ADR replaces is the TCP healthcheck the modern stand carried until stage 6.

## Context

Until stage 6 the modern container's healthcheck opened a TCP connection to port 8080. That
answers "is a socket listening", which is not a question anyone operating a system has. The
application had no health endpoint, no traces, and log output that a human reads and a log
system cannot. §5 has been open in the standards since the repo started; G7 is the milestone
that owns it.

Two constraints shaped every choice below. The runtime image is `eclipse-temurin:25-jre` and
has **neither curl nor wget**. And nothing here may cost the safety net anything: the
characterization and E2E suites talk to the same application, so an observability change that
alters a response or a startup ordering is a regression, not a feature.

## Decision

1. **`spring-boot-starter-actuator` + `spring-boot-starter-opentelemetry`** as build
   dependencies (resolved: micrometer-tracing **1.7.0**, opentelemetry-exporter-otlp
   **1.62.0**). No JVM agent.
2. **Expose `health` and `info` only**, `show-details=never`. Measured: `/actuator/env`,
   `/beans`, `/mappings`, `/metrics`, `/loggers` and `/heapdump` all return **404**.
   (The edge closes `/actuator` on top of that —
   [ADR-0014](0014-authentifizierung-am-edge.md) — but the application must not depend on a
   proxy being in front of it.)
3. **Readiness includes the database; liveness deliberately does not.**
   `management.endpoint.health.group.readiness.include=readinessState,db`,
   `…group.liveness.include=livenessState`.
4. **The container healthcheck asks the application, not the port:** `bash` + `/dev/tcp`
   against `/actuator/health/readiness`, `retries: 3`, `start_period: 90s`.
5. **Structured logs are set by the environment, not baked into the artifact:**
   `LOGGING_STRUCTURED_FORMAT_CONSOLE=ecs` lives in compose only, so a bare `java -jar`
   keeps human-readable output (12-factor: the format belongs to the environment).
   Measured output is valid **ECS 8.11** JSON.
6. **Tracing is off by default** (`management.tracing.enabled=${WERKSTATT_TRACING_ENABLED:false}`),
   and the `observability` profile (`grafana/otel-lgtm:0.11.15` — Grafana, Prometheus, Tempo
   and Loki in one container, Grafana on `127.0.0.1:3000`) turns it on together with the
   collector.

## Alternatives rejected

- **JVM agent-based OpenTelemetry** (`opentelemetry-javaagent.jar`), the zero-code-change
  option. Rejected: it is only zero-change in the source — it needs a Dockerfile change or a
  `JAVA_TOOL_OPTIONS` that every runner must remember, and it moves instrumentation out of
  the build where nothing pins its version. The starter keeps it in `pom.xml`, where the rest
  of this repo's dependency discipline already applies.
- **Tracing on by default.** Rejected on measurement: without a configured collector the OTLP
  exporter runs at `localhost:4318` and logs a connection failure on every batch. Observability
  whose default state is error spam trains people to ignore the log — the opposite of the point.
  One variable turns on the exporter and the collector together.
- **Expose more endpoints** (`metrics`, `prometheus`, `loggers` — "we might want them for the
  demo"). Rejected: an open `/actuator` is a data leak, and each endpoint would have to be
  justified against what it tells an anonymous reader about the system. Nothing in this stage
  needs them.
- **Keep the TCP healthcheck and add the endpoints alongside it.** Rejected on measurement:
  the old probe **never noticed a dead database at all**. Adding an endpoint nobody asks is
  documentation, not a health check.

## Consequences

- **The default readiness group does not contain the database, and we shipped it that way
  before measuring it.** Measured with `modern-db-1` stopped: `/actuator/health/readiness`
  answered **`200 {"status":"UP"}`** while `/actuator/health` answered **`503`**. A readiness
  probe that reports "ready" while the application cannot answer a single business request is
  worse than no probe: it produces confidence instead of information, and an orchestrator
  routes traffic at it. **We got this wrong first and fixed it after measuring.** After the
  fix: database down → readiness `503 {"status":"DOWN"}`, liveness `200 {"status":"UP"}`.
  Liveness keeps `livenessState` only on purpose — a database outage must not restart a
  healthy application, which is how a fault gets turned into an outage.
- **The retry budget was wrong too, for a related reason.** The old healthcheck had
  `retries: 24` because it also had to cover slow startup. Measured, that meant the
  application could be 503 while the container still counted as `healthy` **for two
  minutes**. The two concerns are now separated: `retries: 3` for steady state,
  `start_period: 90s` for the boot — failures during `start_period` do not count against
  retries, so the generous startup buffer costs nothing in reaction time. Measured end to
  end: the container goes **`unhealthy` 25 s** after the database dies and recovers to
  **`healthy` 6 s** after it returns.
- **`bash` + `/dev/tcp`, and the reason is boring and worth writing down:** the
  `eclipse-temurin:25-jre` runtime image has neither curl nor wget, and `CMD-SHELL` would run
  `dash`, which has no `/dev/tcp`. Hence the explicit `CMD ["bash", "-c", …]` form.
  Note for the ops docs while we are here: the *legacy* image `eclipse-temurin:8-jre` **does**
  have curl and wget — the comment in `legacy/docker-compose.yml` claiming otherwise is
  wrong and should be corrected rather than propagated.
- **Log↔trace correlation needed a rename to be true.** Micrometer writes `traceId`/`spanId`
  into the MDC, and those are **not** the ECS field names, so logs labelled "ECS" would have
  been shipped to an ECS consumer that could not find them. Fixed with
  `logging.structured.json.rename.traceId=trace.id` and `.spanId=span.id`; measured after the
  rename: `"trace.id":"0e5fedf62d524c1ce35298b01043d4f4","span.id":"9c85128c55b73b57"`. If a
  format is going to be called ECS, it has to be ECS.
- **The observability profile was verified end to end, once, locally.** Started with
  `WERKSTATT_TRACING_ENABLED=true docker compose -f modern/docker-compose.yml --profile observability up -d --wait`;
  measured: Tempo holds traces with `rootServiceName: werkstatt-crm-modern` and span names
  such as `http get /api/kunden`. That is the whole claim — a local demonstration that the
  wiring works. **No collector runs anywhere else, because no deployment has happened**: no
  host, no retention, no dashboards anyone is on call for. §5 is met at the level the repo can
  prove and not one level above it.
- **`/metrics` is 404 by decision 2**, so nothing scrapes this application today. A
  Prometheus scrape target is a deployment decision and belongs to the stage that has
  somewhere to scrape from.
