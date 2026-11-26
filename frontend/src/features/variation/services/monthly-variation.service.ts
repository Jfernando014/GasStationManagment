import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { getApiBaseUrl } from '../../../core/config/api-base';
import { MonthlyVariationDto } from '../models/monthly-variation.dto';

/** Theoretical stock against measured stock of every fuel, day by day, for a month. */
@Injectable({ providedIn: 'root' })
export class MonthlyVariationService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${getApiBaseUrl()}/api/v1/inventory/variations/monthly`;

  month(month: string): Observable<MonthlyVariationDto> {
    return this.http.get<MonthlyVariationDto>(this.apiUrl, { params: new HttpParams().set('month', month) });
  }
}
