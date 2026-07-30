import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { ApiService } from '../api.service';
import { EuroPipe } from '../euro.pipe';
import { fehlerText } from '../fehler';
import { Auftrag, AuftragPosition } from '../modelle';
import { StatusTextPipe } from '../status-text.pipe';

/** Order detail: status flow, positions, invoice creation — flows of the 2016
 * AuftragDetailCtrl. Server-rejected actions alert the backend's German
 * message via fehlerText() instead of the legacy "undefined" (SD-3, ADR-0004). */
@Component({
  selector: 'app-auftrag-detail',
  imports: [DatePipe, EuroPipe, FormsModule, RouterLink, StatusTextPipe],
  templateUrl: './auftrag-detail.html',
})
export class AuftragDetail implements OnInit {
  private readonly api = inject(ApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  protected readonly auftrag = signal<Auftrag | null>(null);
  // signal-backed for the same zoneless reason as kunde-detail: the reset
  // happens in an async callback and must schedule its own render (session 10
  // review — the plain property relied on the adjacent laden() signal write)
  private readonly neuePositionState = signal<AuftragPosition>(this.leerePosition());

  private id = '';

  protected get neuePosition(): AuftragPosition {
    return this.neuePositionState();
  }

  ngOnInit(): void {
    this.route.paramMap.subscribe((params) => {
      this.id = params.get('id')!;
      this.laden();
    });
  }

  protected statusSetzen(neuerStatus: string): void {
    this.api.auftragStatusSetzen(this.id, neuerStatus).subscribe({
      next: () => this.laden(),
      error: (fehler) => alert(fehlerText(fehler)),
    });
  }

  protected positionSpeichern(): void {
    if (!this.neuePosition.bezeichnung) {
      alert('Bezeichnung fehlt!');
      return;
    }
    this.api.positionAnlegen(this.id, this.neuePosition).subscribe({
      next: () => {
        this.neuePositionState.set(this.leerePosition());
        this.laden();
      },
      error: (fehler) => alert(fehlerText(fehler)),
    });
  }

  protected positionLoeschen(position: AuftragPosition): void {
    this.api.positionLoeschen(position.id!).subscribe(() => this.laden());
  }

  protected rechnungErstellen(): void {
    if (!confirm('Rechnung zu diesem Auftrag erstellen?')) {
      return;
    }
    this.api.rechnungZuAuftrag(this.id).subscribe({
      next: (rechnung) => this.router.navigate(['/rechnungen', rechnung.id]),
      error: (fehler) => alert(fehlerText(fehler)),
    });
  }

  private laden(): void {
    this.api.auftrag(this.id).subscribe((auftrag) => this.auftrag.set(auftrag));
  }

  private leerePosition(): AuftragPosition {
    return { typ: 'ARBEIT', bezeichnung: '', menge: 1 };
  }
}
