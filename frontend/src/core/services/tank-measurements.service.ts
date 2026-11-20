import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { getApiBaseUrl } from '../config/api-base';

import {
  GaugeVolumeResponse,
  TankMeasurementRequest,
  TankMeasurementResponse
} from '../../shared/models/dtos/tank-measurement.dto';

/** Daily tank measurement (the six heights) and the gallons calculated with each gauge table. */
@Injectable({ providedIn: 'root' })
export class TankMeasurementsService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${getApiBaseUrl()}/api/v1/inventory/measurements`;
  private readonly tanksUrl = `${getApiBaseUrl()}/api/v1/inventory/tanks`;

  findByJournalDate(journalDate: string): Observable<TankMeasurementResponse> {
    return this.http.get<TankMeasurementResponse>(`${this.apiUrl}/${journalDate}`);
  }

  /** The previous measurement, used by the variation of the day. */
  findPrevious(journalDate: string): Observable<TankMeasurementResponse> {
    return this.http.get<TankMeasurementResponse>(`${this.apiUrl}/previous`, {
      params: new HttpParams().set('journalDate', journalDate)
    });
  }

  /** Saves the readings sent; the backend merges them with the ones already stored for the date. */
  save(journalDate: string, request: TankMeasurementRequest): Observable<TankMeasurementResponse> {
    return this.http.put<TankMeasurementResponse>(`${this.apiUrl}/${journalDate}`, request);
  }

  /**
   * Resets the whole step 5 of the date: deletes the measurement and the entries, transfers and sales of the
   * products, and returns the step to PENDING. The opening stock and the adjustments are kept.
   */
  resetStep(journalDate: string): Observable<void> {
    return this.http.delete<void>(`${getApiBaseUrl()}/api/v1/inventory/journal-step/${journalDate}`);
  }

  /** Gallons of one height in one tank, without saving anything. */
  volume(tankId: number, heightCm: number): Observable<GaugeVolumeResponse> {
    return this.http.get<GaugeVolumeResponse>(`${this.tanksUrl}/${tankId}/volume`, {
      params: new HttpParams().set('heightCm', heightCm)
    });
  }
}
