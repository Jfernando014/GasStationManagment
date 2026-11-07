import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { DailyControlResponse } from '../../shared/models/dtos/daily-control.dto';
import { InventoryHomeResponse } from '../../shared/models/dtos/inventory-home.dto';

/** Home screen of the inventory: stock per fuel and tank, and the control of a journal. */
@Injectable({ providedIn: 'root' })
export class InventoryHomeService {
  private readonly http = inject(HttpClient);
  private readonly homeUrl = 'http://localhost:8080/api/v1/inventory/home';
  private readonly controlUrl = 'http://localhost:8080/api/v1/inventory/daily-control';

  getHome(): Observable<InventoryHomeResponse> {
    return this.http.get<InventoryHomeResponse>(this.homeUrl);
  }

  getDailyControl(journalDate: string): Observable<DailyControlResponse> {
    return this.http.get<DailyControlResponse>(this.controlUrl, {
      params: new HttpParams().set('journalDate', journalDate)
    });
  }
}
