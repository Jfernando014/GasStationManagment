import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { WorkerMonth } from '../../shared/models/dtos/worker-profile.dto';

/**
 * Reads the month of one worker (schedule plus novelties) from the shifts API. Errors arrive already converted
 * to ApiError by the error interceptor.
 */
@Injectable({
  providedIn: 'root'
})
export class ShiftsWorkerMonthService {
  private readonly http = inject(HttpClient);
  private readonly scheduleUrl = 'http://localhost:8080/api/shifts/schedule';

  /** Every day of the month, with the code of the schedule, the code worked and the novelties. */
  getMonth(workerId: string, month: string): Observable<WorkerMonth> {
    const params = new HttpParams().set('month', month);
    return this.http.get<WorkerMonth>(`${this.scheduleUrl}/workers/${encodeURIComponent(workerId)}`, { params });
  }
}
