import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { BehaviorSubject, Observable, take } from 'rxjs';

import {
  AlertCountResponse,
  AlertStatus,
  AttendAlertRequest,
  InventoryAlertResponse
} from '../../shared/models/dtos/inventory-alert.dto';

/**
 * Autonomy alerts of the inventory module and the counter shown on the bell.
 * The counter lives here so the header and the alerts page stay in sync.
 */
@Injectable({ providedIn: 'root' })
export class InventoryAlertsService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = 'http://localhost:8080/api/v1/inventory/alerts';

  private readonly pendingSubject = new BehaviorSubject<number>(0);
  /** Last known number of pending alerts (ACTIVE or READ). */
  readonly pending$ = this.pendingSubject.asObservable();

  /** Without a status the backend returns the open ones (ACTIVE, READ and ATTENDED). */
  list(status?: AlertStatus): Observable<InventoryAlertResponse[]> {
    let params = new HttpParams();
    if (status) {
      params = params.set('status', status);
    }
    return this.http.get<InventoryAlertResponse[]>(this.apiUrl, { params });
  }

  count(): Observable<AlertCountResponse> {
    return this.http.get<AlertCountResponse>(`${this.apiUrl}/count`);
  }

  markRead(id: number): Observable<InventoryAlertResponse> {
    return this.http.put<InventoryAlertResponse>(`${this.apiUrl}/${id}/read`, {});
  }

  attend(id: number, request: AttendAlertRequest = {}): Observable<InventoryAlertResponse> {
    return this.http.put<InventoryAlertResponse>(`${this.apiUrl}/${id}/attend`, request);
  }

  /** Refreshes the bell counter; a failure keeps the last known value. */
  refreshPending(): void {
    this.count().pipe(take(1)).subscribe({
      next: response => this.pendingSubject.next(response.pending),
      // The bell is informative: a backend hiccup must not break the current screen.
      error: () => undefined
    });
  }

  /** The home screen already brings the pending count, so it is reused instead of asking again. */
  setPending(value: number): void {
    this.pendingSubject.next(value);
  }
}
