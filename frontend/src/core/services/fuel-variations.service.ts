import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { FuelVariationResponse } from '../../shared/models/dtos/fuel-variation.dto';

/** Daily variation per fuel: opening + entries - sold against what was measured. */
@Injectable({ providedIn: 'root' })
export class FuelVariationsService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = 'http://localhost:8080/api/v1/inventory/variations';

  findByJournalDate(journalDate: string): Observable<FuelVariationResponse[]> {
    return this.http.get<FuelVariationResponse[]>(this.apiUrl, {
      params: new HttpParams().set('journalDate', journalDate)
    });
  }
}
