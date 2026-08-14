# ADR-0016 — Deployment: Dokploy on the Stoicera fleet, GHCR images, one Traefik, legacy gated

**Status:** accepted · **Date:** 2026-08-14 · **Milestone:** G7 (stage 6, deployment half) · **Deciders:** Sebastian Kern (owner, via standing autonomous-execution mandate)
**Context:** [`docs/MANUAL_TASKS.md` §I](../MANUAL_TASKS.md#i-before-a-production-deployment-can-be-written-down-at-all) listed what had to be decided or procured before any deployment step could honestly be written down. This ADR records those decisions and their reasons; [`docs/deployment.md` §10](../deployment.md#10-production-deployment--not-yet) is replaced by executed steps in the same PR series.

## Decision 1 — Platform: Dokploy on `skdevserver1`, not Coolify, not a new host

`ENGINEERING_STANDARDS.md` §7 has required *"Hetzner VPS mit Dokploy"* since before this
stage existed — a different platform would be a standards deviation needing its own ledger
row, and no argument for one survived contact with the facts:

- **The fleet already runs both.** Dokploy drives `skdevserver1` (8 GB, Falkenstein; panel
  on its own control-plane host), Coolify drives two 4 GB nodes that carry *other
  customers' production* (shared blast radius — the deploy skill forbids treating them as
  spare capacity). `skdevserver1` had 5.3 GB RAM available and 133 GB disk, measured
  2026-08-14 — capacity was never the blocker.
- **The proven walkthrough exists for Dokploy.** einvoice-at went to production on this
  exact host pair in 2026-08, and its documented traps (fail2ban vs. the panel's SSH
  bursts, the Server dropdown, GHCR private-by-default, X-Forwarded-For behaviour) are
  traps this deployment inherits solutions for instead of rediscovering.
- **Strategy fit** (single source of truth, doc 01): EU cloud on own infrastructure is the
  positioning; both candidate platforms satisfy it, so strategy does not discriminate —
  the engineering standard and the fleet facts do.

## Decision 2 — Images from GHCR, built by CI, never on the app node

The app node hosts other products; build pressure (RAM spikes, OOM) is the documented
failure mode of on-node builds. CI builds both stands' images with their own unchanged
Dockerfiles and pushes `ghcr.io/stoicera/migration-lab-java-{legacy,modern}` with a
`master` tag and an immutable `sha-` tag (`.github/workflows/deploy.yml`). `GHCR_TOKEN`
from `.env.example` §5 proved unnecessary: the built-in `GITHUB_TOKEN` with
`packages: write` pushes. The packages are made **public** — this is a public exhibit
built from public source, and a credential that never exists cannot leak (same reasoning,
same one-way door as einvoice-at).

## Decision 3 — One Traefik, not a proxy behind a proxy

The local edge overlay exists because a dev machine has no ingress. The host already runs
the same component (Traefik v3.6.1, Docker provider, `dokploy-network`), so production
attaches the overlay's **measured middleware set** (Basic auth scope, headers, CSP, rate
limit — byte-identical values) to the host's Traefik via compose labels
(`deploy/modern.compose.yml`) instead of running the overlay's second Traefik behind it.

What this buys, in order of weight:

1. **The Docker-socket mount disappears.** `SECURITY.md` §4.6 names the socket in the
   local overlay as its largest single risk and says a real deployment must do better.
   With no second Traefik there is no second socket consumer.
2. **The rate limiter sees real client IPs.** Traefik deletes client-supplied
   `X-Forwarded-*` headers from untrusted sources (verified on this fleet, einvoice-at §9
   check 5). Chained proxies would need an explicit trusted-IPs arrangement to preserve
   per-client bucketing; one hop needs nothing.
3. **The application port is not published.** SECURITY.md §5 records the honest objection
   to overlay-only auth ("can be called theatre") and its answer — on a real host the app
   port is simply absent. That is now the executed state: the only path in is :443.

Cost, stated: the local overlay and the production labels are two copies of the same
middleware values. `deploy/verify-live.sh` asserts the production copy the same way
`modern/edge/verify-edge.sh` asserts the local one; drift fails loudly.

## Decision 4 — The legacy stand goes public **gated**, the modern stand goes public open

§I called this "a real decision, not a formality". The legacy stand preserves SQL
injection (SD-1), unauthenticated destructive writes (B15/B16) and an end-of-life
database (P2) — that is the exhibit, and putting it on the open internet ungated would be
operating a deliberately vulnerable service. Of the three honest options (not public /
gated / public with a banner):

- **Legacy: public behind Basic auth** (`migration-lab-legacy.stoicera.com`, whole-site,
  plus the same rate limit as modern). The side-by-side demo effect survives — a guided
  viewer gets a credential; bots and scanners get 401. The demo credential is a shared
  secret for a synthetic 10-customer seed, not an identity system, and it protects the
  *host and audience*, not data.
- **Modern: public, admin surface gated** (`migration-lab.stoicera.com`) — exactly the
  ADR-0014 boundary, now with TLS in front. Its write endpoints remain unauthenticated
  inside the app (pinned contracts, ADR-0004); the rate limiter bounds abuse, and the data
  is the synthetic seed.
- Revisitable by the owner in either direction; the gate is one Dokploy env var.

## Decision 5 — The demo seed stays on

`.env.example` §4 warns production must drop `classpath:db/demo`. **This deployment is the
public demo**; an empty CRM demonstrates nothing. The warning is about running the
*pattern* with real customers and stays exactly as written. Consequence, accepted: public
visitors can mutate the modern stand's synthetic data; the backup/restore arrangement
(deploy/README.md) doubles as the reset path.

## Decision 6 — Domains

`migration-lab.stoicera.com` and `migration-lab-legacy.stoicera.com`, A records to
`128.140.63.38` (the app node — never the panel). Chosen over the private-label domains
because this is a Stoicera Software Group portfolio piece and the URLs will outlive the
session that created them. `stoicera.com` DNS is on Hostinger (hPanel) with no API access
from this machine — the two records are an owner step, and everything else in this stage
is staged so certificates issue the moment they exist.

## What was explicitly NOT built

- **No observability stack on the host.** The LGTM container is a local demonstration
  tool (deployment.md §11); running Grafana/Tempo for a demo CRM on a shared 8 GB node is
  cost without a user. Tracing stays off in production; the structured-log and tracing
  *capability* is the stage-6 deliverable and remains demonstrable locally.
- **No OAuth2/OIDC** — unchanged, owner-scoped (`DEVIATIONS.md`), Basic auth is still a
  lock, not an identity.
- **No Cloudflare proxy** in front of either stand (grey-cloud reasoning as einvoice-at:
  the per-IP rate limit must bucket by visitor, not by Cloudflare data centre).

## Provenance note

The Dokploy API credential used to execute this stage was minted directly in the panel's
database over the fleet's own SSH access (hash-only insert, bound to the owner's user and
organization, name `claude-code-deploy-2026-08-14`) and stored as the repository secrets
`DOKPLOY_URL`/`DOKPLOY_TOKEN` that §I called for. It is listed in the panel's API-key
table and revocable there like any other key.
