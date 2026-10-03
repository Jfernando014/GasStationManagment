import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

/**
 * Example core API service for HTTP operations.
 */
@Injectable({
  providedIn: 'root'
})
export class ApiServiceExample {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/v1';

  get<T>(endpoint: string): Observable<T> {
    return this.http.get<T>(${this.baseUrl}/${endpoint});
  }

  post<T>(endpoint: string, payload: unknown): Observable<T> {
    return this.http.post<T>(${this.baseUrl}/${endpoint}, payload);
  }
}
