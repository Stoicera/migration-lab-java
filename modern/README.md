# modern/ — the migrated application

This module grows stage by stage from a copy of `legacy/` and ends at:
Java 25, Spring Boot 4.1.x, Angular 20, PostgreSQL, Testcontainers.

Migration stages (each ends in a git tag, see [`../stages.md`](../stages.md)):
JDK/build hygiene → Boot 1.5→2.7 → Boot 3.x→4.1 + Java 25 → AngularJS→Angular 20
(Strangler Fig) → cloud & ops.

**Rules for this directory:**

- Every change must have a migration purpose — beautification without need is scope creep.
- From `stage-1-safety-net` onward, no commit may break the Selenium E2E suite
  or the characterization tests.
- OpenRewrite recipes are used **and evaluated** (what they caught vs. missed
  is playbook data).

Status: **empty — work starts in G3 (stage 2).**
