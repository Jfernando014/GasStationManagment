import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { JournalService } from './journal.service';
import {
  JournalClosingSummaryResponse,
  JournalHistoryDayDetailResponse,
  JournalHistoryMonthResponse,
  JournalResponse,
  JournalStatus,
  StepStatus
} from '../../shared/models/journal.model';

describe('JournalService (S3-07)', () => {
  let service: JournalService;
  let httpMock: HttpTestingController;

  const mockJournal: JournalResponse = {
    id: 10,
    date: '2026-08-05',
    consecutive: 289,
    status: JournalStatus.OPEN,
    steps: [
      { id: 1, stepNumber: 1, name: 'Personal', status: StepStatus.COMPLETED, completedAt: '2026-08-05T08:00:00Z' },
      { id: 6, stepNumber: 6, name: 'Cierre', status: StepStatus.PENDING, completedAt: null }
    ],
    closedAt: null
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [JournalService, provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(JournalService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should fetch Step 6 closing summary by journalId', () => {
    const mockSummary: JournalClosingSummaryResponse = {
      journalId: 10,
      journalDate: '2026-08-05',
      consecutive: 289,
      status: JournalStatus.OPEN,
      closedAt: null,
      completedStepsCount: 4,
      vendorsWithShortageCount: 1,
      vendorsWithSurplusCount: 0,
      newAlertsCount: 1,
      steps: [
        { stepNumber: 1, stepName: 'Personal del día', status: 'COMPLETED', hasDiscrepancy: false, summaryDetail: 'Paso completado' }
      ],
      fuelVariations: [],
      newAutonomyAlerts: []
    };

    service.getClosingSummary(10).subscribe(res => {
      expect(res).toEqual(mockSummary);
    });

    const req = httpMock.expectOne('http://localhost:8080/api/v1/journals/10/closing-summary');
    expect(req.request.method).toBe('GET');
    req.flush(mockSummary);
  });

  it('should close journal and update currentJournal$ subject', () => {
    const closedJournal: JournalResponse = {
      ...mockJournal,
      status: JournalStatus.CLOSED,
      closedAt: '2026-08-05T18:30:00'
    };

    service.closeJournal(10).subscribe(res => {
      expect(res.status).toBe(JournalStatus.CLOSED);
      expect(service.getCurrentJournalSnapshot()).toEqual(closedJournal);
    });

    const req = httpMock.expectOne('http://localhost:8080/api/v1/journals/10/close');
    expect(req.request.method).toBe('POST');
    req.flush(closedJournal);
  });

  it('should fetch monthly journal history calendar', () => {
    const mockMonth: JournalHistoryMonthResponse = {
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
        }
      ]
    };

    service.getJournalHistory('2026-08').subscribe(res => {
      expect(res.yearMonth).toBe('2026-08');
      expect(res.migratedCount).toBe(25);
    });

    const req = httpMock.expectOne(r => r.url === 'http://localhost:8080/api/v1/journals/history' && r.params.get('yearMonth') === '2026-08');
    expect(req.request.method).toBe('GET');
    req.flush(mockMonth);
  });

  it('should fetch journal history day detail', () => {
    const mockDetail: JournalHistoryDayDetailResponse = {
      date: '2026-08-01',
      source: 'MIGRATED',
      state: 'MIGRATED',
      journalId: null,
      consecutive: 285,
      closedAt: null,
      markers: ['DISCREPANCY'],
      vendorSales: [],
      credibancoSummary: null,
      fuelVariations: [],
      cashBalanceSummary: null
    };

    service.getJournalHistoryDayDetail('2026-08-01').subscribe(res => {
      expect(res.date).toBe('2026-08-01');
      expect(res.source).toBe('MIGRATED');
    });

    const req = httpMock.expectOne('http://localhost:8080/api/v1/journals/history/2026-08-01');
    expect(req.request.method).toBe('GET');
    req.flush(mockDetail);
  });
});
