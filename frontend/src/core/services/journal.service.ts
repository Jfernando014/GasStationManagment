import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject, tap } from 'rxjs';

import {
  JournalClosingSummaryResponse,
  JournalHistoryDayDetailResponse,
  JournalHistoryMonthResponse,
  JournalResponse,
  JournalStepResponse
} from '../../shared/models/journal.model';

@Injectable({
  providedIn: 'root'
})
export class JournalService {
  private readonly http = inject(HttpClient);
  private apiUrl = 'http://localhost:8080/api/v1/journals';

  // Jornada que journal-page.component.ts acaba de abrir, para que un paso hijo (p. ej. personal del día)
  // lea su fecha operativa sin recalcularla en el cliente.
  private readonly currentJournalSubject = new BehaviorSubject<JournalResponse | null>(null);
  readonly currentJournal$ = this.currentJournalSubject.asObservable();

  private selectedDate: string | null = null;

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
}
