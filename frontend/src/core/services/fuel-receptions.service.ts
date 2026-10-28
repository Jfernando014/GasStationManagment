import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import {
  FuelReceptionFilters,
  FuelReceptionRequest,
  FuelReceptionResponse
} from '../../shared/models/dtos/fuel-reception.dto';

/** Fuel that arrives at a tank, with its supplier. */
@Injectable({ providedIn: 'root' })
export class FuelReceptionsService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = 'http://localhost:8080/api/v1/inventory/receptions';

  /** Only the filters with a value are sent. */
  list(filters: FuelReceptionFilters = {}): Observable<FuelReceptionResponse[]> {
    let params = new HttpParams();
    if (filters.tankId) {
      params = params.set('tankId', filters.tankId);
    }
    if (filters.from) {
      params = params.set('from', filters.from);
    }
    if (filters.to) {
      params = params.set('to', filters.to);
    }
    return this.http.get<FuelReceptionResponse[]>(this.apiUrl, { params });
  }

  create(request: FuelReceptionRequest): Observable<FuelReceptionResponse> {
    return this.http.post<FuelReceptionResponse>(this.apiUrl, request);
  }

  update(id: number, request: FuelReceptionRequest): Observable<FuelReceptionResponse> {
    return this.http.put<FuelReceptionResponse>(`${this.apiUrl}/${id}`, request);
  }
}
