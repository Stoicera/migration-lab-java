import { Routes } from '@angular/router';

import { Admin } from './admin/admin';
import { Auftraege } from './auftraege/auftraege';
import { AuftragDetail } from './auftraege/auftrag-detail';
import { AuftragNeu } from './auftraege/auftrag-neu';
import { Bericht } from './bericht/bericht';
import { Dashboard } from './dashboard/dashboard';
import { Fahrzeuge } from './fahrzeuge/fahrzeuge';
import { Kunden } from './kunden/kunden';
import { KundeDetail } from './kunden/kunde-detail';
import { Rechnungen } from './rechnungen/rechnungen';
import { RechnungDetail } from './rechnungen/rechnung-detail';

/** Complete since the stage-5 cutover — the AngularJS app is gone. Unknown
 * paths land on /start, exactly like ngRoute's .otherwise did. */
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
  { path: 'bericht', component: Bericht },
  { path: 'admin', component: Admin },
  { path: '', pathMatch: 'full', redirectTo: 'start' },
  { path: '**', redirectTo: 'start' },
];
