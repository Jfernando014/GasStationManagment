import { Component, computed, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { catchError, forkJoin, of, Subject, switchMap, tap } from 'rxjs';
import { EmptyStateComponent } from '../../../atomic-design/molecules/empty-state/empty-state.component';
import { AvatarComponent } from '../../../atomic-design/atoms/avatar/avatar.component';
import { StatisticsDialogComponent } from './statistics-dialog.component';
import { localDate, monthRange, formatStatistics } from '../../../shared/helpers/sales-statistics.helpers';
import { basketSellers } from '../../../shared/helpers/statistics-contracts.helpers';
import { getErrorMessage, isApiError } from '../../../shared/helpers/problem-details.helpers';
import { SalesStatisticsService } from '../../../core/services/sales-statistics.service';
import { WorkersService } from '../../../core/services/workers.service';
import { Worker } from '../../../shared/models/dtos/worker.dto';
import { BasketSeller } from '../../../shared/models/dtos/basket-statistics.dto';
import { StatisticsWorkspace } from '../statistics-workspace';

@Component({ selector: 'app-basket-statistics', standalone: true, imports: [FormsModule, EmptyStateComponent, AvatarComponent, StatisticsDialogComponent],
  templateUrl: './basket-statistics.component.html', styleUrl: '../statistics.css' })
export class BasketStatisticsComponent {
  readonly workspace = inject(StatisticsWorkspace);
  private readonly api = inject(SalesStatisticsService);
  private readonly directory = inject(WorkersService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly queries = new Subject<string>();
  readonly currentMonth = localDate().slice(0, 7);
  month = this.currentMonth;
  readonly queriedMonth = signal(this.currentMonth);
  readonly loading = signal(false);
  readonly error = signal('');
  readonly workers = signal<Worker[]>([]);
  readonly selected = signal<BasketSeller | null>(null);
  readonly data = this.workspace.basket;
  readonly sellers = computed(() => this.data() ? basketSellers(this.data()!, this.workers()) : []);
  readonly format = formatStatistics;
  readonly status = computed(() => this.data()?.charts.availability ?? 'UNAVAILABLE');
  constructor() {
    this.queries.pipe(tap(month => { this.queriedMonth.set(month); this.loading.set(true); this.error.set(''); this.data.set(null); this.selected.set(null); }),
      switchMap(month => { const dates = monthRange(month); return forkJoin({
        data: this.api.basket(dates.from, dates.to), workers: this.directory.list().pipe(catchError(() => of([] as Worker[])))
      }).pipe(catchError((error: unknown) => {
        this.error.set(isApiError(error) && error.status === 404 ? 'La consulta de ventas por vendedor aún no está disponible en el servidor.' : getErrorMessage(error)); return of(null);
      })); }), takeUntilDestroyed(this.destroyRef)).subscribe(result => {
        this.loading.set(false); if (result) { this.data.set(result.data); this.workers.set(result.workers); }
      });
    this.consult();
  }
  consult(): void { if (this.validMonth()) this.queries.next(this.month); }
  validMonth(): boolean { return /^(?!0000)\d{4}-(0[1-9]|1[0-2])$/.test(this.month); }
  periodLabel(): string {
    const month = this.queriedMonth();
    return new Intl.DateTimeFormat('es-CO', { month: 'long', year: 'numeric', timeZone: 'UTC' })
      .format(new Date(month + '-01T00:00:00Z'));
  }
  reset(): void { this.month = this.currentMonth; this.consult(); }
}
