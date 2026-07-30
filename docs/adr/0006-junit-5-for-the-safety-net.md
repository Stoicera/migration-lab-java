# ADR-0006 — JUnit 5.14 (not JUnit 6) for the safety net

Date: 2026-07-30 · Status: accepted (retroactively records the stage-1 decision) · Deciders: Sebastian Kern (owner)
Prompted by: hostile review session 7 (finding 9 — decision existed only as a worklog line)

## Context

When the e2e and characterization suites were built (stage 1), JUnit 6.1 was
already the current major and JUnit 5.14.4 the mature previous line. The suites
are greenfield, so the newer major was a real option.

## Decision

**The safety net runs on JUnit 5.14.x.** The safety net's one job is to be
boring: it must never itself be a source of change, breakage, or doubt while
everything around it migrates. A brand-new test-framework major (new module
names, behavioural changes, young ecosystem) adds exactly the kind of risk the
net exists to absorb, with zero payoff for these suites' simple needs
(ordered scenarios, parameterless lifecycle, AssertJ assertions).

Revisit trigger (the worklog's vague "if wanted", made concrete): a dependency
of the suites requires JUnit 6, **or** G6 tooling (PIT/JaCoCo integration)
gains a JUnit-6-only capability the experiment needs. Then: dedicated ADR, own
commit series, net proven green before and after.

## Consequences

- e2e and characterization stay pinned to the 5.14 line (Dependabot may bump
  patches; majors are blocked on this ADR).
- The G6 testbed (ai-testgen) also targets JUnit 5.14 per PROTOCOL.md — one
  framework across all test artefacts until the trigger fires.
