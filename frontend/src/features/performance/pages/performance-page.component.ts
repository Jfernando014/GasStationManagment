import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Subject, catchError, of, switchMap } from 'rxjs';
import { AvatarComponent } from '../../../atomic-design/atoms/avatar/avatar.component';
import { SearchFieldComponent } from '../../../atomic-design/atoms/search-field/search-field.component';
import { ShiftChipComponent } from '../../../atomic-design/atoms/shift-chip/shift-chip.component';
import { EmptyStateComponent } from '../../../atomic-design/molecules/empty-state/empty-state.component';
import { DailyDownloadsComponent } from '../../../atomic-design/organisms/daily-downloads/daily-downloads.component';
import { SellerPerformanceService } from '../../../core/services/seller-performance.service';
import { isApiError } from '../../../shared/helpers/problem-details.helpers';
import { formatStatistics, localDate, monthRange, validDate } from '../../../shared/helpers/sales-statistics.helpers';
import { applicableTarget, performanceRangeError, performanceReason } from '../../../shared/helpers/seller-performance.helpers';
import { scenarioKey, scenarioLabel } from '../../../shared/helpers/seller-performance-reports.helpers';
import { FullSellerPerformanceResponse, PerformanceMeasure, PerformanceQuery, ScenarioSale } from '../../../shared/models/dtos/seller-performance.dto';
import { StatisticsChartSeries } from '../../../shared/models/dtos/sales-statistics.dto';
import { StatisticsChartComponent } from '../../statistics/components/statistics-chart.component';
import { StatisticsDialogComponent } from '../../statistics/components/statistics-dialog.component';

@Component({
  selector: 'app-performance-page', standalone: true, changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, RouterLink, AvatarComponent, SearchFieldComponent, ShiftChipComponent, EmptyStateComponent,
    DailyDownloadsComponent, StatisticsChartComponent, StatisticsDialogComponent],
  templateUrl: './performance-page.component.html', styleUrl: './performance-page.component.css'
})
export class PerformancePageComponent {
  private readonly api = inject(SellerPerformanceService);
  private readonly route = inject(ActivatedRoute);
  private readonly requests$ = new Subject<PerformanceQuery | null>();
  readonly today = localDate();
  readonly form = new FormGroup({
    mode: new FormControl<'mes' | 'dia' | 'rango'>('mes', { nonNullable: true }),
    month: new FormControl(this.today.slice(0, 7), { nonNullable: true }),
    from: new FormControl(this.today, { nonNullable: true }),
    to: new FormControl(this.today, { nonNullable: true }),
    measure: new FormControl<PerformanceMeasure>('FUEL_GALLONS', { nonNullable: true })
  });
  readonly filters = signal(this.form.getRawValue());
  readonly tab = signal<'estacion' | 'ajustado'>('estacion');
  readonly data = signal<FullSellerPerformanceResponse | null>(null);
  readonly loading = signal(false);
  readonly error = signal('');
  readonly routeError = signal('');
  readonly fromJournal = signal(false);
  readonly journalDate = signal<string | null>(null);
  readonly selectedWorker = signal('');
  readonly selectedScenario = signal('');
  readonly excludedWorkers = signal<string[]>([]);
  readonly search = signal('');
  readonly dialog = signal<'chart' | 'downloads' | null>(null);
  readonly downloadDate = new FormControl('', { nonNullable: true });
  readonly downloadDateValue = toSignal(this.downloadDate.valueChanges, { initialValue: '' });
  readonly format = formatStatistics;
  readonly reasonText = performanceReason;
  readonly label = scenarioLabel;
  readonly key = scenarioKey;
  readonly target = applicableTarget;
  readonly dailyCompliance = computed(() => {
    const c = this.data()?.compliance;
    return new Map(c && c.window.from === c.window.to ? c.sellers.map(s => [s.workerId, s]) : []);
  });
  readonly range = computed(() => {
    const f = this.filters();
    if (f.mode === 'mes') { try { return monthRange(f.month); } catch { return null; } }
    return { from: f.from, to: f.mode === 'dia' ? f.from : f.to };
  });
  readonly rangeError = computed(() => {
    const range = this.range();
    return this.routeError() || (range ? performanceRangeError(range.from, range.to) : 'Selecciona un mes válido.');
  });
  readonly people = computed(() => this.data()?.compliance.sellers ?? []);
  readonly scenarios = computed(() => {
    const scenarios = new Map<string, ScenarioSale['scenario']>();
    for (const s of this.data()?.comparison.sellers ?? []) scenarios.set(this.scenarioId(s), s.scenario);
    return [...scenarios].map(([id, scenario]) => ({ id, label: scenarioLabel(scenario) }));
  });
  readonly comparisonRows = computed(() => (this.data()?.comparison.sellers ?? []).filter(s => this.matches(s)
    && (!this.selectedWorker() || s.workerId === this.selectedWorker())
    && (!this.selectedScenario() || this.scenarioId(s) === this.selectedScenario())));
  readonly normalizationRows = computed(() => (this.data()?.normalization.sellers ?? []).filter(s => this.matches(s)
    && !this.excludedWorkers().includes(s.workerId)));
  readonly focus = computed(() => this.comparisonRows().length === 1 ? this.comparisonRows()[0] : null);
  readonly activeReport = computed(() => this.tab() === 'estacion' ? this.data()?.comparison : this.data()?.normalization);
  readonly pending = computed(() => this.activeReport()?.coverage.filter(c => c.status === 'PENDING' || c.status === 'ABSENT') ?? []);
  readonly unit = computed(() => this.data()?.unit === 'COP' ? 'COP' : 'gal');
  readonly chartUnit = computed(() => this.unit() + (this.tab() === 'estacion' ? ' / obs.' : ''));
  readonly chartTitle = computed(() => this.tab() === 'estacion' ? 'Vendedor frente a la estación por escenario' : 'Ventas reales y normalizadas por escenario');
  readonly chartSeries = computed<StatisticsChartSeries[]>(() => {
    if (this.tab() === 'estacion') return this.comparisonRows().filter(s => s.availability === 'AVAILABLE').flatMap(s => [
      { label: `${s.recordedName} · ${scenarioLabel(s.scenario)}`, color: '#FF6B00', points: [{ label: 'Vendedor', value: s.sellerAverage }] },
      { label: `Estación · ${s.recordedName} · ${scenarioLabel(s.scenario)}`, color: '#94A3B8', points: [{ label: 'Estación', value: s.stationAverage }] }
    ]);
    return this.normalizationRows().filter(s => s.availability === 'AVAILABLE' && s.normalizedSales !== null).flatMap(s => [
      { label: `${s.recordedName} · ${scenarioLabel(s.scenario)}`, color: '#FF6B00', points: [{ label: 'Real', value: s.sales }] },
      { label: `Normalizado · ${s.recordedName} · ${scenarioLabel(s.scenario)}`, color: '#10B981', points: [{ label: 'Normalizado', value: s.normalizedSales! }] }
    ]);
  });
  readonly downloadError = computed(() => validDate(this.downloadDateValue())
    ? '' : 'Selecciona una fecha operativa para las descargas.');

  constructor() {
    this.requests$.pipe(switchMap(query => query ? this.api.readReport(query).pipe(catchError((error: unknown) => {
      this.error.set(this.message(error)); return of(null);
    })) : of(null)), takeUntilDestroyed()).subscribe(data => {
      this.data.set(data); this.loading.set(false);
      if (data && !data.compliance.sellers.some(s => s.workerId === this.selectedWorker())) this.selectedWorker.set('');
      if (data && !this.scenarios().some(s => s.id === this.selectedScenario())) this.selectedScenario.set('');
    });
    this.form.valueChanges.pipe(takeUntilDestroyed()).subscribe(() => {
      this.filters.set(this.form.getRawValue()); this.routeError.set(''); this.invalidate();
    });
    this.route.queryParamMap.pipe(takeUntilDestroyed()).subscribe(params => {
      const date = params.get('date');
      this.form.reset({ mode: date === null ? 'mes' : 'dia', month: this.today.slice(0, 7),
        from: date ?? this.today, to: date ?? this.today, measure: 'FUEL_GALLONS' }, { emitEvent: false });
      this.filters.set(this.form.getRawValue());
      this.fromJournal.set(params.get('returnTo') === 'jornada' && date !== null && validDate(date));
      this.journalDate.set(this.fromJournal() ? date : null);
      this.routeError.set(date !== null && !validDate(date) ? 'La fecha de la jornada no es válida. Selecciona un período válido.' : '');
      this.consult();
    });
  }

  consult(): void {
    this.invalidate();
    const range = this.range();
    if (range && !this.rangeError()) {
      this.loading.set(true);
      this.requests$.next({ ...range, measure: this.filters().measure, outputScale: 2 });
    }
  }
  selectTab(tab: 'estacion' | 'ajustado'): void { this.dialog.set(null); this.tab.set(tab); }
  tabKey(event: KeyboardEvent): void {
    if (!['ArrowLeft', 'ArrowRight', 'Home', 'End'].includes(event.key)) return;
    event.preventDefault();
    const tab = event.key === 'Home' ? 'estacion' : event.key === 'End' ? 'ajustado' : this.tab() === 'estacion' ? 'ajustado' : 'estacion';
    this.selectTab(tab);
    (event.currentTarget as HTMLElement).parentElement?.querySelectorAll<HTMLButtonElement>('[role=tab]')[tab === 'estacion' ? 0 : 1].focus();
  }
  setSearch(value: string): void { this.search.set(value); this.dialog.set(null); }
  selectWorker(value: string): void { this.selectedWorker.set(value); this.dialog.set(null); }
  selectScenario(value: string): void { this.selectedScenario.set(value); this.dialog.set(null); }
  toggleWorker(id: string): void {
    this.dialog.set(null);
    this.excludedWorkers.update(excluded => excluded.includes(id) ? excluded.filter(value => value !== id) : [...excluded, id]);
  }
  openDownloads(): void {
    const range = this.range();
    if (!this.downloadDate.value && range && range.from === range.to && validDate(range.from)) {
      this.downloadDate.setValue(range.from);
    }
    this.dialog.set('downloads');
  }
  private invalidate(): void { this.error.set(''); this.dialog.set(null); this.requests$.next(null); }
  private scenarioId(s: ScenarioSale): string { return scenarioKey({ ...s, workerId: '' }); }
  private matches(s: ScenarioSale): boolean {
    const normalize = (value: string) => value.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLocaleLowerCase('es-CO').trim();
    return normalize(`${s.recordedName} ${scenarioLabel(s.scenario)}`).includes(normalize(this.search()));
  }
  private message(error: unknown): string {
    if (isApiError(error)) {
      if (error.status === 401) return 'La sesión ha caducado. Inicia sesión nuevamente (HTTP 401).';
      if (error.status === 403) return 'Se requiere autorización de administrador para consultar rendimiento (HTTP 403).';
      if (error.status === 404) return 'La API de rendimiento no está disponible en este servidor (HTTP 404).';
      if (error.status === 502) return 'No se pudieron leer o validar las fuentes de rendimiento (HTTP 502). Reintenta la consulta.';
    }
    return error instanceof Error ? error.message : 'No se pudo consultar el rendimiento.';
  }
}
