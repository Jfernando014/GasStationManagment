import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { map, Observable } from 'rxjs';
import { CompareStatisticsRequest, ExplicitStatisticsRequest, SalesStatisticsResponse } from '../../shared/models/dtos/sales-statistics.dto';
import { dateRangeError, parseStatistics, serializeObservations } from '../../shared/helpers/sales-statistics.helpers';
import { BasketStatisticsResponse } from '../../shared/models/dtos/basket-statistics.dto';
import { parseBasketStatistics } from '../../shared/helpers/statistics-contracts.helpers';

/** SCRUM-40 explicit calculations and the SCRUM-83 basket contract; never manufactures sales. */
@Injectable({ providedIn: 'root' })
export class SalesStatisticsService {
  private readonly http = inject(HttpClient);
  private readonly url = 'http://localhost:8080/api/v1/sales';
  private readonly options = { responseType: 'text' as const, headers: new HttpHeaders({ 'Content-Type': 'application/json' }) };
  averages(request: ExplicitStatisticsRequest): Observable<SalesStatisticsResponse> {
    return this.http.post(`${this.url}/averages/explicit`, serializeObservations(request), this.options).pipe(map(text => parseStatistics(text, request)));
  }
  compare(request: CompareStatisticsRequest): Observable<SalesStatisticsResponse> {
    const body = `{"sales":${serializeObservations(request.sales)},"first":${JSON.stringify(request.first)},"second":${JSON.stringify(request.second)}}`;
    return this.http.post(`${this.url}/scenarios/compare/explicit`, body, this.options).pipe(map(text => parseStatistics(text, request.sales)));
  }
  basket(from: string, to: string): Observable<BasketStatisticsResponse> {
    if (dateRangeError(from, to)) throw new Error('Rango de fechas inválido.');
    const params = new HttpParams().set('from', from).set('to', to).set('period', 'DAY').set('outputScale', 2);
    return this.http.get(`${this.url}/statistics/basket`, { params, responseType: 'text' }).pipe(map(text => {
      const data = parseBasketStatistics(text);
      if (data.from !== from || data.to !== to) throw new Error('El período recibido no corresponde a la consulta.');
      return data;
    }));
  }
}
