import { ChangeDetectionStrategy, Component, computed, effect, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { catchError, filter, map, of, switchMap, tap } from 'rxjs';

import { EmptyStateComponent } from '../../../../atomic-design/molecules/empty-state/empty-state.component';
import { WorkerPerformanceService } from '../../../../core/services/worker-performance.service';
import { getErrorMessage } from '../../../../shared/helpers/problem-details.helpers';
import { ShiftCode } from '../../../../shared/models/dtos/shift-catalog.dto';
import { WorkerPerformance } from '../../../../shared/models/dtos/worker-profile.dto';
import { WorkerProfileMaxSaleComponent } from '../worker-profile-max-sale/worker-profile-max-sale.component';
import { WorkerProfilePerformanceTableComponent } from '../worker-profile-performance-table/worker-profile-performance-table.component';

/**
 * "Desempeño histórico" tab: loads the performance of the worker the first time the tab is shown, independently of
 * the other tab, so a failure here never affects the schedule.
 */
@Component({
  selector: 'app-worker-profile-performance-tab',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [EmptyStateComponent, WorkerProfilePerformanceTableComponent, WorkerProfileMaxSaleComponent],
  templateUrl: './worker-profile-performance-tab.component.html',
  styleUrl: './worker-profile-performance-tab.component.css'
})
export class WorkerProfilePerformanceTabComponent {
  private readonly service = inject(WorkerPerformanceService);

  readonly workerId = input.required<string>();
  /** True while the tab is the one being shown; the first time it is true the data is requested. */
  readonly active = input(false);
  readonly catalog = input<Record<string, ShiftCode>>({});

  protected readonly data = signal<WorkerPerformance | null>(null);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);

  private readonly activated = signal(false);
  private readonly reloads = signal(0);
  private readonly request = computed(() =>
    this.activated() ? { id: this.workerId(), reload: this.reloads() } : null
  );

  constructor() {
    effect(() => {
      if (this.active()) {
        this.activated.set(true);
      }
    });

    toObservable(this.request)
      .pipe(
        filter((request): request is { id: string; reload: number } => request !== null),
        tap(() => {
          this.loading.set(true);
          this.error.set(null);
        }),
        switchMap(({ id }) =>
          this.service.getPerformance(id).pipe(
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

  protected retry(): void {
    this.reloads.update(count => count + 1);
  }
}
