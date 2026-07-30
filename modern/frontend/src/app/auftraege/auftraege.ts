import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { Router, RouterLink } from '@angular/router';

import { ApiService } from '../api.service';
import { Auftrag } from '../modelle';
import { StatusTextPipe } from '../status-text.pipe';

/** Order list with the server-side status filter of the 2016 AuftraegeCtrl
 * (filter buttons re-query the API, they do not filter client-side). */
@Component({
  selector: 'app-auftraege',
  imports: [DatePipe, RouterLink, StatusTextPipe],
  templateUrl: './auftraege.html',
})
export class Auftraege implements OnInit {
  private readonly api = inject(ApiService);
  private readonly router = inject(Router);

  protected readonly auftraege = signal<Auftrag[]>([]);
  protected readonly statusFilter = signal('');

  ngOnInit(): void {
    this.laden();
  }

  protected filterSetzen(status: string): void {
    this.statusFilter.set(status);
    this.laden();
  }

  protected oeffnen(auftrag: Auftrag): void {
    this.router.navigate(['/auftraege', auftrag.id]);
  }

  protected kurz(beschreibung: string): string {
    const text = beschreibung || '';
    return text.length > 60 ? text.slice(0, 60) + '...' : text;
  }

  private laden(): void {
    this.api
      .auftraege(this.statusFilter() || undefined)
      .subscribe((auftraege) => this.auftraege.set(auftraege));
  }
}
