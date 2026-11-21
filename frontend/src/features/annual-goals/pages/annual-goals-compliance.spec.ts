import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Subject, of, throwError } from 'rxjs';
import { vi } from 'vitest';
import { By } from '@angular/platform-browser';
import { SearchFieldComponent } from '../../../atomic-design/atoms/search-field/search-field.component';
import { AnnualGoalsPageComponent } from './annual-goals-page.component';
import { SalesTargetsService } from '../../../core/services/sales-targets.service';
import { SellerPerformanceService } from '../../../core/services/seller-performance.service';
import { PerformanceQuery, SellerPerformanceResponse } from '../../../shared/models/dtos/seller-performance.dto';
import { emptyPerformanceFixture, missingTargetSeller, performanceFixture } from '../../../shared/helpers/seller-performance.test-data';
import { parseSellerPerformance } from '../../../shared/helpers/seller-performance.helpers';
import { ApiError } from '../../../shared/models/api-error.model';

describe('SCRUM-108 C-14 compliance tab', () => {
  let fixture: ComponentFixture<AnnualGoalsPageComponent>, page: AnnualGoalsPageComponent;
  const api = { read: vi.fn() };
  const targets = { list: vi.fn(), create: vi.fn() };
  const decode = (data: ReturnType<typeof performanceFixture>, query: PerformanceQuery) => parseSellerPerformance(JSON.stringify(data), query);
  function create(): void {
    fixture = TestBed.createComponent(AnnualGoalsPageComponent);
    page = fixture.componentInstance;
    fixture.detectChanges();
  }
  const panel = () => fixture.nativeElement.querySelector('#goals-panel-cumplimiento') as HTMLElement;
  const rows = () => Array.from(panel().querySelectorAll<HTMLTableRowElement>('.compliance-seller'));
  beforeEach(() => {
    api.read.mockReset().mockImplementation((query: PerformanceQuery) => of(decode(performanceFixture(query), query)));
    targets.list.mockReset().mockReturnValue(of([])); targets.create.mockReset();
    TestBed.configureTestingModule({ imports: [AnnualGoalsPageComponent], providers: [
      { provide: SalesTargetsService, useValue: targets }, { provide: SellerPerformanceService, useValue: api }
    ] });
  });
  it('automatically reads the selected month from confirmed history without manual uploads', () => {
    create();
    expect(api.read).toHaveBeenCalledTimes(1);
    expect(api.read.mock.calls[0][0]).toMatchObject({ measure: 'FUEL_GALLONS', outputScale: 2 });
    expect(panel().textContent).toContain('Ángela Pérez');
    expect(panel().textContent).toContain('120 %');
    expect(panel().textContent).toContain('Meta alcanzada');
    expect(targets.create).not.toHaveBeenCalled();
    expect(fixture.nativeElement.querySelector('input[type=file]')).toBeNull();
    expect(page.complianceSales()).toBe('120');
  });
  it('submits day, leap-year month and explicit week ranges without guessing week boundaries', () => {
    create();
    page.setComplianceMode('dia'); page.complianceDate.setValue('2026-08-03'); page.consultCompliance();
    expect(api.read).toHaveBeenLastCalledWith({ from: '2026-08-03', to: '2026-08-03', measure: 'FUEL_GALLONS', outputScale: 2 });
    page.setComplianceMode('semana'); page.complianceTo.setValue('2026-08-08');
    fixture.detectChanges();
    expect(panel().textContent).toContain('definición de semana');
    expect(page.compliance()).toBeNull();
    page.consultCompliance();
    expect(api.read).toHaveBeenLastCalledWith({ from: '2026-08-03', to: '2026-08-08', measure: 'FUEL_GALLONS', outputScale: 2 });
    page.setComplianceMode('mes'); page.complianceMonth.setValue('2028-02'); page.consultCompliance();
    expect(api.read).toHaveBeenLastCalledWith({ from: '2028-02-01', to: '2028-02-29', measure: 'FUEL_GALLONS', outputScale: 2 });
  });
  it('uses CONSULTAR and refuses invalid dates/ranges before contacting the API', () => {
    create();
    page.complianceMonth.setValue('2026-08'); fixture.detectChanges();
    const form = panel().querySelector('form') as HTMLFormElement;
    form.dispatchEvent(new Event('submit', { bubbles: true, cancelable: true })); fixture.detectChanges();
    expect(api.read).toHaveBeenCalledTimes(2);
    page.setComplianceMode('semana'); page.complianceDate.setValue('2026-08-04'); page.complianceTo.setValue('2026-08-03');
    page.consultCompliance(); fixture.detectChanges();
    expect(api.read).toHaveBeenCalledTimes(2);
    expect(panel().querySelector<HTMLButtonElement>('button[type=submit]')?.disabled).toBe(true);
    expect(page.compliance()).toBeNull();
    page.setComplianceMode('mes'); page.complianceMonth.setValue(''); page.consultCompliance();
    expect(api.read).toHaveBeenCalledTimes(2);
  });
  it('cancels old requests on input changes and retry; never restores an old-period result', () => {
    const old$ = new Subject<SellerPerformanceResponse>(), fresh$ = new Subject<SellerPerformanceResponse>();
    api.read.mockReturnValueOnce(old$).mockReturnValueOnce(fresh$);
    create();
    const oldQuery = api.read.mock.calls[0][0] as PerformanceQuery;
    expect(page.complianceLoading()).toBe(true);
    expect(panel().textContent).toContain('Consultando cumplimiento');
    page.complianceMonth.setValue('2026-08');
    old$.next(decode(performanceFixture(oldQuery), oldQuery));
    expect(page.compliance()).toBeNull(); expect(page.complianceLoading()).toBe(false);
    page.consultCompliance();
    const freshQuery = api.read.mock.calls[1][0] as PerformanceQuery;
    fresh$.next(decode(performanceFixture(freshQuery), freshQuery));
    expect(page.compliance()?.compliance.window.from).toBe('2026-08-01');
    expect(page.complianceLoading()).toBe(false);
    page.complianceMonth.setValue('2026-09');
    expect(page.complianceSales()).toBeNull();
  });
  it('keeps partial and unavailable rows, zero sales, backend rank and unfiltered totals', () => {
    api.read.mockImplementation((query: PerformanceQuery) => {
      const data = performanceFixture(query);
      data.compliance.sellers.push(missingTargetSeller()); data.compliance.availability = 'PARTIAL';
      data.compliance.reason = 'INCOMPLETE_CALCULATIONS';
      return of(decode(data, query));
    });
    create();
    expect(rows()).toHaveLength(2);
    expect(rows()[1].textContent).toContain('0 gal');
    expect(rows()[1].textContent).toContain('Meta individual no disponible');
    expect(rows()[1].querySelector('app-percent-bar')).toBeNull();
    expect(panel().textContent).toContain('Disponibilidad parcial');
    page.sellerSearch.set('historico'); fixture.detectChanges();
    expect(rows()).toHaveLength(1); expect(page.complianceSales()).toBe('120');
    page.sellerSearch.set('angela'); fixture.detectChanges();
    expect(rows()[0].querySelector('.rank')?.textContent).toBe('1');
    page.sellerSearch.set('no existe'); fixture.detectChanges();
    expect(panel().textContent).toContain('Sin coincidencias');
    page.sellerSearch.set(''); fixture.detectChanges();
    expect(rows()).toHaveLength(2); expect(api.read).toHaveBeenCalledTimes(1);
  });
  it('filters through the existing search atom without resorting backend ties', () => {
    api.read.mockImplementation((query: PerformanceQuery) => {
      const data = performanceFixture(query);
      data.compliance.sellers.unshift({ ...data.compliance.sellers[0], workerId: '30000000-0000-0000-0000-000000000003', recordedName: 'Zulma',
        sales: '120.004', percent: '120.00', ratio: { numerator: '120.004', denominator: '100' } });
      return of(decode(data, query));
    });
    create();
    expect(rows()[0].textContent).toContain('Zulma');
    const search = fixture.debugElement.query(By.directive(SearchFieldComponent)).componentInstance as SearchFieldComponent;
    search.searchChange.emit('Ángela'); fixture.detectChanges();
    expect(rows()[0].querySelector('.rank')?.textContent).toBe('2');
  });
  it.each(['NO_DATA', 'PENDING', 'ABSENT', 'NEGATIVE_OBSERVATIONS_REQUIRE_POLICY'])('shows %s distinctly without totals or percentages', state => {
    api.read.mockImplementation((query: PerformanceQuery) => {
      const data = emptyPerformanceFixture(query);
      if (state !== 'NO_DATA') {
        data.compliance.availability = 'UNAVAILABLE';
        data.compliance.reason = state === 'NEGATIVE_OBSERVATIONS_REQUIRE_POLICY' ? state : 'HISTORY_INCOMPLETE';
        if (state === 'NEGATIVE_OBSERVATIONS_REQUIRE_POLICY') data.compliance.coverage[0].status = 'PROCESSED';
        else data.compliance.coverage[0].status = state;
      }
      return of(decode(data, query));
    });
    create();
    expect(page.complianceSales()).toBeNull(); expect(rows()).toHaveLength(0);
    expect(panel().querySelector('app-percent-bar')).toBeNull();
    expect(panel().textContent).toContain(state === 'NO_DATA' ? 'Sin datos de ventas' : state === 'NEGATIVE_OBSERVATIONS_REQUIRE_POLICY' ? 'ajustes negativos' : 'historial del período aún no está completo');
    if (state === 'PENDING') expect(panel().textContent).toContain('Procesamiento pendiente');
    if (state === 'ABSENT') expect(panel().textContent).toContain('Sin historial confirmado');
    expect(panel().textContent).not.toContain('0 vendedores');
  });
  it.each(['TARGET_PERIOD_MISMATCH', 'TARGET_MEASURE_MISMATCH', 'ZERO_TARGET'])('shows an honest target/percentage for %s', reason => {
    api.read.mockImplementation((query: PerformanceQuery) => {
      const data = performanceFixture(query);
      data.compliance.availability = 'UNAVAILABLE'; data.compliance.reason = 'INCOMPLETE_CALCULATIONS';
      data.compliance.sellers = [{ ...missingTargetSeller(), reason, target: reason === 'ZERO_TARGET' ? '0' : '999', targetSource: 'source' }];
      return of(decode(data, query));
    });
    create();
    const cells = rows()[0].querySelectorAll('td');
    expect(cells[3].textContent?.trim()).toBe(reason === 'ZERO_TARGET' ? '0 gal' : '—');
    expect(cells[4].textContent?.trim()).toBe('—');
    expect(cells[5].textContent).toContain('No calculable');
    expect(panel().querySelector('app-percent-bar')).toBeNull();
  });
  it.each([0, 400, 401, 403, 404, 502])('shows HTTP/network failure %s, clears results and retries without altering Metas', status => {
    create();
    api.read.mockReturnValueOnce(throwError(() => new ApiError(status, { detail: 'Error de consulta' })));
    page.consultCompliance(); fixture.detectChanges();
    expect(page.complianceError()).not.toBe(''); expect(page.compliance()).toBeNull();
    expect(page.complianceLoading()).toBe(false); expect(rows()).toHaveLength(0);
    expect(panel().querySelector('[role=alert]')?.textContent).toContain(page.complianceError());
    const retry = Array.from(panel().querySelectorAll('button')).find(button => button.textContent?.includes('Reintentar'));
    retry?.click(); fixture.detectChanges();
    expect(rows()).toHaveLength(1); expect(page.complianceError()).toBe('');
    expect(targets.list).toHaveBeenCalledTimes(1); expect(targets.create).not.toHaveBeenCalled();
    expect(page.form.enabled).toBe(true);
  });
});
