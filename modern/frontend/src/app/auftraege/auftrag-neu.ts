import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { ApiService } from '../api.service';
import { fehlerText } from '../fehler';
import { Fahrzeug, Kunde, NeuerAuftrag } from '../modelle';

/** New-order form: customer select loads all customers, choosing one loads its
 * vehicles — flows and the blunt validation alert of the 2016 AuftragNeuCtrl. */
@Component({
  selector: 'app-auftrag-neu',
  imports: [FormsModule, RouterLink],
  templateUrl: './auftrag-neu.html',
})
export class AuftragNeu implements OnInit {
  private readonly api = inject(ApiService);
  private readonly router = inject(Router);

  protected readonly kunden = signal<Kunde[]>([]);
  protected readonly fahrzeuge = signal<Fahrzeug[]>([]);
  protected gewaehlterKunde: Kunde | null = null;
  protected auftrag: NeuerAuftrag = {};

  ngOnInit(): void {
    this.api.kunden().subscribe((kunden) => this.kunden.set(kunden));
  }

  protected kundeGewaehlt(): void {
    this.auftrag.fahrzeugId = null;
    this.fahrzeuge.set([]);
    if (this.gewaehlterKunde) {
      this.api
        .fahrzeugeZuKunde(this.gewaehlterKunde.id!)
        .subscribe((fahrzeuge) => this.fahrzeuge.set(fahrzeuge));
    }
  }

  protected anlegen(): void {
    if (!this.gewaehlterKunde || !this.auftrag.fahrzeugId) {
      alert('Bitte Kunde und Fahrzeug auswählen!');
      return;
    }
    this.auftrag.kundeId = this.gewaehlterKunde.id;
    this.api.auftragAnlegen(this.auftrag).subscribe({
      next: (angelegt) => this.router.navigate(['/auftraege', angelegt.id]),
      error: (fehler) => alert('Auftrag anlegen fehlgeschlagen: ' + fehlerText(fehler)),
    });
  }
}
