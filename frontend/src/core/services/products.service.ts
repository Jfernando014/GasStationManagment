import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import {
  ProductFilters,
  ProductRequest,
  ProductResponse
} from '../../shared/models/dtos/product.dto';

/** Catalog of lubricants, additives and other products sold at the station. */
@Injectable({ providedIn: 'root' })
export class ProductsService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = 'http://localhost:8080/api/v1/inventory/products';

  /** Only the filters with a value are sent. */
  list(filters: ProductFilters = {}): Observable<ProductResponse[]> {
    let params = new HttpParams();
    if (filters.category) {
      params = params.set('category', filters.category);
    }
    if (filters.active !== undefined && filters.active !== null) {
      params = params.set('active', filters.active);
    }
    return this.http.get<ProductResponse[]>(this.apiUrl, { params });
  }

  create(request: ProductRequest): Observable<ProductResponse> {
    return this.http.post<ProductResponse>(this.apiUrl, request);
  }

  update(id: number, request: ProductRequest): Observable<ProductResponse> {
    return this.http.put<ProductResponse>(`${this.apiUrl}/${id}`, request);
  }
}
