import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';

import { getApiBaseUrl } from '../../../core/config/api-base';
import { CardSettingDto, FuelProfitReportDto } from '../models/fuel-profit.dto';

/** Text fields of the report: everything else that is not a list or an object is a figure. */
const TEXT_FIELDS = new Set(['month', 'referenceDate', 'mode', 'realPercentageNote', 'commissionRateDate', 'fuelType',
  'datum', 'since', 'date']);
const TEXT_LISTS = new Set(['incompleteFuels', 'fuelsWithoutTarget']);

/** Monthly profit per fuel and the card percentage each month uses. */
@Injectable({ providedIn: 'root' })
export class FuelProfitService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${getApiBaseUrl()}/api/v1/sales/fuel-profit`;

  report(month: string): Observable<FuelProfitReportDto> {
    return this.http.get<unknown>(this.apiUrl, { params: new HttpParams().set('month', month) })
      .pipe(map(body => normalize(body) as FuelProfitReportDto));
  }

  /** Only the administrator may call it. */
  saveCardSetting(month: string, setting: CardSettingDto): Observable<unknown> {
    return this.http.put(`${this.apiUrl}/card-setting/${encodeURIComponent(month)}`, setting);
  }
}

/** Figures may arrive with or without decimals, or as text: all become numbers; null stays null. */
function normalize(value: unknown, key = ''): unknown {
  if (value === null || value === undefined) return null;
  if (Array.isArray(value)) return TEXT_LISTS.has(key) ? value : value.map(item => normalize(item));
  if (typeof value === 'object') {
    return Object.fromEntries(Object.entries(value).map(([name, item]) => [name, normalize(item, name)]));
  }
  if (typeof value === 'boolean' || TEXT_FIELDS.has(key)) return value;
  return Number(value);
}
