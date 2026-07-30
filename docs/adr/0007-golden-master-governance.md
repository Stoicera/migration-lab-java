# ADR-0007 — Golden-master governance: how a golden may change

Date: 2026-07-30 · Status: accepted · Deciders: Sebastian Kern (owner)
Prompted by: hostile review session 7 (findings 9, 17 — "never silently updated" was a README sentence with a nonexistent procedure)

## Context

The golden masters in `characterization/golden/` define functional equivalence
(ADR-0004). "Goldens are never silently updated" was the rule from stage 1 —
but it lived as a module-README sentence, the advertised re-capture procedure
("documented in the git history") did not exist, and nothing distinguished a
legitimate re-capture from quiet test-bending. The enforcement that DID exist
(legacy-ci re-runs the suite against the freshly built legacy stand, so a
tampered golden fails CI unless legacy itself changed) was undocumented.

## Decision

1. **A golden changes only together with** (same commit): the sanctioning
   ADR-0004 register entry or ADR, the re-captured file, and a conventional
   commit whose message names the sanctioned change. A golden diff in any other
   commit is a red-flag review blocker.
2. **The re-capture procedure is documented in `characterization/README.md`**
   (exact commands against a pristine seeded stand) — executable, not folklore.
3. **Enforcement stays mechanical:** legacy-ci re-validates every golden against
   the rebuilt legacy stand on every push — goldens cannot drift from frozen
   legacy behaviour without a red pipeline. The modern-ci equivalence gate does
   the same against the modern stand (stand-aware pins per ADR-0004).
4. **Environment coupling is declared, not hidden:** goldens are valid for
   PostgreSQL 9.6 collation/ordering and the committed seed. A PG upgrade (G7,
   see docs/DEVIATIONS.md) is a golden-impact event that follows this ADR's
   procedure like any contract change.

## Consequences

- "Never silently updated" is now a checkable property: `git log -- '*/golden/'`
  must only ever show commits that also touch an ADR-0004 register entry.
- Re-capture is a five-minute documented operation instead of an improvisation
  under deadline pressure — which is exactly when silent smoothing happens.
