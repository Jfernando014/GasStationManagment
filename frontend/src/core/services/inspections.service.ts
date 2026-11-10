import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import {
  InspectionFilters,
  InspectionRequest,
  InspectionResponse
} from '../../shared/models/dtos/inspection.dto';

/** Inspection, hermeticity and ONAC calibration log of the station. */
@Injectable({ providedIn: 'root' })
export class InspectionsService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = 'http://localhost:8080/api/v1/inventory/inspections';

  /** Only the filters with a value are sent. */
  list(filters: InspectionFilters = {}): Observable<InspectionResponse[]> {
    let params = new HttpParams();
    if (filters.category) {
      params = params.set('category', filters.category);
    }
    if (filters.status) {
      params = params.set('status', filters.status);
    }
    if (filters.from) {
      params = params.set('from', filters.from);
    }
    if (filters.to) {
      params = params.set('to', filters.to);
    }
    return this.http.get<InspectionResponse[]>(this.apiUrl, { params });
  }

  create(request: InspectionRequest): Observable<InspectionResponse> {
    return this.http.post<InspectionResponse>(this.apiUrl, request);
  }

  update(id: number, request: InspectionRequest): Observable<InspectionResponse> {
    return this.http.put<InspectionResponse>(`${this.apiUrl}/${id}`, request);
  }
}
