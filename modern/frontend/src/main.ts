import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app/app.config';
import { App } from './app/app';

// Legacy hash-bang bookmarks (/#!/rechnungen/12) keep working after the
// stage-5 cutover: rewrite the fragment to the path route BEFORE the router's
// initial navigation. Pinned by e2e DeepLinkTest on both stands.
if (window.location.hash.startsWith('#!')) {
  history.replaceState(null, '', window.location.hash.substring(2) || '/');
}

const zaehler = window as unknown as { werkstattOffeneRequests?: number };

bootstrapApplication(App, appConfig)
  .then(() => {
    // E2E wait contract (e2e/README.md, "Wait strategy"): the suite gates
    // writes on "no pending HTTP work" via this counter (maintained by the
    // interceptor in app.config.ts, which also creates it on first traffic).
    // The marker appears only AFTER bootstrap — before that the idle probe
    // must report "not idle", otherwise a wait between page load and first
    // render passes vacuously (review session 10). ??= so an already-running
    // request's count is never clobbered.
    zaehler.werkstattOffeneRequests ??= 0;
  })
  .catch((err) => console.error(err));
