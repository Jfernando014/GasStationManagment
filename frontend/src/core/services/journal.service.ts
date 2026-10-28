import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject } from 'rxjs';

import { JournalResponse, JournalStepResponse } from '../../shared/models/journal.model';

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

  openJournal(date: string): Observable<JournalResponse> {
    return this.http.post<JournalResponse>(`${this.apiUrl}/open?date=${date}`, {});
  }

  completeStep(journalId: number, stepNumber: number): Observable<JournalStepResponse> {
    return this.http.post<JournalStepResponse>(`${this.apiUrl}/${journalId}/steps/${stepNumber}/complete`, {});
  }

  setCurrentJournal(journal: JournalResponse): void {
    this.currentJournalSubject.next(journal);
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
