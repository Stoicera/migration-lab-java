import { Component, OnInit, inject, signal } from '@angular/core';
import { JsonPipe } from '@angular/common';

import { ApiService } from '../api.service';
import { fehlerText } from '../fehler';
import { AdminStatistik } from '../modelle';

/** Successor of the JSP admin page (SD-2, ADR-0004): same numbers, same
 * cleanup action with the same confirm question and result meldung — served
 * as an SPA route instead of server-rendered JSP. */
@Component({
  selector: 'app-admin',
  imports: [JsonPipe],
  templateUrl: './admin.html',
})
export class Admin implements OnInit {
  private readonly api = inject(ApiService);

  protected readonly daten = signal<AdminStatistik | null>(null);
  protected readonly meldung = signal('');

  ngOnInit(): void {
    this.laden();
  }

  protected bereinigen(): void {
    if (!confirm('Wirklich alle alten stornierten Aufträge löschen?')) {
      return;
    }
    this.api.adminBereinigen().subscribe({
      next: (ergebnis) => {
        this.meldung.set(ergebnis.meldung);
        this.laden();
      },
      error: (fehler) => alert(fehlerText(fehler)),
    });
  }

  private laden(): void {
    this.api.adminStatistik().subscribe((daten) => this.daten.set(daten));
  }
}
