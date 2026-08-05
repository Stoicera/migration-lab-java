# ADR-0014 — Authentication, security headers and rate limiting at the edge, not in the application

**Status:** accepted · **Date:** 2026-08-05 · **Milestone:** G7 (stage 6) · **Deciders:** Sebastian Kern (owner)
**Context:** [`docs/DEVIATIONS.md`](../DEVIATIONS.md) records the modern stand as
unauthenticated by inherited design — including the destructive `POST /admin/bereinigen`
that wart B16 bequeathed — and names the closing condition verbatim: *"the public demo
deployment must protect `/admin` at minimum (**reverse-proxy auth counts**); full
OAuth2/OIDC per §4 needs an owner-scoped stage of its own plus the §9 auth ADR."*
`ENGINEERING_STANDARDS.md` §4 additionally requires security headers, CSRF protection and
rate limiting at the public endpoint. Narrated in `playbook/07-betrieb-und-haertung.md`.

## Context

The 2016 application has no authentication at all, and stage after stage kept it that way
on purpose: adding auth changes every pinned contract, so it cannot happen as a side
effect of another stage ([ADR-0004](0004-functional-equivalence-and-sanctioned-divergence.md)'s
gate). That was defensible while the app only ever ran on a laptop. It stops being
defensible the moment the repo talks about a public demo, because one of the unauthenticated
endpoints deletes data.

So the question of this stage is not *"should there be auth"* — it is **where the boundary
goes**, and what that choice costs the safety net.

## Decision

**The boundary is a Traefik reverse proxy in front of the application
([`modern/docker-compose.edge.yml`](../../modern/docker-compose.edge.yml), `traefik:v3.6.1`),
not Spring Security inside it.** Reasons, in order of weight:

1. **It is the scope-respecting option, and the ledger says so in advance.** DEVIATIONS
   already fixed "reverse-proxy auth counts" as the requirement and full OAuth2/OIDC as
   owner-scoped work needing its own ADR. Doing more here would be scope creep dressed as
   diligence; doing less would leave a destructive endpoint open.
2. **Spring Security in the application would change pinned contracts — measured, not
   feared.** From the safety-net map: a matcher scoped to `/admin` breaks **exactly 4
   tests**, and enabling CSRF with the Spring default matcher breaks **17 characterization
   write calls plus every E2E write scenario**. Every one of those is an ADR-0004
   sanctioned-divergence decision requiring an ADR entry, both-stand pins and a playbook
   mention. That is a stage of its own, which is exactly what DEVIATIONS calls it.
3. **Selenium cannot dismiss Chrome's native Basic-auth dialog.** The workaround is the
   CDP `HasAuthentication` hook, which is Chrome-version-fragile; this suite has a stated
   **zero-flaky-tolerance** policy, and trading it away to authenticate a demo would be a
   bad trade in the one place this repo claims to be strict.
4. **Dokploy runs Traefik.** The local edge is a rehearsal of the target platform's own
   component, not a stand-in for it.

**Layout.** The edge listens on `:8091`; the application keeps `:8090` **locally**, so the
characterization and E2E suites keep their direct path and the equivalence gate is not
re-plumbed by a security change. Protected surface: `/admin`, `/api/admin`, `/actuator`
(health is ours to read, not free reconnaissance for a stranger). Public surface:
everything else, with headers and rate limiting still applied. An **overlay, not a compose
profile** — it requires `MODERN_ADMIN_AUTH`, and a required variable in the base file would
break `up -d --wait` for everyone who only wants the stand.

### Measured — all assertions run by [`modern/edge/verify-edge.sh`](../../modern/edge/verify-edge.sh), exit 0

| what | measured |
|---|---|
| unauthenticated `/admin`, `/api/admin/statistik`, `POST /admin/bereinigen`, `/actuator/health` | **401** |
| the same four, authenticated | **200** |
| wrong password | **401** |
| public surface `/`, `/api/kunden`, `/rechnungen` | **200** |
| headers on public responses | `X-Frame-Options: DENY`, `X-Content-Type-Options: nosniff`, `Referrer-Policy: no-referrer`, full CSP |
| rate limit (average 30/s, burst 60), 80 rapid requests through the edge | **5 × 429** |
| the same burst straight at the application | **0 × 429** |

HSTS is **off by default** (`stsSeconds: 0`) and that is deliberate, not an omission:
switched on over plain HTTP it teaches the browser to refuse `http://localhost` for a year.
A TLS-terminating host sets it.

## Alternatives rejected

- **Spring Security in the application** (the textbook answer). Rejected on the measured
  blast radius above: 4 tests for the `/admin` scoping alone, and 17 characterization write
  calls plus every E2E write scenario once CSRF comes with it. Not "hard" — *out of scope
  by the repo's own gate*, and the requirement is fully satisfied without it. It remains
  the right long-term answer; it belongs to the owner-scoped auth stage together with
  OAuth2/OIDC.
- **Route the E2E suite through the edge using Selenium's CDP `HasAuthentication` hook.**
  Rejected: Chrome-version-fragile against a zero-flaky-tolerance policy. The suite keeps
  the direct path to `:8090` instead, and the edge is verified by its own script — a
  separate check that fails on its own terms rather than a browser trick inside the safety
  net.
- **Do nothing this stage; close it at deployment time.** Rejected: it leaves an
  unauthenticated destructive endpoint as the last state of the repo, and it makes the
  ledger's "hard requirement for G7" a sentence the project wrote about itself and did not
  keep.

## The honest part

- **"Auth that only exists in a compose overlay is theatre."** That is a fair charge and it
  is recorded here rather than answered somewhere quieter. Three answers: it is the same
  component the target platform runs, so it is a rehearsal and not a mock; it is verified by
  an automated script rather than by assertion; and on a real host the application port is
  **not published at all** ([`docs/deployment.md` §12](../deployment.md#12-the-reverse-proxy-edge)) — that
  obligation, not the overlay, is what turns the edge from a suggestion into the only path. What remains true of the charge: **no deployment has
  happened.** There is no host, no TLS, no public URL. This ADR decides where the boundary
  goes and proves it holds locally — nothing more.
- **The CSP finding — the item worth the whole section.** The first policy was fully strict:
  `style-src 'self'`. **32 of the suite's 34 scenarios ran green through the edge** — the two `AdminTest`
  scenarios cannot run through it at all, because `/admin` is behind Basic auth and Selenium
  cannot answer the browser's native credential dialog; they keep running against the
  application port, where they remain part of the 34/34 gate. And the browser
  console showed the policy **being violated and styles blocked** — Angular injects
  component styles as `<style>` elements at runtime. **We got this wrong first and fixed it
  after measuring**, and the measurement that found it was not a test: it was opening a real
  browser and reading the console. **The Selenium safety net cannot see a CSP violation.**
  It asserts behaviour and text, not appearance, so a green suite proved less than it looked
  like it proved. Resolution: `style-src 'self' 'unsafe-inline'`, and **nothing else was
  relaxed** — `script-src 'self'` stays strict, and that is the directive that stops injected
  code from executing. Re-measured after the change: **0 console errors**. `verify-edge.sh`
  asserts `script-src 'self'` verbatim so that a later "just add `unsafe-inline` until it
  works" cannot pass unnoticed, and the script states this blind spot in its own output. The
  clean fix is Angular's `CSP_NONCE`, which requires `index.html` to be rendered per request
  instead of served as a static resource — owner-scoped, registered in DEVIATIONS.
- **CSRF was deliberately not added, and here is the reason and the residual risk.** The
  application has no sessions, no cookies and no ambient authority, so a classic CSRF token
  would protect nothing. What Basic auth at the edge *does* introduce is **browser-cached
  credentials**, which a cross-site POST could ride. That residual risk is **accepted for a
  demo** and recorded in the threat-model sketch `SECURITY.md` (ENGINEERING_STANDARDS §4)
  rather than left implicit. The real fix — session-based OIDC plus a token — is part of the
  owner-scoped auth stage, not this one. This is a deviation from §4's "CSRF-Schutz" bullet
  and is stated as one.
- **Basic auth is a lock, not an identity system.** It has no users, no roles, no audit
  trail. It closes the one door that deletes data. Anything that reads like "the demo is
  secured" beyond that sentence is overselling it.
- The edge mounts the Docker socket read-only for Traefik's own discovery, and that is the
  largest single risk in the file — the socket is root on the host. Named in the compose
  file and in `SECURITY.md` rather than hidden in a `volumes:` block nobody rereads.
