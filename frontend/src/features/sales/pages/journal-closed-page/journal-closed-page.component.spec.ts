import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { BehaviorSubject, of, throwError } from 'rxjs';
import { vi } from 'vitest';
import { JournalClosedPageComponent } from './journal-closed-page.component';
import { JournalService } from '../../../../core/services/journal.service';
import {
  JournalClosingSummaryResponse,
  JournalResponse,
  JournalStatus,
  StepStatus
} from '../../../../shared/models/journal.model';

describe('JournalClosedPageComponent (C-09, S3-07, SR-11)', () => {
  let component: JournalClosedPageComponent;
  let fixture: ComponentFixture<JournalClosedPageComponent>;
  let router: Router;

  const currentJournal$ = new BehaviorSubject<JournalResponse | null>(null);
  const journalServiceMock = {
    currentJournal$: currentJournal$.asObservable(),
    getClosingSummary: vi.fn(),
    downloadFullWorkbook: vi.fn()
  };

  const mockClosedJournal: JournalResponse = {
    id: 10,
    date: '2026-08-05',
    consecutive: 289,
    status: JournalStatus.CLOSED,
    closedAt: '2026-08-05T18:30:00',
    steps: [
      { id: 1, stepNumber: 1, name: 'Personal', status: StepStatus.COMPLETED, completedAt: '2026-08-05T08:00:00Z' },
      { id: 6, stepNumber: 6, name: 'Cierre', status: StepStatus.COMPLETED, completedAt: '2026-08-05T18:30:00Z' }
    ]
  };

  const mockSummary: JournalClosingSummaryResponse = {
    journalId: 10,
    journalDate: '2026-08-05',
    consecutive: 289,
    status: JournalStatus.CLOSED,
    closedAt: '2026-08-05T18:30:00',
    completedStepsCount: 5,
    vendorsWithShortageCount: 0,
    vendorsWithSurplusCount: 0,
    newAlertsCount: 1,
    steps: [],
    fuelVariations: [],
    newAutonomyAlerts: []
  };

  beforeEach(async () => {
    journalServiceMock.getClosingSummary.mockReset();
    journalServiceMock.getClosingSummary.mockReturnValue(of(mockSummary));
    journalServiceMock.downloadFullWorkbook.mockReset();
    currentJournal$.next(mockClosedJournal);

    await TestBed.configureTestingModule({
      imports: [JournalClosedPageComponent],
      providers: [
        provideRouter([]),
        { provide: JournalService, useValue: journalServiceMock }
      ]
    }).compileComponents();

    router = TestBed.inject(Router);
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('should display closed journal title and three post-close option cards', () => {
    fixture = TestBed.createComponent(JournalClosedPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent;
    expect(text).toContain('Jornada del 5 de agosto cerrada');
    expect(text).toContain('Ver rendimiento de los vendedores del día');
    expect(text).toContain('Ver control de inventario del día');
    expect(text).toContain('Ver la jornada completa');
  });

  it('opens C-09 performance and inventory subviews and downloads full Excel workbook without redirecting to step 1', () => {
    const navSpy = vi.spyOn(router, 'navigate').mockResolvedValue(true);
    const createUrlSpy = vi.fn(() => 'blob:test');
    const revokeUrlSpy = vi.fn();
    URL.createObjectURL = createUrlSpy;
    URL.revokeObjectURL = revokeUrlSpy;
    const clickSpy = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => undefined);

    const fakeBlob = new Blob(['xlsx']);
    journalServiceMock.downloadFullWorkbook.mockReturnValue(
      of({ content: fakeBlob, fileName: 'jornada-289-2026-08-05.xlsx' })
    );

    fixture = TestBed.createComponent(JournalClosedPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    component.setViewMode('rendimiento');
    fixture.detectChanges();
    expect(navSpy).toHaveBeenCalledWith(['/resultados/rendimiento'], { queryParams: { date: '2026-08-05', returnTo: 'jornada' } });
    expect(component.viewMode).toBe('inicio');

    component.setViewMode('inventario');
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Control de inventario del día');

    component.setViewMode('inicio');
    fixture.detectChanges();
    navSpy.mockClear();

    const fullJournalButton = fixture.nativeElement.querySelector('#btn-opcion-jornada-completa') as HTMLButtonElement;
    fullJournalButton.click();
    fixture.detectChanges();

    expect(journalServiceMock.downloadFullWorkbook).toHaveBeenCalledWith(10, '2026-08-05', 289);
    expect(createUrlSpy).toHaveBeenCalledWith(fakeBlob);
    expect(clickSpy).toHaveBeenCalledTimes(1);
    expect(revokeUrlSpy).toHaveBeenCalledWith('blob:test');
    expect(navSpy).not.toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).toContain('jornada-289-2026-08-05.xlsx');
  });

  it('shows error feedback when full journal Excel download fails', () => {
    journalServiceMock.downloadFullWorkbook.mockReturnValue(
      throwError(() => new Error('Network failure'))
    );

    fixture = TestBed.createComponent(JournalClosedPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    component.downloadFullJournal();
    fixture.detectChanges();

    expect(component.isDownloadingWorkbook).toBe(false);
    expect(component.downloadFeedback?.isError).toBe(true);
    const alert = fixture.nativeElement.querySelector('[role="alert"]');
    expect(alert).toBeTruthy();
  });
});
