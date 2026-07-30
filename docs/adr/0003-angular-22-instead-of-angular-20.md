# ADR-0003 — Angular 22 instead of Angular 20 as the stage-5 target

Date: 2026-07-30 · Status: accepted · Deciders: Sebastian Kern (owner)
Supersedes: the Angular-20 pin in PRD §3/§5, SPEC §1/§4, MILESTONES G5, CLAUDE.md

## Context

PRD and SPEC were written on 2026-07-23 and pin **Angular 20** as the stage-5
target. Version check on 2026-07-30 (npm registry `@angular/core` dist-tags,
GitHub releases):

| Line | Latest | Status |
|---|---|---|
| Angular 20 | 20.3.27 | **LTS**, support ends **November 2026** |
| Angular 21 | 21.2.19 | LTS |
| **Angular 22** | **22.1.0** (2026-07-29) | **current stable** (`latest`) |

Stage 5 is the largest milestone (5–6 days) and stage 6 follows. Angular 20
would therefore leave support **within months of this repository going public**
— on a portfolio piece whose entire argument is *"do not defer upgrades until
your stack is out of support"*. Migrating a 2016 application onto a version
that is already in maintenance mode would undercut the message the playbook
sells.

Counter-argument considered: Angular 20 is what the PRD promised, and the JKU
tender context mentions Angular generally, not a version. That is exactly why
the change is cheap — no external commitment names a specific major.

## Decision

**Stage 5 targets Angular 22 (currently 22.1.0).** All specification documents
are updated in the same commit as this ADR; the version is pinned to the major
(22.x), with the exact patch recorded in the stage-5 worklog entry when the
work runs.

Rationale, in order of weight:

1. **Credibility.** The repository must live the rule it preaches: land on a
   version with a support runway ahead of it, not behind it.
2. **The migration distance is unchanged.** AngularJS 1.8 → Angular 22 is the
   same Strangler-Fig exercise as → 20 (standalone components, signals, new
   control flow). The playbook chapter, the effort figures and the selector-map
   v2 approach are unaffected — the target major is a parameter, not a method.
3. **Longevity of the artefact.** The playbook is a sales asset meant to be
   handed to prospects for a year or more. "We migrated to the current stable"
   ages far better than "we migrated to a version that went EOL that autumn".

## Consequences

- `CLAUDE.md`, `docs/PRD.md`, `docs/SPEC.md`, `docs/MILESTONES.md`,
  `stages.md`, `README.md` and `modern/README.md` now say Angular 22.
  `docs/VERMARKTUNG.md` case-study wording updated accordingly.
- Earlier worklog entries keep their original "Angular 20" wording — they are a
  dated record, not a specification; this ADR documents the change of course.
- The stage-5 E2E work is unaffected in design: the same scenarios run against
  the new UI via `selectors/modern.properties` v2. Only the selector values
  change, as planned since stage 1.
- Node/CLI toolchain versions for Angular 22 are verified at the start of
  stage 5 (never pinned from memory), and the exact patch is logged.
