# deploy/ — the production translation of the two stands

This directory is what runs on the Stoicera fleet (Dokploy on `skdevserver1`); the
decisions behind every file are in [ADR-0016](../docs/adr/0016-deployment-dokploy-stoicera-fleet.md),
and the executed walkthrough with its verification evidence is
[`docs/deployment.md` §10](../docs/deployment.md#10-production-deployment).

| File | What it is |
|---|---|
| `legacy.compose.yml` | Legacy stand, **whole site behind Basic auth** (SD-1 stays an exhibit, never an open service) |
| `modern.compose.yml` | Modern stand, admin surface gated — the edge overlay's measured middlewares on the host's Traefik |
| `verify-live.sh` | The production sibling of `modern/edge/verify-edge.sh`: certificates, redirects, auth boundary, headers, rate limit — asserted from outside |

The local stands (`legacy/docker-compose.yml`, `modern/docker-compose.yml` + edge
overlay) are unchanged by any of this; the quickstart still needs no env file.

## Operational facts

- Each stand is a Dokploy **compose service** (project *migration-lab*) pulling the
  repository and running its compose file; images come from GHCR, built by
  [`.github/workflows/deploy.yml`](../.github/workflows/deploy.yml) on every merge to
  `master`. The workflow is not a required check on purpose (same reasoning as the
  playbook PDF).
- Secrets live in Dokploy's env store per service, never in this repository:
  `LEGACY_ADMIN_AUTH`, `LEGACY_DB_PASSWORD` · `MODERN_ADMIN_AUTH`, `MODERN_DB_PASSWORD`,
  `MODERN_HSTS_SECONDS`. Rotating one is: change it there, redeploy the service.
- No application or database port is published on the host. Path in: the host Traefik
  on 443. Path to the data: `docker exec` on the app node.
- Backups: `pg_dump` from both stands' db containers via the host cron
  (`/etc/cron.d/migration-lab-backup` on the app node), dated dumps under
  `/var/backups/migration-lab/`, 14 days retention, and a copy pulled off the machine.
  The restore rehearsal — an actual restore, not a hope — is recorded with its date in
  `docs/deployment.md` §10.
- After any CSP or frontend change: `deploy/verify-live.sh` **and** a real browser's
  console on the live site. No script in this repository can see a CSP violation
  (`docs/MANUAL_TASKS.md` §H).
