import { computed, DestroyRef, inject, Injectable, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Subject, catchError, of, switchMap, takeUntil, tap } from 'rxjs';
import { SalesStatisticsService } from '../../core/services/sales-statistics.service';
import { getErrorMessage, isApiError } from '../../shared/helpers/problem-details.helpers';
import { dateRangeError, localDate } from '../../shared/helpers/sales-statistics.helpers';
import { CompareStatisticsRequest, ExplicitStatisticsRequest, SalesObservation, SalesScenario, SalesStatisticsResponse } from '../../shared/models/dtos/sales-statistics.dto';
import { SalesTargetCategory } from '../../shared/models/dtos/sales-target.dto';
import { BasketStatisticsResponse } from '../../shared/models/dtos/basket-statistics.dto';
import { StatisticsChartSeries } from '../../shared/models/dtos/sales-statistics.dto';

/** Page-scoped, ephemeral observations. Nothing is persisted or promoted to real sales. */
@Injectable()
export class StatisticsWorkspace {
  private readonly api = inject(SalesStatisticsService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly cancel$ = new Subject<void>();
  private readonly comparisonChanged$ = new Subject<void>();
  private readonly averages$ = new Subject<ExplicitStatisticsRequest>();
  private readonly comparisons$ = new Subject<CompareStatisticsRequest>();
  readonly today = localDate();
  readonly from = signal(this.today.slice(0, 8) + '01');
  readonly to = signal(this.today);
  readonly category = signal<SalesTargetCategory>('FUEL_AGGREGATE');
  readonly observations = signal<SalesObservation[]>([]);
  readonly rangeError = computed(() => dateRangeError(this.from(), this.to()));
  readonly averages = signal<SalesStatisticsResponse | null>(null);
  readonly comparison = signal<SalesStatisticsResponse | null>(null);
  readonly basket = signal<BasketStatisticsResponse | null>(null);
  readonly competitionBars = signal<StatisticsChartSeries[]>([]);
  readonly competitionLines = signal<StatisticsChartSeries[]>([]);
  readonly loadingAverages = signal(false);
  readonly loadingComparison = signal(false);
  readonly error = signal('');
  readonly selected = computed(() => this.observations().filter(row => row.date >= this.from() && row.date <= this.to()));
  readonly unit = computed(() => this.category() === 'BASKET' ? 'COP' : 'Gal');

  constructor() {
    this.averages$.pipe(
      tap(() => { this.loadingAverages.set(true); this.averages.set(null); this.error.set(''); }),
      switchMap(request => this.api.averages(request).pipe(takeUntil(this.cancel$), catchError((error: unknown) => {
        this.error.set(this.message(error)); return of(null);
      }))), takeUntilDestroyed(this.destroyRef)
    ).subscribe(result => { this.averages.set(result); this.loadingAverages.set(false); });
    this.comparisons$.pipe(
      tap(() => { this.loadingComparison.set(true); this.comparison.set(null); this.error.set(''); }),
      switchMap(request => this.api.compare(request).pipe(takeUntil(this.cancel$), takeUntil(this.comparisonChanged$), catchError((error: unknown) => {
        this.error.set(this.message(error)); return of(null);
      }))), takeUntilDestroyed(this.destroyRef)
    ).subscribe(result => { this.comparison.set(result); this.loadingComparison.set(false); });
  }
  setRange(from: string, to: string): void {
    if (from === this.from() && to === this.to()) return;
    this.invalidate(); this.from.set(from); this.to.set(to);
  }
  replaceObservations(category: SalesTargetCategory, observations: SalesObservation[]): void {
    this.invalidate(); this.category.set(category); this.observations.set(observations.map(row => ({ ...row })));
  }
  consult(): void { if (!this.rangeError()) this.averages$.next(this.request()); }
  compare(first: SalesScenario, second: SalesScenario): void {
    if (!this.rangeError()) this.comparisons$.next({ sales: this.request(), first, second });
  }
  invalidateComparison(): void {
    // A changed selector cannot keep a result calculated for a previous scenario.
    this.comparisonChanged$.next(); this.comparison.set(null); this.loadingComparison.set(false); this.error.set('');
  }
  private request(): ExplicitStatisticsRequest {
    return { from: this.from(), to: this.to(), category: this.category(), outputScale: 2, observations: this.selected() };
  }
  private invalidate(): void {
    this.cancel$.next(); this.averages.set(null); this.comparison.set(null);
    this.loadingAverages.set(false); this.loadingComparison.set(false); this.error.set('');
  }
  private message(error: unknown): string {
    if (isApiError(error) && (error.status === 401 || error.status === 403)) return 'No tienes autorización para consultar estadísticas. Se requiere una sesión de administrador.';
    return isApiError(error) ? getErrorMessage(error) : error instanceof Error ? error.message : 'No se pudo completar el análisis. Intenta nuevamente.';
  }
}
