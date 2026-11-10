import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { catchError, map, Observable, of } from 'rxjs';

import { getErrorMessage, isApiError } from '../../../shared/helpers/problem-details.helpers';
import { MonthlyIncentivesDto, VendorIncentiveDto } from '../models/incentive-calculation.dto';
import { MonthlyIncentives, MonthResult, VendorIncentive } from '../models/incentive-calculation.model';

/** Backend codes of a month that cannot be calculated (400). */
const INVALID_MONTH_CODES = new Set(['MONTH_IN_FUTURE', 'MONTH_BEFORE_START', 'INVALID_PARAMETER']);

const SESSION_MESSAGE = 'Tu sesión ya no es válida. Inicia sesión de nuevo';

/** Monthly incentives of every seller who sold additives. Read only. */
@Injectable({ providedIn: 'root' })
export class IncentiveCalculationService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = 'http://localhost:8080/api/v1/incentives/monthly';

  /** Never errors: every outcome, including a failed request, comes back as a result. */
  getMonth(month: string): Observable<MonthResult> {
    return this.http.get<MonthlyIncentivesDto>(`${this.apiUrl}/${encodeURIComponent(month)}`).pipe(
      map<MonthlyIncentivesDto, MonthResult>(dto => ({ outcome: 'OK', data: toMonthly(dto) })),
      catchError((error: unknown) => of(toErrorResult(error)))
    );
  }
}

function toMonthly(dto: MonthlyIncentivesDto): MonthlyIncentives {
  return { month: dto.month, accumulatedFrom: dto.accumulatedFrom, vendors: dto.vendors.map(toVendor) };
}

function toVendor(dto: VendorIncentiveDto): VendorIncentive {
  return {
    document: dto.vendorDocument,
    name: dto.vendorName?.trim() || dto.vendorDocument,
    lines: dto.lines.map(line => ({
      sku: line.sku,
      productName: line.productName,
      units: line.units,
      valuePerUnit: line.valuePerUnit,
      subtotal: line.subtotal,
      periodStart: line.periodStart,
      periodEnd: line.periodEnd ?? null
    })),
    unresolved: dto.unresolved.map(item => ({
      sku: item.sku, productName: item.productName, units: item.units, reason: item.reason
    })),
    monthTotal: dto.monthTotal,
    accumulatedTotal: dto.accumulatedTotal,
    accumulatedUnresolvedUnits: dto.accumulatedUnresolvedUnits
  };
}

function toErrorResult(error: unknown): MonthResult {
  if (isApiError(error)) {
    if (error.status === 400 && (!error.code || INVALID_MONTH_CODES.has(error.code))) {
      return { outcome: 'INVALID_MONTH', message: getErrorMessage(error) };
    }
    if (error.status === 403 || error.status === 401) {
      return { outcome: 'ERROR', message: SESSION_MESSAGE };
    }
  }
  return { outcome: 'ERROR', message: getErrorMessage(error) };
}
