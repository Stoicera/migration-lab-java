import { Pipe, PipeTransform } from '@angular/core';

import { AuftragStatus } from './modelle';

/** Display texts for order statuses — the $rootScope.statusText map of the
 * AngularJS UI, verbatim ("muessen mit dem Backend zusammenpassen"). */
const STATUS_TEXT: Record<AuftragStatus, string> = {
  ANGENOMMEN: 'Angenommen',
  IN_ARBEIT: 'In Arbeit',
  FERTIG: 'Fertig',
  ABGEHOLT: 'Abgeholt',
  STORNIERT: 'Storniert',
};

@Pipe({ name: 'statusText' })
export class StatusTextPipe implements PipeTransform {
  transform(status: AuftragStatus | null | undefined): string {
    return status ? STATUS_TEXT[status] : '';
  }
}
