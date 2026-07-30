# Deviations & deferred work

`ENGINEERING_STANDARDS.md` is binding ("Jedes Repo wird daran gemessen"). A repo
that silently ignores binding rules has no standards — so every deviation is
recorded here, dated, with a disposition. Same for remediations the wart
catalogue promises but no stage owns. Hostile review (worklog session 7) forced
this file into existence; that is working as intended.

Status values: **deferred(stage)** = lands in a named stage · **deferred(post-v1.0)**
= explicitly outside this project's scope, would need owner re-scoping ·
**waived** = will not be done in this repo, reason given.

## Engineering-Standards items

| Standard (DoD) | Status | Disposition |
|---|---|---|
| Coverage-Gate ≥ 80 % auf Neucode | deferred(G6) | `modern/` has no unit tests until the AI test-gen experiment produces them; a coverage gate over zero tests would be theatre. The gate is armed in G6 together with the first real unit-test suite. |
| Security-Scan in CI | partially met, rest deferred(G7) | Since 2026-07-30: Dependabot (modern/e2e/characterization + actions; `legacy/` **excluded on purpose** — its EOL dependencies are the exhibit). Full OWASP dependency-check/image scanning lands with the ops stage G7. |
| Lint/Format-Check in CI | **met** for `modern/`+test modules (2026-07-31, G5) · waived for `legacy/` | Spotless (google-java-format) bound to `verify` in modern/e2e/characterization; `ng lint` (angular-eslint) + `prettier --check` bound to modern `verify` — the existing CI verify steps enforce all gates. One-time mechanical reformat in the same commit series (stage 5). `legacy/` stays waived: a formatter would destroy the 2016 exhibit (hard rule: legacy stays legacy). |
| Testcontainers für Integrationstests | deferred(G6) | SPEC §6 described the target state as current state — corrected. The characterization suite against real compose stands is the integration gate until G6. |
| `.env.example` gepflegt | **met** (2026-07-30) | Was missing for four stages despite being a hard rule — created; documents that the dev stands intentionally need no secrets. |
| OTel/Observability | deferred(G7) | As planned in SPEC §6 / MILESTONES G7 — no drift, listed for completeness. |

## Wart-catalogue promises without an owner (LEGACY_NOTES)

| Item | Status | Disposition |
|---|---|---|
| B8 money as `double` → BigDecimal | deferred(post-v1.0) | No remaining stage owns it (G5 = UI, G6 = tests, G7 = ops). Converting money math without a business driver mid-project would be exactly the purpose-free refactoring the playbook warns against. The wart is pinned by characterization tests (rounding cases) so it cannot drift silently. Owner decision needed if v1.x wants it. |
| B18 hand-run SQL → Flyway | deferred(G7) | Schema management is an ops concern; G7 (deployment) is the natural owner. Until then both stands init from the same reviewed SQL files. |
| P2 PostgreSQL 9.6 (EOL) | deferred(G7) | The modern stand keeps 9.6 through G5/G6 deliberately: identical DB = one variable fewer while UI and tests change. The PG upgrade (with collation/ordering impact on goldens — see `characterization/README.md`) is a G7 decision with its own ADR. |

## Non-goals restated (no deviation, just clarity)

- No DB **engine** migration (PRD §Scope) — the PG *version* upgrade above is a
  different thing.
- `legacy/` never gets tests, lint, or dependency updates — it is the exhibit.
