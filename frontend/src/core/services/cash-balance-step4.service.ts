import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import {
  BalanceResponseDto,
  CashBalanceStepResponseDto,
  SavingsFundMovementRequestDto,
  SavingsFundMovementResponseDto
} from '../../shared/models/dtos/cash-balance-step4.dto';

@Injectable({ providedIn: 'root' })
export class CashBalanceStep4Service {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = 'http://localhost:8080/api/v1/sales';

  getCashBalance(date: string): Observable<CashBalanceStepResponseDto> {
    return this.http.get<CashBalanceStepResponseDto>(`${this.apiUrl}/cash-balance/${date}`);
  }

  saveDominusBalance(date: string, accumulatedBalance: number): Observable<CashBalanceStepResponseDto> {
    return this.http.put<CashBalanceStepResponseDto>(`${this.apiUrl}/cash-balance/${date}/dominus`, {
      accumulatedBalance
    });
  }

  registerFundPayment(request: SavingsFundMovementRequestDto): Observable<SavingsFundMovementResponseDto> {
    return this.http.post<SavingsFundMovementResponseDto>(`${this.apiUrl}/savings-fund/payments`, request);
  }

  getFundBalance(date: string): Observable<BalanceResponseDto> {
    const params = new HttpParams().set('date', date);
    return this.http.get<BalanceResponseDto>(`${this.apiUrl}/savings-fund/balance`, { params });
  }
}
