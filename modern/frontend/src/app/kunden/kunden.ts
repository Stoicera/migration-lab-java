import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { ApiService } from '../api.service';
import { fehlerText } from '../fehler';
import { Kunde } from '../modelle';

/** Customer list with server-side search — same flows (and the same blunt
 * alert/confirm dialogs) as the 2016 KundenCtrl. */
@Component({
  selector: 'app-kunden',
  imports: [FormsModule, RouterLink],
  templateUrl: './kunden.html',
})
export class Kunden implements OnInit {
  private readonly api = inject(ApiService);

  protected readonly kunden = signal<Kunde[]>([]);
  protected suche = '';

  ngOnInit(): void {
    this.laden();
  }

  protected laden(): void {
    this.api.kunden(this.suche || undefined).subscribe({
      next: (kunden) => this.kunden.set(kunden),
      error: () => alert('Kunden konnten nicht geladen werden!'),
    });
  }

  protected suchen(): void {
    this.laden();
  }

  protected loeschen(kunde: Kunde): void {
    if (!confirm('Kunde "' + kunde.nachname + '" wirklich löschen?')) {
      return;
    }
    this.api.kundeLoeschen(kunde.id!).subscribe({
      next: () => this.laden(),
      error: (fehler) => alert('Löschen fehlgeschlagen: ' + fehlerText(fehler)),
    });
  }
}
