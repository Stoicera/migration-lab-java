// One load scenario, as ENGINEERING_STANDARDS §3 asks for ("Gatling oder k6 — ein
// Szenario reicht"). It walks the read path a real user walks: open the dashboard,
// list customers, search, open one customer, look at their orders, pull the report.
//
//   docker run --rm -i --network host grafana/k6:latest run - < load/k6/lesepfad.js
//   BASE_URL=http://localhost:8080 docker run ... (same file against the legacy stand)
//
// What this measures and what it does not:
//   - it measures the two stands on ONE machine under ONE load shape, which is
//     exactly enough to (a) catch a migration that made a request path an order of
//     magnitude slower and (b) compare legacy against modern on equal terms.
//   - it does NOT measure production capacity. The database, the application and
//     the load generator share a laptop; the numbers are a baseline to compare
//     against, not a figure to put in front of a customer as "the system handles X".
//     docs/deployment.md §13 states the measured values with that caveat attached.
import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Trend } from 'k6/metrics';

const BASE = __ENV.BASE_URL || 'http://localhost:8090';

const berichtDauer = new Trend('bericht_duration', true);

export const options = {
  stages: [
    { duration: '10s', target: 5 }, // ramp up
    { duration: '30s', target: 5 }, // hold
    { duration: '5s', target: 0 }, // ramp down
  ],
  thresholds: {
    // Deliberately loose, and that is the point: this gate exists to catch a
    // migration that broke performance by an order of magnitude, not to enforce a
    // service level nobody agreed to. A threshold tuned to today's laptop would go
    // red on the next machine and get switched off — which is how gates die.
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<1000'],
    // the report aggregates over subselects per month and is the slowest endpoint
    // in both stands; it gets its own, more generous budget
    bericht_duration: ['p(95)<2000'],
  },
};

export default function () {
  group('dashboard', () => {
    const res = http.get(`${BASE}/api/auftraege`);
    check(res, { 'auftraege 200': (r) => r.status === 200 });
  });

  group('kundenliste und suche', () => {
    const alle = http.get(`${BASE}/api/kunden`);
    check(alle, {
      'kunden 200': (r) => r.status === 200,
      'kunden nicht leer': (r) => r.json().length > 0,
    });

    const treffer = http.get(`${BASE}/api/kunden?suche=hofer`);
    check(treffer, { 'suche 200': (r) => r.status === 200 });

    const kunden = alle.json();
    if (kunden.length > 0) {
      const id = kunden[Math.floor(Math.random() * kunden.length)].id;
      const detail = http.get(`${BASE}/api/kunden/${id}`);
      const fahrzeuge = http.get(`${BASE}/api/kunden/${id}/fahrzeuge`);
      check(detail, { 'kunde detail 200': (r) => r.status === 200 });
      check(fahrzeuge, { 'fahrzeuge des kunden 200': (r) => r.status === 200 });
    }
  });

  group('bericht', () => {
    const res = http.get(`${BASE}/api/bericht/monat`);
    berichtDauer.add(res.timings.duration);
    check(res, {
      'bericht 200': (r) => r.status === 200,
      'bericht hat zwoelf monate': (r) => r.json().length === 12,
    });
  });

  sleep(1);
}
