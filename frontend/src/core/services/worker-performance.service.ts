import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { WorkerPerformance } from '../../shared/models/dtos/worker-profile.dto';

/**
 * Historical performance of a worker, from the closed journals. Calls the statistics API directly; errors arrive
 * already converted to ApiError by the error interceptor.
 */
@Injectable({
  providedIn: 'root'
})
export class WorkerPerformanceService {
  private readonly http = inject(HttpClient);
  private readonly statisticsUrl = 'http://localhost:8080/api/v1/statistics/workers';

  getPerformance(workerId: string): Observable<WorkerPerformance> {
    return this.http.get<WorkerPerformance>(`${this.statisticsUrl}/${encodeURIComponent(workerId)}/performance`);
  }
}
