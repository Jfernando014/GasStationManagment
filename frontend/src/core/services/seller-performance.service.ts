import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { map, Observable } from 'rxjs';
import { FullSellerPerformanceResponse, PerformanceQuery, SellerPerformanceResponse } from '../../shared/models/dtos/seller-performance.dto';
import { parseSellerPerformance, validatePerformanceQuery } from '../../shared/helpers/seller-performance.helpers';
import { parsePerformanceReport } from '../../shared/helpers/seller-performance-reports.helpers';
import { getApiBaseUrl } from '../config/api-base';

@Injectable({ providedIn: 'root' })
export class SellerPerformanceService {
  private readonly http = inject(HttpClient);
  private readonly url = `${getApiBaseUrl()}/api/v1/statistics/seller-performance`;

  read(query: PerformanceQuery): Observable<SellerPerformanceResponse> {
    return this.request(query, parseSellerPerformance);
  }

  readReport(query: PerformanceQuery): Observable<FullSellerPerformanceResponse> {
    return this.request(query, parsePerformanceReport);
  }

  private request<T>(query: PerformanceQuery, parse: (text: string, query: PerformanceQuery) => T): Observable<T> {
    validatePerformanceQuery(query);
    const request = { ...query };
    const params = new HttpParams().set('from', request.from).set('to', request.to)
      .set('measure', request.measure).set('outputScale', request.outputScale);
    return this.http.get(this.url, { params, responseType: 'text' }).pipe(
      map(text => parse(text, request))
    );
  }
}
