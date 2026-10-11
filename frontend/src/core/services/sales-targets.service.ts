import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { map, Observable } from 'rxjs';
import { CreateSalesTargetRequest, SalesTargetCategory, SalesTargetResponse } from '../../shared/models/dtos/sales-target.dto';
import { decimalText, parseSalesTarget, parseSalesTargets } from '../../shared/helpers/sales-target.helpers';

@Injectable({ providedIn: 'root' })
export class SalesTargetsService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = 'http://localhost:8080/api/v1/sales/targets';

  list(year: number): Observable<SalesTargetResponse[]> {
    return this.http.get(this.apiUrl, { params: new HttpParams().set('year', year), responseType: 'text' })
      .pipe(map(parseSalesTargets));
  }

  findById(id: string): Observable<SalesTargetResponse> {
    return this.http.get(`${this.apiUrl}/${encodeURIComponent(id)}`, { responseType: 'text' })
      .pipe(map(parseSalesTarget));
  }

  findByIdentity(year: number, category: SalesTargetCategory, month?: number): Observable<SalesTargetResponse> {
    let params = new HttpParams().set('year', year).set('category', category);
    if (month !== undefined) params = params.set('month', month);
    return this.http.get(`${this.apiUrl}/by-identity`, { params, responseType: 'text' })
      .pipe(map(parseSalesTarget));
  }

  create(request: CreateSalesTargetRequest): Observable<SalesTargetResponse> {
    const value = decimalText(request.value), maximum = decimalText(request.maximumSale);
    if (value === null || maximum === null || !Number.isInteger(request.year)) {
      throw new Error('Solicitud de meta inválida');
    }
    // Send JSON NUMBER tokens exactly; JSON.stringify(Number(value)) would lose precision.
    const metadata = JSON.stringify({ year: request.year, month: request.month ?? null,
      category: request.category, unit: request.unit });
    const body = `${metadata.slice(0, -1)},"value":${value},"maximumSale":${maximum}}`;
    return this.http.post(this.apiUrl, body, {
      headers: new HttpHeaders({ 'Content-Type': 'application/json' }), responseType: 'text'
    }).pipe(map(parseSalesTarget));
  }
}
