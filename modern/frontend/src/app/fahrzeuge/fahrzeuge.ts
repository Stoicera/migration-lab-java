import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { ApiService } from '../api.service';
import { Fahrzeug } from '../modelle';

/** Global vehicle list, read-only — vehicles are created on the owning
 * customer's page. The filter reproduces AngularJS' `filter:filter`
 * including the leading-`!` negation prefix (review session 10: the port
 * had dropped it): case-insensitive substring match across all field
 * values, client-side, inverted when the term starts with `!`. */
@Component({
  selector: 'app-fahrzeuge',
  imports: [DatePipe, FormsModule, RouterLink],
  templateUrl: './fahrzeuge.html',
})
export class Fahrzeuge implements OnInit {
  private readonly api = inject(ApiService);

  private readonly fahrzeuge = signal<Fahrzeug[]>([]);
  protected readonly filter = signal('');

  protected readonly gefiltert = computed(() => {
    const begriff = this.filter().toLowerCase();
    if (!begriff) {
      return this.fahrzeuge();
    }
    // a bare "!" negates the match-everything empty term ⇒ empty list, like AngularJS
    const negiert = begriff.startsWith('!');
    const suchbegriff = negiert ? begriff.substring(1) : begriff;
    return this.fahrzeuge().filter(
      (f) =>
        Object.values(f).some(
          (wert) => wert != null && String(wert).toLowerCase().includes(suchbegriff),
        ) !== negiert,
    );
  });

  ngOnInit(): void {
    this.api.fahrzeuge().subscribe({
      next: (fahrzeuge) => this.fahrzeuge.set(fahrzeuge),
      error: () => alert('Fahrzeuge konnten nicht geladen werden!'),
    });
  }
}
