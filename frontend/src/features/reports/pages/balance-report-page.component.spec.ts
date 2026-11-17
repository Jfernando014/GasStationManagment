import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { vi } from 'vitest';
import { DailyDownloadService } from '../../../core/services/daily-download.service';
import { BalanceReportPageComponent } from './balance-report-page.component';

interface BalancePageSignals {
  atrFrom: { set(value: string): void };
  atrTo: { set(value: string): void };
}

/**
 * @author javiersolanop777
 */
describe('BalanceReportPageComponent', () => {
  let fixture: ComponentFixture<BalanceReportPageComponent>;
  let service: { downloadBalance: ReturnType<typeof vi.fn> };

  beforeEach(async () => {
    service = { downloadBalance: vi.fn() };
    URL.createObjectURL = vi.fn(() => 'blob:test');
    URL.revokeObjectURL = vi.fn();
    vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => undefined);

    await TestBed.configureTestingModule({
      imports: [BalanceReportPageComponent],
      providers: [{ provide: DailyDownloadService, useValue: service }]
    }).compileComponents();

    fixture = TestBed.createComponent(BalanceReportPageComponent);
    fixture.detectChanges();
  });

  afterEach(() => vi.restoreAllMocks());

  it('downloads the balance for the selected inclusive period', () => {
    service.downloadBalance.mockReturnValue(
      of({ fileName: 'balance-operativo-2026-08-01_2026-08-31.pptx', content: new Blob(['pptx']) })
    );
    const signals = fixture.componentInstance as unknown as BalancePageSignals;
    signals.atrFrom.set('2026-08-01');
    signals.atrTo.set('2026-08-31');
    fixture.detectChanges();

    const button = fixture.nativeElement.querySelector('.balance-download') as HTMLButtonElement;
    button.click();
    fixture.detectChanges();

    expect(service.downloadBalance).toHaveBeenCalledWith('2026-08-01', '2026-08-31');
    expect(URL.createObjectURL).toHaveBeenCalledTimes(1);
    expect(fixture.nativeElement.textContent).toContain('Balance descargado');
  });

  it('does not call the backend when the start date is after the end date', () => {
    const signals = fixture.componentInstance as unknown as BalancePageSignals;
    signals.atrFrom.set('2026-09-01');
    signals.atrTo.set('2026-08-01');
    fixture.detectChanges();

    const button = fixture.nativeElement.querySelector('.balance-download') as HTMLButtonElement;
    button.click();
    fixture.detectChanges();

    expect(service.downloadBalance).not.toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).toContain('La fecha inicial no puede ser posterior');
  });
});
