import { HttpEvent, HttpHandlerFn, HttpRequest } from '@angular/common/http';
import { Observable } from 'rxjs';
import { finalize } from 'rxjs/operators';

const zaehler = window as unknown as { werkstattOffeneRequests: number };

/**
 * Keeps `window.werkstattOffeneRequests` equal to the number of in-flight HTTP
 * requests — the app's replacement for AngularJS' `$http.pendingRequests`,
 * which the Selenium suite polls as its "UI is idle" gate (wait.strategy
 * `angular`, see e2e/README.md). The counter is part of the app's testability
 * contract, not debug tooling: the app is zoneless, so the classic
 * Testability#isStable probe has nothing to observe.
 */
export function offeneRequestsInterceptor(
  req: HttpRequest<unknown>,
  next: HttpHandlerFn,
): Observable<HttpEvent<unknown>> {
  zaehler.werkstattOffeneRequests = (zaehler.werkstattOffeneRequests ?? 0) + 1;
  return next(req).pipe(finalize(() => zaehler.werkstattOffeneRequests--));
}
