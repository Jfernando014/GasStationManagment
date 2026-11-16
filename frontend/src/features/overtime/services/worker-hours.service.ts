import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { catchError, map, Observable, of } from 'rxjs';

import { getErrorMessage, isApiError } from '../../../shared/helpers/problem-details.helpers';
import { DayHours, DayWorker, LoadResult, ReprocessResult } from '../models/overtime.model';
import { DayHoursDto } from '../models/worker-hours.dto';
import { toLoadError } from './overtime-errors';

/** Messages the screen shows as they are, by the stable `code` of the backend error. */
const ERROR_MESSAGES: Record<string, string> = {
  DAY_HOURS_NOT_REGISTERED: 'Ese día no tiene registro de horas',
  DAY_HOURS_PROCESSING_FAILED: 'No se pudo procesar ese día. Puedes intentarlo de nuevo'
};

const FORBIDDEN_MESSAGE = 'No tienes permiso para reprocesar';

const NOVELTY_TEXT: Record<string, string> = {
  REPLACEMENT: 'Reemplazo', ABSENCE: 'Ausencia', SHIFT_CHANGE: 'Cambio de turno', SWAP: 'Intercambio'
};

/** The hours of a closed day as Turnos delivered them, and the reprocessing of a day. */
@Injectable({ providedIn: 'root' })
export class WorkerHoursService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = 'http://localhost:8080/api/v1/workers/hours';

  /** A day without a record of hours comes back as `NOT_FOUND`: its journal was not closed or saved no hours. */
  getDay(date: string): Observable<LoadResult<DayHours>> {
    return this.http.get<DayHoursDto>(`${this.apiUrl}/${encodeURIComponent(date)}`).pipe(
      map<DayHoursDto, LoadResult<DayHours>>(dto => ({ outcome: 'OK', data: toDayHours(dto) })),
      catchError((error: unknown) => of(toLoadError(error)))
    );
  }

  /** Administrator only. Never errors: every outcome, including a failed request, comes back as a result. */
  reprocess(date: string): Observable<ReprocessResult> {
    return this.http.post<DayHoursDto>(`${this.apiUrl}/${encodeURIComponent(date)}/reprocess`, null).pipe(
      map<DayHoursDto, ReprocessResult>(() => ({ outcome: 'PROCESSED' })),
      catchError((error: unknown) => of(toReprocessError(error)))
    );
  }
}

function toDayHours(dto: DayHoursDto): DayHours {
  return {
    date: dto.sync.date,
    status: dto.sync.status,
    noveltiesApplied: dto.sync.noveltiesApplied,
    attempts: dto.sync.attempts,
    workers: dto.workers.map<DayWorker>(worker => ({
      workerId: worker.workerId,
      name: worker.workerName,
      workedCode: worker.workedCode,
      gridCode: worker.gridCode,
      hours: worker.totalHours,
      absent: worker.absent,
      novelty: noveltyText(worker.noveltyType, worker.replacedWorkerName)
    }))
  };
}

function noveltyText(type: string, replaced: string | null): string | null {
  const label = NOVELTY_TEXT[type];
  if (!label) return null;
  return replaced ? `${label} de ${replaced}` : label;
}

function toReprocessError(error: unknown): ReprocessResult {
  if (isApiError(error)) {
    if (error.status === 403) return { outcome: 'ERROR', message: FORBIDDEN_MESSAGE };
    const known = error.code ? ERROR_MESSAGES[error.code] : undefined;
    if (known) return { outcome: 'ERROR', message: known };
  }
  return { outcome: 'ERROR', message: getErrorMessage(error) };
}
