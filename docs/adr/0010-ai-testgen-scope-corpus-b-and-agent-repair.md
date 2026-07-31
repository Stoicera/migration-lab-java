# ADR-0010 — G6 experiment scope: corpus B is in, and the repairer is an agent

Date: 2026-07-31 · Status: accepted · Deciders: Sebastian Kern (owner), delegated to the executing agent for this session
Prompted by: protocol freeze (`ai-testgen/PROTOCOL.md` v1.0, tag `ai-testgen-protocol-v1`)

The frozen protocol is the authoritative text; this ADR records the three decisions
taken at freeze so they are findable from the decision index rather than only inside a
40-page protocol.

## Context

Draft v0.1 of the protocol left three questions open for the freeze: whether to also run
the experiment against the migrated code (`modern/`), which test libraries the models may
use, and who performs the Phase-B repair. All three change what the published numbers
mean, so none could be decided silently after seeing results.

## Decision

**1. Corpus B (the migrated counterparts) is IN.** The same six classes are measured twice:
as 2016 legacy (corpus A) and as their Boot 4.1 / Java 25 counterparts (corpus B). Reasons:

- It answers the question this project's audience actually has — *does modernizing pay off
  in testability, measurably?* — instead of only "can an LLM test legacy code?".
- It is what the stage-4 constructor-injection sweep was a precondition for (worklog
  session 6); leaving it out would have made that sweep a claim rather than a measurement.
- Cost is ~€0.60 additional API spend against a €20 cap.
- It is the only route by which `modern/` acquires a unit-test suite at all, which three
  `deferred(G6)` rows in `docs/DEVIATIONS.md` depend on.

Guard against the obvious failure mode: an A/B delta is a **migration** effect, not an
injection effect. Corpus B also differs by parameterized SQL (SD-1), the absorbed admin
page (SD-2) and formatting. Pre-registered as threat T7; the report may not claim otherwise.

**2. `spring-test` is on the classpath of both corpora — `ReflectionTestUtils` only.** The
draft allowed JUnit + Mockito + AssertJ. Field injection cannot be mocked without either
reflection or a container, and every real Spring shop has `spring-test`. Withholding it
would have handicapped corpus A and made the A/B result look better than reality. MockMvc
and any Spring context stay forbidden by the prompt, so "unit test, no container" holds;
a generated test that boots a context is a `STRUCTURAL` repair case.

**3. Phase-B repair is performed by the executing Claude Code agent under the owner's
supervision, and is labelled as such.** A human-with-a-stopwatch would have been the
cleaner design, but 24 cells × 30 min is not a realistic ask of a solo owner, and pretending
otherwise would have produced a "human-fix minutes" column that no human produced. So the
metric is renamed to **repair effort in agent wall-clock minutes under supervision**, it is
never presented as a person-minutes estimate for a human team, and the risk that a Claude
repairer favours the Claude arm (M1) is pre-registered as threat T8.

## Consequences

- 24 generation calls instead of 12; `Catalog` and the testbeds exist per corpus.
- `REPORT.md` carries T7 and T8 next to the affected numbers, not in a footnote.
- Adopting repaired corpus-B tests into `modern/src/test` is a **separate step after** the
  experiment; the artifacts under `ai-testgen/runs/` stay immutable when it happens.
- The playbook may not turn the repair-effort number into a person-day claim for readers'
  own projects without restating the caveat (Kap. 6).
