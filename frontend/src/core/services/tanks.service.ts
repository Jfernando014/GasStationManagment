import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { TankRequest, TankResponse } from '../../shared/models/dtos/tank.dto';

@Injectable({ providedIn: 'root' })
export class TanksService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = 'http://localhost:8080/api/v1/inventory/tanks';

  list(): Observable<TankResponse[]> {
    return this.http.get<TankResponse[]>(this.apiUrl);
  }

  create(request: TankRequest): Observable<TankResponse> {
    return this.http.post<TankResponse>(this.apiUrl, request);
  }

  update(id: number, request: TankRequest): Observable<TankResponse> {
    return this.http.put<TankResponse>(`${this.apiUrl}/${id}`, request);
  }
}
