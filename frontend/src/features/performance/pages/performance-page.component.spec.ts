import { TestBed, ComponentFixture } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { BehaviorSubject, of, Subject, throwError } from 'rxjs';
import { vi } from 'vitest';
import { By } from '@angular/platform-browser';
import { PerformancePageComponent } from './performance-page.component';
import { SellerPerformanceService } from '../../../core/services/seller-performance.service';
import { DailyDownloadService } from '../../../core/services/daily-download.service';
import { DailyDownloadsComponent } from '../../../atomic-design/organisms/daily-downloads/daily-downloads.component';
import { StatisticsChartComponent } from '../../statistics/components/statistics-chart.component';
import { ApiError } from '../../../shared/models/api-error.model';
import { FullSellerPerformanceResponse, PerformanceQuery } from '../../../shared/models/dtos/seller-performance.dto';
import { fullPerformanceFixture, unavailablePerformanceFixture } from '../../../shared/helpers/performance-report.test-data';
import { parsePerformanceReport } from '../../../shared/helpers/seller-performance-reports.helpers';

describe('SCRUM-108 C-09 page', () => {
  let fixture: ComponentFixture<PerformancePageComponent>, page: PerformancePageComponent;
  let params: BehaviorSubject<ReturnType<typeof convertToParamMap>>;
  const api = { readReport: vi.fn() };
  const downloads = { downloadSalesSheet: vi.fn(), downloadLubricantsAndMisc: vi.fn() };
  const decode = (q: PerformanceQuery, data = fullPerformanceFixture(q)) => parsePerformanceReport(JSON.stringify(data), q);
  function create(): void { fixture = TestBed.createComponent(PerformancePageComponent); page = fixture.componentInstance; fixture.detectChanges(); }
  beforeEach(() => {
    params = new BehaviorSubject(convertToParamMap({}));
    api.readReport.mockReset().mockImplementation((q: PerformanceQuery) => of(decode(q)));
    downloads.downloadSalesSheet.mockReset().mockReturnValue(throwError(() => new ApiError(404, { detail: 'Archivo no disponible' })));
    TestBed.configureTestingModule({ imports: [PerformancePageComponent], providers: [provideRouter([]),
      { provide: ActivatedRoute, useValue: { queryParamMap: params } }, { provide: SellerPerformanceService, useValue: api },
      { provide: DailyDownloadService, useValue: downloads }
    ] });
  });
  it('renders the two prototype sections with observed scenarios, real factors and no combined ranking', () => {
    create();
    expect(fixture.nativeElement.querySelectorAll('[role=tab]')).toHaveLength(2);
    expect(fixture.nativeElement.textContent).toContain('100 % equivale al promedio');
    expect(page.chartSeries()).toHaveLength(2);
    expect(page.chartSeries()[0].points[0].value).toBe('120');
    page.selectTab('ajustado'); fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('1,5×');
    expect(fixture.nativeElement.textContent).toContain('approved-test-source');
    expect(fixture.nativeElement.textContent).toContain('Noche');
    expect(fixture.nativeElement.textContent).toContain('No hay ranking combinado');
    expect(page.chartSeries()[1].points[0].value).toBe('180');
    expect(fixture.nativeElement.textContent).not.toContain('EFICIENCIA DE DESPACHO');
  });
  it('consults exactly the journal operational date and offers a fixed safe return link', () => {
    params.next(convertToParamMap({ date: '2026-08-03', returnTo: 'jornada' })); create();
    expect(api.readReport).toHaveBeenCalledWith({ from: '2026-08-03', to: '2026-08-03', measure: 'FUEL_GALLONS', outputScale: 2 });
    expect(fixture.nativeElement.querySelector('a[href="/jornada/cerrada?date=2026-08-03"]')).not.toBeNull();
    params.next(convertToParamMap({ date: '2026-08-04', returnTo: 'https://invalid.example' })); fixture.detectChanges();
    expect(page.fromJournal()).toBe(false); expect(api.readReport).toHaveBeenLastCalledWith({ from: '2026-08-04', to: '2026-08-04', measure: 'FUEL_GALLONS', outputScale: 2 });
  });
  it('does not fall back to another date when the URL date is invalid', () => {
    params.next(convertToParamMap({ date: '2026-02-30' })); create();
    expect(api.readReport).not.toHaveBeenCalled(); expect(page.rangeError()).toContain('fecha de la jornada no es válida');
    page.form.controls.from.setValue('2026-08-03'); page.consult();
    expect(api.readReport).toHaveBeenCalledTimes(1);
  });
  it('submits selected ranges and metric and rejects reversed dates', () => {
    create(); page.form.patchValue({ mode: 'rango', from: '2026-08-01', to: '2026-08-31', measure: 'BASKET_COP' }); fixture.detectChanges();
    expect(page.data()).toBeNull(); fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
    expect(api.readReport).toHaveBeenLastCalledWith({ from: '2026-08-01', to: '2026-08-31', measure: 'BASKET_COP', outputScale: 2 });
    page.form.controls.to.setValue('2026-07-31'); page.consult();
    expect(api.readReport).toHaveBeenCalledTimes(2); expect(page.data()).toBeNull();
  });
  it('cancels stale requests on date changes and clears the chart and dialogs', () => {
    const old$ = new Subject<FullSellerPerformanceResponse>(); api.readReport.mockReturnValueOnce(old$); create();
    expect(page.loading()).toBe(true); const q = api.readReport.mock.calls[0][0] as PerformanceQuery;
    page.dialog.set('chart'); page.form.controls.month.setValue('2026-08');
    old$.next(decode(q)); expect(page.data()).toBeNull(); expect(page.chartSeries()).toEqual([]); expect(page.dialog()).toBeNull();
    page.consult(); expect(page.data()?.compliance.window.from).toBe('2026-08-01');
  });
  it('filters the already computed station population and recomputes visible charts without new HTTP requests', () => {
    create(); const station = page.comparisonRows()[0].stationAverage;
    page.setSearch('no existe'); expect(page.comparisonRows()).toEqual([]); expect(page.chartSeries()).toEqual([]);
    page.setSearch('angela'); expect(page.comparisonRows()[0].stationAverage).toBe(station);
    page.selectWorker('not-in-snapshot'); expect(page.comparisonRows()).toEqual([]);
    page.selectWorker(''); page.selectTab('ajustado'); const id = page.people()[0].workerId;
    page.toggleWorker(id); expect(page.normalizationRows()).toEqual([]); expect(page.chartSeries()).toEqual([]);
    page.toggleWorker(id); expect(page.normalizationRows()).toHaveLength(1); expect(api.readReport).toHaveBeenCalledTimes(1);
  });
  it.each([
    ['SHIFT_CLASSIFICATION_NOT_PROVIDED', 'Clasificación histórica de turnos no disponible.'],
    ['HISTORY_INCOMPLETE', 'El historial del período aún no está completo.'],
    ['NO_CONFIRMED_SELLER_CLOSES', 'No hay cierres de vendedor en el período confirmado.'],
    ['NEGATIVE_OBSERVATIONS_REQUIRE_POLICY', 'ajustes negativos']
  ])('shows %s with its own reason and no invented chart points', (reason, message) => {
    api.readReport.mockImplementation((q: PerformanceQuery) => of(decode(q, unavailablePerformanceFixture(q, reason)))); create();
    for (const tab of ['estacion', 'ajustado'] as const) {
      page.selectTab(tab); fixture.detectChanges();
      expect(page.chartSeries()).toEqual([]);
      expect(fixture.nativeElement.querySelector('.performance-page > [role=status]').textContent).toContain(message);
    }
    if (reason === 'HISTORY_INCOMPLETE') {
      expect(page.pending()).not.toHaveLength(0);
      expect(fixture.nativeElement.querySelector('.vendor-chips-selector')).toBeNull();
    }
  });
  it('keeps missing-factor rows but never graphs their normalized sales', () => {
    api.readReport.mockImplementation((q: PerformanceQuery) => {
      const data = fullPerformanceFixture(q); data.normalization.availability = 'UNAVAILABLE'; data.normalization.reason = 'INCOMPLETE_CALCULATIONS';
      Object.assign(data.normalization.sellers[0], { availability: 'UNAVAILABLE', reason: 'MULTIPLIER_NOT_PROVIDED', multiplier: null, multiplierSource: null, normalizedSales: null });
      return of(decode(q, data));
    });
    create(); page.selectTab('ajustado'); fixture.detectChanges();
    expect(page.normalizationRows()[0].sales).toBe('120'); expect(page.chartSeries()).toEqual([]);
    expect(fixture.nativeElement.textContent).toContain('Factor de ajuste no disponible');
  });
  it.each([0, 400, 401, 403, 404, 502])('preserves HTTP error %s and retries with clean data', status => {
    api.readReport.mockReturnValueOnce(throwError(() => new ApiError(status, { detail: 'Fallo de consulta' }))); create();
    expect(page.data()).toBeNull(); expect(page.error()).not.toBe(''); expect(page.loading()).toBe(false);
    page.consult(); expect(page.error()).toBe(''); expect(page.data()).not.toBeNull();
  });
  it('keeps the daily download date independent of the performance period and preserves real download capabilities', () => {
    params.next(convertToParamMap({ date: '2026-08-03', returnTo: 'jornada' })); create();
    page.openDownloads(); fixture.detectChanges();
    expect(fixture.debugElement.query(By.directive(DailyDownloadsComponent)).componentInstance.atrDate).toBe('2026-08-03');
    const buttons = fixture.nativeElement.querySelectorAll('app-daily-downloads button') as NodeListOf<HTMLButtonElement>;
    expect([...buttons].filter(b => b.disabled)).toHaveLength(3);
    fixture.nativeElement.querySelector('button[aria-label="Descargar PLANILLA VENTAS"]').click();
    expect(downloads.downloadSalesSheet).toHaveBeenCalledWith('2026-08-03');
    page.form.patchValue({ mode: 'mes', month: '2026-08' }); page.openDownloads(); fixture.detectChanges();
    expect(page.downloadDate.value).toBe('2026-08-03');
    page.downloadDate.setValue('2026-07-10'); fixture.detectChanges();
    expect(page.downloadError()).toBe('');
    expect(fixture.debugElement.query(By.directive(DailyDownloadsComponent)).componentInstance.atrDate).toBe('2026-07-10');
    const input = fixture.nativeElement.querySelector('dialog input[type=date]') as HTMLInputElement;
    expect(input.hasAttribute('min')).toBe(false); expect(input.hasAttribute('max')).toBe(false);
    fixture.nativeElement.querySelector('button[aria-label="Descargar PLANILLA VENTAS"]').click();
    expect(downloads.downloadSalesSheet).toHaveBeenLastCalledWith('2026-07-10');
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.daily-downloads-message--error')).not.toBeNull();
    page.downloadDate.setValue('2026-07-11'); fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.daily-downloads-message--error')).toBeNull();
    expect(fixture.debugElement.query(By.directive(DailyDownloadsComponent)).componentInstance.atrDate).toBe('2026-07-11');
    page.form.controls.month.setValue(''); fixture.detectChanges();
    expect(page.rangeError()).not.toBe('');
    const openButton = [...fixture.nativeElement.querySelectorAll('button') as NodeListOf<HTMLButtonElement>]
      .find(b => b.textContent?.trim() === 'Balance y descargas')!;
    expect(openButton.disabled).toBe(false); openButton.click(); fixture.detectChanges();
    expect(page.downloadDate.value).toBe('2026-07-11');
    expect(fixture.nativeElement.querySelector('dialog').textContent).toContain('independiente del período de rendimiento');
    page.downloadDate.setValue(''); fixture.detectChanges();
    expect(fixture.debugElement.query(By.directive(DailyDownloadsComponent))).toBeNull();
  });
  it('requires an explicit download date for a monthly query, even when performance is unavailable', () => {
    create(); page.openDownloads(); fixture.detectChanges();
    expect(page.downloadError()).toContain('Selecciona una fecha operativa');
    expect(fixture.debugElement.query(By.directive(DailyDownloadsComponent))).toBeNull();
    page.downloadDate.setValue('2026-02-30'); fixture.detectChanges();
    expect(fixture.debugElement.query(By.directive(DailyDownloadsComponent))).toBeNull();
    expect(downloads.downloadSalesSheet).not.toHaveBeenCalled();
  });
  it('uses the existing chart viewer and keeps statistical KPI behavior opt-in', () => {
    create(); page.dialog.set('chart'); fixture.detectChanges();
    expect(fixture.nativeElement.querySelectorAll('app-statistics-chart')).toHaveLength(1);
    expect(fixture.nativeElement.querySelector('dialog')).not.toBeNull();
    const chart = fixture.debugElement.query(By.directive(StatisticsChartComponent)).componentInstance as StatisticsChartComponent;
    expect(chart.showIndicators()).toBe(false); expect(chart.description()).toContain('escenario');
    page.selectTab('ajustado'); expect(page.dialog()).toBeNull();
  });
});
