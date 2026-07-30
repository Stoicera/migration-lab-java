# ADR-0004 — Functional equivalence: definition, and how a divergence becomes legal

Date: 2026-07-30 · Status: accepted · Deciders: Sebastian Kern (owner)
Prompted by: hostile review session 7 (findings 5, 9 — "equivalence amended off the books")

## Context

The repo's core claim is "every stage keeps the app functionally equivalent —
proven, not asserted." Stage 4 deliberately broke equivalence in one place: the
B4 SQL-injection fix makes hostile search input behave differently on the modern
stand (legacy leaks all customers; modern returns none). The review found that
(a) no document defined *who* sanctions such a divergence and *how*, (b) the
divergence itself was pinned by no test, and (c) README/stages claimed
equivalence unqualified. A migration methodology that cannot say precisely what
"equivalent" means is not one an SME can buy.

## Decision

1. **Definition.** Functional equivalence = identical observable behaviour of
   the REST API (status, body, wire formats), the rendered admin page, and DB
   state transitions **for all legitimate inputs**, as pinned by the
   characterization suite. The suite is the definition; behaviour it does not
   pin is not yet part of the proven contract (and closing such gaps is review
   work, not an excuse).
2. **Sanctioned divergence.** A deliberate behaviour change on the modern side
   is legal only with all three of: (a) an ADR (or an entry in this ADR's
   register below) naming the change and its migration reason; (b) a
   characterization pin of BOTH sides' behaviour in the same change (the
   `stand` property selects the expectation per stand); (c) a playbook mention
   where the stage is narrated. Anything else that makes the suites diverge is
   a regression, full stop.
3. **Register of sanctioned divergences** (grows only with owner sign-off):

   | # | Since | Divergence | Reason |
   |---|---|---|---|
   | SD-1 | stage 4 | Hostile input to `GET /api/kunden?suche=` (and the `?status=` filter): legacy is injectable, modern is parameterised. Pinned observed behaviour: `%' OR '1'='1` → legacy 200/10 rows vs modern 200/0 rows; lone `'` → legacy 500 (PSQLException leaks) vs modern 200/0 rows | Security fix B4 — carrying an injection into the target stack to preserve bug-for-bug equality would be malpractice; equivalence holds for all legitimate inputs |

## Consequences

- The characterization suite carries stand-aware pins for SD-1 (hostile inputs
  asserted against each stand's actual behaviour) — the divergence is now
  *proven on both sides*, not narrated.
- README/stages wording is qualified: "functionally equivalent for legitimate
  inputs; deliberate security divergences are registered in ADR-0004."
- Future candidates (e.g. error-message sanitisation, BigDecimal money) must go
  through the same three-part gate before any code changes behaviour.
