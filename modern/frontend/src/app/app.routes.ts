import { Routes } from '@angular/router';

import { AltWeiche } from './alt-weiche';
import { Dashboard } from './dashboard/dashboard';
import { Kunden } from './kunden/kunden';
import { KundeDetail } from './kunden/kunde-detail';

/** Route table = Strangler-Fig scoreboard: a route appears here when its slice
 * is ported; everything else falls through to the AngularJS UI (AltWeiche).
 * The catch-all becomes a redirect to /start when the last slice lands. */
export const routes: Routes = [
  { path: 'start', component: Dashboard },
  { path: 'kunden', component: Kunden },
  { path: 'kunden/neu', component: KundeDetail },
  { path: 'kunden/:id', component: KundeDetail },
  { path: '', pathMatch: 'full', redirectTo: 'start' },
  { path: '**', component: AltWeiche },
];
