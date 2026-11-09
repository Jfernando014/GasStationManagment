import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { of } from 'rxjs';
import { vi } from 'vitest';
import { JournalHistoryPageComponent } from './journal-history-page.component';
import { JournalService } from '../../../../core/services/journal.service';
import {
  JournalHistoryDayDetailResponse,
  JournalHistoryMonthResponse
} from '../../../../shared/models/journal.model';

describe('JournalHistoryPageComponent (C-10, S3-06, S3-07)', () => {
  let component: JournalHistoryPageComponent;
  let fixture: ComponentFixture<JournalHistoryPageComponent>;
  let router: Router;

  const journalServiceMock = {
    getSelectedDate: vi.fn(() => '2026-08-05'),
    setSelectedDate: vi.fn(),
    getJournalHistory: vi.fn(),
    getJournalHistoryDayDetail: vi.fn()
  };

  const mockAugustMonth: JournalHistoryMonthResponse = {
    yearMonth: '2026-08',
    closedSystemCount: 1,
    migratedCount: 25,
    withMarkersCount: 4,
    days: [
      {
        date: '2026-08-01',
        dayOfMonth: 1,
        source: 'MIGRATED',
        state: 'MIGRATED',
        journalId: null,
        consecutive: 285,
        markers: ['DISCREPANCY']
      },
      {
        date: '2026-08-05',
        dayOfMonth: 5,
        source: 'JOURNAL',
        state: 'CLOSED',
        journalId: 10,
        consecutive: 289,
        markers: ['ALERT']
      }
    ]
  };

  const mockAug01Detail: JournalHistoryDayDetailResponse = {
    date: '2026-08-01',
    source: 'MIGRATED',
    state: 'MIGRATED',
    journalId: null,
    consecutive: 285,
    closedAt: null,
    markers: ['DISCREPANCY'],
    vendorSales: [
      {
        vendorName: 'Mónica Dorado',
        shift: 'dia',
        gallons: 294.086,
        basketsAmount: 8400,
        totalSalesAmount: 4724107.35,
        discrepancyAmount: 989.35
      }
    ],
    credibancoSummary: {
      mastercardAmount: 1945034,
      visaAmount: 2224734,
      qrAmount: 651696,
      totalEdsAmount: 4169768
    },
    fuelVariations: [
      {
        fuelName: 'Gasolina Corriente',
        soldGallons: 766.434,
        theoreticalGallons: 4844.566,
        physicalGallons: 4849.0,
        variationGallons: 4.434
      }
    ],
    cashBalanceSummary: {
      sheetNumber: 285,
      previousCashAmount: 8420000,
      accumulatedSavingsAmount: 300000,
      dominusBalanceAmount: 8384700
    }
  };

  beforeEach(async () => {
    journalServiceMock.getJournalHistory.mockReset();
    journalServiceMock.getJournalHistoryDayDetail.mockReset();
    journalServiceMock.setSelectedDate.mockClear();
    journalServiceMock.getJournalHistory.mockReturnValue(of(mockAugustMonth));
    journalServiceMock.getJournalHistoryDayDetail.mockReturnValue(of(mockAug01Detail));

    await TestBed.configureTestingModule({
      imports: [JournalHistoryPageComponent],
      providers: [
        provideRouter([]),
        { provide: JournalService, useValue: journalServiceMock }
      ]
    }).compileComponents();

    router = TestBed.inject(Router);
  });

  it('should render August 2026 calendar with KPIs, migrated days, and closed system days', () => {
    fixture = TestBed.createComponent(JournalHistoryPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    expect(journalServiceMock.getJournalHistory).toHaveBeenCalledWith('2026-08');
    const text = fixture.nativeElement.textContent;
    expect(text).toContain('Historial de jornadas');
    expect(text).toContain('25');
    expect(text).toContain('Migrada');
    expect(text).toContain('Cerrada');
  });

  it('should open side detail panel when clicking a migrated day', () => {
    fixture = TestBed.createComponent(JournalHistoryPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    component.onDayClick(mockAugustMonth.days[0]);
    fixture.detectChanges();

    expect(journalServiceMock.getJournalHistoryDayDetail).toHaveBeenCalledWith('2026-08-01');
    const panelText = fixture.nativeElement.querySelector('#jr-hist-panel').textContent;
    expect(panelText).toContain('Cerrada (datos migrados)');
    expect(panelText).toContain('Mónica Dorado');
    expect(panelText).toContain('285');
  });

  it('should navigate to journal page when clicking a system journal day', () => {
    const navSpy = vi.spyOn(router, 'navigate').mockResolvedValue(true);

    fixture = TestBed.createComponent(JournalHistoryPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    component.onDayClick(mockAugustMonth.days[1]);
    expect(journalServiceMock.setSelectedDate).toHaveBeenCalledWith('2026-08-05');
    expect(navSpy).toHaveBeenCalledWith(['/jornada/personal'], { queryParams: { date: '2026-08-05' } });
  });
});

