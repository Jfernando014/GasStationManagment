import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common';
import { Observable } from 'rxjs';

import { JournalResponse, JournalStepResponse } from '../../shared/models/journal.model';

@Injectable({
  providedIn: 'root'
})
export class JournalService {
  private apiUrl = 'http://localhost:8080/api/v1/journals';

  constructor(private http: HttpClient) {}

  openJournal(date: string): Observable<JournalResponse> {
    return this.http.post<JournalResponse>(`${this.apiUrl}/open?date=${date}`, {});
  }

  completeStep(journalId: number, stepNumber: number): Observable<JournalStepResponse> {
    return this.http.post<JournalStepResponse>(`${this.apiUrl}/${journalId}/steps/${stepNumber}/complete`, {});
  }
}
