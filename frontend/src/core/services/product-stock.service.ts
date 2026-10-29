import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { ProductCategory } from '../../shared/models/dtos/product.dto';
import {
  InitialStockRequest,
  ProductStockResponse,
  SalesSyncResponse,
  StockAdjustmentRequest,
  StockEntryRequest,
  StockMovementResponse,
  StockTransferRequest
} from '../../shared/models/dtos/product-stock.dto';

/**
 * Stock of lubricants, additives and other products in the office and the counter.
 * Quantities are whole units; errors arrive already converted to ApiError by the error interceptor.
 */
@Injectable({ providedIn: 'root' })
export class ProductStockService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = 'http://localhost:8080/api/v1/inventory/product-stock';

  listByJournalDate(journalDate: string, category?: ProductCategory | null): Observable<ProductStockResponse[]> {
    let params = new HttpParams().set('journalDate', journalDate);
    if (category) {
      params = params.set('category', category);
    }
    return this.http.get<ProductStockResponse[]>(this.apiUrl, { params });
  }

  /** The opening stock can be registered only once per product. */
  registerInitial(request: InitialStockRequest): Observable<StockMovementResponse[]> {
    return this.http.post<StockMovementResponse[]>(`${this.apiUrl}/initial`, request);
  }

  registerEntry(request: StockEntryRequest): Observable<StockMovementResponse> {
    return this.http.post<StockMovementResponse>(`${this.apiUrl}/entries`, request);
  }

  /** Moves units from the office to the counter; the backend returns both ledger lines. */
  registerTransfer(request: StockTransferRequest): Observable<StockMovementResponse[]> {
    return this.http.post<StockMovementResponse[]>(`${this.apiUrl}/transfers`, request);
  }

  registerAdjustment(request: StockAdjustmentRequest): Observable<StockMovementResponse> {
    return this.http.post<StockMovementResponse>(`${this.apiUrl}/adjustments`, request);
  }

  /** Replaces the units sold in step 2 for that date, so it can be repeated after a correction. */
  syncSales(journalDate: string): Observable<SalesSyncResponse> {
    return this.http.put<SalesSyncResponse>(`${this.apiUrl}/sales/${journalDate}`, {});
  }
}
