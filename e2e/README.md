# e2e/ — Selenium safety net

Selenium 4 + JUnit 5 + Java 25. **The same scenarios run against both UIs** —
page objects address elements by intent key only; the mapping to concrete CSS
selectors lives in `src/test/resources/selectors/<target>.properties`.
Porting the suite to the stage-5 Angular UI means writing `modern.properties`,
not new tests.

## Scenarios

| Class | Flow |
|---|---|
| `KundenCrudTest` | create → edit → search (hits the legacy search path) → delete |
| `AuftragLebenszyklusTest` | accept → work + position → finish → pick up, list states |
| `RechnungTest` | invoice from finished seed order: exact number, 20% USt math, mark paid |
| `BerichtTest` | monthly report numbers of the frozen seed months + top customer |

## Zero-flake rules (binding)

1. **Explicit waits only** (`support/Waits`, 10s/200ms poll). Implicit waits are
   set to ZERO in the driver — mixing wait styles is the classic flakiness source.
2. **Deterministic data**: every scenario class resets the DB to the committed
   seed in `@BeforeAll`. Assertions may rely on exact seed values. Tests write
   only into the current month; report assertions use frozen past months.
3. **Settled views**: page `open()` starts from a full page load, then performs a
   real route change, then waits for the list to finish loading before any
   interaction (two documented AngularJS races found during stabilisation —
   see playbook ch. 1).
4. A red test is analysed with evidence (screenshot in `target/screenshots/`),
   fixed deterministically, and the finding is logged. Never retried-until-green,
   never `@Disabled`.

## Run

```bash
docker compose -f legacy/docker-compose.yml up -d
./mvnw verify -f e2e/pom.xml -Dtarget=legacy     # later: -Dtarget=modern
```

Requires Chrome/Chromium (Selenium Manager resolves the driver; CI uses the
preinstalled Chrome on ubuntu runners).
