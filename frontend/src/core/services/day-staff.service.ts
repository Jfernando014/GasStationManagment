import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import {
  ConfirmDayStaffRequest,
  DayStaffStepResponse,
  RegisterNoveltyRequest
} from '../../shared/models/dtos/day-staff.dto';

/**
 * Calls the day staff API (step 1 of the journal, "Personal del día"). Errors arrive already converted to
 * ApiError by the error interceptor, same as WorkersService.
 */
@Injectable({
  providedIn: 'root'
})
export class DayStaffService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = 'http://localhost:8080/api/shifts/day-staff';

  /** Step 1 of the given operational day, with its active novelties already applied. */
  getStep(date: string): Observable<DayStaffStepResponse> {
    return this.http.get<DayStaffStepResponse>(`${this.apiUrl}/${date}`);
  }

  /** Registers a replacement, absence, shift change or swap. The grid itself is never modified. */
  registerNovelty(date: string, request: RegisterNoveltyRequest): Observable<DayStaffStepResponse> {
    return this.http.post<DayStaffStepResponse>(`${this.apiUrl}/${date}/novelties`, request);
  }

  /** Cancels a novelty; the day goes back to its grid code. */
  cancelNovelty(date: string, noveltyId: string): Observable<DayStaffStepResponse> {
    return this.http.delete<DayStaffStepResponse>(`${this.apiUrl}/${date}/novelties/${noveltyId}`);
  }

  /** Saves any pending novelties and marks step 1 as completed, in a single transaction. */
  confirm(date: string, request?: ConfirmDayStaffRequest): Observable<DayStaffStepResponse> {
    return this.http.post<DayStaffStepResponse>(`${this.apiUrl}/${date}/confirmation`, request ?? {});
  }
}
