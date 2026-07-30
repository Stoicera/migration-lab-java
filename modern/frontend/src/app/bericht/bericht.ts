import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ApiService } from '../api.service';
import { EuroPipe } from '../euro.pipe';
import { MonatsBericht, TopKunde } from '../modelle';

/** Monthly report — the 2018 intern's BerichtCtrl, ported: year dropdown from
 * the current year down to 2016 (keeps every seed year selectable forever,
 * see e2e/README.md "Year handling"), client-side sums over the 12 rows. */
@Component({
  selector: 'app-bericht',
  imports: [EuroPipe, FormsModule],
  templateUrl: './bericht.html',
})
export class Bericht implements OnInit {
  private readonly api = inject(ApiService);

  protected readonly jahre: number[] = [];
  protected jahr = new Date().getFullYear();

  protected readonly monate = signal<MonatsBericht[]>([]);
  protected readonly topKunden = signal<TopKunde[]>([]);
  // null until the first response, like the 2016 controller's undefined —
  // the Gesamt row renders BLANK, not "0 / 0,00 €" (parity, session 10)
  protected readonly summeNetto = signal<number | null>(null);
  protected readonly summeBrutto = signal<number | null>(null);
  protected readonly summeAuftraege = signal<number | null>(null);

  constructor() {
    for (let j = this.jahr; j >= 2016; j--) {
      this.jahre.push(j);
    }
  }

  ngOnInit(): void {
    this.laden();
  }

  protected laden(): void {
    this.api.berichtMonate(this.jahr).subscribe((monate) => {
      this.monate.set(monate);
      this.summeNetto.set(monate.reduce((s, m) => s + m.umsatzNetto, 0));
      this.summeBrutto.set(monate.reduce((s, m) => s + m.umsatzBrutto, 0));
      this.summeAuftraege.set(monate.reduce((s, m) => s + m.anzahlAuftraege, 0));
    });
    this.api.berichtTopKunden(this.jahr).subscribe((topKunden) => this.topKunden.set(topKunden));
  }
}
