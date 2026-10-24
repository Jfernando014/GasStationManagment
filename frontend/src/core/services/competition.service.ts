import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { map, Observable } from 'rxjs';
import { Competitor, CompetitorRequest, PriceComparison, PriceObservation, PriceObservationRequest, PriceSimulation, PriceSimulationRequest } from '../../shared/models/dtos/competition.dto';
import { parseComparison, parseCompetitor, parseCompetitors, parsePriceObservation, parseSimulation } from '../../shared/helpers/statistics-contracts.helpers';
import { decimalText } from '../../shared/helpers/sales-target.helpers';

@Injectable({ providedIn: 'root' })
export class CompetitionService {
  private readonly http = inject(HttpClient);
  private readonly url = 'http://localhost:8080/api/v1/competition';
  private readonly options = { responseType: 'text' as const, headers: new HttpHeaders({ 'Content-Type': 'application/json' }) };
  list(): Observable<Competitor[]> { return this.http.get(`${this.url}/competitors`, { responseType: 'text' }).pipe(map(parseCompetitors)); }
  register(body: CompetitorRequest): Observable<Competitor> { return this.http.post(`${this.url}/competitors`, body, this.options).pipe(map(parseCompetitor)); }
  compare(asOf?: string): Observable<PriceComparison> {
    const params = asOf ? new HttpParams().set('asOf', asOf) : new HttpParams();
    return this.http.get(`${this.url}/comparison`, { params, responseType: 'text' }).pipe(map(parseComparison));
  }
  observe(request: PriceObservationRequest): Observable<PriceObservation> {
    if (!/^[1-9]\d*$/.test(request.competitorId)) throw new Error('Selecciona una estación válida.');
    const price = this.money(request.pricePerGallon, false);
    const body = `{"competitorId":${request.competitorId},"fuelType":${JSON.stringify(request.fuelType)},"pricePerGallon":${price},"observedAt":${JSON.stringify(request.observedAt)}}`;
    return this.http.post(`${this.url}/observations`, body, this.options).pipe(map(parsePriceObservation));
  }
  simulate(request: PriceSimulationRequest): Observable<PriceSimulation> {
    const price = this.money(request.proposedPrice, false), cost = request.acquisitionCost === null ? 'null' : this.money(request.acquisitionCost, true);
    const metadata = JSON.stringify({ fuelType: request.fuelType, ...(request.asOf ? { asOf: request.asOf } : {}) });
    const body = `${metadata.slice(0, -1)},"proposedPrice":${price},"acquisitionCost":${cost}}`;
    return this.http.post(`${this.url}/simulation`, body, this.options).pipe(map(parseSimulation));
  }
  private money(value: string, allowZero: boolean): string {
    const normalized = decimalText(value);
    if (normalized === null || normalized.split('.')[0].length > 10 || (normalized.split('.')[1]?.length ?? 0) > 2 || (!allowZero && normalized === '0')) throw new Error('Precio inválido: máximo 10 enteros y dos decimales.');
    return normalized;
  }
}
