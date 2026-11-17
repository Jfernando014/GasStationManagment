import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { GaugeTableRequest, GaugeTableResponse } from '../../shared/models/dtos/gauge-table.dto';
import { getApiBaseUrl } from '../config/api-base';

@Injectable({ providedIn: 'root' })
export class GaugeTablesService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${getApiBaseUrl()}/api/v1/inventory/gauge-tables`;

  list(): Observable<GaugeTableResponse[]> {
    return this.http.get<GaugeTableResponse[]>(this.apiUrl);
  }

  create(request: GaugeTableRequest): Observable<GaugeTableResponse> {
    return this.http.post<GaugeTableResponse>(this.apiUrl, request);
  }

  update(id: number, request: GaugeTableRequest): Observable<GaugeTableResponse> {
    return this.http.put<GaugeTableResponse>(`${this.apiUrl}/${id}`, request);
  }
}
