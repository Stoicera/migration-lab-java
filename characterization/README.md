# characterization/ — golden-master tests

Approval/golden-master tests captured from the legacy application's actual behaviour
(REST/JSP responses, DB state transitions), built in **G2 (stage 1)**.

These tests **define functional equivalence** for the migration: they describe what
the system *does* (including its quirks), not what it *should* do. A migration step
is correct when these captures still match.

Together with the Selenium suite in [`../e2e/`](../e2e/) they form the safety net —
from tag `stage-1-safety-net` onward, no commit may break them.

Status: **empty — work starts in G2.** See [`../stages.md`](../stages.md).
