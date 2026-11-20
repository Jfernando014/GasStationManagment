import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject, tap } from 'rxjs';
import { getApiBaseUrl } from '../config/api-base';

import {
  JournalClosingSummaryResponse,
  JournalHistoryDayDetailResponse,
  JournalHistoryMonthResponse,
  JournalResponse,
  JournalStatus,
  JournalStepResponse,
  StepStatus
} from '../../shared/models/journal.model';

export const JOURNAL_STEP_ROUTES = ['personal', 'vendedores', 'electronicos', 'caja', 'inventario', 'cierre'] as const;
const LAST_STEP_STORAGE_PREFIX = 'eds.journal.lastStep.';

@Injectable({
  providedIn: 'root'
})
export class JournalService {
  private readonly http = inject(HttpClient);
  private apiUrl = `${getApiBaseUrl()}/api/v1/journals`;

  // Jornada que journal-page.component.ts acaba de abrir, para que un paso hijo (p. ej. personal del día)
  // lea su fecha operativa sin recalcularla en el cliente.
  private readonly currentJournalSubject = new BehaviorSubject<JournalResponse | null>(null);
  readonly currentJournal$ = this.currentJournalSubject.asObservable();

  private selectedDate: string | null = null;
  private readonly lastStepByDate = new Map<string, string>();

  openJournal(date: string): Observable<JournalResponse> {
    return this.http.post<JournalResponse>(`${this.apiUrl}/open?date=${date}`, {});
  }

  completeStep(journalId: number, stepNumber: number): Observable<JournalStepResponse> {
    return this.http.post<JournalStepResponse>(`${this.apiUrl}/${journalId}/steps/${stepNumber}/complete`, {});
  }

  getClosingSummary(journalId: number): Observable<JournalClosingSummaryResponse> {
    return this.http.get<JournalClosingSummaryResponse>(`${this.apiUrl}/${journalId}/closing-summary`);
  }

  closeJournal(journalId: number): Observable<JournalResponse> {
    return this.http.post<JournalResponse>(`${this.apiUrl}/${journalId}/close`, {}).pipe(
      tap(closed => this.setCurrentJournal(closed))
    );
  }

  getJournalHistory(yearMonth: string): Observable<JournalHistoryMonthResponse> {
    return this.http.get<JournalHistoryMonthResponse>(`${this.apiUrl}/history`, {
      params: { yearMonth }
    });
  }

  getJournalHistoryDayDetail(date: string): Observable<JournalHistoryDayDetailResponse> {
    return this.http.get<JournalHistoryDayDetailResponse>(`${this.apiUrl}/history/${date}`);
  }

  setCurrentJournal(journal: JournalResponse): void {
    this.selectedDate = journal.date;
    this.currentJournalSubject.next(journal);
  }

  getCurrentJournalSnapshot(): JournalResponse | null {
    return this.currentJournalSubject.value;
  }

  setSelectedDate(date: string): void {
    this.selectedDate = date;
  }

  getSelectedDate(): string | null {
    return this.selectedDate;
  }

  /** Aplica localmente el resultado de completar un paso, sin volver a abrir la jornada. */
  updateCurrentStep(step: JournalStepResponse): void {
    const journal = this.currentJournalSubject.value;
    if (!journal) return;
    this.currentJournalSubject.next({
      ...journal,
      steps: journal.steps.map(s => (s.id === step.id ? step : s))
    });
  }

  /** Remembers the active step route for a journal date across module navigation and reloads. */
  rememberStep(date: string, stepRoute: string): void {
    if (!date || !(JOURNAL_STEP_ROUTES as readonly string[]).includes(stepRoute)) return;
    this.lastStepByDate.set(date, stepRoute);
    try {
      if (typeof localStorage !== 'undefined') {
        localStorage.setItem(`${LAST_STEP_STORAGE_PREFIX}${date}`, stepRoute);
      }
    } catch {
      // Ignore storage quota/access errors.
    }
  }

  getRememberedStep(date: string): string | null {
    const inMemory = this.lastStepByDate.get(date);
    if (inMemory && (JOURNAL_STEP_ROUTES as readonly string[]).includes(inMemory)) {
      return inMemory;
    }
    try {
      if (typeof localStorage === 'undefined') return null;
      const stored = localStorage.getItem(`${LAST_STEP_STORAGE_PREFIX}${date}`);
      if (stored && (JOURNAL_STEP_ROUTES as readonly string[]).includes(stored)) {
        this.lastStepByDate.set(date, stored);
        return stored;
      }
    } catch {
      return null;
    }
    return null;
  }

  /** Resolves where /jornada should land: last visited step, else first incomplete step (defaulting to Step 1). */
  resolveLandingStep(journal: JournalResponse): string {
    const remembered = this.getRememberedStep(journal.date);
    if (remembered) return remembered;

    if (journal.status === JournalStatus.CLOSED) {
      return 'cierre';
    }

    const ordered = [...journal.steps].sort((a, b) => a.stepNumber - b.stepNumber);
    const firstPending = ordered.find(step => step.status !== StepStatus.COMPLETED);
    if (!firstPending) return 'cierre';
    return JOURNAL_STEP_ROUTES[firstPending.stepNumber - 1] ?? 'personal';
  }
}
