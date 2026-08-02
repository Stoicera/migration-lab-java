# ADR-0011 — Adopting repaired AI-generated tests into `modern/src/test`

**Status:** accepted · **Date:** 2026-08-02 · **Milestone:** G6 (closing step)
**Context:** [ADR-0010](0010-ai-testgen-scope-corpus-b-and-agent-repair.md) foresaw this decision;
[`ai-testgen/PROTOCOL.md`](../../ai-testgen/PROTOCOL.md) §4 requires it to be a **separate step
after the experiment**, with the run artifacts left immutable.

## Context

G6 produced, for corpus B (`modern/`), twelve generated test classes — six units × two model arms
— of which eleven compile and pass after Phase-B repair. `modern/` itself had 11 tests (5 ArchUnit
+ 6 Testcontainers) and a coverage ratchet armed at 35 %, well under the 80 % that
`ENGINEERING_STANDARDS.md` §3 requires. `docs/DEVIATIONS.md` records the 80 % target as reachable
"when the repaired ai-testgen tests are adopted".

Adopting all of them would be the obvious move and the wrong one. Phase B measured that for
`AuftragController` the two arms produced **13** and **134** test methods with **identical**
measured value — 21/21 lines, 2/2 branches, 13/13 mutants. Adopting the larger class would import
121 test methods that find nothing and must be maintained forever.

## Decision

**Adopt exactly one class per unit, chosen by a rule fixed before looking at which arm it favours:**

1. Only corpus B (these are tests for `modern/`), and only cells that are green after Phase B.
2. Highest **PIT mutation score** wins — the measure of what a test actually catches.
3. **Ties go to the class with fewer test methods.** Equal fault detection at lower maintenance
   cost is strictly better, and every controller tie in this experiment was exactly that.

Applied mechanically, the rule selects a **mixed** set — two classes from the frontier arm, four
from the open-weight arm — which is the sign one wants that it is not a preference in disguise:

| Unit | Adopted from | Tests | Mutation score |
|---|---|---|---|
| `WerkstattService` (God class) | M2 `qwen/qwen3-coder-next` | 40 | 49/111 = 44.1 % |
| `AuftragController` | M1 `anthropic/claude-sonnet-5` | 13 | 13/13 = 100 % |
| `KundenController` | M1 `anthropic/claude-sonnet-5` | 13 | 17/17 = 100 % |
| `RechnungController` | M2 `qwen/qwen3-coder-next` | 9 | 8/8 = 100 % |
| `AdminController` | M2 `qwen/qwen3-coder-next` | 6 | 2/2 = 100 % |
| `Rechnung` | M2 `qwen/qwen3-coder-next` | 7 | 12/12 = 100 % |

**88 test methods adopted**, in package `at.werkstatt.crm.gen`, each file carrying a provenance
header naming its run, arm, corpus and measured numbers. The 134-method class is **not** adopted;
rule 3 rejected it, and that rejection is the most valuable thing the rule does.

**Consequences for the ratchet.** Measured after adoption: **81.3 % line, 58.3 % branch**
(from 37.3 % / 14.2 %), 99 tests green. The ratchet moves **0.35 → 0.80**. This is the first
time the standard's 80 % target is *reached* rather than *declared* — and it is still set just
below a measured value, never above one.

## The honest part

- **These tests were not written by a human.** They were generated, then repaired by an AI agent
  under supervision, and every file says so in its header. From adoption onward they are ordinary
  repo code: editable, and bound by the rule that the suite stays green.
- **They pin behaviour; they do not audit it.** Phase B recorded **zero `BUG-FOUND`** across 15
  wrong-expectation repairs — not one generated test caught a real defect. Their value is
  regression safety, and they sit *underneath* the characterization and E2E layers, which remain
  the definition of functional equivalence. They do not replace either.
- **The God-class suite is the weak one, and it is also the one that matters.** 40 tests, 88.8 %
  line coverage on `WerkstattService` — and a **44.1 % mutation score**, meaning roughly half of
  injected faults survive it. It is adopted because half a net is better than none over the
  project's riskiest class, and it is flagged here so nobody reads the new 81 % coverage number as
  "the God class is covered". A follow-up that raises *that* mutation score by hand is worth more
  than any further generated tests.
- **Branch coverage (58.3 %) lags line coverage (81.3 %) by 23 points.** The ratchet gates lines
  only, inherited from the earlier configuration. Adding a branch limit is deferred rather than
  smuggled in: it belongs in the same pass that improves the God-class suite.
- **The experiment artifacts under `ai-testgen/runs/2026-07-31/` are untouched and stay
  immutable.** The adopted files are copies. If they diverge later — and they should, when someone
  improves them — the run still records exactly what the models produced.

## Alternatives rejected

- **Adopt everything (12 classes).** Rejected: imports the 134-method class whose measured value
  is identical to a 13-method one. "More tests" is not the goal; the experiment measured that.
- **Adopt nothing, write tests by hand.** Rejected: it discards a working, measured suite and
  leaves the 80 % obligation open for another milestone, with no evidence that hand-written tests
  would be better — the controller suites already kill 100 % of mutants.
- **Adopt only the 100 %-mutation classes** (i.e. drop the God class). Rejected: it would produce
  a flattering coverage number by excluding precisely the class the project is about. The weak
  suite is disclosed instead of dropped.
