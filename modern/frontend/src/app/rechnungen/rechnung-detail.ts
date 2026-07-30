import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { ApiService } from '../api.service';
import { EuroPipe } from '../euro.pipe';
import { Auftrag, Rechnung } from '../modelle';

/** Invoice sheet — like the 2016 RechnungDetailCtrl it loads the invoice
 * first, then reloads the order NESTED for the position lines. */
@Component({
  selector: 'app-rechnung-detail',
  imports: [DatePipe, EuroPipe, RouterLink],
  templateUrl: './rechnung-detail.html',
})
export class RechnungDetail implements OnInit {
  private readonly api = inject(ApiService);
  private readonly route = inject(ActivatedRoute);

  protected readonly rechnung = signal<Rechnung | null>(null);
  protected readonly auftrag = signal<Auftrag | null>(null);

  ngOnInit(): void {
    this.api.rechnung(this.route.snapshot.paramMap.get('id')!).subscribe((rechnung) => {
      this.rechnung.set(rechnung);
      this.api.auftrag(rechnung.auftragId).subscribe((auftrag) => this.auftrag.set(auftrag));
    });
  }

  protected drucken(): void {
    window.print();
  }
}
