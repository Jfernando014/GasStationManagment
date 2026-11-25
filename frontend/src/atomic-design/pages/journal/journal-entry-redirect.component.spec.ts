import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter, Router } from '@angular/router';
import { BehaviorSubject } from 'rxjs';
import { vi } from 'vitest';
import { JournalEntryRedirectComponent } from './journal-entry-redirect.component';
import { JournalService } from '../../../core/services/journal.service';
import { yesterdayCalendarDay } from '../../../shared/helpers/format.helpers';
import { JournalResponse, JournalStatus, StepStatus } from '../../../shared/models/journal.model';

describe('JournalEntryRedirectComponent (SR-10)', () => {
  const buildJournal = (date: string): JournalResponse => ({
    id: 12,
    date,
    consecutive: 301,
    status: JournalStatus.OPEN,
    steps: [
      { id: 1, stepNumber: 1, name: 'Personal', status: StepStatus.COMPLETED, completedAt: `${date}T08:00:00Z` },
      { id: 2, stepNumber: 2, name: 'Vendedores', status: StepStatus.PENDING, completedAt: null }
    ]
  });

  it('waits for yesterday journal by default and navigates to the resolved landing step', async () => {
    const yesterday = yesterdayCalendarDay();
    const currentJournal$ = new BehaviorSubject<JournalResponse | null>(null);
    const journalService = {
      currentJournal$: currentJournal$.asObservable(),
      getSelectedDate: vi.fn().mockReturnValue(null),
      resolveLandingStep: vi.fn().mockReturnValue('vendedores')
    };

    await TestBed.configureTestingModule({
      imports: [JournalEntryRedirectComponent],
      providers: [
        provideRouter([]),
        { provide: JournalService, useValue: journalService },
        { provide: ActivatedRoute, useValue: { parent: { snapshot: { queryParamMap: convertToParamMap({}) } } } }
      ]
    }).compileComponents();

    const router = TestBed.inject(Router);
    const navigateSpy = vi.spyOn(router, 'navigate').mockResolvedValue(true);

    TestBed.createComponent(JournalEntryRedirectComponent);
    currentJournal$.next(buildJournal('2026-08-05'));
    expect(navigateSpy).not.toHaveBeenCalled();

    currentJournal$.next(buildJournal(yesterday));
    expect(journalService.resolveLandingStep).toHaveBeenCalledWith(expect.objectContaining({ date: yesterday }));
    expect(navigateSpy).toHaveBeenCalledWith(['/jornada', 'vendedores'], {
      replaceUrl: true,
      queryParamsHandling: 'preserve'
    });
  });

  it('respects an explicit date query parameter over yesterday default', async () => {
    const currentJournal$ = new BehaviorSubject<JournalResponse | null>(null);
    const journalService = {
      currentJournal$: currentJournal$.asObservable(),
      getSelectedDate: vi.fn().mockReturnValue(null),
      resolveLandingStep: vi.fn().mockReturnValue('cierre')
    };

    await TestBed.configureTestingModule({
      imports: [JournalEntryRedirectComponent],
      providers: [
        provideRouter([]),
        { provide: JournalService, useValue: journalService },
        {
          provide: ActivatedRoute,
          useValue: { parent: { snapshot: { queryParamMap: convertToParamMap({ date: '2026-08-05' }) } } }
        }
      ]
    }).compileComponents();

    const router = TestBed.inject(Router);
    const navigateSpy = vi.spyOn(router, 'navigate').mockResolvedValue(true);

    TestBed.createComponent(JournalEntryRedirectComponent);
    currentJournal$.next(buildJournal('2026-08-05'));

    expect(navigateSpy).toHaveBeenCalledWith(['/jornada', 'cierre'], {
      replaceUrl: true,
      queryParamsHandling: 'preserve'
    });
  });
});
