import { Routes } from '@angular/router';

import { AltWeiche } from './alt-weiche';
import { Dashboard } from './dashboard/dashboard';

/** Route table = Strangler-Fig scoreboard: a route appears here when its slice
 * is ported; everything else falls through to the AngularJS UI (AltWeiche).
 * The catch-all becomes a redirect to /start when the last slice lands. */
export const routes: Routes = [
  { path: 'start', component: Dashboard },
  { path: '', pathMatch: 'full', redirectTo: 'start' },
  { path: '**', component: AltWeiche },
];
