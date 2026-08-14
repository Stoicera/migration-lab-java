# MANUAL_TASKS.md — the steps a human has to do by hand

Everything in this repository that is **not** automated, as checklists. Nothing here duplicates
[`deployment.md`](deployment.md) — that document explains how and why, this one is what you tick
off. Where a step needs more than one line of explanation, it links there.

Print it, copy it into an issue, or just work down it. The order inside each list is the order to
do things in.

---

## What is actually open right now — state of 2026-08-14, after session 15

Stage 6 is **complete**: both stands are deployed, verified, backed up and tagged
(`stage-6-cloud-ops`, release v1.0.0). What remains by hand is small and periodic:

| | Task | Where | Effort |
|

---

## A. Once per machine

Do this once. [Details and per-OS install commands: `deployment.md` §1](deployment.md#1-prerequisites)

- [ ] **Docker + Compose plugin** installed, daemon running
      → `docker compose version` prints v2.x or v5.x, `docker version` shows a `Server:` block
- [ ] **Your user is in the `docker` group** (Linux), and you have logged out and back in
      → `docker ps` works without `sudo`
- [ ] **A JDK 25 or newer** on `PATH`
      → `java -version` prints 25+
- [ ] **Chrome or Chromium** installed — only if you will run the E2E suite
      → `chromium --version` or `google-chrome --version`
- [ ] **`psql` client** — only if you want to inspect the databases
      → `psql --version`
- [ ] `python3` and `column` available — only if you will run `ai-testgen/measure.sh`
- [ ] **First start works**
      → `docker compose -f legacy/docker-compose.yml up -d --wait` and
        <http://localhost:8080> loads

Do **not** install Maven — the repo ships `./mvnw`, which fetches its own.
Do **not** install Node — the `modern` build installs its own.
Do **not** install a JDK 8 unless you specifically need to build `legacy/` outside Docker
([why](deployment.md#14-the-one-module-you-cannot-build-with-a-modern-jdk)).

---

## B. Start of every working session

- [ ] `git pull`
- [ ] Read the last entry in [`worklog.md`](worklog.md) — it ends with a **Next** line
- [ ] Read the current milestone in [`MILESTONES.md`](MILESTONES.md)
- [ ] Bring up the stands you need:
      ```bash
      docker compose -f legacy/docker-compose.yml up -d --wait
      docker compose -f modern/docker-compose.yml up -d --wait
      ```
      **`--wait` is not optional** — [why](deployment.md#2-first-run)
- [ ] Run the suites that guard what you are about to touch, **before** you touch it, so you know
      the baseline was green and any red is yours:
      ```bash
      ./mvnw verify -f characterization/pom.xml
      ./mvnw verify -f e2e/pom.xml -Dtarget=legacy
      ```

---

## C. Every change, before the PR

- [ ] Work on a branch. Direct pushes to `master` are blocked by branch protection.
- [ ] The safety net is green — **both** stands where the change could affect either:
      ```bash
      ./mvnw verify -f characterization/pom.xml
      ./mvnw verify -f characterization/pom.xml \
        -DbaseUrl=http://localhost:8090 \
        -DdbUrl=jdbc:postgresql://localhost:5434/werkstatt -Dstand=modern
      ./mvnw verify -f e2e/pom.xml -Dtarget=legacy
      ./mvnw verify -f e2e/pom.xml -Dtarget=modern
      ```
      ⚠️ The characterization suite has **no `-Dtarget`** — using it gives you a green run against
      the *wrong* stand. [Read this once](deployment.md#51-characterization--the-equivalence-gate).
- [ ] `./mvnw verify -f modern/pom.xml` if `modern/` changed (needs Docker running)
- [ ] A **red suite is a stop condition.** Never skip, disable or `@Disabled` a test to get green.
- [ ] If you did not meet a standard, add a row to [`DEVIATIONS.md`](DEVIATIONS.md). Silence is
      the one thing the ledger does not allow.
- [ ] An ADR in `docs/adr/` for every real migration decision
- [ ] Conventional Commit message
- [ ] `docs/worklog.md` entry: date, what, **measured hours**, decisions, next
- [ ] Open the PR; wait for all seven required checks
- [ ] Merge only on green

---

## D. Completing a stage

A stage is only done when all four exist — and they travel in **one** PR:

- [ ] The working `docker compose up` state
- [ ] The playbook chapter in `playbook/` (German)
- [ ] The row in [`../stages.md`](../stages.md)
- [ ] The **annotated** git tag:
      ```bash
      git tag -a stage-N-name -m "what this state is"
      git push origin stage-N-name
      ```
      Annotated, not lightweight — a lightweight tag carries no author, date or message, and
      provenance is the point of a stage tag.

---

## E. Repository administration on GitHub

### One-time (already done for this repo)

- [ ] Branch protection on `master`: require a PR, require status checks, block force-push and
      deletion, no bypass for admins

### Every time a workflow is added or a job is renamed — **this one is easy to forget and breaks merges**

- [ ] Merge the workflow first and **let the job run at least once**. A check that has never
      reported cannot be selected as required.
- [ ] Add the new check to the required list in **Settings → Branches** (or **Rules → Rulesets**)
- [ ] Matrix jobs report as `job (value)` — this repo requires `e2e (legacy)` and `e2e (modern)`,
      **not** `e2e`
- [ ] Renaming a job renames its check: the old required name never reports again and **every PR
      blocks forever**. Update the protection rule in the same PR series.

Currently required on `master` — **still seven checks**, unchanged by stage 6:
`legacy-build` · `modern-build` · `e2e (legacy)` · `e2e (modern)` · `harness` ·
`testbed-validation (legacy)` · `testbed-validation (modern)`

**Stage 6 added gates without adding checks, deliberately.** The edge verification and the Trivy
image scan are *steps inside* `modern-build`, and the schema-drift guard is a step inside
`legacy-build`. A new job would mean a new required-check name, a manual protection change, and a
window in which `master` is unmergeable — so the new gates were put where they cost nothing to
adopt. **Nothing on this list needs to change.**

One workflow *was* added — `playbook` (job `playbook-pdf`), which builds the playbook PDF as an
artefact. It is **intentionally not required**: it produces a deliverable, it does not guard
behaviour, and a failing PDF build must not be able to block a fix to the application. If you ever
want it required, it must run once first, then be added in **Settings → Branches** as
`playbook-pdf`.

### One-time, done 2026-08-14

- [x] **Private vulnerability reporting enabled** — via
      `gh api -X PUT repos/Stoicera/migration-lab-java/private-vulnerability-reporting`, verified
      with the GET returning `{"enabled":true}`. The *Security → Report a vulnerability* button
      now exists, and [`SECURITY.md`](../SECURITY.md) §2 points at it instead of documenting its
      absence.

### Ongoing

- [ ] Review Dependabot PRs. `legacy/` is deliberately excluded from Dependabot — its EOL
      dependencies are the exhibit, not a vulnerability to patch.
- [ ] Read the **Trivy image-scan output** in the `modern-build` job. It runs with
      `exit-code: 0`, so it never fails the build — which means nobody sees it unless somebody
      looks. That is the trade the ledger records; looking is the other half of it.

---

## F. The AI test-generation experiment (G6)

The only step in this repository that needs a credential.

- [ ] Create an API key at <https://openrouter.ai/keys> and load it with credit
- [ ] `cp .env.example .env`, then set `OPENROUTER_API_KEY=` in `.env` (git-ignored; the harness
      reads it itself, so the key never enters your shell history)
- [ ] Dry-run first — costs nothing:
      `./mvnw -q -f ai-testgen/harness/pom.xml compile exec:java -Dexec.args="plan"`
- [ ] Generate, then measure — [commands](deployment.md#8-the-ai-test-generation-experiment-g6)
- [ ] Check the row count of the resulting CSV: `measure.sh` does not validate its `phase`
      argument, and a typo produces an empty-but-plausible-looking measurement

Guard rails: the harness refuses any model outside the frozen price table and hard-aborts at €20
total. The full executed run cost €0.65.

---

## G. Re-capturing a golden master

Only when a **sanctioned** behaviour change makes an existing golden master wrong. This is the
most dangerous manual task in the repo: a carelessly re-captured golden master silently deletes
the evidence that behaviour used to be different.

- [ ] The change is registered in [ADR-0004](adr/0004-functional-equivalence-and-sanctioned-divergence.md)
      **first**. No register entry, no re-capture.
- [ ] Re-capture per the procedure in [`../characterization/README.md`](../characterization/README.md)
- [ ] Prove green against **both** stands, twice
- [ ] Re-captured file + ADR-0004 register entry + commit travel together (ADR-0007)

---

## H. Operating the stage-6 add-ons

Both are opt-in and neither is needed for the normal quickstart.

### The reverse-proxy edge (authentication, headers, rate limit)

- [ ] Generate a credential — it is never committed, and compose refuses to start without it:
      ```bash
      MODERN_ADMIN_AUTH="admin:$(openssl passwd -apr1 'dein-passwort')"
      export MODERN_ADMIN_AUTH
      ```
- [ ] Start with the overlay (**both** `-f` flags, order matters):
      ```bash
      docker compose -f modern/docker-compose.yml -f modern/docker-compose.edge.yml up -d --wait
      ```
- [ ] Verify rather than trust: `EDGE_USER=admin EDGE_PASSWORD=dein-passwort modern/edge/verify-edge.sh`
- [ ] **The one visual check no suite can do for you.** Open <http://localhost:8091/kunden> in a
      real browser, open the developer console, and confirm there are **no Content-Security-Policy
      violations** and the page looks styled. This is on the human list because it has to be: on
      2026-08-05 32 of the suite's 34 scenarios ran green while the browser was blocking Angular's styles —
      Selenium asserts behaviour and text, never appearance. Do this whenever the CSP or the
      frontend build changes.

### The observability profile

- [ ] `WERKSTATT_TRACING_ENABLED=true docker compose -f modern/docker-compose.yml --profile observability up -d --wait`
- [ ] Grafana on <http://localhost:3000>; generate traffic, then search Tempo for service
      `werkstatt-crm-modern`. Both switches belong together — tracing without a collector just
      logs export failures.

### One-off after the PostgreSQL upgrade

- [ ] The retired 9.6 volume is not deleted automatically and holds disk for nothing:
      ```bash
      docker volume ls | grep werkstatt
      docker volume rm modern_modern-werkstatt-db      # the OLD one; the live one ends in -pg18
      ```

---

## I. The production deployment — decided, procured, executed (2026-08-14)

This section used to be the list of things only the owner could decide or procure.
Every line has been decided and executed; the decisions live in
[ADR-0016](adr/0016-deployment-dokploy-stoicera-fleet.md), the executed walkthrough in
[`deployment.md` §10](deployment.md#10-production-deployment). For the record:

- **Host:** the existing Dokploy app node (ADR-0016 §1). No new VPS.
- **Domains:** `migration-lab.stoicera.cyou` (modern) · `migration-lab-legacy.stoicera.cyou`
  (legacy), A records at Hostinger → the app node.
- **Legacy exposure:** public **behind Basic auth**, whole site + rate limit (ADR-0016 §4).
  The demo credential is shared on request and lives in the Dokploy env store.
- **Secrets:** `DOKPLOY_URL` + `DOKPLOY_TOKEN` exist as repository secrets (created
  2026-08-14). `GHCR_TOKEN` was **deliberately not created** — `GITHUB_TOKEN` with
  `packages: write` pushes, and both GHCR packages allow anonymous pulls (verified).
- **Edge credentials** (`MODERN_ADMIN_AUTH`, `LEGACY_ADMIN_AUTH`) sit in Dokploy's env
  store per service — **htpasswd values need doubled dollars** (`$$apr1$$…`), the reason
  is a measured trap in `deployment.md` §10.3.

---

## J. Operating the deployment

- [ ] **Deploy** = merge to `master`. CI builds both images, pushes to GHCR and triggers
      both Dokploy services; the app services carry `pull_policy: always`, so a redeploy
      without a fresh image is a no-op and a redeploy with one recreates the container.
      Watch it end-to-end the first time after any workflow change: run green, then
      `docker ps` on the app node showing a new container from the new digest.
- [ ] **After any CSP or frontend change:** `deploy/verify-live.sh` AND a real browser
      console on <https://migration-lab.stoicera.cyou/kunden> — no script here can see a
      CSP violation (§H's rule, now with a live URL).
- [ ] **Rotate a credential:** Dokploy → service → Environment (htpasswd values with
      `$$`), redeploy the service, then re-run `deploy/verify-live.sh` — it asserts the
      lock opens as well as closes, which is exactly the direction that broke once.
- [ ] **Off-site backups (open):** procure the Storage Box, create
      `/opt/einvoice-at/offsite.env` per einvoice-at's §10.4 — the migration-lab cron
      already calls the shared sync script and will start shipping the moment the target
      exists. Until then: dumps + rehearsal exist, off-site copies do **not**.
- [ ] **Restore rehearsal** after any schema change: procedure in `deployment.md` §10.6.
