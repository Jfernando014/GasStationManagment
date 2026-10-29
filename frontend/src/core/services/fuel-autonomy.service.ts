import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { FuelAutonomyResponse } from '../../shared/models/dtos/fuel-autonomy.dto';

/** Days of stock left per fuel, compared with the warning and critical thresholds of the module. */
@Injectable({ providedIn: 'root' })
export class FuelAutonomyService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = 'http://localhost:8080/api/v1/inventory/autonomy';

  findByJournalDate(journalDate: string): Observable<FuelAutonomyResponse[]> {
    return this.http.get<FuelAutonomyResponse[]>(this.apiUrl, {
      params: new HttpParams().set('journalDate', journalDate)
    });
  }
}
