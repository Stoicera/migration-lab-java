import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { ApiService } from '../api.service';
import { EuroPipe } from '../euro.pipe';
import { Rechnung } from '../modelle';

/** Invoice list with the "nur unbezahlte" client-side toggle and the
 * mark-as-paid action of the 2016 RechnungenCtrl. */
@Component({
  selector: 'app-rechnungen',
  imports: [DatePipe, EuroPipe, FormsModule, RouterLink],
  templateUrl: './rechnungen.html',
})
export class Rechnungen implements OnInit {
  private readonly api = inject(ApiService);

  protected readonly rechnungen = signal<Rechnung[]>([]);
  protected readonly nurOffene = signal(false);

  protected readonly sichtbare = computed(() =>
    this.nurOffene() ? this.rechnungen().filter((r) => !r.bezahlt) : this.rechnungen(),
  );

  ngOnInit(): void {
    this.laden();
  }

  protected bezahltSetzen(rechnung: Rechnung): void {
    this.api.rechnungBezahlt(rechnung.id).subscribe(() => this.laden());
  }

  private laden(): void {
    this.api.rechnungen().subscribe((rechnungen) => this.rechnungen.set(rechnungen));
  }
}
