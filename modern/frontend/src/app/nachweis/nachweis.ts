import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { HttpClient } from '@angular/common/http';

import { Nachweis, Urteil, Zeile } from './nachweis-daten';

const URTEIL_TEXT: Record<Urteil, string> = {
  identisch: 'identisch',
  sanktioniert: 'sanktionierte Abweichung',
  regression: 'Regression',
  unvollstaendig: 'unvollständig',
};

const STATUS_TEXT: Record<string, string> = {
  bestanden: 'grün',
  fehlgeschlagen: 'rot',
  abgebrochen: 'abgebrochen',
  deaktiviert: 'deaktiviert',
  fehlt: 'kein Ergebnis',
};

/** Number literals compared as values, like the test does (912.0 and 912.00 are equal). */
function zahlenNormiert(zeile: string): string {
  return zeile.replace(
    /(:\s*|^\s*)(-?\d+(?:\.\d+)?(?:[eE][+-]?\d+)?)(,?)$/,
    (_, a, n, b) => a + Number(n) + b,
  );
}

/** One line of a side-by-side view; `anders` marks lines that differ at the same position. */
interface VergleichsZeile {
  links: string;
  rechts: string;
  anders: boolean;
}

/**
 * Äquivalenz-Nachweis — the result of one CI run, test by test, as committed in
 * /nachweis.json (scripts/nachweis/). Read-only, no backend call: the page shows evidence
 * that was produced and reviewed before deployment, never anything computed at request time.
 */
@Component({
  selector: 'app-nachweis',
  imports: [DatePipe],
  templateUrl: './nachweis.html',
  styleUrl: './nachweis.css',
})
export class NachweisSeite implements OnInit {
  private readonly http = inject(HttpClient);

  protected readonly daten = signal<Nachweis | null>(null);
  protected readonly fehler = signal(false);
  protected readonly offen = signal<string | null>(null);

  protected readonly zeilen = computed(() => this.daten()?.charakterisierung.zeilen ?? []);
  protected readonly kurzSha = computed(() => this.daten()?.commit?.sha.slice(0, 7) ?? '');

  ngOnInit(): void {
    this.http.get<Nachweis>('nachweis.json').subscribe({
      next: (d) => this.daten.set(d),
      error: () => this.fehler.set(true),
    });
  }

  protected umschalten(zeile: Zeile): void {
    this.offen.set(this.offen() === zeile.id ? null : zeile.id);
  }

  protected urteilText(u: Urteil): string {
    return URTEIL_TEXT[u];
  }

  protected statusText(s: string): string {
    return STATUS_TEXT[s] ?? s;
  }

  protected vergleich(zeile: Zeile): VergleichsZeile[] {
    const v = zeile.vergleich;
    if (!v) {
      return [];
    }
    const links = v.golden.split('\n');
    const rechts = (v.modern ?? '').split('\n');
    const n = Math.max(links.length, rechts.length);
    return Array.from({ length: n }, (_, i) => ({
      links: links[i] ?? '',
      rechts: rechts[i] ?? '',
      anders: zahlenNormiert(links[i] ?? '') !== zahlenNormiert(rechts[i] ?? ''),
    }));
  }
}
