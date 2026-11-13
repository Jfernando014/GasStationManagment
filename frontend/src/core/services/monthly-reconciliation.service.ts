import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { MonthlyReconciliation } from '../../shared/models/dtos/monthly-reconciliation.dto';

@Injectable({ providedIn: 'root' })
export class MonthlyReconciliationService {
  private readonly http = inject(HttpClient);
  private readonly url = 'http://localhost:8080/api/v1/sales/reconciliation/monthly';

  getMonthly(month: string): Observable<MonthlyReconciliation> {
    return this.http.get<MonthlyReconciliation>(`${this.url}/${encodeURIComponent(month)}`);
  }
}
