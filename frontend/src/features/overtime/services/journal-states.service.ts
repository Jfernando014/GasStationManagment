import { inject, Injectable } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router } from '@angular/router';
import { catchError, filter, map, Observable, of, shareReplay } from 'rxjs';

import { JournalService } from '../../../core/services/journal.service';

/** The screens that share what was read; leaving them forgets it, because journals are closed elsewhere. */
const SECTION = '/personal/horas-extra';

/** Which journals of a month are closed, read from the history of journals once per month. */
@Injectable({ providedIn: 'root' })
export class JournalStatesService {
  private readonly journals = inject(JournalService);
  private readonly cache = new Map<string, Observable<ReadonlySet<string> | null>>();

  constructor() {
    inject(Router).events.pipe(
      filter((event): event is NavigationEnd => event instanceof NavigationEnd),
      filter(event => !event.urlAfterRedirects.startsWith(SECTION)),
      takeUntilDestroyed()
    ).subscribe(() => this.cache.clear());
  }

  /**
   * Days of a month ("yyyy-MM") whose journal is closed or was migrated; null when the history could not be read.
   * Never errors. A failed read is not kept, so the next one asks again.
   */
  closedDays(month: string): Observable<ReadonlySet<string> | null> {
    let request = this.cache.get(month);
    if (!request) {
      request = this.journals.getJournalHistory(month).pipe(
        map(history => new Set(history.days
          .filter(day => day.state === 'CLOSED' || day.state === 'MIGRATED')
          .map(day => day.date)) as ReadonlySet<string>),
        catchError(() => {
          this.cache.delete(month);
          return of(null);
        }),
        shareReplay(1)
      );
      this.cache.set(month, request);
    }
    return request;
  }
}
