import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, map } from 'rxjs';

import { RoleOption, ShiftCode } from '../../shared/models/dtos/shift-catalog.dto';
import { Worker, WorkerRequest, WorkerStatusRequest } from '../../shared/models/dtos/worker.dto';
import { WorkerFilters } from '../../shared/models/worker.model';

/**
 * Calls the workers API. Errors arrive already converted to ApiError by the error interceptor.
 */
@Injectable({
  providedIn: 'root'
})
export class WorkersService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = 'http://localhost:8080/api/workers';
  private readonly catalogUrl = 'http://localhost:8080/api/shifts/catalog';

  /** Workers ordered by name. Only the filters with a value are sent. */
  list(filters: WorkerFilters = {}): Observable<Worker[]> {
    let params = new HttpParams();
    if (filters.active !== undefined && filters.active !== null) {
      params = params.set('active', filters.active);
    }
    if (filters.roleId) {
      params = params.set('roleId', filters.roleId);
    }
    const search = filters.search?.trim();
    if (search) {
      params = params.set('search', search);
    }
    return this.http.get<Worker[]>(this.apiUrl, { params });
  }

  create(request: WorkerRequest): Observable<Worker> {
    return this.http.post<Worker>(this.apiUrl, request);
  }

  update(id: string, request: WorkerRequest): Observable<Worker> {
    return this.http.put<Worker>(`${this.apiUrl}/${id}`, request);
  }

  changeStatus(id: string, active: boolean): Observable<Worker> {
    const body: WorkerStatusRequest = { active };
    return this.http.patch<Worker>(`${this.apiUrl}/${id}/status`, body);
  }

  /** Distinct roles taken from the shift catalog, since there is no roles endpoint yet. */
  listRoles(): Observable<RoleOption[]> {
    return this.http.get<ShiftCode[]>(this.catalogUrl).pipe(
      map(codes => {
        const roles = new Map<string, RoleOption>();
        for (const code of codes) {
          if (code.role && !roles.has(code.role.id)) {
            roles.set(code.role.id, code.role);
          }
        }
        return [...roles.values()];
      })
    );
  }
}
