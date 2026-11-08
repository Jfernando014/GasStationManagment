import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { BehaviorSubject, of } from 'rxjs';
import { vi } from 'vitest';
import { JournalClosedPageComponent } from './journal-closed-page.component';
import { JournalService } from '../../../../core/services/journal.service';
import {
  JournalClosingSummaryResponse,
  JournalResponse,
  JournalStatus,
  StepStatus
} from '../../../../shared/models/journal.model';

describe('JournalClosedPageComponent (C-09, S3-07)', () => {
  let component: JournalClosedPageComponent;
  let fixture: ComponentFixture<JournalClosedPageComponent>;
  let router: Router;

  const currentJournal$ = new BehaviorSubject<JournalResponse | null>(null);
  const journalServiceMock = {
    currentJournal$: currentJournal$.asObservable(),
    getClosingSummary: vi.fn()
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

  it('should switch subviews for performance and inventory and navigate to Step 1 in read-only mode', () => {
    const navSpy = vi.spyOn(router, 'navigate').mockResolvedValue(true);

    fixture = TestBed.createComponent(JournalClosedPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    component.setViewMode('rendimiento');
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Rendimiento de los vendedores del día');

    component.setViewMode('inventario');
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Control de inventario del día');

    component.openFullJournalReadOnly();
    expect(navSpy).toHaveBeenCalledWith(['/jornada/personal']);
  });
});
