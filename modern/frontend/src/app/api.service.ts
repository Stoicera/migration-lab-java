import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import {
  Auftrag,
  AuftragPosition,
  Fahrzeug,
  Kunde,
  MonatsBericht,
  NeuerAuftrag,
  Rechnung,
  TopKunde,
} from './modelle';

/**
 * The one place that talks to the backend — what the 2016 `Api` factory wanted
 * to be ("Idee war mal, alle $http-Aufrufe hier zu sammeln") and never became:
 * half the controllers called $http directly. Same endpoints, same payloads;
 * the URLs are root-relative because the app is served from "/".
 */
@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);

  kunden(suche?: string): Observable<Kunde[]> {
    return this.http.get<Kunde[]>('api/kunden', suche ? { params: { suche } } : {});
  }

  kunde(id: number | string): Observable<Kunde> {
    return this.http.get<Kunde>(`api/kunden/${id}`);
  }

  kundeSpeichern(kunde: Kunde): Observable<Kunde> {
    if (kunde.id) {
      return this.http.put<Kunde>(`api/kunden/${kunde.id}`, kunde);
    }
    return this.http.post<Kunde>('api/kunden', kunde);
  }

  kundeLoeschen(id: number): Observable<void> {
    return this.http.delete<void>(`api/kunden/${id}`);
  }

  fahrzeugeZuKunde(kundeId: number | string): Observable<Fahrzeug[]> {
    return this.http.get<Fahrzeug[]>(`api/kunden/${kundeId}/fahrzeuge`);
  }

  fahrzeuge(): Observable<Fahrzeug[]> {
    return this.http.get<Fahrzeug[]>('api/fahrzeuge');
  }

  fahrzeugSpeichern(fahrzeug: Fahrzeug): Observable<Fahrzeug> {
    return this.http.post<Fahrzeug>('api/fahrzeuge', fahrzeug);
  }

  fahrzeugLoeschen(id: number): Observable<void> {
    return this.http.delete<void>(`api/fahrzeuge/${id}`);
  }

  auftraege(status?: string): Observable<Auftrag[]> {
    return this.http.get<Auftrag[]>('api/auftraege', status ? { params: { status } } : {});
  }

  auftrag(id: number | string): Observable<Auftrag> {
    return this.http.get<Auftrag>(`api/auftraege/${id}`);
  }

  auftragAnlegen(auftrag: NeuerAuftrag): Observable<Auftrag> {
    return this.http.post<Auftrag>('api/auftraege', auftrag);
  }

  auftragStatusSetzen(id: number | string, neu: string): Observable<void> {
    return this.http.put<void>(`api/auftraege/${id}/status`, null, { params: { neu } });
  }

  positionAnlegen(auftragId: number | string, position: AuftragPosition): Observable<void> {
    return this.http.post<void>(`api/auftraege/${auftragId}/positionen`, position);
  }

  positionLoeschen(positionId: number): Observable<void> {
    return this.http.delete<void>(`api/auftraege/positionen/${positionId}`);
  }

  rechnungen(): Observable<Rechnung[]> {
    return this.http.get<Rechnung[]>('api/rechnungen');
  }

  rechnung(id: number | string): Observable<Rechnung> {
    return this.http.get<Rechnung>(`api/rechnungen/${id}`);
  }

  rechnungZuAuftrag(auftragId: number | string): Observable<Rechnung> {
    return this.http.post<Rechnung>(`api/rechnungen/auftrag/${auftragId}`, null);
  }

  rechnungBezahlt(id: number): Observable<void> {
    return this.http.put<void>(`api/rechnungen/${id}/bezahlt`, null);
  }

  berichtMonate(jahr: number): Observable<MonatsBericht[]> {
    return this.http.get<MonatsBericht[]>('api/bericht/monat', { params: { jahr } });
  }

  berichtTopKunden(jahr: number): Observable<TopKunde[]> {
    return this.http.get<TopKunde[]>('api/bericht/topkunden', { params: { jahr } });
  }
}
