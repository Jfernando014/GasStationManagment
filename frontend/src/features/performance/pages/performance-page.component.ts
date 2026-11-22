import { ChangeDetectionStrategy, Component, computed, effect, inject, signal, untracked } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Subject, catchError, debounce, defer, forkJoin, map, of, switchMap, timer } from 'rxjs';

import { ActionButtonDirective } from '../../../atomic-design/atoms/action-button/action-button.directive';
import { EmptyStateComponent } from '../../../atomic-design/molecules/empty-state/empty-state.component';
import { TabsComponent, TabItem } from '../../../atomic-design/molecules/tabs/tabs.component';
import { WarningPanelComponent } from '../../../atomic-design/molecules/warning-panel/warning-panel.component';
import { DailyDownloadsComponent } from '../../../atomic-design/organisms/daily-downloads/daily-downloads.component';
import { DayStaffService } from '../../../core/services/day-staff.service';
import { SellerPerformanceService } from '../../../core/services/seller-performance.service';
import { isApiError } from '../../../shared/helpers/problem-details.helpers';
import { localDate, monthRange, validDate } from '../../../shared/helpers/sales-statistics.helpers';
import { monthOptions } from '../../../shared/helpers/month-options.helpers';
import { performanceRangeError } from '../../../shared/helpers/seller-performance.helpers';
import {
  FullSellerPerformanceResponse, PerformanceCoverage, PerformanceMeasure, PerformanceQuery
} from '../../../shared/models/dtos/seller-performance.dto';
import { StatisticsDialogComponent } from '../../statistics/components/statistics-dialog.component';
import { DaySalesCardComponent } from '../components/day-sales-card/day-sales-card.component';
import { DivergentIndexChartComponent } from '../components/divergent-index-chart/divergent-index-chart.component';
import { NormalizedSalesTableComponent } from '../components/normalized-sales-table/normalized-sales-table.component';
import { PerformanceFiltersComponent } from '../components/performance-filters/performance-filters.component';
import { SellerComparisonTableComponent } from '../components/seller-comparison-table/seller-comparison-table.component';
import {
  buildDaySales, buildIndexChart, buildSellerViews, dataRangeLabel, longDayLabel, plainReason, unitText
} from '../helpers/performance-view.helpers';
import { HISTORY_START, PerformanceMode, PerformanceTab } from '../models/performance-view.model';

/** Wait this long after the last change of a filter before asking the server again. */
const FILTER_DELAY_MS = 250;

interface PerformanceRequest {
  query: PerformanceQuery;
  mode: PerformanceMode;
  date: string;
  /** Changes with every "Reintentar", so the same query can be asked again. */
  attempt: number;
}

/** What the page is showing: the answer and the filters it was asked with, so a dimmed old answer stays coherent. */
interface PerformanceView {
  data: FullSellerPerformanceResponse;
  mode: PerformanceMode;
  measure: PerformanceMeasure;
  date: string;
  resting: { workerId: string; name: string }[];
}

interface PerformanceNotice {
  title: string;
  text: string;
  pending: PerformanceCoverage[];
}

const normalize = (value: string) => value.normalize('NFD').replace(/[̀-ͯ]/g, '').toLocaleLowerCase('es-CO').trim();

/**
 * Rendimiento: how much each seller sells compared with the station on the same days, shifts and role. The filters
 * apply by themselves; while a new answer arrives the previous one stays on screen, dimmed.
 */
@Component({
  selector: 'app-performance-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule, RouterLink, ActionButtonDirective, EmptyStateComponent, TabsComponent, WarningPanelComponent, DailyDownloadsComponent,
    StatisticsDialogComponent, PerformanceFiltersComponent, DivergentIndexChartComponent, SellerComparisonTableComponent,
    DaySalesCardComponent, NormalizedSalesTableComponent
  ],
  templateUrl: './performance-page.component.html',
  styleUrl: './performance-page.component.css'
})
export class PerformancePageComponent {
  private readonly api = inject(SellerPerformanceService);
  private readonly dayStaff = inject(DayStaffService);
  private readonly route = inject(ActivatedRoute);
  private readonly requests$ = new Subject<PerformanceRequest | null>();

  readonly today = localDate();
  readonly monthOptions = monthOptions(HISTORY_START.slice(0, 7), this.today.slice(0, 7));
  protected readonly tabs: TabItem[] = [
    { id: 'estacion', label: 'Frente a la estación' }, { id: 'ajustado', label: 'Ajustado por turno' }
  ];

  readonly mode = signal<PerformanceMode>('mes');
  readonly date = signal(this.today);
  readonly month = signal(this.today.slice(0, 7));
  readonly measure = signal<PerformanceMeasure>('FUEL_GALLONS');
  readonly tab = signal<PerformanceTab>('estacion');
  readonly search = signal('');
  readonly excludedWorkers = signal<string[]>([]);
  readonly dialog = signal<'downloads' | null>(null);

  readonly shown = signal<PerformanceView | null>(null);
  readonly loading = signal(false);
  readonly error = signal('');
  readonly routeError = signal('');
  readonly fromJournal = signal(false);
  readonly journalDate = signal<string | null>(null);
  /** Changes with every new answer, so the bars and rows come in again. */
  readonly version = signal(0);
  private readonly attempt = signal(0);

  readonly downloadDate = new FormControl('', { nonNullable: true });
  readonly downloadDateValue = toSignal(this.downloadDate.valueChanges, { initialValue: '' });
  readonly downloadError = computed(() => validDate(this.downloadDateValue())
    ? '' : 'Selecciona una fecha operativa para las descargas.');

  /** Days the question covers. The month in progress stops today: the days that have not happened do not count. */
  readonly range = computed<{ from: string; to: string } | null>(() => {
    switch (this.mode()) {
      case 'dia': return { from: this.date(), to: this.date() };
      case 'historial': return { from: HISTORY_START, to: this.today };
      default:
        try {
          const range = monthRange(this.month());
          return { from: range.from, to: range.to > this.today ? this.today : range.to };
        } catch { return null; }
    }
  });
  readonly rangeError = computed(() => {
    const range = this.range();
    return this.routeError() || (range ? performanceRangeError(range.from, range.to) : 'Selecciona un mes válido.');
  });
  private readonly request = computed<PerformanceRequest | null>(() => {
    const range = this.range();
    if (this.rangeError() || !range) return null;
    return {
      query: { ...range, measure: this.measure(), outputScale: 2 }, mode: this.mode(), date: this.date(), attempt: this.attempt()
    };
  });

  readonly loadingFirst = computed(() => this.loading() && !this.shown());
  readonly busy = computed(() => this.loading() && !!this.shown());
  readonly unit = computed(() => unitText(this.shown()?.measure ?? this.measure()));
  readonly rangeLabel = computed(() => {
    const view = this.shown();
    return view ? dataRangeLabel(view.data.compliance.coverage) : null;
  });
  readonly dayLabel = computed(() => longDayLabel(this.shown()?.date ?? this.date()));
  readonly noCloses = computed(() => !(this.shown()?.data.compliance.sellers.length ?? 0));

  private readonly allViews = computed(() => {
    const view = this.shown();
    return view && view.mode !== 'dia'
      ? buildSellerViews(view.data.compliance.sellers, view.data.comparison.sellers, view.measure) : [];
  });
  readonly sellerViews = computed(() => this.allViews().filter(item => this.matches(item.name)));
  readonly chart = computed(() => {
    const shownIds = new Set(this.sellerViews().map(item => item.workerId));
    const chart = buildIndexChart(this.allViews());
    return { ...chart, bars: chart.bars.filter(bar => shownIds.has(bar.workerId)) };
  });
  private readonly day = computed(() => {
    const view = this.shown();
    return view && view.mode === 'dia'
      ? buildDaySales(view.data.compliance.sellers, view.data.comparison.sellers, view.measure, view.resting)
      : { rows: [], total: '' };
  });
  readonly dayRows = computed(() => this.day().rows.filter(row => this.matches(row.name)));
  readonly dayTotal = computed(() => this.day().total);
  readonly normalizedRows = computed(() => (this.shown()?.data.normalization.sellers ?? [])
    .filter(row => this.matches(row.recordedName) && !this.excludedWorkers().includes(row.workerId)));
  readonly people = computed(() => this.shown()?.data.compliance.sellers ?? []);
  readonly noMatches = computed(() => this.search().trim() !== '' && !this.noCloses());

  /** Warning about data that is missing, written for the person who reads the page; pending dates fold inside it. */
  readonly notice = computed<PerformanceNotice | null>(() => {
    const view = this.shown();
    if (!view) return null;
    const dayStation = view.mode === 'dia' && this.tab() === 'estacion';
    const report = this.tab() === 'ajustado' ? view.data.normalization
      : dayStation ? view.data.compliance : view.data.comparison;
    const pending = report.coverage.filter(item => item.status === 'PENDING' || item.status === 'ABSENT');
    let title = '', text = '';
    if (!dayStation && report.availability === 'PARTIAL') {
      title = 'Resultado parcial.';
      text = plainReason(report.reason) || 'Faltan algunos datos, así que lo que ves puede cambiar.';
    } else if (!dayStation && report.availability === 'UNAVAILABLE') {
      title = 'Todavía no se puede calcular.';
      text = plainReason(report.reason) || 'Faltan datos para este período.';
    } else if (pending.length) {
      title = pending.length === 1 ? 'Hay 1 fecha sin confirmar.' : `Hay ${pending.length} fechas sin confirmar.`;
      text = 'Todavía no están procesadas o no tienen historial, así que no cuentan en lo que ves.';
    }
    return title ? { title, text, pending } : null;
  });

  constructor() {
    this.requests$.pipe(
      debounce(request => timer(request ? FILTER_DELAY_MS : 0)),
      switchMap(request => request ? this.load(request) : of(null)),
      takeUntilDestroyed()
    ).subscribe(result => {
      this.loading.set(false);
      if (!result) { this.shown.set(null); return; }
      this.error.set(result.error);
      this.shown.set(result.data ? {
        data: result.data, mode: result.request.mode, measure: result.request.query.measure,
        date: result.request.date, resting: result.resting
      } : null);
      if (result.data) this.version.update(value => value + 1);
    });

    // Every change of a filter asks again; a new question cancels the one still on its way.
    effect(() => {
      const request = this.request();
      untracked(() => {
        this.error.set('');
        this.loading.set(request !== null);
        this.dialog.set(null);
        this.requests$.next(request);
      });
    });

    this.route.queryParamMap.pipe(takeUntilDestroyed()).subscribe(params => {
      const date = params.get('date');
      const valid = date !== null && validDate(date);
      this.mode.set(valid ? 'dia' : 'mes');
      if (valid) this.date.set(date);
      this.fromJournal.set(params.get('returnTo') === 'jornada' && valid);
      this.journalDate.set(this.fromJournal() ? date : null);
      this.routeError.set(date !== null && !valid ? 'La fecha de la jornada no es válida. Selecciona un período válido.' : '');
    });
  }

  setMode(mode: PerformanceMode): void {
    this.routeError.set('');
    // From a day to "Mes" the month of that day stays selected, so the person does not lose their place.
    const month = this.date().slice(0, 7);
    if (mode === 'mes' && this.mode() === 'dia' && this.monthOptions.some(option => option.value === month)) this.month.set(month);
    this.mode.set(mode);
  }
  setDate(date: string): void { this.routeError.set(''); this.date.set(date); }
  setMonth(month: string): void { this.routeError.set(''); this.month.set(month); }
  setMeasure(measure: PerformanceMeasure): void { this.measure.set(measure); }
  setSearch(value: string): void { this.search.set(value); }
  selectTab(id: string): void { this.tab.set(id === 'ajustado' ? 'ajustado' : 'estacion'); }
  toggleWorker(id: string): void {
    this.excludedWorkers.update(excluded => excluded.includes(id) ? excluded.filter(value => value !== id) : [...excluded, id]);
  }
  retry(): void { this.attempt.update(value => value + 1); }

  openDownloads(): void {
    const range = this.range();
    if (!this.downloadDate.value && range && range.from === range.to && validDate(range.from)) {
      this.downloadDate.setValue(range.from);
    }
    this.dialog.set('downloads');
  }

  private matches(name: string): boolean {
    return normalize(name).includes(normalize(this.search()));
  }

  /** Asks for the report and, for a single day, who rested that day. A failure to read the rest never hides the report. */
  private load(request: PerformanceRequest) {
    const resting$ = request.mode === 'dia'
      ? this.dayStaff.getStep(request.date).pipe(
        map(step => step.resting.map(person => ({ workerId: person.workerId, name: person.fullName }))),
        catchError(() => of([] as { workerId: string; name: string }[])))
      : of([] as { workerId: string; name: string }[]);
    const report$ = defer(() => this.api.readReport(request.query)).pipe(
      map(data => ({ data: data as FullSellerPerformanceResponse | null, error: '' })),
      catchError((error: unknown) => of({ data: null as FullSellerPerformanceResponse | null, error: this.message(error) })));
    return forkJoin({ report: report$, resting: resting$ }).pipe(
      map(({ report, resting }) => ({ data: report.data, error: report.error, resting, request })));
  }

  private message(error: unknown): string {
    if (isApiError(error)) {
      if (error.status === 401) return 'La sesión ha caducado. Inicia sesión nuevamente.';
      if (error.status === 403) return 'Se requiere autorización de administrador para consultar el rendimiento.';
      if (error.status === 404) return 'La consulta de rendimiento no está disponible en este servidor.';
      if (error.status === 502) return 'No se pudieron leer las fuentes de rendimiento. Intenta de nuevo.';
    }
    return error instanceof Error ? error.message : 'No se pudo consultar el rendimiento.';
  }
}
