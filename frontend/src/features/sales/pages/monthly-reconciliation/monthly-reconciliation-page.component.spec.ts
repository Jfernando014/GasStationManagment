import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';
import { routes } from '../../../../app/app.routes';
import { AuthService } from '../../../../core/services/auth.service';
import { InventoryAlertsService } from '../../../../core/services/inventory-alerts.service';
import { MonthlyReconciliationService } from '../../../../core/services/monthly-reconciliation.service';
import { ApiError } from '../../../../shared/models/api-error.model';
import { MonthlyReconciliation } from '../../../../shared/models/dtos/monthly-reconciliation.dto';
import { MonthlyReconciliationPageComponent } from './monthly-reconciliation-page.component';

function augustFixture(): MonthlyReconciliation {
  const days = Array.from({ length: 31 }, (_, index) => {
    const date = `2026-08-${String(index + 1).padStart(2, '0')}`;
    const isFifth = index === 4;
    return {
      date,
      hasData: isFifth,
      mastercard: isFifth ? 3_618_384 : 0,
      visa: isFifth ? 2_072_928 : 0,
      amex: 0,
      totalEds: isFifth ? 5_691_312 : 0,
      qr: isFifth ? 876_008 : 0,
      qrSource: isFifth ? 'PLANILLA' as const : 'NONE' as const,
      totalWithQr: isFifth ? 6_567_320 : 0,
      points: isFifth ? 41_380 : 0
    };
  });
  return {
    month: '2026-08',
    from: '2026-08-01',
    to: '2026-08-31',
    days,
    daysWithData: 6,
    totals: {
      mastercard: 66_683_965,
      visa: 32_184_828,
      amex: 0,
      totalEds: 98_868_793,
      qr: 19_476_459,
      totalWithQr: 118_345_252,
      points: 1_796_955
    },
    commission: {
      rate: 0.0265,
      rateDate: '2026-08-31',
      base: 98_868_793,
      amount: 2_620_023.01
    }
  };
}

describe('MonthlyReconciliationPageComponent', () => {
  let fixture: ComponentFixture<MonthlyReconciliationPageComponent>;
  const service = { getMonthly: vi.fn() };

  beforeEach(async () => {
    service.getMonthly.mockReset().mockReturnValue(of(augustFixture()));
    await TestBed.configureTestingModule({
      imports: [MonthlyReconciliationPageComponent],
      providers: [
        provideRouter(routes),
        { provide: AuthService, useValue: { hasToken: () => true, logout: vi.fn() } },
        { provide: InventoryAlertsService, useValue: { pending$: of(0), refreshPending: vi.fn() } },
        { provide: MonthlyReconciliationService, useValue: service }
      ]
    }).compileComponents();
  });

  function createPage(): HTMLElement {
    fixture = TestBed.createComponent(MonthlyReconciliationPageComponent);
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  it('loads the full August 2026 table and report totals on first view', () => {
    const root = createPage();
    const rows = root.querySelectorAll('tbody tr');

    expect(service.getMonthly).toHaveBeenCalledWith('2026-08');
    expect(root.textContent).toContain('Conciliaciones del mes');
    expect(root.textContent).toContain('Agosto de 2026');
    expect(rows).toHaveLength(31);
    expect(rows[4].textContent).toContain('5.691.312');
    expect(rows[4].textContent).toContain('876.008');
    expect(rows[4].textContent).toContain('41.380');
    expect(rows[30].textContent).toContain('31 de ago');
    expect(root.textContent).toContain('98.868.793');
    expect(root.textContent).toContain('66.683.965');
    expect(root.textContent).toContain('32.184.828');
    expect(root.textContent).toContain('19.476.459');
    expect(root.textContent).toContain('1.796.955');
    expect(root.textContent).toContain('2.620.023,01');
  });

  it('loads the adjacent month when the month navigation is used', () => {
    service.getMonthly.mockReturnValue(of({ ...augustFixture(), month: '2026-09', days: [] }));
    const root = createPage();

    root.querySelector<HTMLButtonElement>('[aria-label="Consultar mes siguiente"]')!.click();
    fixture.detectChanges();

    expect(service.getMonthly).toHaveBeenLastCalledWith('2026-09');
    expect(root.querySelector('.monthly-table-heading')?.textContent).toContain('Septiembre de 2026');
  });

  it('shows the server error and allows retrying the month', () => {
    service.getMonthly.mockReturnValueOnce(throwError(() => new ApiError(404, {
      detail: 'No hay una comisión vigente.'
    }))).mockReturnValueOnce(of(augustFixture()));
    const root = createPage();

    expect(root.querySelector('[role="alert"]')?.textContent).toContain('No hay una comisión vigente.');
    root.querySelector<HTMLButtonElement>('.monthly-error button')!.click();
    fixture.detectChanges();

    expect(service.getMonthly).toHaveBeenCalledTimes(2);
    expect(root.querySelector('[role="alert"]')).toBeNull();
    expect(root.querySelectorAll('tbody tr')).toHaveLength(31);
  });

  it('rejects a malformed month without making a request', () => {
    const root = createPage();
    const component = fixture.componentInstance;
    service.getMonthly.mockClear();

    component.month.set('2026-13');
    component.consult();
    fixture.detectChanges();

    expect(service.getMonthly).not.toHaveBeenCalled();
    expect(root.querySelector('[role="alert"]')?.textContent).toContain('Selecciona un mes válido');
  });

  it('opens from Resultados with the navigation item and breadcrumb active', async () => {
    const harness = await RouterTestingHarness.create('/resultados/conciliaciones');
    await harness.fixture.whenStable();
    harness.detectChanges();

    const root = harness.routeNativeElement!;
    const link = root.querySelector<HTMLAnchorElement>('a[href="/resultados/conciliaciones"]');
    expect(link?.textContent).toContain('Conciliaciones');
    expect(link?.getAttribute('aria-current')).toBe('page');
    expect(root.querySelector('.trail-active')?.textContent).toContain('Resultados / Conciliaciones');
    expect(root.querySelectorAll('tbody tr')).toHaveLength(31);
  });
});
