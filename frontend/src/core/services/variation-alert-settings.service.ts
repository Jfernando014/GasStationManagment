import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { getApiBaseUrl } from '../config/api-base';

import {
  VariationAlertSettingRequest,
  VariationAlertSettingResponse
} from '../../shared/models/dtos/variation-alert-setting.dto';

/** Rules of the monthly variation alerts, configured with a validity date. */
@Injectable({ providedIn: 'root' })
export class VariationAlertSettingsService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${getApiBaseUrl()}/api/v1/inventory/variation-alert-settings`;

  list(): Observable<VariationAlertSettingResponse[]> {
    return this.http.get<VariationAlertSettingResponse[]>(this.apiUrl)
      .pipe(map(settings => settings.map(normalize)));
  }

  create(request: VariationAlertSettingRequest): Observable<VariationAlertSettingResponse> {
    return this.http.post<VariationAlertSettingResponse>(this.apiUrl, request).pipe(map(normalize));
  }

  update(id: number, request: VariationAlertSettingRequest): Observable<VariationAlertSettingResponse> {
    return this.http.put<VariationAlertSettingResponse>(`${this.apiUrl}/${id}`, request).pipe(map(normalize));
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}

/** The percentage is a decimal on the server: it may arrive as 0.5 or as 0.5000. */
function normalize(setting: VariationAlertSettingResponse): VariationAlertSettingResponse {
  return { ...setting, cumulativeLimitPercentage: Number(setting.cumulativeLimitPercentage) };
}
