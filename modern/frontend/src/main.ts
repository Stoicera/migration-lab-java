import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app/app.config';
import { App } from './app/app';

// E2E wait contract (e2e/README.md, "Wait strategy"): the suite gates writes on
// "no pending HTTP work". The AngularJS UI exposed $http.pendingRequests; this
// app maintains the same semantic as a window counter (incremented/decremented
// by the interceptor in app.config.ts). Initialised BEFORE bootstrap so the
// probe can distinguish "Angular page, idle" from "marker not present" while
// both UIs coexist (hybrid phase).
(window as unknown as { werkstattOffeneRequests: number }).werkstattOffeneRequests = 0;

bootstrapApplication(App, appConfig).catch((err) => console.error(err));
