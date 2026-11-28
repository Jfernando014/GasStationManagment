import { TestBed, ComponentFixture } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { BehaviorSubject, of, Subject, throwError } from 'rxjs';
import { afterEach, vi } from 'vitest';
import { By } from '@angular/platform-browser';
import { PerformancePageComponent } from './performance-page.component';
import { SellerPerformanceService } from '../../../core/services/seller-performance.service';
import { DayStaffService } from '../../../core/services/day-staff.service';
import { DailyDownloadService } from '../../../core/services/daily-download.service';
import { DailyDownloadsComponent } from '../../../atomic-design/organisms/daily-downloads/daily-downloads.component';
import { ApiError } from '../../../shared/models/api-error.model';
import { FullSellerPerformanceResponse, PerformanceQuery } from '../../../shared/models/dtos/seller-performance.dto';
import { fullPerformanceFixture, unavailablePerformanceFixture } from '../../../shared/helpers/performance-report.test-data';
import { parsePerformanceReport } from '../../../shared/helpers/seller-performance-reports.helpers';
import { HISTORY_START } from '../models/performance-view.model';

describe('SCRUM-108 C-09 page', () => {
  let fixture: ComponentFixture<PerformancePageComponent>, page: PerformancePageComponent;
  let params: BehaviorSubject<ReturnType<typeof convertToParamMap>>;
  const api = { readReport: vi.fn() };
  const dayStaff = { getStep: vi.fn() };
  const downloads = {
    downloadSalesSheet: vi.fn(),
    downloadLubricantsAndMisc: vi.fn(),
    downloadVariation: vi.fn(),
    downloadDominusComplement: vi.fn()
  };
  const decode = (q: PerformanceQuery, data = fullPerformanceFixture(q)) => parsePerformanceReport(JSON.stringify(data), q);
  const text = () => fixture.nativeElement.textContent as string;
  /** The filters ask the server 250 ms after the last change: let that time pass and draw the answer. */
  function settle(): void { fixture.detectChanges(); vi.advanceTimersByTime(250); fixture.detectChanges(); }
  function create(): void { fixture = TestBed.createComponent(PerformancePageComponent); page = fixture.componentInstance; settle(); }
  beforeEach(() => {
    vi.useFakeTimers();
    params = new BehaviorSubject(convertToParamMap({}));
    api.readReport.mockReset().mockImplementation((q: PerformanceQuery) => of(decode(q)));
    dayStaff.getStep.mockReset().mockReturnValue(of({ resting: [] }));
    downloads.downloadSalesSheet.mockReset().mockReturnValue(throwError(() => new ApiError(404, { detail: 'Archivo no disponible' })));
    TestBed.configureTestingModule({ imports: [PerformancePageComponent], providers: [provideRouter([]),
      { provide: ActivatedRoute, useValue: { queryParamMap: params } }, { provide: SellerPerformanceService, useValue: api },
      { provide: DayStaffService, useValue: dayStaff }, { provide: DailyDownloadService, useValue: downloads }
    ] });
  });
  afterEach(() => { vi.useRealTimers(); });
  it('renders the two sections with the index against the station, real factors and no combined ranking', () => {
    create();
    expect(fixture.nativeElement.querySelectorAll('[role=tab]')).toHaveLength(2);
    expect(page.mode()).toBe('mes');
    expect(text()).toContain('Índice frente a la estación');
    expect(text()).toContain('¿Y por qué?');
    expect(page.chart().bars).toHaveLength(1);
    expect(page.sellerViews()[0].indexText).toBe('100,0 %');
    expect(fixture.nativeElement.querySelector('[aria-label="Ángela Pérez: 100,0 % frente a la estación"]')).not.toBeNull();
    page.selectTab('ajustado'); fixture.detectChanges();
    expect(text()).toContain('1,5 ×');
    expect(text()).toContain('approved-test-source');
    expect(text()).toContain('de noche');
    expect(text()).toContain('No hay un puesto general entre turnos');
    expect(text()).not.toContain('EFICIENCIA DE DESPACHO');
  });
  it('consults exactly the journal operational date and offers a fixed safe return link', () => {
    params.next(convertToParamMap({ date: '2026-08-03', returnTo: 'jornada' })); create();
    expect(page.mode()).toBe('dia');
    expect(api.readReport).toHaveBeenCalledWith({ from: '2026-08-03', to: '2026-08-03', measure: 'FUEL_GALLONS', outputScale: 2 });
    expect(fixture.nativeElement.querySelector('a[href="/jornada/cerrada?date=2026-08-03"]')).not.toBeNull();
    expect(text()).toContain('En un solo día no hay con qué comparar');
    expect(text()).toContain('Ventas de Lunes 3 ago 2026');
    params.next(convertToParamMap({ date: '2026-08-04', returnTo: 'https://invalid.example' })); settle();
    expect(page.fromJournal()).toBe(false); expect(api.readReport).toHaveBeenLastCalledWith({ from: '2026-08-04', to: '2026-08-04', measure: 'FUEL_GALLONS', outputScale: 2 });
  });
  it('does not fall back to another date when the URL date is invalid', () => {
    params.next(convertToParamMap({ date: '2026-02-30' })); create();
    expect(api.readReport).not.toHaveBeenCalled(); expect(page.rangeError()).toContain('fecha de la jornada no es válida');
    page.setMode('dia'); page.setDate('2026-08-03'); settle();
    expect(api.readReport).toHaveBeenCalledTimes(1);
  });
  it('applies the filters by itself, once after the last change, and the history starts at the first confirmed day', () => {
    create(); api.readReport.mockClear();
    page.setMode('historial'); page.setMeasure('BASKET_COP'); settle();
    expect(api.readReport).toHaveBeenCalledTimes(1);
    expect(api.readReport).toHaveBeenLastCalledWith({ from: HISTORY_START, to: page.today, measure: 'BASKET_COP', outputScale: 2 });
    page.setMode('mes'); page.setMonth('2026-08'); settle();
    expect(api.readReport).toHaveBeenLastCalledWith({ from: '2026-08-01', to: '2026-08-31', measure: 'BASKET_COP', outputScale: 2 });
    expect(api.readReport).toHaveBeenCalledTimes(2);
    page.setMonth(page.today.slice(0, 7)); settle();
    expect(api.readReport.mock.lastCall![0].to).toBe(page.today);
  });
  it('does not ask when the period is invalid and shows why', () => {
    create(); api.readReport.mockClear();
    page.setMonth(''); settle();
    expect(api.readReport).not.toHaveBeenCalled(); expect(page.rangeError()).not.toBe(''); expect(page.shown()).toBeNull();
  });
  it('cancels stale requests when a filter changes and keeps the newest answer', () => {
    const old$ = new Subject<FullSellerPerformanceResponse>(); api.readReport.mockReturnValueOnce(old$); create();
    expect(page.loading()).toBe(true); const q = api.readReport.mock.calls[0][0] as PerformanceQuery;
    page.setMonth('2026-08'); settle();
    old$.next(decode(q));
    expect(page.shown()?.data.compliance.window.from).toBe('2026-08-01'); expect(page.loading()).toBe(false);
  });
  it('keeps the previous answer on screen, dimmed and busy, while the next one arrives', () => {
    create(); const next$ = new Subject<FullSellerPerformanceResponse>(); api.readReport.mockReturnValueOnce(next$);
    page.setMonth('2026-08'); settle();
    expect(page.busy()).toBe(true);
    expect(fixture.nativeElement.querySelector('.panel').classList.contains('is-busy')).toBe(true);
    expect(fixture.nativeElement.querySelector('.panel').getAttribute('aria-busy')).toBe('true');
    expect(page.chart().bars).toHaveLength(1);
    next$.next(decode({ from: '2026-08-01', to: '2026-08-31', measure: 'FUEL_GALLONS', outputScale: 2 })); next$.complete(); fixture.detectChanges();
    expect(page.busy()).toBe(false);
  });
  it('filters the already computed station population and recomputes without new HTTP requests', () => {
    create();
    page.setSearch('no existe'); expect(page.sellerViews()).toEqual([]); expect(page.chart().bars).toEqual([]);
    fixture.detectChanges(); expect(text()).toContain('Sin coincidencias');
    page.setSearch('angela'); expect(page.sellerViews()).toHaveLength(1);
    page.selectTab('ajustado'); const id = page.people()[0].workerId;
    page.toggleWorker(id); expect(page.normalizedRows()).toEqual([]);
    page.toggleWorker(id); expect(page.normalizedRows()).toHaveLength(1); expect(api.readReport).toHaveBeenCalledTimes(1);
  });
  it('lists who rested on a single day, dimmed, without breaking when that list fails', () => {
    dayStaff.getStep.mockReturnValue(of({ resting: [{ workerId: 'rest-1', fullName: 'Marta Descanso' }] }));
    params.next(convertToParamMap({ date: '2026-08-03' })); create();
    expect(dayStaff.getStep).toHaveBeenCalledWith('2026-08-03');
    expect(fixture.nativeElement.querySelector('.is-resting')?.textContent).toContain('Marta Descanso');
    dayStaff.getStep.mockReturnValue(throwError(() => new ApiError(500, { detail: 'Fallo' })));
    params.next(convertToParamMap({ date: '2026-08-04' })); settle();
    expect(fixture.nativeElement.querySelector('.is-resting')).toBeNull();
    expect(text()).toContain('Ángela Pérez');
  });
  it('warns when a seller has a shift and no sales, with a link to that closing', () => {
    api.readReport.mockImplementation((q: PerformanceQuery) => {
      // Edited after decoding: the point is how the page draws a seller with a shift and no sales.
      const data = decode(q); data.compliance.sellers[0].sales = '0'; data.comparison.sellers[0].sales = '0';
      return of(data);
    });
    params.next(convertToParamMap({ date: '2026-08-03' })); create();
    expect(text()).toContain('Ángela Pérez no tiene ventas registradas en su turno NOCHE');
    expect(fixture.nativeElement.querySelector('a[href="/jornada/cerrada?date=2026-08-03"]')).not.toBeNull();
  });
  it.each([
    ['SHIFT_CLASSIFICATION_NOT_PROVIDED', 'falta la clasificación de los turnos'],
    ['HISTORY_INCOMPLETE', 'faltan jornadas por procesar'],
    ['NEGATIVE_OBSERVATIONS_REQUIRE_POLICY', 'ajustes negativos']
  ])('shows %s with its own reason in a warning and no invented bars', (reason, message) => {
    api.readReport.mockImplementation((q: PerformanceQuery) => of(decode(q, unavailablePerformanceFixture(q, reason)))); create();
    for (const tab of ['estacion', 'ajustado'] as const) {
      page.selectTab(tab); fixture.detectChanges();
      expect(page.chart().bars).toEqual([]);
      expect(fixture.nativeElement.querySelector('app-warning-panel').textContent).toContain(message);
    }
    if (reason === 'HISTORY_INCOMPLETE') {
      expect(page.notice()?.pending).not.toHaveLength(0);
      expect(fixture.nativeElement.querySelector('.chip')).toBeNull();
    }
  });
  it('says there are no closed days instead of inventing zeros', () => {
    api.readReport.mockImplementation((q: PerformanceQuery) => of(decode(q, unavailablePerformanceFixture(q, 'NO_CONFIRMED_SELLER_CLOSES')))); create();
    expect(page.noCloses()).toBe(true); expect(page.notice()).toBeNull();
    expect(text()).toContain('No hay jornadas cerradas en este período');
  });
  it('lists a seller without comparable shifts as "Sin datos suficientes" and keeps them out of the chart', () => {
    api.readReport.mockImplementation((q: PerformanceQuery) => of(decode(q, unavailablePerformanceFixture(q))));
    create();
    expect(page.chart().bars).toEqual([]); expect(page.sellerViews()[0].hasData).toBe(false);
    expect(text()).toContain('Sin datos suficientes');
    expect(page.notice()?.title).toBe('Todavía no se puede calcular.');
  });
  it('keeps missing-factor rows with their reason and never an adjusted number', () => {
    api.readReport.mockImplementation((q: PerformanceQuery) => {
      const data = fullPerformanceFixture(q); data.normalization.availability = 'UNAVAILABLE'; data.normalization.reason = 'INCOMPLETE_CALCULATIONS';
      Object.assign(data.normalization.sellers[0], { availability: 'UNAVAILABLE', reason: 'MULTIPLIER_NOT_PROVIDED', multiplier: null, multiplierSource: null, normalizedSales: null });
      return of(decode(q, data));
    });
    create(); page.selectTab('ajustado'); fixture.detectChanges();
    expect(page.normalizedRows()[0].sales).toBe('120');
    expect(text()).toContain('falta el factor de ajuste de este turno');
  });
  it.each([0, 400, 401, 403, 404, 502])('preserves HTTP error %s and retries with clean data', status => {
    api.readReport.mockReturnValueOnce(throwError(() => new ApiError(status, { detail: 'Fallo de consulta' }))); create();
    expect(page.shown()).toBeNull(); expect(page.error()).not.toBe(''); expect(page.loading()).toBe(false);
    expect(text()).toContain('Reintentar');
    page.retry(); settle(); expect(page.error()).toBe(''); expect(page.shown()).not.toBeNull();
  });
  it('keeps the daily download date independent of the performance period and preserves real download capabilities', () => {
    params.next(convertToParamMap({ date: '2026-08-03', returnTo: 'jornada' })); create();
    page.openDownloads(); fixture.detectChanges();
    expect(fixture.debugElement.query(By.directive(DailyDownloadsComponent)).componentInstance.atrDate).toBe('2026-08-03');
    const buttons = fixture.nativeElement.querySelectorAll('app-daily-downloads button') as NodeListOf<HTMLButtonElement>;
    expect([...buttons].filter(b => b.disabled)).toHaveLength(1);
    fixture.nativeElement.querySelector('button[aria-label="Descargar PLANILLA VENTAS"]').click();
    expect(downloads.downloadSalesSheet).toHaveBeenCalledWith('2026-08-03');
    page.dialog.set(null); page.setMode('mes'); page.setMonth('2026-08'); settle(); page.openDownloads(); fixture.detectChanges();
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
    page.dialog.set(null); page.setMonth(''); settle();
    expect(page.rangeError()).not.toBe('');
    const openButton = [...fixture.nativeElement.querySelectorAll('button') as NodeListOf<HTMLButtonElement>]
      .find(b => b.textContent?.trim() === 'Descargar')!;
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
});
