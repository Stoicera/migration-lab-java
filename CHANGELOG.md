# Changelog

All notable changes to this repository are recorded here. The format follows
[Keep a Changelog 1.1.0](https://keepachangelog.com/en/1.1.0/); versions follow
[Semantic Versioning](https://semver.org/spec/v2.0.0.html). Dates are merge dates on `master`
(UTC, from `git log` and the pull requests). Stage tags (`stage-0-legacy` … `stage-6-cloud-ops`)
are checkout-and-run states described in [`stages.md`](stages.md); only `v1.0.0` is a release.

## [Unreleased]

Everything merged since v1.0.0. No new release or tag has been cut since 2026-08-14.

### Added

- `CHANGELOG.md`, a social preview image rendered by `scripts/social-preview.mjs`
  (`docs/assets/social-preview.png`, 1280×640) and launch drafts in `docs/launch/`; README opens
  with the live before/after screenshot pair, badges and a "What you can verify in 5 minutes"
  block (2026-10-15).
- `docs/demo.md` (ten-minute walkthrough of both stands, in German) and `docs/enterprise.md`
  (how a modernisation runs in six stages, in German); `/.well-known/security.txt` (RFC 9116) on
  the modern stand with a classpath test and an assertion in `deploy/verify-live.sh`; README
  section "For companies / Für Unternehmen" (#89, 2026-10-13).
- The playbook as `migration-playbook.pdf`, attached to release v1.0.0 (2026-10-13; a release
  asset, not a commit).
- Repository baseline: `AGENTS.md`, `CLAUDE.md`, Copilot instructions and the organisation's
  baseline workflow `ci.yml` (#72, #76, 2026-09-30); non-goals and the rollback command in
  `AGENTS.md` (#87, 2026-10-08).

### Changed

- Demo instance taken out of search indexes: `robots.txt` with `Disallow: /` and
  `<meta name="robots" content="noindex, nofollow">` on both stands, neutral page title on the
  modern stand, and a `verify-live.sh` step that checks it. No application behaviour changed
  (#95, 2026-10-15).
- Both deployed stands restart after a host reboot (`restart: unless-stopped`) and have hard
  memory limits: application 1536 MB, database 512 MB per stand (#70, 2026-09-23).
- Deployment docs: from Dokploy 0.30 the htpasswd values take single dollar signs; nightly
  database dumps are copied to a second server (#71, 2026-09-24).
- Every document brought in line with the deployed state: 38 statements that had become false
  after the go-live were dated and pointed forward rather than deleted (#43, 2026-08-17).
- The README screenshot pair replaced with genuine captures from the live instances, address bar
  in the frame (#45, 2026-08-17).

### Fixed

- Modern stand: tables rendered without Bootstrap styling (no padding, borders or striping)
  because the Angular templates had no `<tbody>`; live from 2026-08-14 until the fix. Found by
  comparing screenshots, not by a test suite (#44, 2026-08-17).

### Removed

- Server identifiers (host names, private addresses, panel URL, host paths) from the working
  tree; they remain in the git history (#90, 2026-10-13).
- `docs/VERMARKTUNG.md` from the public repository (#91, 2026-10-13).

### Dependencies

- Spring Boot 4.1.0 → 4.1.1 in `modern/` (#57, 2026-10-13).
- Selenium 4.46.0 → 4.51.0 in `e2e/` (#86, 2026-10-13).
- PostgreSQL JDBC 42.7.12 → 42.7.14 in `characterization/` and `e2e/` (#10, #11, 2026-10-13).
- jackson-databind 2.22.1 → 2.22.3 in `characterization/` and `ai-testgen/harness` (#81, #84,
  2026-10-13).
- ArchUnit 1.4.2 → 1.5.0 → 1.5.1 (#32, 2026-08-17; #77, 2026-10-13); Spotless Maven plugin
  2.44.5 → 3.9.0 in `modern/` and `e2e/` (#17, #18, 2026-08-17); frontend-maven-plugin
  1.15.1 → 2.0.2 (#31, 2026-08-17); `actions/setup-java` 5.6.0 → 5.7.0 (#29, 2026-08-17).
- Frontend tooling in `modern/frontend`: eslint, typescript-eslint, hono, fast-uri, ip-address,
  brace-expansion (#54, #56, #59, #64, #74, #75, 2026-10-13).

### Known issues

- The Trivy scan of the modern image reports 11 findings on 2026-10-15 (`modern-ci` run
  38686720064 on `af42a2c`): 8 in the application JAR, 3 of them CRITICAL in
  `tomcat-embed-core`, and 3 HIGH in the base image. Spring Boot 4.1.1 did not close the Tomcat
  findings. The scan step does not fail the build by design, so CI stays green; see
  [`docs/DEVIATIONS.md`](docs/DEVIATIONS.md) and [`docs/MANUAL_TASKS.md`](docs/MANUAL_TASKS.md).
- No restore rehearsal from the off-server backup copy has been run for this project yet.

## [1.0.0] - 2026-08-14

The migration, complete and deployed. Release notes:
[v1.0.0](https://github.com/Stoicera/migration-lab-java/releases/tag/v1.0.0).

### Added

- **Stage 0 — the legacy stand** (`stage-0-legacy`, 2026-07-30): WerkstattCRM as found — Java 8,
  Spring Boot 1.5.22, AngularJS 1.8.2, a JSP admin page, PostgreSQL 9.6, no tests; synthetic but
  pattern-faithful, its weaknesses catalogued (ADR-0001; #1, #2).
- **Stage 1 — the safety net** (`stage-1-safety-net`, 2026-07-30): Selenium 4 suite (34
  scenarios) and characterization tests (47 golden masters over HTTP responses and database
  state), CI gates armed (#3).
- **Stage 2** (`stage-2-jdk-build`, 2026-07-30): `modern/` bootstrapped as a faithful copy
  running side by side, logging unified, JDK deliberately not raised yet (#4).
- **Stage 3** (`stage-3-boot-2.7`, 2026-07-30): Spring Boot 1.5.22 → 2.7.18 and Java 17, three
  real breaks, one of them caught only by the golden master (#5).
- **Stage 4** (`stage-4-boot-4x`, 2026-07-30): Spring Boot 2.7 → 3.5 → 4.1 and Java 25;
  OpenRewrite used and evaluated (ADR-0002); the SQL-injection-shaped search closed as a
  registered divergence (ADR-0004; #6).
- **Stage 5** (`stage-5-angular`, 2026-07-31): AngularJS → Angular 22 via Strangler Fig on a URL
  seam, no ngUpgrade (ADR-0009); the same E2E scenarios green on both UIs; JSP admin page
  absorbed; WAR → JAR (#7, #14).
- **AI test-generation experiment (G6)**: protocol frozen before the first call (tag
  `ai-testgen-protocol-v1`, 2026-07-31; #22), Phase A measured (#23, 2026-07-31), Phase B
  measured and Phase A identified as survivorship bias (#24, 2026-08-02); 88 repaired test
  methods adopted into `modern/` (ADR-0011). Report: [`ai-testgen/REPORT.md`](ai-testgen/REPORT.md).
- **Stage 6 — operations and deployment** (`stage-6-cloud-ops`, 2026-08-14): PostgreSQL 18,
  Flyway, Actuator health probes, OpenTelemetry, structured logs, optional Traefik edge, k6 load
  baseline (#25, 2026-08-05); GHCR images and one Dokploy compose service per stand, legacy stand
  entirely behind Basic auth (ADR-0016; #38); both stands live with TLS, nightly `pg_dump` and an
  executed restore rehearsal; playbook chapter 8 (#42).
- German migration playbook, chapters 01–08 ([`playbook/`](playbook/)).

### Fixed

- Two rounds of commissioned hostile review, all findings remediated in the open (#8, 23
  findings, 2026-07-30; #16, stage 5).
- Worklog numbers corrected from estimate to measurement (#13, #15, 2026-07-30).

### Security

- PostgreSQL JDBC driver pinned to 42.7.12 in the application (CVE-2026-54291, found by the
  first Trivy run, 2026-08-05) and in the suite poms (#40, 2026-08-14).
- Private vulnerability reporting enabled and referenced in `SECURITY.md` (#37, 2026-08-14).

[Unreleased]: https://github.com/Stoicera/migration-lab-java/compare/v1.0.0...HEAD
[1.0.0]: https://github.com/Stoicera/migration-lab-java/releases/tag/v1.0.0
