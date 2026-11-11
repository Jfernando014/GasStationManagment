import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { catchError, map, Observable, of } from 'rxjs';

import { getErrorMessage, isApiError } from '../../../shared/helpers/problem-details.helpers';
import { SyncDto } from '../models/additive-sales.dto';
import { PendingDay, ReprocessResult } from '../models/incentive-calculation.model';

/** Messages the screen shows as they are, by the stable `code` of the backend error. */
const ERROR_MESSAGES: Record<string, string> = {
  ADDITIVE_SALES_NOT_REGISTERED: 'Ese día no tiene registro de ventas de aditivos',
  ADDITIVE_SALES_PROCESSING_FAILED: 'No se pudo procesar ese día. Puedes intentarlo de nuevo'
};

const FORBIDDEN_MESSAGE = 'No tienes permiso para reprocesar';

/** Days whose additive sales are stored (or still have to be) and the reprocessing of a day. */
@Injectable({ providedIn: 'root' })
export class AdditiveSalesService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = 'http://localhost:8080/api/v1/incentives/additive-sales';

  listPending(): Observable<PendingDay[]> {
    return this.http.get<SyncDto[]>(`${this.apiUrl}/pending`).pipe(
      map(days => days
        .filter(day => day.status === 'PENDING')
        .map(day => ({ date: day.date, attempts: day.attempts }))
        .sort((a, b) => a.date.localeCompare(b.date)))
    );
  }

  /** Administrator only. Never errors: every outcome, including a failed request, comes back as a result. */
  reprocess(date: string): Observable<ReprocessResult> {
    return this.http.post<unknown>(`${this.apiUrl}/${encodeURIComponent(date)}/reprocess`, null).pipe(
      map<unknown, ReprocessResult>(() => ({ outcome: 'PROCESSED' })),
      catchError((error: unknown) => of(toErrorResult(error)))
    );
  }
}

function toErrorResult(error: unknown): ReprocessResult {
  if (isApiError(error)) {
    if (error.status === 403) return { outcome: 'ERROR', message: FORBIDDEN_MESSAGE };
    const known = error.code ? ERROR_MESSAGES[error.code] : undefined;
    if (known) return { outcome: 'ERROR', message: known };
  }
  return { outcome: 'ERROR', message: getErrorMessage(error) };
}
