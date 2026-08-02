# modern/frontend — the Angular 22 UI

The stage-5 replacement for the AngularJS 1.8 UI, built **into** the Spring Boot jar.
Generated with Angular CLI 22.1.2; this file replaces the CLI boilerplate, which documented
commands this project does not have.

## You usually do not build this directly

`./mvnw verify -f modern/pom.xml` builds the frontend as part of the backend:
`frontend-maven-plugin` installs its own pinned **Node v24.18.1**, runs `npm ci` against the
committed lockfile, and runs the production build. **No system-wide Node is required** — the
same idea as the Dockerized JDK 8 build of `legacy/`.

## The dev loop, when you want one

```bash
cd modern/frontend
npm ci        # first time, or after a lockfile change — NOT npm install
npm start     # ng serve on http://localhost:4200
```

This one does need a locally installed Node. `npm ci` requires `package-lock.json` to be in
sync with `package.json` and deletes `node_modules` first; if the two have drifted it fails
loudly rather than silently rewriting the lockfile — then use `npm install` and commit the
result.

**There is no backend proxy configured, on purpose.** `ng serve` gives you the UI with no API
behind it. The reference environment is the compose stand
(`docker compose -f modern/docker-compose.yml up -d --wait` → <http://localhost:8090>), because
that is what the E2E suite and the per-commit equivalence gate actually test.

## Gates that will fail your build

Both run at `verify` in the Maven build and in CI:

```bash
npm run lint            # ng lint (angular-eslint)
npm run format:check    # prettier --check
npm run format          # prettier --write — the fix
```

## Tests

**There are none in this directory, and `npm test` will not work** — no test runner, no
configuration and no spec files are installed, despite the `test` script the CLI left behind.

That is a deliberate position, not an oversight: this UI's correctness is covered from the
outside by the Selenium suite in [`../../e2e/`](../../e2e/), which runs the *same scenarios*
against the old AngularJS UI and this one. That equivalence is the exhibit; a component-level
suite would not demonstrate it. Frontend unit tests are a post-v1.0 candidate, and this
paragraph exists so the gap is stated rather than discovered.

`ng e2e` is likewise not configured.

## The one contract you must not break

The app maintains `window.werkstattOffeneRequests`, an HTTP-interceptor counter
(`src/app/offene-requests.interceptor.ts`). The E2E wait strategy reads it to decide when the UI
is idle, because the app is **zoneless** and Angular's classic Testability API therefore
observes nothing. It is a testability contract: do not remove it, and route new traffic through
`HttpClient` rather than raw `fetch`, or the counter stops telling the truth.

See [`../README.md`](../README.md), [`../../e2e/README.md`](../../e2e/README.md) and
[`../../docs/deployment.md`](../../docs/deployment.md).
