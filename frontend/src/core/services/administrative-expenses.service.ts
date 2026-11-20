import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import {
  AdministrativeExpenseDto,
  AdministrativeExpenseListDto,
  AdministrativeExpenseRequestDto
} from '../../shared/models/dtos/administrative-expense.dto';
import { getApiBaseUrl } from '../config/api-base';

/** API client for administrative expenses (cost center 761 / petty cash). */
@Injectable({ providedIn: 'root' })
export class AdministrativeExpensesService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${getApiBaseUrl()}/api/v1/sales/administrative-expenses`;

  listByFortnight(year: number, month: number, fortnight: 1 | 2): Observable<AdministrativeExpenseListDto> {
    const params = new HttpParams()
      .set('year', year)
      .set('month', month)
      .set('fortnight', fortnight);
    return this.http.get<AdministrativeExpenseListDto>(this.apiUrl, { params });
  }

  listByDateRange(fromDate: string, toDate: string): Observable<AdministrativeExpenseListDto> {
    const params = new HttpParams()
      .set('fromDate', fromDate)
      .set('toDate', toDate);
    return this.http.get<AdministrativeExpenseListDto>(this.apiUrl, { params });
  }

  register(request: AdministrativeExpenseRequestDto): Observable<AdministrativeExpenseDto> {
    return this.http.post<AdministrativeExpenseDto>(this.apiUrl, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
