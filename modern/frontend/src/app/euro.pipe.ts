import { Pipe, PipeTransform } from '@angular/core';

/**
 * Replica of the hand-rolled 2016 `euro` filter — NOT Angular's CurrencyPipe:
 * the amounts must render byte-identically to the legacy UI ("1439,00 €", no
 * thousands separator), because the E2E suite pins the formatting contract
 * (e2e/README.md). Changing the format would be an undeclared divergence.
 */
@Pipe({ name: 'euro' })
export class EuroPipe implements PipeTransform {
  transform(betrag: number | null | undefined): string {
    if (betrag === null || betrag === undefined) {
      return '';
    }
    return betrag.toFixed(2).replace('.', ',') + ' €';
  }
}
