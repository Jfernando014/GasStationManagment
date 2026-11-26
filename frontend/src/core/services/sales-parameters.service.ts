import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { getApiBaseUrl } from '../config/api-base';

import {
  SalesParameterDto, SalesParameterKey, SalesParameterRequestDto
} from '../../shared/models/dtos/sales-parameter.dto';

/** Values of Ventas with a start date: freight, financial cost and card commission. */
@Injectable({ providedIn: 'root' })
export class SalesParametersService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${getApiBaseUrl()}/api/v1/sales/parameters`;

  /** Every value of a parameter, newest first. */
  history(key: SalesParameterKey): Observable<SalesParameterDto[]> {
    return this.http.get<SalesParameterDto[]>(`${this.apiUrl}/${key}/history`)
      .pipe(map(values => values.map(normalize)));
  }

  /** Only the administrator may call it. A value already starting that day is replaced. */
  register(key: SalesParameterKey, request: SalesParameterRequestDto): Observable<SalesParameterDto> {
    return this.http.put<SalesParameterDto>(`${this.apiUrl}/${key}`, request).pipe(map(normalize));
  }
}

function normalize(parameter: SalesParameterDto): SalesParameterDto {
  return { ...parameter, value: Number(parameter.value) };
}
