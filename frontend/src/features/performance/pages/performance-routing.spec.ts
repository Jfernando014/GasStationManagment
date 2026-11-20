import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { BehaviorSubject, of } from 'rxjs';
import { vi } from 'vitest';
import { routes } from '../../../app/app.routes';
import { AuthService } from '../../../core/services/auth.service';
import { InventoryAlertsService } from '../../../core/services/inventory-alerts.service';
import { JournalService } from '../../../core/services/journal.service';
import { SalesVendorCloseService } from '../../../core/services/sales-vendor-close.service';
import { DayStaffService } from '../../../core/services/day-staff.service';
import { SellerPerformanceService } from '../../../core/services/seller-performance.service';
import { JournalResponse, JournalStatus } from '../../../shared/models/journal.model';
import { PerformanceQuery } from '../../../shared/models/dtos/seller-performance.dto';
import { fullPerformanceFixture } from '../../../shared/helpers/performance-report.test-data';
import { parsePerformanceReport } from '../../../shared/helpers/seller-performance-reports.helpers';

describe('SCRUM-108 C-09 navigation', () => {
  const api = { readReport: vi.fn() };
  beforeEach(() => {
    api.readReport.mockReset().mockImplementation((q: PerformanceQuery) => of(parsePerformanceReport(JSON.stringify(fullPerformanceFixture(q)), q)));
  });
  const providers = () => [provideRouter(routes), { provide: AuthService, useValue: { hasToken: () => true, logout: () => undefined } },
    { provide: InventoryAlertsService, useValue: { pending$: of(0), refreshPending: () => undefined } },
    { provide: SellerPerformanceService, useValue: api }, { provide: DayStaffService, useValue: { getStep: () => of({ resting: [] }) } }];
  it('uses the existing layout/menu and exposes the new route as mobile-ready', async () => {
    TestBed.configureTestingModule({ providers: providers() });
    const harness = await RouterTestingHarness.create('/resultados/rendimiento');
    await harness.fixture.whenStable(); harness.detectChanges();
    const root = harness.routeNativeElement!;
    expect(root.querySelectorAll('a[href="/resultados/rendimiento"]')).toHaveLength(1);
    expect(root.querySelector('a[href="/resultados/rendimiento"]')?.getAttribute('aria-current')).toBe('page');
    expect(root.querySelector('.trail-active')?.textContent).toContain('Resultados / Rendimiento');
    expect(root.matches('.statistics-mobile-layout')).toBe(true);
    expect(root.querySelectorAll('[role=tab]')).toHaveLength(2);
  });
  it('navigates from the real closed-journal button, consults its date, and returns to that date', async () => {
    const journal: JournalResponse = { id: 10, date: '2026-08-03', consecutive: 100, status: JournalStatus.CLOSED, closedAt: '2026-08-04T07:00:00Z', steps: [] };
    const current = new BehaviorSubject<JournalResponse | null>(journal);
    const service = { currentJournal$: current.asObservable(), openJournal: vi.fn().mockReturnValue(of(journal)),
      setCurrentJournal: (j: JournalResponse) => current.next(j), getSelectedDate: () => journal.date,
      getClosingSummary: () => of({ journalId: 10, journalDate: journal.date, status: JournalStatus.CLOSED, steps: [], closedAt: journal.closedAt }) };
    TestBed.configureTestingModule({ providers: [...providers(), { provide: JournalService, useValue: service },
      { provide: SalesVendorCloseService, useValue: { getVendorCloses: () => of([]) } }] });
    const harness = await RouterTestingHarness.create('/jornada/cerrada?date=2026-08-03');
    await harness.fixture.whenStable(); harness.detectChanges();
    harness.routeNativeElement!.querySelector<HTMLButtonElement>('#btn-opcion-rendimiento')!.click();
    await harness.fixture.whenStable(); await new Promise(resolve => setTimeout(resolve, 320)); harness.detectChanges();
    expect(TestBed.inject(Router).url).toBe('/resultados/rendimiento?date=2026-08-03&returnTo=jornada');
    expect(api.readReport).toHaveBeenLastCalledWith({ from: '2026-08-03', to: '2026-08-03', measure: 'FUEL_GALLONS', outputScale: 2 });
    harness.routeNativeElement!.querySelector<HTMLAnchorElement>('a[href="/jornada/cerrada?date=2026-08-03"]')!.click();
    await harness.fixture.whenStable(); harness.detectChanges();
    expect(service.openJournal).toHaveBeenLastCalledWith('2026-08-03');
    expect(harness.routeNativeElement!.querySelector('#btn-opcion-rendimiento')).not.toBeNull();
  });
  it('keeps anonymous access protected by the existing auth guard', async () => {
    TestBed.configureTestingModule({ providers: [provideRouter(routes), { provide: AuthService, useValue: { hasToken: () => false } }] });
    const harness = await RouterTestingHarness.create('/resultados/rendimiento');
    expect(harness.routeNativeElement?.querySelector('#view-login')).not.toBeNull(); expect(api.readReport).not.toHaveBeenCalled();
  });
});
