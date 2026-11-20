import { Component, DestroyRef, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';
import { filter, take } from 'rxjs';

import { JournalService } from '../../../core/services/journal.service';
import { todayCalendarDay } from '../../../shared/helpers/format.helpers';

/** Resolves the landing step when navigating to /jornada without a specific step segment. */
@Component({
  selector: 'app-journal-entry-redirect',
  standalone: true,
  template: ''
})
export class JournalEntryRedirectComponent {
  private readonly journalService = inject(JournalService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);

  constructor() {
    const queryDate = this.route.parent?.snapshot?.queryParamMap?.get('date');
    const selectedDate = this.journalService.getSelectedDate?.() ?? null;
    const expectedDate = queryDate || selectedDate || todayCalendarDay();

    this.journalService.currentJournal$
      .pipe(
        filter((journal): journal is NonNullable<typeof journal> => journal !== null && journal.date === expectedDate),
        take(1),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe(journal => {
        const targetStep = this.journalService.resolveLandingStep(journal);
        void this.router.navigate(['/jornada', targetStep], {
          replaceUrl: true,
          queryParamsHandling: 'preserve'
        });
      });
  }
}
