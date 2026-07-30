import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DatePipe } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { ApiService } from '../api.service';
import { fehlerText } from '../fehler';
import { Fahrzeug, Kunde } from '../modelle';

/** Customer master data + the customer's vehicles (incl. inline create/delete)
 * — flows and validation behaviour of the 2016 KundeDetailCtrl, one to one.
 * Serves /kunden/neu (empty form) and /kunden/:id; saving a new customer
 * navigates to its id route, exactly like $location.path did. */
@Component({
  selector: 'app-kunde-detail',
  imports: [FormsModule, DatePipe, RouterLink],
  templateUrl: './kunde-detail.html',
})
export class KundeDetail implements OnInit {
  private readonly api = inject(ApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  protected readonly neu = signal(true);
  protected readonly fahrzeuge = signal<Fahrzeug[]>([]);
  protected readonly zeigeFahrzeugForm = signal(false);
  // Zoneless lesson (found by the e2e suite, worklog session 9): state that an
  // ASYNC callback replaces must be signal-tracked, or nothing schedules the
  // re-render — a plain `kunde` property stayed green for eight runs only
  // because the parallel fahrzeuge signal-set raced a render in afterwards.
  // The getter keeps the template/ngModel syntax; user-input mutation of the
  // current object is safe (DOM events schedule CD themselves).
  private readonly kundeState = signal<Kunde>(this.leererKunde());
  protected neuesFahrzeug: Partial<Fahrzeug> = {};

  protected get kunde(): Kunde {
    return this.kundeState();
  }

  ngOnInit(): void {
    // paramMap instead of a one-shot read: saving a NEW customer navigates
    // /kunden/neu -> /kunden/:id onto the same component instance
    this.route.paramMap.subscribe((params) => {
      const id = params.get('id');
      this.neu.set(id === null);
      if (id !== null) {
        this.api.kunde(id).subscribe((kunde) => this.kundeState.set(kunde));
        this.fahrzeugeLaden(id);
      } else {
        this.kundeState.set(this.leererKunde());
        this.fahrzeuge.set([]);
      }
    });
  }

  protected speichern(): void {
    if (!this.kunde.nachname) {
      alert('Nachname ist Pflicht!');
      return;
    }
    this.api.kundeSpeichern(this.kunde).subscribe({
      next: (gespeichert) => {
        this.kundeState.set(gespeichert);
        this.neu.set(false);
        this.router.navigate(['/kunden', gespeichert.id]);
      },
      error: (fehler) => alert('Speichern fehlgeschlagen: ' + fehlerText(fehler)),
    });
  }

  protected fahrzeugSpeichern(): void {
    this.neuesFahrzeug.kundeId = this.kunde.id;
    this.api.fahrzeugSpeichern(this.neuesFahrzeug as Fahrzeug).subscribe({
      next: () => {
        this.neuesFahrzeug = {};
        this.zeigeFahrzeugForm.set(false);
        this.fahrzeugeLaden(this.kunde.id!);
      },
      error: (fehler) => alert('Fahrzeug speichern fehlgeschlagen: ' + fehlerText(fehler)),
    });
  }

  protected fahrzeugLoeschen(fahrzeug: Fahrzeug): void {
    if (!confirm('Fahrzeug ' + fahrzeug.kennzeichen + ' wirklich löschen?')) {
      return;
    }
    this.api.fahrzeugLoeschen(fahrzeug.id!).subscribe(() => this.fahrzeugeLaden(this.kunde.id!));
  }

  private fahrzeugeLaden(kundeId: number | string): void {
    this.api.fahrzeugeZuKunde(kundeId).subscribe((fahrzeuge) => this.fahrzeuge.set(fahrzeuge));
  }

  private leererKunde(): Kunde {
    return { anrede: 'Herr', vorname: '', nachname: '' };
  }
}
