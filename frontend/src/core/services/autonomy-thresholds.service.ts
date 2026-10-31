import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import {
  AutonomyThresholdRequest,
  AutonomyThresholdResponse
} from '../../shared/models/dtos/autonomy-threshold.dto';

/** Warning and critical days of autonomy, configured with a validity date. */
@Injectable({ providedIn: 'root' })
export class AutonomyThresholdsService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = 'http://localhost:8080/api/v1/inventory/autonomy-thresholds';

  list(): Observable<AutonomyThresholdResponse[]> {
    return this.http.get<AutonomyThresholdResponse[]>(this.apiUrl);
  }

  create(request: AutonomyThresholdRequest): Observable<AutonomyThresholdResponse> {
    return this.http.post<AutonomyThresholdResponse>(this.apiUrl, request);
  }
}
