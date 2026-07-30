# ADR-0008 — CI enforcement: branch protection, armed gates, and the solo-review reality

Date: 2026-07-30 · Status: accepted · Deciders: Sebastian Kern (owner)
Prompted by: hostile review session 7 (findings 7, 22, 23 — "the sacred net has no lock on the door")

## Context

Until this ADR, every "hard rule" (red pipeline = stop; no commit may break the
net) was convention: `master` had no branch protection, the presence-gated
workflow steps could silently skip and report green, a zero-test run passed the
build, and all PRs were self-merged within minutes while the worklog said
"owner reviewed". For a repo selling *enforced* discipline, enforcement must be
mechanical wherever a machine can do it — and honestly labelled where it cannot.

## Decision

1. **Branch protection on `master`:** required status checks (`legacy-build`,
   `modern-build`, `e2e (legacy)`, `e2e (modern)`), strict up-to-date merges,
   force-pushes and deletions blocked, PRs required.
2. **No required human review count.** This is a solo-maintainer repo; a
   required-approvals rule would either block all work or be satisfied by the
   author's own approval — enforcement theatre. The honest model, stated
   publicly: gates are mechanical (the suites), review is the owner plus
   commissioned hostile reviews (like session 7), and "owner reviewed" in the
   worklog means exactly that — author-is-reviewer.
3. **Presence gates are retired.** All modules exist since stage 2; every suite
   step now runs unconditionally. A missing pom, compose file, or selector map
   fails loudly instead of skipping silently. (Their historical use — honest
   green before a module existed — is documented in the stage-0/1 worklog.)
4. **Zero-test runs fail:** `failIfNoTests=true` in both test modules; a
   discovery breakage can no longer impersonate a pass.
5. **Workflow hygiene:** unique job names (`legacy-build`/`modern-build`),
   `timeout-minutes` on every job, actions pinned, seed-copy drift guard, and
   compose `--wait` backed by real app healthchecks in both stacks.

## Consequences

- Breaking the net now requires deliberately dismantling protection with admin
  rights — an auditable act, not an accident.
- The solo-review model is a stated limitation, not a hidden one; external
  review (JKU, clients) is invited rather than implied.
- CI runs a few seconds longer (healthcheck waits) in exchange for the race
  the review predicted ("green by compile-time luck") being closed.
