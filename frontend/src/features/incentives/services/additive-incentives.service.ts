import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable, signal } from '@angular/core';
import { catchError, map, Observable, of, Subscription } from 'rxjs';

import { getErrorMessage, getFieldErrors, isApiError } from '../../../shared/helpers/problem-details.helpers';
import { stationToday } from '../../../shared/helpers/shift-display.helpers';
import {
  AdditiveIncentiveRow, CreateIncentiveRuleRequest, CreateIncentiveRuleResult, IncentiveRuleHistoryItem,
  IncentiveRuleStatus
} from '../models/additive-incentive.model';
import {
  AdditiveIncentiveDto, CreateRuleRequestDto, CreateRuleResponseDto, RuleHistoryDto, RuleStatusDto
} from '../models/incentive-rule.dto';

const CREATED_MESSAGE = 'Regla de incentivo creada';

/** Messages the screen shows as they are, by the stable `code` of the backend error. */
const ERROR_MESSAGES: Record<string, string> = {
  INVALID_VALUE_PER_UNIT: 'El valor por unidad debe ser un número entero de pesos entre 0 y 100.000',
  VALID_FROM_NOT_AFTER_LAST_CLOSED: 'La fecha de vigencia debe ser posterior a la última jornada cerrada',
  RULE_ALREADY_EXISTS: 'Ya existe una regla con vigencia desde esa fecha; elija otra',
  PRODUCT_INACTIVE: 'El aditivo está inactivo; no se pueden crear reglas nuevas'
};

const STATUS_BY_DTO: Record<RuleStatusDto, IncentiveRuleStatus> = {
  PROGRAMADO: 'SCHEDULED',
  VIGENTE: 'CURRENT',
  ANTERIOR: 'PREVIOUS'
};

/**
 * Incentive rules of the additives. It also keeps the queried day and the list, so the table and the
 * "new rule" form read the same state; `refresh()` reloads it after a rule is created.
 * Days are Colombian calendar days: always "YYYY-MM-DD", never converted with `toISOString()`.
 */
@Injectable({ providedIn: 'root' })
export class AdditiveIncentivesService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = 'http://localhost:8080/api/v1/incentives/rules';
  private loadSubscription: Subscription | null = null;

  /** Day the values are shown for. */
  readonly date = signal(stationToday());
  readonly additives = signal<AdditiveIncentiveRow[]>([]);
  readonly loading = signal(false);
  readonly loadError = signal('');

  getAdditives(date: string): Observable<AdditiveIncentiveRow[]> {
    return this.http.get<AdditiveIncentiveDto[]>(this.apiUrl, { params: new HttpParams().set('date', date) })
      .pipe(map(rows => rows.map(toRow)));
  }

  getHistory(additiveId: number): Observable<IncentiveRuleHistoryItem[]> {
    return this.http.get<RuleHistoryDto[]>(`${this.apiUrl}/${encodeURIComponent(additiveId)}/history`)
      .pipe(map(items => items.map(toHistoryItem)));
  }

  /** Never errors: every outcome, including a failed request, comes back as a result. */
  createRule(request: CreateIncentiveRuleRequest): Observable<CreateIncentiveRuleResult> {
    const body: CreateRuleRequestDto = {
      productId: request.additiveId,
      valuePerUnit: request.valuePerUnit,
      validFrom: request.effectiveFrom,
      reason: request.reason?.trim() || null
    };
    return this.http.post<CreateRuleResponseDto>(this.apiUrl, body).pipe(
      map(toCreateResult),
      catchError((error: unknown) => of(toErrorResult(error)))
    );
  }

  /** Loads the list for a day and keeps it in the state; a newer request replaces one still running. */
  load(date: string): void {
    this.date.set(date);
    this.loadSubscription?.unsubscribe();
    this.loading.set(true);
    this.loadError.set('');
    this.loadSubscription = this.getAdditives(date).subscribe({
      next: rows => {
        this.additives.set(rows);
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.loadError.set(getErrorMessage(error));
        this.loading.set(false);
      }
    });
  }

  /** Reloads the current day, e.g. after creating a rule. */
  refresh(): void {
    this.load(this.date());
  }
}

function toRow(dto: AdditiveIncentiveDto): AdditiveIncentiveRow {
  return {
    additiveId: dto.productId,
    additiveName: dto.productName,
    currentValue: dto.valuePerUnit,
    currentSince: dto.validFrom,
    nextValue: dto.nextValuePerUnit,
    nextSince: dto.nextValidFrom,
    hasEverHadIncentive: !dto.noIncentive,
    salePrice: null // TODO: the backend does not send the sale price yet; fill it in when it does.
  };
}

function toHistoryItem(dto: RuleHistoryDto): IncentiveRuleHistoryItem {
  return {
    value: dto.valuePerUnit,
    effectiveFrom: dto.validFrom,
    reason: dto.reason?.trim() ? dto.reason : null,
    status: STATUS_BY_DTO[dto.status]
  };
}

/**
 * Backend contract (IncentiveRuleController.create):
 * 201 + created=true (with `warnings` when the value is above half the price or the day is not the 1st),
 * 200 + created=false when the additive already pays that value that day, and
 * a problem detail with a stable `code` for every failure.
 */
function toCreateResult(response: CreateRuleResponseDto): CreateIncentiveRuleResult {
  if (!response.created) {
    return { outcome: 'NO_CHANGE', message: response.message };
  }
  const message = response.message || CREATED_MESSAGE;
  return response.warnings.length > 0
    ? { outcome: 'CREATED_WITH_WARNINGS', message, warnings: response.warnings }
    : { outcome: 'CREATED', message };
}

function toErrorResult(error: unknown): CreateIncentiveRuleResult {
  const code = isApiError(error) ? error.code ?? null : null;
  const known = code ? ERROR_MESSAGES[code] : undefined;
  const fieldMessage = getFieldErrors(error)[0]?.message;
  return { outcome: 'ERROR', code, message: known ?? fieldMessage ?? getErrorMessage(error) };
}
