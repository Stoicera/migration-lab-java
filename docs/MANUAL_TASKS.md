# MANUAL_TASKS.md — the steps a human has to do by hand

Everything in this repository that is **not** automated, as checklists. Nothing here duplicates
[`deployment.md`](deployment.md) — that document explains how and why, this one is what you tick
off. Where a step needs more than one line of explanation, it links there.

Print it, copy it into an issue, or just work down it. The order inside each list is the order to
do things in.

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

Currently required on `master` — seven checks:
`legacy-build` · `modern-build` · `e2e (legacy)` · `e2e (modern)` · `harness` ·
`testbed-validation (legacy)` · `testbed-validation (modern)`

### Ongoing

- [ ] Review Dependabot PRs. `legacy/` is deliberately excluded from Dependabot — its EOL
      dependencies are the exhibit, not a vulnerability to patch.

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

## H. What is *not* here yet

Deployment to a server is not on this list because **there is nothing to deploy to yet** — see
[`deployment.md` §10](deployment.md#10-production-deployment--not-yet). When G7 lands, this
document gains a section for it. It will not gain one before.
