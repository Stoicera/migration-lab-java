# deployment.md — running and operating migration-lab

Required by [`ENGINEERING_STANDARDS.md`](ENGINEERING_STANDARDS.md) §7. **This file was missing
until 2026-08-02** — the obligation existed, the document did not, and it was not in the
deviations ledger either. That hole is now closed in both directions: the document exists, and
[`DEVIATIONS.md`](DEVIATIONS.md) carries the part it cannot yet deliver.

**Scope, stated up front so you do not go looking for something that is not here:**

| | Status |
|---|---|
| Running both stands locally (Docker Compose) | **documented here, works today** |
| Running every test suite | **documented here, works today** |
| Running the AI test-generation experiment | **documented here, works today** |
| Production deployment (Hetzner VPS + Dokploy, TLS, backups) | **does not exist yet — [§10](#10-production-deployment--not-yet)** |

Every command below was executed against this repository before being written down — §1–§10 on
2026-08-02, the stage-6 material (§4.5, §4.6, §11–§13) on 2026-08-05. Where a command's output is
quoted, that is the real output, not an illustration.

**Companion document:** [`MANUAL_TASKS.md`](MANUAL_TASKS.md) is the checklist of steps a human
must do by hand. This file explains *how* and *why*; that one is what you tick off.

---

## 1. Prerequisites

### 1.1 The table the repo never had

There is **no root `pom.xml`**. Every module is built with `-f <module>/pom.xml`, and the
modules do not all want the same JDK.

| You want to… | Needs | Notes |
|---|---|---|
| Run either stand | **Docker + Compose v2 or v5** | Nothing else. The applications are built inside Docker. |
| Build `modern/` | **JDK 25 or newer** + Docker running + network | Docker is needed because the test suite starts a real PostgreSQL (Testcontainers). Network because the build downloads its own Node. |
| Build `e2e/`, `characterization/`, `ai-testgen/harness/`, `ai-testgen/testbed/*` | **JDK 25 or newer** | |
| Run the E2E suite | + **Chrome or Chromium** installed | The driver downloads itself; the browser does not. |
| Build `legacy/` outside Docker | **JDK 8 — and only JDK 8** | You almost certainly do not need this. See [§1.4](#14-the-one-module-you-cannot-build-with-a-modern-jdk). |
| Run `ai-testgen/measure.sh` | + `python3`, `column` (util-linux) | Both are usually already present on Linux. |
| Run the AI generation step | + an OpenRouter API key | [§8](#8-the-ai-test-generation-experiment-g6) |

**Verified on the development machine, 2026-08-02:** Docker Compose 5.3.1, `psql` 18.4,
OpenJDK 26.0.1, Maven 3.9.11 (supplied by the wrapper), Chromium present at `/usr/bin/chromium`.
JDK **26** builds every Java-25 module without complaint — "25 or newer" is literal.

### 1.2 Docker

`docker compose` (with a space) and `docker-compose` (with a hyphen) are **different programs**.
The hyphenated one is Compose v1, written in Python, and is no longer maintained. Everything
here needs the plugin.
<https://docs.docker.com/compose/intro/history/>

```bash
docker compose version     # must succeed and print v2.x or v5.x
docker version             # a populated "Server:" block proves the daemon is reachable
```

**Install:**

- **Ubuntu / Debian** — the official apt repository. Remove the distro packages first
  (`docker.io`, `docker-compose`, `docker-compose-v2`, `docker-doc`, `docker-buildx`,
  `podman-docker`), then follow <https://docs.docker.com/engine/install/ubuntu/> (or
  `…/debian/`). The five packages you end up with are `docker-ce docker-ce-cli containerd.io
  docker-buildx-plugin docker-compose-plugin`.
- **Arch Linux** — Docker publishes **no** Engine install page for Arch
  (<https://docs.docker.com/engine/install/> lists CentOS, Debian, Fedora, Raspberry Pi OS,
  RHEL, Ubuntu and static binaries only; the Arch URL returns 404). The working route is the
  Arch-maintained package, which is *not* Docker documentation:
  ```bash
  sudo pacman -S docker docker-compose
  sudo systemctl enable --now docker.service
  docker compose version          # confirm this is the v2/v5 plugin despite the package name
  ```
- **macOS / Windows** — Docker Desktop is the only supported route
  (<https://docs.docker.com/desktop/>). On Windows use the WSL 2 backend and enable
  **Settings → Resources → WSL Integration** for your distro.

**Linux post-install — do this or every command needs `sudo`.** The daemon owns a Unix socket
that only root can use by default:

```bash
sudo groupadd docker
sudo usermod -aG docker $USER
newgrp docker            # or log out and back in
```

Docker states plainly: *"The `docker` group grants root-level privileges to the user."* That is
a real root-equivalence. On a shared machine, prefer `sudo docker`.
<https://docs.docker.com/engine/install/linux-postinstall/>

### 1.3 A JDK

Maven needs **either** `JAVA_HOME` set **or** `java` on your `PATH`
(<https://maven.apache.org/install.html>). You do **not** need to install Maven — this repo
ships the Maven Wrapper, which downloads Maven 3.9.11 itself. Always call `./mvnw`, never `mvn`.

```bash
java -version            # must be 25 or newer
```

| Platform | Install | Switch the default |
|---|---|---|
| Arch | `sudo pacman -S jdk-openjdk` (add `jdk8-openjdk` only if you really need §1.4) | `sudo archlinux-java set java-25-openjdk` — Arch does **not** set `JAVA_HOME`; it retargets `/usr/lib/jvm/default`, which is on your `PATH` |
| Ubuntu / Debian | Eclipse Temurin via <https://adoptium.net/installation/linux/> | `sudo update-alternatives --config java` |
| macOS | `brew install --cask temurin` or the Adoptium `.pkg` | `export JAVA_HOME=$(/usr/libexec/java_home -v 25)` |
| Windows | The Adoptium `.msi` | Set `JAVA_HOME` in system environment variables |

**Use a different JDK for exactly one command** — the trick worth knowing in a migration repo,
because it changes nothing globally:

```bash
JAVA_HOME=/usr/lib/jvm/java-8-openjdk ./mvnw verify -f legacy/pom.xml
```

### 1.4 The one module you cannot build with a modern JDK

`legacy/` is Java 8 (`<java.version>1.8</java.version>`, Spring Boot 1.5.22, packaged as a WAR),
and `./mvnw verify -f legacy/pom.xml` fails on a modern JDK — **but not for the reason everyone
assumes.** Measured on JDK 26, 2026-08-02:

```
compiler:3.1:compile   OK      ← javac still accepts source/target 8
surefire:2.18.1:test   OK      ← there are no tests, by design
war:2.6:war            FAILS   ← "due to an API incompatibility"
    ExceptionInInitializerError: Unable to make field private final
    java.util.Comparator java.util.TreeMap.comparator accessible:
    module java.base does not "opens java.util" to unnamed module
```

The blocker is `maven-war-plugin` **2.6** — pinned by Spring Boot 1.5's dependency management,
written before the module system, reflecting into `java.util` internals that JDK 16 sealed. No
compiler flag fixes it. This is a small, exact illustration of the project's thesis: what stops
an upgrade is usually the build plugins, not your source code.

**Everything else builds fine on JDK 25/26**, including corpus A of the AI experiment — which
compiles the *same* Java-8 sources, but at `--release 8` through a modern compiler plugin.

You normally never build `legacy/` yourself: `docker compose` builds it inside a
`maven:3.9-eclipse-temurin-8` image. Only reach for a local JDK 8 if you are debugging the
legacy build itself.

### 1.5 A browser, for the E2E suite only

The E2E suite drives **Chrome or Chromium**, always headless (`--headless=new`). There is no
non-headless switch.

The **driver** installs itself: Selenium 4.46 has Selenium Manager built in, which detects your
browser, downloads the matching `chromedriver`, and caches it under `~/.cache/selenium`. Nothing
in this repo installs a driver, and nothing should.
<https://www.selenium.dev/documentation/selenium_manager/>

The **browser** does not install itself in the version you have here. Install it:

```bash
sudo pacman -S chromium                       # Arch
sudo apt install chromium-browser             # Ubuntu/Debian
chromium --version    # or: google-chrome --version
```

Selenium Manager needs network access the first time it resolves a driver (results are cached
for one hour by default, and unused versions are pruned after 30 days).

---

## 2. First run

Five commands, from a fresh clone. This is the whole of it.

```bash
git clone https://github.com/Stoicera/migration-lab-java.git
cd migration-lab-java

docker compose -f legacy/docker-compose.yml up -d --wait
docker compose -f modern/docker-compose.yml up -d --wait
```

Open <http://localhost:8080> (the 2016 application) and <http://localhost:8090> (the migrated
one). They are the same application, nine years apart.

**`--wait` is not optional, and this is the single most common way to lose an hour here.**
Without it, `up -d` returns as soon as the containers are *running*, which is well before
PostgreSQL accepts connections and before Spring Boot has finished starting. The very next
`./mvnw verify` then fails against a stand that is not ready, with an error that points at the
tests rather than at the timing. `--wait` blocks until the healthchecks pass — both compose
files define them, and CI has always used `--wait`.
<https://docs.docker.com/reference/cli/docker/compose/up/>

The first run builds two application images and pulls `postgres:9.6`; expect several minutes.
Later runs start in about a second.

**Verify:**

```bash
docker compose -f legacy/docker-compose.yml ps
```

```
SERVICE   STATUS                   PORTS
app       Up 23 minutes (healthy)  0.0.0.0:8080->8080/tcp, [::]:8080->8080/tcp
db        Up 23 minutes (healthy)  127.0.0.1:5433->5432/tcp
```

Both services must say **`(healthy)`**, not merely `Up`.

---

## 3. The two stands

| | Legacy stand | Modern stand |
|---|---|---|
| Compose file | `legacy/docker-compose.yml` | `modern/docker-compose.yml` |
| Stack | Java 8 · Spring Boot 1.5.22 · AngularJS 1.8 · WAR | Java 25 · Spring Boot 4.1 · Angular 22 · executable JAR |
| Application | <http://localhost:8080> | <http://localhost:8090> |
| Admin page | <http://localhost:8080/admin> (JSP) | <http://localhost:8090/admin> (SPA route) |
| PostgreSQL | `127.0.0.1:5433` | `127.0.0.1:5434` |
| DB credentials | `werkstatt` / `werkstatt` / `werkstatt` | identical |
| Named volume | `werkstatt-db` | `modern-werkstatt-db` |
| Container names | `legacy-db-1`, `legacy-app-1` | `modern-db-1`, `modern-app-1` |

The two run side by side on purpose — that is the exhibit. Both use PostgreSQL **9.6** so that
the database is one variable fewer while everything above it changes.

**On the credentials:** they are dev-only and deliberately in plain sight in the compose files.
Nothing here is a secret. The only real credential in the project is the OpenRouter API key
([§8](#8-the-ai-test-generation-experiment-g6)).

**On network exposure — read this before running on an untrusted network.** The two **database**
ports are bound to `127.0.0.1` and are unreachable from outside the machine. The two
**application** ports (8080, 8090) are **not** — they are published on all interfaces, and
neither stand has any authentication, including the destructive `POST /admin/bereinigen`. Do not
run these stands on a public network.

Container names are derived from the compose file's parent directory. If you override the project
name (`-p` or `COMPOSE_PROJECT_NAME`), every `docker exec legacy-db-1 …` command in the docs
stops matching.

### Stopping

```bash
docker compose -f legacy/docker-compose.yml down       # stop, KEEP the database
docker compose -f legacy/docker-compose.yml down -v    # stop, DESTROY the database
```

`down` removes containers and networks but **not** named volumes. Only `-v` removes the data.
<https://docs.docker.com/reference/cli/docker/compose/down/>

---

## 4. The database

### 4.1 The seeding contract — the rule that costs people an afternoon

> **Changed on 2026-08-05 for the modern stand only.** Since stage 6 the modern stand has no
> `db/init/` mount at all: its schema and demo data are **Flyway migrations inside the
> application** (`modern/src/main/resources/db/migration` and `.../db/demo`, [ADR-0013](adr/0013-flyway-statt-handgestarteter-sql.md)),
> applied by Boot at startup. So on the modern stand, editing the SQL and restarting **does**
> take effect — for a *new* migration file. Editing `V1__baseline_schema.sql` after it has run
> does not, and worse, Flyway will refuse to start on a checksum mismatch. Changes go into a
> new `V…` file. Everything below still describes the **legacy** stand exactly.

Both stands mounted `db/init/` read-only at `/docker-entrypoint-initdb.d`; the legacy stand
still does. The official PostgreSQL
image runs those scripts **only when the data directory is empty** — i.e. only on the very first
start against a fresh volume. If a volume already holds a database, initialization is skipped
entirely, *by design*, to avoid overwriting data.
<https://docs.docker.com/guides/postgresql/advanced-configuration-and-initialization>

The consequence, stated bluntly because it is not intuitive:

> **Editing `db/init/*.sql` and restarting the container does nothing.**
> Not `restart`, not `down` + `up`, not `up --build`. Rebuilding the image does not help either —
> the data is in the volume, not the image.

To actually re-seed, destroy the volume:

```bash
docker compose -f legacy/docker-compose.yml down -v
docker compose -f legacy/docker-compose.yml up -d --wait
docker volume ls | grep werkstatt      # confirm the old volume is really gone
```

Scripts run in alphabetical order, which is why they are named `01-schema.sql` and `02-daten.sql`.

**`legacy/db/init/` and `modern/db/init/` are byte-identical copies**, and `legacy-ci` fails the
build if they ever diverge (`diff -q` on both files). Edit both, or edit one and let CI catch you.

### 4.2 Both test suites reset from the *legacy* seed file

Worth knowing before you edit seed data: `e2e/` and `characterization/` both read
`legacy/db/init/02-daten.sql` to reset the database — **even when they are targeting the modern
stand.** That is safe only because of the `diff -q` guard above, and that guard runs in CI, not
locally. Edit `modern/db/init/02-daten.sql` alone and you get a stand and a test-reset that
disagree, with no local signal at all.

### 4.3 Three different ways the data gets reset

| Mechanism | Scope | When |
|---|---|---|
| The suites' own reset | `TRUNCATE` of the five tables + replay of `02-daten.sql` | automatically, before test classes |
| Manual psql | whatever you type | when you are poking around |
| `down -v` + `up` | the whole volume, re-runs `db/init/` | after a **schema** change |

The suites' reset truncates exactly `kunde, fahrzeug, auftrag, auftrag_position, rechnung`. A new
table is never reset by it — that needs mechanism three.

### 4.4 Connecting with psql

You need only the **client**, not a server: `sudo pacman -S postgresql-libs` (Arch),
`sudo apt install postgresql-client` (Debian/Ubuntu), `brew install libpq` (macOS).

```bash
# legacy stand (port 5433) — non-interactive, no password prompt
PGPASSWORD=werkstatt psql -h 127.0.0.1 -p 5433 -U werkstatt -d werkstatt -w -c 'SELECT count(*) FROM kunde;'

# modern stand (port 5434)
PGPASSWORD=werkstatt psql -h 127.0.0.1 -p 5434 -U werkstatt -d werkstatt -w -c '\dt'

# URI form, if you prefer one argument
psql "postgresql://werkstatt:werkstatt@127.0.0.1:5433/werkstatt"
```

Real output of the first command against a seeded stand:

```
 count
-------
    10
(1 row)
```

Inside psql: `\dt` lists tables, `\d kunde` describes one, `\l` lists databases, `\q` quits.

A modern `psql` (18.x) against the 9.6 server works and prints a version-mismatch notice on
connect; that notice is expected, not a problem.

### 4.5 The two stands run different PostgreSQL majors — on purpose

Since 2026-08-05 the **modern** stand runs PostgreSQL **18** and the **legacy** stand stays on
**9.6** ([ADR-0012](adr/0012-postgresql-18-und-fixierte-collation.md)). That is not drift: 9.6
reached its final release 2021-11-11 and receives no security patches, so it belongs to the
exhibit and nowhere near a public endpoint. Keeping it on the legacy side is what makes the
equivalence gate worth running — it now proves behaviour across a ten-year database gap rather
than across two identical containers. The `postgres:9.6` image is still pullable but is no
longer a supported tag; pin it by digest if you need reproducibility years from now.
<https://www.postgresql.org/support/versioning/>

### 4.6 Upgrading the database image: the two traps, both measured

Neither of these announces itself. Both were found on 2026-08-05 while doing this upgrade.

**The image can report a collation it does not use.** Collation decides `ORDER BY` on text,
which decides the order of every list the application returns, which the golden masters pin.
Measured with the same probe against three images:

| image | `pg_database.datcollate` says | how it actually sorts |
|---|---|---|
| `postgres:9.6` (legacy) | `en_US.utf8` | `de Vries, Hubermann, Huber Transporte GmbH, Ohler, Öhler, van Dijk, Zach` |
| `postgres:18` | `en_US.utf8` | **the same** |
| `postgres:18-alpine` | `en_US.utf8` | `Huber Transporte GmbH, Hubermann, Ohler, Zach, de Vries, van Dijk, Öhler` |

The alpine variant accepts the locale name and ignores it (musl has no locale support), so it
answers `en_US.utf8` while sorting in C order. **A review step that reads the setting and
compares it passes.** Only sorting is evidence, which is why the modern stand pins
`LANG=en_US.utf8` plus `POSTGRES_INITDB_ARGS=--locale=en_US.utf8`, uses the Debian image, and
why `WerkstattServiceIntegrationTest` asserts an actual ordering.

**`PGDATA` and the declared `VOLUME` moved.** `postgres:9.6` uses `PGDATA=/var/lib/postgresql/data`;
`postgres:18` uses `/var/lib/postgresql/18/docker` and declares its volume at
`/var/lib/postgresql`. Bump the tag while keeping a `…:/var/lib/postgresql/data` mount and the
container starts cleanly, creates an **empty cluster in an anonymous volume**, and persists
nothing — and because Flyway re-migrates every start, the stand still looks healthy. Silent
data loss behind a green health check. The compose volume was therefore renamed to
`modern-werkstatt-db-pg18`, which also makes the first start clean instead of the cryptic
`database files are incompatible with server`.

The old 9.6 volume is still on disk and is not removed automatically:

```bash
docker volume ls | grep werkstatt          # modern-werkstatt-db is the retired 9.6 one
docker volume rm modern_modern-werkstatt-db   # only when you are sure you want the disk back
```

---

## 5. Running the test suites

**Every suite except the module tests needs a running stand.** Start it with `--wait` first.

| Suite | What it proves | Stand needed |
|---|---|---|
| `characterization` | The HTTP + DB behaviour still matches the frozen 2016 golden masters | the one you point it at |
| `e2e` | The same Selenium scenarios pass through both user interfaces | the one you point it at |
| `modern` module tests | Architecture rules and real SQL, without any stand | none — but Docker must be running |
| `ai-testgen` harness | The experiment harness itself | none |

### 5.1 Characterization — the equivalence gate

```bash
# against the legacy stand (all defaults)
./mvnw verify -f characterization/pom.xml

# against the modern stand — all three flags are required
./mvnw verify -f characterization/pom.xml \
  -DbaseUrl=http://localhost:8090 \
  -DdbUrl=jdbc:postgresql://localhost:5434/werkstatt \
  -Dstand=modern
```

> **Trap, and it fails silently.** This suite has **no `-Dtarget` flag** — that one belongs to
> `e2e` only. `./mvnw verify -f characterization/pom.xml -Dtarget=modern` runs happily, goes
> **green**, and has tested the *legacy* stand. You would then believe you had proven equivalence
> when you had proven nothing. There are three separate flags here and you need all three.
> (Since 2026-08-02 the suite fails fast on a stray `-Dtarget` instead of ignoring it.)

`-Dstand` selects which side of a sanctioned divergence (ADR-0004) is expected. It changes no URL.

### 5.2 E2E — the Selenium safety net

```bash
./mvnw verify -f e2e/pom.xml -Dtarget=legacy
./mvnw verify -f e2e/pom.xml -Dtarget=modern
```

Here `-Dtarget` is the *only* switch you need: it selects the base URL, the database URL **and**
the selector map in one go. Anything other than `legacy` or `modern` fails immediately.

Screenshots of failures land in `e2e/target/screenshots/`.

### 5.3 The modern module build

```bash
./mvnw verify -f modern/pom.xml
```

**Docker must be running.** This is a hard precondition nobody wrote down before: the suite
includes a Testcontainers test that starts a real `postgres:9.6` from the same `db/init` scripts
the stand uses. No Docker daemon, no build.

This one command also: installs its own Node (v24.18.1 — you do **not** need Node installed),
runs `npm ci` and the Angular production build, then at `verify` runs `ng lint`,
`prettier --check`, Spotless (google-java-format) and the JaCoCo coverage ratchet. It is the
slowest command in the repo and the one CI cares most about.

### 5.4 What `verify` adds over `test` here

Not what the textbook says. `characterization/` and `e2e/` have **no Failsafe plugin** — their
tests are ordinary Surefire tests that happen to talk to `http://localhost:8080`. So:

```
test    → Surefire runs the tests (the stand must already be up)
verify  → additionally runs the format gate (Spotless), and in modern/ the lint + coverage gates
```

Use `verify`. `test` skips the gates that CI enforces, so a green `test` tells you less than you
think.

### 5.5 Useful Maven flags

| Flag | Effect |
|---|---|
| `-f <pom>` | which module — always required, there is no root pom |
| `-q` | quiet: warnings and errors only |
| `-B` | batch mode, no ANSI colour — what CI uses |
| `-o` | offline; fails rather than downloading |
| `-Dtest=ClassName` | run one test class |
| `-DskipTests` | compile tests but do not run them |

Both suites set `failIfNoTests=true`, so a run that discovers zero tests fails loudly instead of
passing. That is deliberate.

---

## 6. Reproducing CI locally

CI is the arbiter, so it is worth being able to run exactly what it runs. Seven checks are
required on `master`.

| Check | Local equivalent |
|---|---|
| `legacy-build` | `diff -q legacy/db/init/01-schema.sql modern/db/init/01-schema.sql` (and `02-daten.sql`), then `JAVA_HOME=<jdk8> ./mvnw -B verify -f legacy/pom.xml`, then the legacy stand + characterization |
| `modern-build` | `./mvnw -B verify -f modern/pom.xml`, then the modern stand + characterization with the three flags |
| `e2e (legacy)` | `./mvnw -B verify -f e2e/pom.xml -Dtarget=legacy` |
| `e2e (modern)` | `./mvnw -B verify -f e2e/pom.xml -Dtarget=modern` |
| `harness` | `./mvnw -B verify -f ai-testgen/harness/pom.xml` |
| `testbed-validation (legacy)` | `./mvnw -B -Pvalidation -f ai-testgen/testbed/legacy/pom.xml test org.pitest:pitest-maven:mutationCoverage` |
| `testbed-validation (modern)` | same with `modern` |

CI additionally runs the E2E suite **nightly at 02:00 UTC**, which is where flakiness would show
up before a human notices it.

---

## 7. Rebuilding after a code change

```bash
docker compose -f modern/docker-compose.yml up -d --build --wait
```

`--build` rebuilds the image before starting. If a change genuinely seems to be ignored, escalate:

```bash
docker compose -f modern/docker-compose.yml build --no-cache
docker compose -f modern/docker-compose.yml up -d --force-recreate --wait
```

Note that the **image build runs `mvn package -DskipTests`** — deliberately. The lint, format,
coverage and architecture gates live in `verify` and run in CI and locally, not in the image
build. A green `docker compose build` therefore proves nothing about quality gates.

---

## 8. The AI test-generation experiment (G6)

Everything except the generation calls works without a key:

```bash
# what would run and what it has cost so far
./mvnw -q -f ai-testgen/harness/pom.xml compile exec:java -Dexec.args="plan"

# render the prompts without calling anything
./mvnw -q -f ai-testgen/harness/pom.xml compile exec:java \
  -Dexec.args="render --corpus A --model anthropic/claude-sonnet-5 --out /tmp/prompts"
```

**The one manual credential in this repository.** Create a key at
<https://openrouter.ai/keys>, then:

```bash
cp .env.example .env        # .env is git-ignored
$EDITOR .env                # set OPENROUTER_API_KEY=...
```

The harness reads `.env` by itself, so the key never enters your shell history. An
`OPENROUTER_API_KEY` in the environment wins over the file.

```bash
./mvnw -q -f ai-testgen/harness/pom.xml compile exec:java \
  -Dexec.args="generate --corpus A --model anthropic/claude-sonnet-5"

./ai-testgen/measure.sh 2026-07-31 anthropic_claude-sonnet-5 A as-generated
```

Guard rails, so a mistake cannot get expensive: the harness refuses any model outside the frozen
price table, and hard-aborts at **€20** total spend summed over every recorded call. The executed
run of 24 calls cost **€0.65**.

`measure.sh` needs `python3` and `column`. It does **not** validate its `phase` argument: a typo
produces "skip … no `<typo>`/ directory" for every unit and a header-only CSV that looks like a
completed measurement. Check the row count.

---

## 9. Troubleshooting

Symptoms are what you actually see; causes are what is actually wrong.

| Symptom | Cause | Fix |
|---|---|---|
| `permission denied … /var/run/docker.sock` | Your user is not in the `docker` group | `sudo usermod -aG docker $USER`, then `newgrp docker` or re-login |
| `Cannot connect to the Docker daemon` | Daemon not running, or `DOCKER_HOST` points elsewhere | `sudo systemctl start docker`; `env \| grep DOCKER_HOST` |
| `port is already allocated` / `address already in use` | Something else holds 8080/8090/5433/5434 — often a stale container | `docker ps -a`, `docker compose ps -a`; stop it or free the port |
| Tests fail immediately with connection refused or `DB reset to seed state failed` | The stand was not healthy yet | You omitted `--wait`. Re-run `up -d --wait` |
| Your `db/init/*.sql` edit has no effect | Init scripts run only against an **empty** data directory | `down -v` then `up -d --wait` — [§4.1](#41-the-seeding-contract--the-rule-that-costs-people-an-afternoon) |
| Characterization is green but you tested the wrong stand | You used `-Dtarget` — which this suite does not have | Use `-DbaseUrl` + `-DdbUrl` + `-Dstand` together |
| `release version 8 not supported` / `invalid target release` | You are building `legacy/` on a modern JDK | Build it via Docker, or prefix `JAVA_HOME=<jdk8>` |
| `modern` build fails in `WerkstattServiceIntegrationTest` | Docker is not running — Testcontainers needs it | Start Docker |
| `npm ci` fails: lockfile out of sync | `package.json` was edited without regenerating the lockfile | `cd modern/frontend && npm install`, commit the lockfile |
| `prettier --check` or `ng lint` fails the build | Formatting gate | `cd modern/frontend && npm run format` |
| Spotless fails the build | Java formatting gate | `./mvnw spotless:apply -f <module>/pom.xml` |
| `session not created: This version of ChromeDriver only supports Chrome version N` | Cached driver no longer matches an updated browser | `rm -rf ~/.cache/selenium` and re-run; Selenium Manager re-resolves |
| E2E fails with no browser found | Chrome/Chromium is not installed | [§1.5](#15-a-browser-for-the-e2e-suite-only) |
| `No tests were executed` | The suites treat this as a failure on purpose | Check your `-Dtest` filter or module path |
| Disk filling up | Docker never reclaims automatically | `docker system df`, then `docker system prune` (add `--volumes` **only** if you mean it) |

---

## 10. Production deployment — not yet

**There is no production deployment, and this section deliberately contains no instructions for
one.** `ENGINEERING_STANDARDS.md` §7 requires deployment documentation covering a Hetzner VPS
with Dokploy, `pg_dump` backup cron, and TLS via Traefik/Let's Encrypt. None of that has been
built. Writing plausible steps for infrastructure that does not exist is precisely the kind of
documentation this project exists to argue against — you would follow it, it would not work, and
you would trust the rest of the repo less.

What is true today, so the gap is measurable rather than vague:

| Required by §7 | Status |
|---|---|
| Local full environment via Compose | **done** — [§2](#2-first-run); the observability profile arrived 2026-08-05 ([§11](#11-observability-locally)), Keycloak and Mailpit are still absent and are ledgered, not forgotten |
| Hetzner VPS + Dokploy deployment steps | **not started** |
| Backups (`pg_dump` cron) | **not started** |
| TLS (Traefik / Let's Encrypt via Dokploy) | **not started** — the Traefik configuration that will carry it exists and is tested locally ([§12](#12-the-reverse-proxy-edge)); terminating TLS needs a host and a domain |
| 12-factor config via environment | **partly** — both stands take `SPRING_DATASOURCE_*` from the environment, and stage 6 added log format, tracing, Flyway locations and every edge setting as environment variables; `.env.example` reserves `DOKPLOY_URL`, `DOKPLOY_TOKEN`, `GHCR_TOKEN` |
| Automatic migrations (Flyway) | **done** 2026-08-05 — [ADR-0013](adr/0013-flyway-statt-handgestarteter-sql.md), wart B18 closed |

What stage 6 *did* deliver is operations readiness, and it is all runnable on a laptop today:
health checks the platform can actually use ([§11](#11-observability-locally)), the reverse
proxy with authentication, security headers and rate limiting that a public demo needs
([§12](#12-the-reverse-proxy-edge)), and a load baseline ([§13](#13-the-load-scenario)). What is
missing is the host — and a host is exactly the thing that cannot be written down before it
exists.

Two things must be settled **before** anything is exposed publicly, and both are already in the
ledger rather than being discovered later:

1. **Neither stand has authentication** — including the destructive `POST /admin/bereinigen`
   inherited from the JSP admin page. A public demo must protect `/admin` at minimum.
2. **PostgreSQL 9.6 is end-of-life** ([§4.5](#45-postgresql-96-is-end-of-life)). An unpatched
   9.6 reachable from a network is not acceptable; the upgrade needs its own ADR because
   collation changes can move the golden masters.

This section gets replaced by real, executed steps when G7 lands — not before.

---

---

## 11. Observability, locally

Nothing here is on by default. A stand you start the normal way exports no traces and needs no
extra container; you opt in.

### 11.1 Health, and why the probe changed

The modern stand exposes exactly two Actuator endpoints, `health` and `info`. Everything else —
`env`, `beans`, `mappings`, `metrics`, `loggers`, `heapdump` — answers **404**, verified rather
than assumed. An open `/actuator` is a data leak and a free map of the application.

```bash
curl -s localhost:8090/actuator/health              # {"groups":["liveness","readiness"],"status":"UP"}
curl -s localhost:8090/actuator/health/readiness    # {"status":"UP"}
curl -s localhost:8090/actuator/health/liveness     # {"status":"UP"}
```

Two things about this were wrong when first written down, and the fixes are the useful part:

**Spring Boot's default readiness group does not include your database.** Measured: with
`modern-db-1` stopped, `/actuator/health/readiness` answered `200 {"status":"UP"}` while
`/actuator/health` answered `503`. A readiness probe that reports ready while the application
cannot answer a single business request is worse than none — it manufactures confidence. Hence
`management.endpoint.health.group.readiness.include=readinessState,db`.

**Liveness deliberately does *not* include the database.** If it did, a database outage would
restart a perfectly healthy application in a loop and turn a fault into an outage. That is the
classic mistake when translating health into probes.

You can watch both:

```bash
docker stop modern-db-1
curl -s -o /dev/null -w '%{http_code}\n' localhost:8090/actuator/health/readiness   # 503
curl -s -o /dev/null -w '%{http_code}\n' localhost:8090/actuator/health/liveness    # 200
docker inspect --format '{{.State.Health.Status}}' modern-app-1                     # unhealthy after ~25s
docker start modern-db-1                                                            # healthy again ~6s later
```

The 25 seconds are `interval: 5s` × `retries: 3` plus scheduling. Until stage 6 the check had
`retries: 24`, because the same number also had to absorb a slow start — measured, that meant a
503 application counted as healthy for two minutes. Startup is now covered by `start_period: 90s`,
during which a failure does not count against `retries`, so the generous startup budget costs
nothing in reaction time.

The check itself is `bash` with `/dev/tcp`, not `curl`: the `eclipse-temurin:25-jre` runtime image
ships neither `curl` nor `wget`, and `CMD-SHELL` would run `dash`, which has no `/dev/tcp`.
(The *legacy* image, `eclipse-temurin:8-jre`, does have both — the comment in
`legacy/docker-compose.yml` that claimed otherwise was wrong and has been corrected.)

### 11.2 Structured logs

In the container, logs are ECS JSON; a bare `java -jar` keeps the human-readable format. The
format is environment configuration, not a property of the artefact.

```bash
docker compose -f modern/docker-compose.yml logs app | tail -1
```

Application log lines carry `trace.id` and `span.id`. Micrometer writes them into the MDC as
`traceId`/`spanId`, which are *not* the ECS field names — `logging.structured.json.rename.*` maps
them, because a format called ECS should be ECS. Framework lines emitted outside a request carry
no trace, which is correct rather than missing.

### 11.3 Traces

```bash
WERKSTATT_TRACING_ENABLED=true \
  docker compose -f modern/docker-compose.yml --profile observability up -d --wait
```

That starts `grafana/otel-lgtm` (Grafana, Prometheus, Tempo, Loki in one container). Grafana:
<http://localhost:3000>, bound to loopback. Generate traffic, then search Tempo in Grafana — you
will find traces named `http get /api/kunden` under the service `werkstatt-crm-modern`, and the
`trace.id` from any log line pastes straight into the search.

Both switches belong together. Tracing is off by default because with no collector configured the
OTLP exporter retries against `localhost:4318` and logs a connection failure on every batch —
noise that teaches people to ignore logs.

---

## 12. The reverse-proxy edge

The modern stand has **no authentication of its own**. `POST /admin/bereinigen` permanently
deletes cancelled orders and, on the bare stand, anyone who can reach the port can call it. Stage 6
put a Traefik reverse proxy in front instead of changing the application
([ADR-0014](adr/0014-authentifizierung-am-edge.md)) — the same component Dokploy runs, so this is a
rehearsal rather than a stand-in.

```bash
MODERN_ADMIN_AUTH="admin:$(openssl passwd -apr1 'ein-passwort')" \
  docker compose -f modern/docker-compose.yml -f modern/docker-compose.edge.yml up -d --wait
```

Edge on <http://localhost:8091>. It is an **overlay file, not a profile**, because it requires
`MODERN_ADMIN_AUTH` and a required variable in the base file would break the plain quickstart for
everyone.

Verify it — and prefer this over trusting the configuration:

```bash
EDGE_USER=admin EDGE_PASSWORD=ein-passwort modern/edge/verify-edge.sh
```

It asserts that `/admin`, `/api/admin` and `/actuator` are 401 without credentials and 200 with
them, that a wrong password stays out, that the public application is untouched, that the security
headers are present, and that the rate limiter actually fires (measured: 200 concurrent requests
through the edge → 103–173 × 429 over five runs; the same burst straight at the application → 0).

**Locally, port 8090 stays published** so the safety net keeps its direct path to the application.
**On a real host it must not be** — otherwise the lock has a door beside it. That is the single
most important line in this section.

Two settings ship deliberately switched off, because switching them on locally does damage:

- **HSTS** (`MODERN_HSTS_SECONDS`, default `0`). Sent over plain HTTP it teaches the browser to
  refuse `http://localhost` for a year. The TLS-terminating host sets it.
- **A CSP without `'unsafe-inline'` for styles.** The policy is strict everywhere else;
  `style-src` needs it because Angular injects component styles at runtime. Which brings up the
  finding worth carrying away from this section: with the strict version **32 of the suite's 34
  scenarios ran green through the edge while the browser was blocking those styles** (the two
  `AdminTest` scenarios cannot run through Basic auth at all — Selenium cannot answer the
  browser's native credential dialog — and keep running against the application port, where they
  remain part of the 34/34 gate). Selenium asserts behaviour and text,
  never appearance. A green suite is evidence about what it asserts and about nothing else — the
  visual check stayed manual, and that is written into `MANUAL_TASKS.md` rather than implied.

---

## 13. The load scenario

One scenario, as `ENGINEERING_STANDARDS.md` §3 asks for. It walks the read path a user walks.

```bash
docker run --rm -i --network host grafana/k6:latest run - < load/k6/lesepfad.js
BASE_URL=http://localhost:8080 \
  docker run --rm -i --network host -e BASE_URL grafana/k6:latest run - < load/k6/lesepfad.js
```

Measured 2026-08-05, 5 virtual users over 45 s, both stands on the same machine:

| | modern (Boot 4.1 / Java 25 / PG 18) | legacy (Boot 1.5 / Java 8 / PG 9.6) |
|---|---|---|
| p(95) request duration | 1.60 ms | 1.56 ms |
| average | 0.87 ms | 0.77 ms |
| max | 4.13 ms | 5.68 ms |
| requests / failures | 1146 / 0 | 1146 / 0 |

**The modernisation is not measurably faster.** Ten years of framework and JDK versions bought
supportability, security and a labour market — not speed on this workload. If someone is selling a
migration on performance, they should measure first.

Read the numbers with their caveat attached: load generator, application and database share one
laptop, and the dataset is a ten-customer demo seed. This is a baseline for comparison, not a
statement about capacity. The thresholds in the script are deliberately loose for the same reason —
they exist to catch an order-of-magnitude regression, and a threshold tuned to today's machine
would go red on the next one and then get switched off.

---

## Deutsche Kurzfassung

Dieses Dokument ist die **Betriebsanleitung** des Repos und war bis 2026-08-02 nicht vorhanden,
obwohl `ENGINEERING_STANDARDS.md` §7 es verlangt.

Der schnellste Weg zu zwei laufenden Ständen:

```bash
docker compose -f legacy/docker-compose.yml up -d --wait
docker compose -f modern/docker-compose.yml up -d --wait
```

Die drei Dinge, die erfahrungsgemäß Zeit kosten und deshalb oben ausführlich stehen:

1. **`--wait` weglassen.** Ohne `--wait` läuft der Container zwar, die Datenbank nimmt aber noch
   keine Verbindungen an — der nächste Testlauf scheitert an einem Timing-Problem, das wie ein
   Testfehler aussieht ([§2](#2-first-run)).
2. **`db/init/*.sql` ändern und neu starten.** Das tut nichts: Die Init-Skripte laufen nur gegen
   ein leeres Datenverzeichnis. Nur `down -v` setzt wirklich zurück
   ([§4.1](#41-the-seeding-contract--the-rule-that-costs-people-an-afternoon)).
3. **`-Dtarget=modern` bei den Charakterisierungstests.** Diesen Schalter gibt es dort nicht —
   der Lauf wird grün und hat den *Legacy*-Stand geprüft. Es braucht `-DbaseUrl`, `-DdbUrl` und
   `-Dstand` zusammen ([§5.1](#51-characterization--the-equivalence-gate)).

**Produktivdeployment gibt es noch nicht** (Etappe G7). [§10](#10-production-deployment--not-yet)
listet die offenen Punkte, statt Schritte für eine Infrastruktur zu erfinden, die es nicht gibt.
