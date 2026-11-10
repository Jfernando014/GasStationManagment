import { ChangeDetectionStrategy, Component, computed, inject, input, output, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { catchError, map, of, switchMap, tap } from 'rxjs';

import { ShiftsWorkerMonthService } from '../../../../core/services/shifts-worker-month.service';
import { getErrorMessage } from '../../../../shared/helpers/problem-details.helpers';
import { addMonths, operationalToday } from '../../../../shared/helpers/shift-display.helpers';
import { ShiftCode } from '../../../../shared/models/dtos/shift-catalog.dto';
import { WorkerMonth } from '../../../../shared/models/dtos/worker-profile.dto';
import {
  CalendarDirection,
  WorkerProfileCalendarComponent
} from '../worker-profile-calendar/worker-profile-calendar.component';
import { WorkerProfileSummaryComponent } from '../worker-profile-summary/worker-profile-summary.component';

/** "Turnos del mes" tab: loads the month of the worker and shows the calendar with its summary. */
@Component({
  selector: 'app-worker-profile-shifts-tab',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [WorkerProfileCalendarComponent, WorkerProfileSummaryComponent],
  templateUrl: './worker-profile-shifts-tab.component.html',
  styleUrl: './worker-profile-shifts-tab.component.css'
})
export class WorkerProfileShiftsTabComponent {
  private readonly service = inject(ShiftsWorkerMonthService);

  readonly workerId = input.required<string>();
  /** yyyy-MM. */
  readonly month = input.required<string>();
  readonly catalog = input<Record<string, ShiftCode>>({});
  readonly monthChange = output<string>();

  protected readonly today = operationalToday();
  protected readonly direction = signal<CalendarDirection>('none');
  protected readonly data = signal<WorkerMonth | null>(null);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);

  private readonly reloads = signal(0);
  private readonly request = computed(() => ({ id: this.workerId(), month: this.month(), reload: this.reloads() }));

  constructor() {
    toObservable(this.request)
      .pipe(
        tap(() => {
          this.loading.set(true);
          this.error.set(null);
        }),
        switchMap(({ id, month }) =>
          this.service.getMonth(id, month).pipe(
            map(data => ({ data, error: null as string | null })),
            catchError((error: unknown) => of({ data: null, error: getErrorMessage(error) }))
          )
        ),
        takeUntilDestroyed()
      )
      .subscribe(result => {
        this.data.set(result.data);
        this.error.set(result.error);
        this.loading.set(false);
      });
  }

  protected goTo(step: -1 | 1): void {
    this.direction.set(step === 1 ? 'next' : 'previous');
    this.monthChange.emit(addMonths(this.month(), step));
  }

  protected retry(): void {
    this.reloads.update(count => count + 1);
  }
}
