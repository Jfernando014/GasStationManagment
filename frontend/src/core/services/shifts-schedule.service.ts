import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { ShiftCode } from '../../shared/models/dtos/shift-catalog.dto';
import {
  AssignRangeRequest,
  AssignShiftRequest,
  ScheduleResponse,
  ScheduleUpdateResponse,
  ShiftPeriod
} from '../../shared/models/dtos/shift-schedule.dto';
import {
  RotationConfirmRequest,
  RotationPreviewRequest,
  RotationPreviewResponse,
  RotationResultResponse
} from '../../shared/models/dtos/shift-rotation.dto';

/**
 * Calls the shift schedule, catalog and automatic rotation APIs. Errors arrive already converted to
 * ApiError by the error interceptor, same as WorkersService.
 */
@Injectable({
  providedIn: 'root'
})
export class ShiftsScheduleService {
  private readonly http = inject(HttpClient);
  private readonly catalogUrl = 'http://localhost:8080/api/shifts/catalog';
  private readonly scheduleUrl = 'http://localhost:8080/api/shifts/schedule';

  /** Full catalog, including inactive codes. */
  getCatalog(): Observable<ShiftCode[]> {
    return this.http.get<ShiftCode[]>(this.catalogUrl);
  }

  /** Schedule of a date range with its coverage. Past days included. */
  getSchedule(from: string, to: string, workerId?: string, period?: ShiftPeriod): Observable<ScheduleResponse> {
    let params = new HttpParams().set('from', from).set('to', to);
    if (workerId) {
      params = params.set('workerId', workerId);
    }
    if (period) {
      params = params.set('period', period);
    }
    return this.http.get<ScheduleResponse>(this.scheduleUrl, { params });
  }

  /** Saves or edits the shift of a worker on one day. */
  assign(request: AssignShiftRequest): Observable<ScheduleUpdateResponse> {
    return this.http.put<ScheduleUpdateResponse>(`${this.scheduleUrl}/assignments`, request);
  }

  /** Same code for a worker on every day of a range, up to 62 days. */
  assignRange(request: AssignRangeRequest): Observable<ScheduleUpdateResponse> {
    return this.http.put<ScheduleUpdateResponse>(`${this.scheduleUrl}/assignments/range`, request);
  }

  /** Calculates, without saving anything, the rotation of every active titular for a month. */
  previewRotation(request: RotationPreviewRequest): Observable<RotationPreviewResponse> {
    return this.http.post<RotationPreviewResponse>(`${this.scheduleUrl}/rotation/preview`, request);
  }

  /** Saves exactly what the preview showed, only if the grid did not change since (gridVersion). */
  confirmRotation(request: RotationConfirmRequest): Observable<RotationResultResponse> {
    return this.http.post<RotationResultResponse>(`${this.scheduleUrl}/rotation/confirm`, request);
  }
}
