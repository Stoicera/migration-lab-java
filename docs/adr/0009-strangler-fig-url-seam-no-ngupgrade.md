# ADR-0009 — Stage-5 Strangler Fig: URL seam with two SPAs, no ngUpgrade; zoneless wait contract; WAR→JAR

Date: 2026-07-31 · Status: accepted · Deciders: Sebastian Kern (owner, via
standing autonomous-execution mandate); executed per MILESTONES G5 / SPEC §4

## Context

Stage 5 replaces AngularJS 1.8 with Angular 22 (ADR-0003) under two hard
constraints: the Selenium suite must stay green against the modern stand on
every commit (safety-net rule), and the hybrid period must be real and
documented, not simulated. Three architectural questions had to be decided:
how the two frameworks coexist, how the E2E suite waits on a zoneless Angular
app, and what happens to the WAR packaging once the JSP admin page dies.

## Decision

1. **Coexistence: one origin, the URL scheme is the seam — no ngUpgrade.**
   The Angular app owns `/` with path routes from the first slice; the
   AngularJS app stays fully functional at `/alt.html#!/…` (its hash routing
   makes the split free). One route group migrates per commit; cross-overs are
   full page loads (Angular catch-all → `/alt.html#!<path>`; the old app keeps
   a ported-routes list and hands over via `window.location`). Both shells
   carry byte-identical nav hrefs so the selector map holds mid-flow.
   `@angular/upgrade` exists at 22.1.0 (checked live 2026-07-30, session start) and was
   REJECTED for this size class: dual change detection, AngularJS inside the
   Angular build and `$injector` bridging buy page-internal mixing we do not
   need — ten views with clean route boundaries make the URL seam strictly
   cheaper and trivially reversible. Decision rule recorded in playbook Kap. 5.
2. **E2E wait contract for zoneless Angular: an app-maintained pending-request
   counter.** The CLI-22 default app is zoneless, so the classic
   Testability/`isStable` probe observes nothing. The app exposes
   `window.werkstattOffeneRequests`, maintained by an HTTP interceptor — the
   same semantic the suite already polled on AngularJS (`$http.pendingRequests`).
   The counter is a testability contract of the app, not test tooling. The
   suite gains `angular` and (for the hybrid window) `hybrid` wait strategies;
   unknown strategy values still throw (fail-loud, e2e/README).
3. **Selector map v2 on `data-testid` anchors + key-set parity guard.** The new
   UI carries explicit test anchors instead of 2016 positional selectors; a
   `SelectorMapParityTest` makes the two maps' key sets provably identical, so
   per-slice map edits cannot silently drift. Per-stand EXPECTATION values
   (SD-3's alert text) live in the maps too — the only e2e code change of the
   stage besides the wait strategies.
4. **WAR→JAR at cutover.** The JSP admin page was the only WAR reason; with it
   absorbed (SD-2), `modern/` packages as an executable JAR and
   `src/main/webapp/` is deleted. The Angular build ships inside the Boot
   artifact via frontend-maven-plugin (pinned Node v24.18.1 LTS, `npm ci`
   against the committed lockfile) — no local Node toolchain requirement,
   mirroring the Dockerized JDK-8 build of `legacy/`.

## Consequences

- The hybrid period is verifiable history: one commit per route slice, each
  verified green against the modern stand (e2e + characterization); the legacy
  legs were re-run at every commit that touched shared suite code (slices 1, 4,
  7, the format pass) and in the full both-stand closing matrix — a risk-based
  cadence, recorded precisely in worklog sessions 9/10 rather than rounded up
  to "everything, always". The hybrid window includes a Selenium scenario that
  crosses the framework seam mid-flow (order detail → invoice sheet).
- The old app was strangled to an empty shell BEFORE deletion — every slice
  removed its AngularJS view/controller/route immediately (no zombie copies).
- Zoneless is a real behavioural constraint, not a toggle: async-written
  component state must be signal-tracked (bug found by the suite, fixed and
  documented in worklog session 9 / playbook Kap. 5).
- Formatting parity is enforced by replicating the 2016 `euro` filter
  byte-for-byte (EuroPipe); `alert()`/`confirm()` stay. UX modernisation is
  explicitly out of scope for the migration (playbook rule).
- SD-2 and SD-3 are registered in ADR-0004 with characterization/e2e pins on
  both stands.
