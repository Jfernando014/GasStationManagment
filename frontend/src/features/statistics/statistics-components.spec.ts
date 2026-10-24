import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { of, Subject, throwError } from 'rxjs';
import { vi } from 'vitest';
import { routes } from '../../app/app.routes';
import { AuthService } from '../../core/services/auth.service';
import { CompetitionService } from '../../core/services/competition.service';
import { SalesStatisticsService } from '../../core/services/sales-statistics.service';
import { WorkersService } from '../../core/services/workers.service';
import { ApiError } from '../../shared/models/api-error.model';
import { PriceSimulation } from '../../shared/models/dtos/competition.dto';
import { averagesFixture, basketFixture, comparisonFixture, observationRequest } from './statistics-test-data';
import { StatisticsWorkspace } from './statistics-workspace';
import { CompetitionStatisticsComponent } from './components/competition-statistics.component';
import { BasketStatisticsComponent } from './components/basket-statistics.component';
import { ObservationsEditorComponent } from './components/observations-editor.component';
import { StatisticsBalanceComponent } from './components/statistics-balance.component';

function mocks() {
  return {
    sales: { basket: vi.fn().mockImplementation((from: string, to: string) => of({ ...basketFixture(), from, to })), averages: vi.fn().mockReturnValue(of(averagesFixture())), compare: vi.fn().mockReturnValue(of(averagesFixture())) },
    competition: { list: vi.fn().mockReturnValue(of([{ id: '1', name: 'Estación de prueba', brand: 'Terpel', location: 'Ubicación de prueba', active: true }])), compare: vi.fn().mockReturnValue(of(comparisonFixture())), register: vi.fn(), observe: vi.fn(), simulate: vi.fn() },
    workers: { list: vi.fn().mockReturnValue(of([])) }
  };
}
function providers(mock: ReturnType<typeof mocks>) {
  return [{ provide: SalesStatisticsService, useValue: mock.sales }, { provide: CompetitionService, useValue: mock.competition }, { provide: WorkersService, useValue: mock.workers }];
}

describe('SCRUM-84 navigation and dialogs', () => {
  it('reuses the protected layout and the single existing sidebar entry, with four tabs in one page', async () => {
    const mock = mocks();
    TestBed.configureTestingModule({ providers: [provideRouter(routes), ...providers(mock), { provide: AuthService, useValue: { hasToken: () => true, logout: vi.fn() } }] });
    const harness = await RouterTestingHarness.create('/resultados/estadisticas'); await harness.fixture.whenStable(); harness.detectChanges();
    const root = harness.routeNativeElement!;
    expect(root.querySelectorAll('.app-floating-sidebar')).toHaveLength(1);
    expect(root.querySelectorAll('a[href="/resultados/estadisticas"]')).toHaveLength(1);
    expect(root.querySelector('a[href="/resultados/estadisticas"]')?.getAttribute('aria-current')).toBe('page');
    expect(root.querySelector('.trail-active')?.textContent).toContain('Resultados / Estadísticas');
    const tabs = root.querySelectorAll<HTMLButtonElement>('[role=tab]'); expect(tabs).toHaveLength(4);
    tabs[2].click(); harness.detectChanges(); expect(root.querySelector('#statistics-panel-canastas')?.hasAttribute('hidden')).toBe(false);
    expect(root.querySelector('#statistics-panel-promedios')?.hasAttribute('hidden')).toBe(true);
    [...root.querySelectorAll<HTMLButtonElement>('button')].find(b => b.textContent?.trim() === 'Ver gráficas')!.click(); harness.detectChanges(); await harness.fixture.whenStable();
    expect(root.querySelector('dialog')).not.toBeNull(); expect(root.querySelector('dialog')?.textContent).toContain('Volver al reporte');
    root.querySelector<HTMLButtonElement>('dialog .btn-dialog-close')!.click(); harness.detectChanges(); expect(root.querySelector('dialog')).toBeNull();
  });
  it('redirects anonymous statistics access through the existing auth guard', async () => {
    TestBed.configureTestingModule({ providers: [provideRouter(routes), { provide: AuthService, useValue: { hasToken: () => false } }] });
    const harness = await RouterTestingHarness.create('/resultados/estadisticas'); expect(harness.routeNativeElement?.querySelector('#view-login')).not.toBeNull();
  });
  it('shows one connection error per active tab and keeps the basket retry action', async () => {
    const mock = mocks(), message = 'No se pudo conectar con el servidor';
    const offline = () => throwError(() => new ApiError(0, { detail: message }));
    mock.sales.averages.mockImplementation(offline); mock.sales.basket.mockImplementation(offline);
    mock.competition.compare.mockImplementation(offline);
    TestBed.configureTestingModule({ providers: [provideRouter(routes), ...providers(mock), { provide: AuthService, useValue: { hasToken: () => true, logout: vi.fn() } }] });
    const harness = await RouterTestingHarness.create('/resultados/estadisticas'); await harness.fixture.whenStable(); harness.detectChanges();
    const root = harness.routeNativeElement!;
    [...root.querySelectorAll<HTMLButtonElement>('button')].find(b => b.textContent?.trim() === 'CONSULTAR')!.click(); harness.detectChanges();
    const tabs = root.querySelectorAll<HTMLButtonElement>('[role=tab]');
    for (const index of [0, 2, 3, 1]) {
      tabs[index].click(); harness.detectChanges();
      const visibleErrors = [...root.querySelectorAll<HTMLElement>('[role=alert]')].filter(error => !error.closest('[hidden]'));
      expect(visibleErrors).toHaveLength(1); expect(visibleErrors[0].textContent).toContain(message);
      if (index === 2) {
        visibleErrors[0].querySelector<HTMLButtonElement>('button')!.click(); harness.detectChanges();
        expect(mock.sales.basket).toHaveBeenCalledTimes(2);
      }
    }
  });
});

describe('SCRUM-84 workspace and explicit editor', () => {
  it('filters observations by inclusive dates and cancels stale results when the range changes', () => {
    const mock = mocks(), response = new Subject<ReturnType<typeof averagesFixture>>(); mock.sales.averages.mockReturnValue(response);
    TestBed.configureTestingModule({ providers: [StatisticsWorkspace, ...providers(mock)] });
    const workspace = TestBed.inject(StatisticsWorkspace); workspace.replaceObservations('FUEL_AGGREGATE', observationRequest.observations);
    workspace.setRange('2026-08-03', '2026-08-03'); workspace.consult();
    expect(mock.sales.averages.mock.calls[0][0].observations).toHaveLength(2);
    workspace.setRange('2026-08-04', '2026-08-31'); response.next(averagesFixture()); expect(workspace.averages()).toBeNull(); expect(workspace.loadingAverages()).toBe(false);
  });
  it('keeps averages when scenario selectors change and exposes authorization failures', () => {
    const mock = mocks(); TestBed.configureTestingModule({ providers: [StatisticsWorkspace, ...providers(mock)] });
    const workspace = TestBed.inject(StatisticsWorkspace); workspace.setRange(observationRequest.from, observationRequest.to); workspace.consult();
    workspace.invalidateComparison(); expect(workspace.averages()).not.toBeNull();
    mock.sales.compare.mockReturnValue(throwError(() => new ApiError(403, { detail: 'Denied' })));
    workspace.compare({ dayOfWeek: 'MONDAY', journey: 'DAY' }, { dayOfWeek: 'MONDAY', journey: 'NIGHT' }); expect(workspace.error()).toContain('administrador');
  });
  it('validates secondary observations without inventing seller IDs or accepting duplicates', () => {
    const mock = mocks(); TestBed.configureTestingModule({ imports: [ObservationsEditorComponent], providers: [StatisticsWorkspace, ...providers(mock)] });
    const workspace = TestBed.inject(StatisticsWorkspace); workspace.setRange(observationRequest.from, observationRequest.to);
    const fixture = TestBed.createComponent(ObservationsEditorComponent), editor = fixture.componentInstance; editor.add(); editor.save(); expect(editor.error).not.toBe('');
    editor.rows.at(0).setValue({ date: '2026-08-03', journey: 'DAY', value: '0' }); editor.save(); expect(workspace.observations()[0].value).toBe('0');
    editor.add(); editor.rows.at(1).setValue({ date: '2026-08-03', journey: 'DAY', value: '10' }); editor.save(); expect(editor.error).toContain('Solo se admite');
  });
});

describe('SCRUM-84 competition forms and filters', () => {
  function setup() {
    const mock = mocks(); TestBed.configureTestingModule({ imports: [CompetitionStatisticsComponent], providers: [StatisticsWorkspace, ...providers(mock)] });
    const fixture = TestBed.createComponent(CompetitionStatisticsComponent); fixture.detectChanges(); return { mock, fixture, page: fixture.componentInstance };
  }
  it('filters brands and fuels while preserving the server zone mean', () => {
    const { page } = setup(); expect(page.rows()).toHaveLength(2); page.brand.set('Terpel'); expect(page.rows()).toHaveLength(1);
    expect(page.reference('MOTOR')).toBe('90'); page.fuel.set('DIESEL'); expect(page.rows()).toHaveLength(0); expect(page.reference('MOTOR')).toBe('90');
  });
  it('registers manual stations only after valid input and reports actual server success', () => {
    const { page, mock } = setup(); page.saveStation(); expect(mock.competition.register).not.toHaveBeenCalled();
    page.stationForm.setValue({ name: 'Nueva estación', brand: 'Prueba', location: 'Dirección de prueba' });
    mock.competition.register.mockReturnValue(of({ id: '2', ...page.stationForm.getRawValue(), active: true })); page.saveStation();
    expect(mock.competition.register).toHaveBeenCalledWith({ name: 'Nueva estación', brand: 'Prueba', location: 'Dirección de prueba' }); expect(page.success()).toContain('registrada');
  });
  it('converts user-entered Colombian observation time to an explicit UTC instant and rejects the future', () => {
    const { page, mock } = setup(); page.observationForm.setValue({ competitorId: '1', fuelType: 'MOTOR', pricePerGallon: '100.25', observedAt: '9999-01-01T10:30' });
    page.saveObservation(); expect(mock.competition.observe).not.toHaveBeenCalled();
    page.observationForm.controls.observedAt.setValue('2020-01-01T10:30'); mock.competition.observe.mockReturnValue(of({})); page.saveObservation();
    expect(mock.competition.observe.mock.calls[0][0].observedAt).toBe('2020-01-01T15:30:00.000Z');
  });
  it('accepts optional zero cost, keeps competitive differences separate and cancels stale simulations', () => {
    const { page, mock } = setup(); const response = new Subject<PriceSimulation>(); mock.competition.simulate.mockReturnValue(response);
    page.simulationForm.patchValue({ MOTOR: '100', acquisitionCost: '0' }); page.simulate(); expect(mock.competition.simulate.mock.calls[0][0].acquisitionCost).toBe('0');
    page.simulationForm.controls.MOTOR.setValue('101'); response.next({} as PriceSimulation); expect(page.simulation()).toBeNull(); expect(page.simulating()).toBe(false);
  });
  it('does not turn unavailable endpoints into a market with zero prices', () => {
    const mock = mocks(); mock.competition.compare.mockReturnValue(throwError(() => new ApiError(404, {})));
    TestBed.configureTestingModule({ imports: [CompetitionStatisticsComponent], providers: [StatisticsWorkspace, ...providers(mock)] });
    const fixture = TestBed.createComponent(CompetitionStatisticsComponent); fixture.detectChanges(); expect(fixture.componentInstance.rows()).toEqual([]); expect(fixture.componentInstance.error()).toContain('otras pestañas');
  });
});

describe('SCRUM-84 basket availability', () => {
  it.each(['AVAILABLE', 'NO_DATA', 'UNAVAILABLE'] as const)('renders %s without inventing attendance or seller data', status => {
    const mock = mocks(); mock.sales.basket.mockReturnValue(of(basketFixture(status)));
    TestBed.configureTestingModule({ imports: [BasketStatisticsComponent], providers: [StatisticsWorkspace, ...providers(mock)] });
    const fixture = TestBed.createComponent(BasketStatisticsComponent); fixture.detectChanges();
    expect(fixture.componentInstance.status()).toBe(status);
    expect(fixture.nativeElement.querySelectorAll('.stats-seller').length).toBe(status === 'AVAILABLE' ? 1 : 0);
    expect(fixture.nativeElement.textContent).not.toContain('0 Turnos');
    if (status === 'AVAILABLE') { expect(fixture.nativeElement.querySelector('app-avatar')).not.toBeNull(); expect(fixture.nativeElement.textContent).toContain('Documento no disponible'); }
  });
});

describe('SCRUM-84 monthly balance', () => {
  it('discovers real data for the selected month and cancels responses from a previous period', () => {
    const mock = mocks();
    TestBed.configureTestingModule({ imports: [StatisticsBalanceComponent], providers: [StatisticsWorkspace, ...providers(mock)] });
    const fixture = TestBed.createComponent(StatisticsBalanceComponent), balance = fixture.componentInstance;
    mock.sales.basket.mockImplementation((from: string, to: string) => of({ ...basketFixture('AVAILABLE'), from, to }));
    balance.month.set('2024-02'); fixture.detectChanges();
    expect(mock.sales.basket).toHaveBeenCalledWith('2024-02-01', '2024-02-29'); expect(balance.available()).toBe(true);
    const previous = new Subject<ReturnType<typeof basketFixture>>(); mock.sales.basket.mockReturnValue(previous);
    balance.month.set('2026-01'); fixture.detectChanges(); expect(balance.available()).toBe(false); expect(balance.checking()).toBe(true);
    mock.sales.basket.mockReturnValue(of({ ...basketFixture('NO_DATA'), from: '2026-02-01', to: '2026-02-28' }));
    balance.month.set('2026-02'); fixture.detectChanges();
    previous.next({ ...basketFixture('AVAILABLE'), from: '2026-01-01', to: '2026-01-31' });
    expect(balance.available()).toBe(false); expect(balance.realBasket()?.charts.availability).toBe('NO_DATA'); expect(balance.checking()).toBe(false);
  });
  it('allows provided observations when real sales are unavailable without inventing a ranking', () => {
    const mock = mocks(); mock.sales.basket.mockReturnValue(throwError(() => new ApiError(404, {})));
    TestBed.configureTestingModule({ imports: [StatisticsBalanceComponent], providers: [StatisticsWorkspace, ...providers(mock)] });
    const workspace = TestBed.inject(StatisticsWorkspace); workspace.replaceObservations('FUEL_AGGREGATE', observationRequest.observations);
    const fixture = TestBed.createComponent(StatisticsBalanceComponent), balance = fixture.componentInstance; balance.month.set('2026-08'); fixture.detectChanges();
    expect(balance.available()).toBe(true); expect(balance.realBasket()).toBeNull(); expect(balance.basketWarning()).not.toBe('');
    balance.month.set('2026-07'); fixture.detectChanges(); expect(balance.available()).toBe(false); expect(balance.total()).toBeNull();
  });
});
