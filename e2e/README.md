# e2e/ — Selenium safety net

Selenium 4 + JUnit 5 + Java 25 E2E suite, built in **G2 (stage 1)**.

Design constraints (binding, see `docs/SPEC.md` §3):

- **Page Object pattern** with a **selector-map abstraction**: the same scenarios
  run against the AngularJS UI and, later, the Angular UI — only the per-app
  selector map differs. Run with `-Dtarget=legacy` or `-Dtarget=modern`.
- Scenarios: customer CRUD, repair-order lifecycle, invoice creation, report values.
- **Explicit waits only.** Zero flaky tolerance: a flaky test is retried, analysed,
  fixed — never ignored, never `@Disabled` to get green.

This suite is the contract of the whole project: from tag `stage-1-safety-net`
onward it must stay green through every migration commit.

Status: **empty — work starts in G2.** See [`../stages.md`](../stages.md).
