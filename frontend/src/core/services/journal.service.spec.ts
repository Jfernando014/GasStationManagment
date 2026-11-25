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
    if (typeof localStorage !== 'undefined' && typeof localStorage.clear === 'function') {
      localStorage.clear();
    }
    TestBed.configureTestingModule({
      providers: [JournalService, provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(JournalService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    if (typeof localStorage !== 'undefined' && typeof localStorage.clear === 'function') {
      localStorage.clear();
    }
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

  it('should resolve landing step to personal when a journal has no completed or remembered steps', () => {
    const freshJournal: JournalResponse = {
      ...mockJournal,
      date: '2026-11-19',
      steps: [
        { id: 1, stepNumber: 1, name: 'Personal', status: StepStatus.PENDING, completedAt: null },
        { id: 2, stepNumber: 2, name: 'Vendedores', status: StepStatus.PENDING, completedAt: null }
      ]
    };

    expect(service.resolveLandingStep(freshJournal)).toBe('personal');
  });

  it('should persist and restore the last visited step per date', () => {
    service.rememberStep('2026-08-05', 'inventario');

    expect(service.getRememberedStep('2026-08-05')).toBe('inventario');
    expect(service.resolveLandingStep(mockJournal)).toBe('inventario');
  });

  it('should download the full journal workbook and parse Content-Disposition filename', () => {
    const blob = new Blob(['xlsx-data'], {
      type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
    });
    let downloadedName = '';
    let downloadedBlob: Blob | null = null;

    service.downloadFullWorkbook(10, '2026-08-05', 289).subscribe(file => {
      downloadedName = file.fileName;
      downloadedBlob = file.content;
    });

    const req = httpMock.expectOne('http://localhost:8080/api/v1/journals/10/workbook.xlsx');
    expect(req.request.method).toBe('GET');
    expect(req.request.responseType).toBe('blob');
    req.flush(blob, {
      headers: { 'Content-Disposition': 'attachment; filename="jornada-289-2026-08-05.xlsx"' }
    });

    expect(downloadedName).toBe('jornada-289-2026-08-05.xlsx');
    expect(downloadedBlob).toBe(blob);
  });

  it('should fall back to generated filename when Content-Disposition header is missing', () => {
    const blob = new Blob(['xlsx-data']);
    let downloadedName = '';

    service.downloadFullWorkbook(10, '2026-08-05', 289).subscribe(file => {
      downloadedName = file.fileName;
    });

    const req = httpMock.expectOne('http://localhost:8080/api/v1/journals/10/workbook.xlsx');
    req.flush(blob);

    expect(downloadedName).toBe('jornada-289-2026-08-05.xlsx');
  });

  it('should update journal observations and broadcast updated journal to currentJournal$', () => {
    const updatedJournal: JournalResponse = {
      ...mockJournal,
      observations: 'Se ajustó surtidor 2 antes del cierre.'
    };

    service.updateObservations(10, 'Se ajustó surtidor 2 antes del cierre.').subscribe(res => {
      expect(res.observations).toBe('Se ajustó surtidor 2 antes del cierre.');
      expect(service.getCurrentJournalSnapshot()).toEqual(updatedJournal);
    });

    const req = httpMock.expectOne('http://localhost:8080/api/v1/journals/10/observations');
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({ observations: 'Se ajustó surtidor 2 antes del cierre.' });
    req.flush(updatedJournal);
  });
});
