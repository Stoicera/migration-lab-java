import { Routes } from '@angular/router';

import { AltWeiche } from './alt-weiche';
import { Dashboard } from './dashboard/dashboard';
import { Auftraege } from './auftraege/auftraege';
import { AuftragDetail } from './auftraege/auftrag-detail';
import { AuftragNeu } from './auftraege/auftrag-neu';
import { Fahrzeuge } from './fahrzeuge/fahrzeuge';
import { Kunden } from './kunden/kunden';
import { KundeDetail } from './kunden/kunde-detail';
import { Rechnungen } from './rechnungen/rechnungen';
import { RechnungDetail } from './rechnungen/rechnung-detail';

/** Route table = Strangler-Fig scoreboard: a route appears here when its slice
 * is ported; everything else falls through to the AngularJS UI (AltWeiche).
 * The catch-all becomes a redirect to /start when the last slice lands. */
export const routes: Routes = [
  { path: 'start', component: Dashboard },
  { path: 'kunden', component: Kunden },
  { path: 'kunden/neu', component: KundeDetail },
  { path: 'kunden/:id', component: KundeDetail },
  { path: 'fahrzeuge', component: Fahrzeuge },
  { path: 'auftraege', component: Auftraege },
  { path: 'auftraege/neu', component: AuftragNeu },
  { path: 'auftraege/:id', component: AuftragDetail },
  { path: 'rechnungen', component: Rechnungen },
  { path: 'rechnungen/:id', component: RechnungDetail },
  { path: '', pathMatch: 'full', redirectTo: 'start' },
  { path: '**', component: AltWeiche },
];
