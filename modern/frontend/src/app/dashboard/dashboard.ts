import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';

import { ApiService } from '../api.service';
import { Auftrag } from '../modelle';
import { StatusTextPipe } from '../status-text.pipe';

/** Start page — KPIs and the two workshop tables, derived exactly like the
 * 2016 DashboardCtrl (client-side filtering of the full order list). */
@Component({
  selector: 'app-dashboard',
  imports: [DatePipe, RouterLink, StatusTextPipe],
  templateUrl: './dashboard.html',
})
export class Dashboard implements OnInit {
  private readonly api = inject(ApiService);

  private readonly auftraege = signal<Auftrag[]>([]);
  protected readonly offeneRechnungen = signal(0);

  protected readonly offeneAuftraege = computed(() =>
    this.auftraege().filter((a) => a.status === 'ANGENOMMEN' || a.status === 'IN_ARBEIT'),
  );
  protected readonly fertigeAuftraege = computed(() =>
    this.auftraege().filter((a) => a.status === 'FERTIG'),
  );

  ngOnInit(): void {
    this.api.auftraege().subscribe((alle) => this.auftraege.set(alle));
    this.api
      .rechnungen()
      .subscribe((alle) => this.offeneRechnungen.set(alle.filter((r) => !r.bezahlt).length));
  }
}
