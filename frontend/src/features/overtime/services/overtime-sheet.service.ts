import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { catchError, map, Observable, of } from 'rxjs';

import { AdjustmentDto, CreateAdjustmentRequestDto, SheetDto, SummaryDto } from '../models/overtime.dto';
import { ActionResult, LoadResult, NewAdjustment, OvertimeSheet, OvertimeSummary } from '../models/overtime.model';
import { toActionError, toLoadError } from './overtime-errors';
import { toSheet, toSummary } from './overtime-mappers';

const FORBIDDEN_MESSAGE = 'No tienes permiso para registrar ni eliminar ajustes';

/** The backend says which field a message is about only through its first words. */
function adjustmentField(message: string): string | null {
  if (message.startsWith('El motivo')) return 'reason';
  if (message.startsWith('Las horas')) return 'hours';
  return null;
}

/** Fortnight sheets of the overtime hours and their manual adjustments. */
@Injectable({ providedIn: 'root' })
export class OvertimeSheetService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = 'http://localhost:8080/api/v1/workers';

  /** Never errors: every outcome, including a failed request, comes back as a result. */
  getSummary(month: string, fortnight: number): Observable<LoadResult<OvertimeSummary>> {
    return this.http.get<SummaryDto>(`${this.apiUrl}/overtime-sheet`, { params: this.params(month, fortnight) }).pipe(
      map<SummaryDto, LoadResult<OvertimeSummary>>(dto => ({ outcome: 'OK', data: toSummary(dto) })),
      catchError((error: unknown) => of(toLoadError(error)))
    );
  }

  /** A worker that does not exist (or a malformed id) comes back as `NOT_FOUND`. */
  getSheet(workerId: string, month: string, fortnight: number): Observable<LoadResult<OvertimeSheet>> {
    return this.http.get<SheetDto>(`${this.apiUrl}/${encodeURIComponent(workerId)}/overtime-sheet`,
      { params: this.params(month, fortnight) }).pipe(
      map<SheetDto, LoadResult<OvertimeSheet>>(dto => ({ outcome: 'OK', data: toSheet(dto) })),
      catchError((error: unknown) => of(toLoadError(error, true)))
    );
  }

  /** Administrator only. */
  createAdjustment(
    workerId: string, month: string, fortnight: number, adjustment: NewAdjustment
  ): Observable<ActionResult> {
    const body: CreateAdjustmentRequestDto = {
      month, fortnight, category: adjustment.category, direction: adjustment.direction,
      hours: adjustment.hours, reason: adjustment.reason.trim()
    };
    return this.http.post<AdjustmentDto>(`${this.apiUrl}/${encodeURIComponent(workerId)}/overtime-adjustments`, body)
      .pipe(
        map<AdjustmentDto, ActionResult>(() => ({ outcome: 'OK' })),
        catchError((error: unknown) => {
          const result = toActionError(error, {}, FORBIDDEN_MESSAGE);
          return of(result.outcome === 'ERROR' && result.field === null
            ? { ...result, field: adjustmentField(result.message) } : result);
        })
      );
  }

  /** Administrator only. */
  deleteAdjustment(id: number): Observable<ActionResult> {
    return this.http.delete<void>(`${this.apiUrl}/overtime-adjustments/${encodeURIComponent(id)}`).pipe(
      map<void, ActionResult>(() => ({ outcome: 'OK' })),
      catchError((error: unknown) => of(toActionError(error, {}, FORBIDDEN_MESSAGE)))
    );
  }

  private params(month: string, fortnight: number): HttpParams {
    return new HttpParams().set('month', month).set('fortnight', fortnight);
  }
}
