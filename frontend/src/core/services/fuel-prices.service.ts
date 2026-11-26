import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { getApiBaseUrl } from '../config/api-base';

import { FuelValueDto, FuelValueKind, FuelValueRequestDto } from '../../shared/models/dtos/fuel-price.dto';

const RESOURCES: Record<FuelValueKind, string> = {
  sale: 'fuel-prices',
  purchase: 'fuel-purchase-prices'
};

/** Sale prices and purchase costs per gallon, each with the day it starts to rule. */
@Injectable({ providedIn: 'root' })
export class FuelPricesService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${getApiBaseUrl()}/api/v1/inventory`;

  /** Every value of every fuel, past, current and scheduled. */
  history(kind: FuelValueKind): Observable<FuelValueDto[]> {
    return this.http.get<FuelValueDto[]>(this.url(kind)).pipe(map(values => values.map(normalize)));
  }

  create(kind: FuelValueKind, request: FuelValueRequestDto): Observable<FuelValueDto> {
    return this.http.post<FuelValueDto>(this.url(kind), request).pipe(map(normalize));
  }

  update(kind: FuelValueKind, id: number, request: FuelValueRequestDto): Observable<FuelValueDto> {
    return this.http.put<FuelValueDto>(`${this.url(kind)}/${id}`, request).pipe(map(normalize));
  }

  private url(kind: FuelValueKind): string {
    return `${this.baseUrl}/${RESOURCES[kind]}`;
  }
}

/** The backend answers 14100 right after saving and 14100.00 when it is read again: both become the same number. */
function normalize(value: FuelValueDto): FuelValueDto {
  return { ...value, pricePerGallon: Number(value.pricePerGallon) };
}
