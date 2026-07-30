import { HttpErrorResponse } from '@angular/common/http';

/**
 * Extracts the backend's German error message from any error shape this API
 * produces. The legacy UI alerted `fehler.data` raw, which showed
 * "[object Object]" for JSON error bodies and literally "undefined" for the
 * plain-string 500s that Boot 1.5 mislabels as JSON (pinned by e2e as legacy
 * behaviour; replacing it in this UI is sanctioned divergence SD-3, ADR-0004).
 * The server-side message contract itself is pinned by the error-contract
 * characterization tests — this helper only surfaces it.
 */
export function fehlerText(err: unknown): string {
  if (err instanceof HttpErrorResponse) {
    const body: unknown = err.error;
    if (typeof body === 'string') {
      return body;
    }
    if (body && typeof body === 'object') {
      const b = body as { message?: unknown; text?: unknown };
      // Boot default error JSON (include-message=always, ADR-0005)
      if (typeof b.message === 'string') {
        return b.message;
      }
      // non-JSON body labeled application/json: HttpClient parse failure
      // wraps the raw text — exactly the Boot plain-string 500 case
      if (typeof b.text === 'string') {
        return b.text;
      }
    }
    return err.message;
  }
  return String(err);
}
