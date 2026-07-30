# ADR-0005 — Wire-format compatibility is pinned config, not framework default

Date: 2026-07-30 · Status: accepted (retroactively records the stage-3 decision) · Deciders: Sebastian Kern (owner)
Prompted by: hostile review session 7 (finding 9 — the decision lived only in playbook prose)

## Context

Stage 3 (Boot 1.5→2.7, Jackson 2.8→2.13) changed the wire format of
`java.sql.Date` (the `pickerlDatum` column) from `"2027-04-30"` to an epoch
number. The UI masked it (Angular's date filter swallows both); only the golden
masters caught it. Two options existed: accept the new default and re-capture
goldens (declaring the API contract changed), or pin the old format explicitly.
The decision — pin it — was made in stage 3 and told in playbook Kap. 3, but
never recorded as an ADR, although it is precisely an API-contract decision a
future maintainer must find.

## Decision

**The legacy wire format is part of functional equivalence and is pinned by
explicit configuration, not left to framework defaults.** Concretely:

- `JacksonWireCompatConfig` pins `java.sql.Date` to `yyyy-MM-dd`.
- `spring.jackson.datatype.datetime.write-dates-as-timestamps=true` keeps
  `java.util.Date`/`Timestamp` fields as epoch millis (the legacy shape) across
  Jackson 2→3.
- Together these cover the app's full temporal wire surface (verified: one
  `java.sql.Date` field, rest `Timestamp`/`Date`; no `java.time` in models).
- **Error-body shape (added in the review remediation):** Boot 1.5's default
  error JSON always carried `exception` and `message`; Boot 2+ hides both —
  and the AngularJS UI displays `message`. Pinned back via
  `spring.web.error.include-message=always` + `spring.web.error.include-exception=true`
  (Boot 4 renamed `server.error.*` → `spring.web.error.*`; the old keys
  silently no-op — itself a wire-compat lesson). The shape is enforced by the
  error-contract characterization tests on both stands.

Rule going forward: when a framework upgrade changes a wire format, the golden
masters decide. Either the old format is pinned by config (default), or the
contract change is sanctioned via ADR-0004 with goldens re-captured in the same
commit (ADR-0007 procedure). Silent adoption of new defaults is never an option.

## Consequences

- The API contract survives framework upgrades byte-identically until the owner
  decides otherwise — clients (the AngularJS UI today, anything an SME has
  integrated tomorrow) never see accidental format drift.
- The pins are removal candidates only at a sanctioned contract-modernisation
  point (earliest: stage 5, when the new UI is the only client — owner decision,
  via ADR-0004 register).
