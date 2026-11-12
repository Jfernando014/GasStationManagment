import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { catchError, map, Observable, of } from 'rxjs';

import { stationToday } from '../../../shared/helpers/shift-display.helpers';
import { CreateParametersRequestDto, ParameterVersionDto } from '../models/overtime.dto';
import { ActionResult, LoadResult, NewParameterVersion, ParameterVersion } from '../models/overtime.model';
import { toActionError, toLoadError } from './overtime-errors';

const FORBIDDEN_MESSAGE = 'No tienes permiso para crear versiones';

/** Where each backend error belongs in the "Nueva versión" form. */
const FIELD_BY_CODE: Record<string, string> = {
  INVALID_PARAMETER_REASON: 'reason',
  PARAMETER_VERSION_EXISTS: 'validFrom',
  PARAMETER_VALID_FROM_NOT_AFTER_LAST_REGISTERED: 'validFrom'
};

/** Versions of the overtime parameters; they are never edited, a change is a new version with a start day. */
@Injectable({ providedIn: 'root' })
export class OvertimeParametersService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = 'http://localhost:8080/api/v1/workers/overtime-parameters';

  /** Every version, the newest first. */
  getHistory(): Observable<LoadResult<ParameterVersion[]>> {
    return this.http.get<ParameterVersionDto[]>(`${this.apiUrl}/history`).pipe(
      map<ParameterVersionDto[], LoadResult<ParameterVersion[]>>(dtos => ({ outcome: 'OK', data: toVersions(dtos) })),
      catchError((error: unknown) => of(toLoadError(error)))
    );
  }

  /** Administrator only. The reason is optional: without it nothing is sent. */
  create(version: NewParameterVersion): Observable<ActionResult> {
    const reason = version.reason.trim();
    const body: CreateParametersRequestDto = {
      validFrom: version.validFrom,
      nightStartHour: version.nightStartHour,
      restHours: version.restHours,
      restMinShiftHours: version.restMinShiftHours,
      fixedHedoHours: version.fixedHedoHours,
      fixedHedoRoles: version.fixedHedoRoles,
      ...(reason ? { reason } : {})
    };
    return this.http.post<unknown>(this.apiUrl, body).pipe(
      map<unknown, ActionResult>(() => ({ outcome: 'OK' })),
      catchError((error: unknown) => of(toActionError(error, FIELD_BY_CODE, FORBIDDEN_MESSAGE)))
    );
  }
}

function toVersions(dtos: ParameterVersionDto[]): ParameterVersion[] {
  const today = stationToday();
  return dtos.map((dto, index) => ({
    id: dto.id,
    number: dtos.length - index,
    validFrom: dto.validFrom,
    validTo: dto.validTo,
    current: dto.current,
    scheduled: dto.validFrom > today,
    nightStartHour: dto.nightStartHour,
    restHours: dto.restHours,
    restMinShiftHours: dto.restMinShiftHours,
    fixedHedoHours: dto.fixedHedoHours,
    fixedHedoRoles: dto.fixedHedoRoles,
    reason: dto.reason?.trim() ? dto.reason : null,
    createdBy: dto.createdBy,
    createdAt: dto.createdAt
  }));
}
