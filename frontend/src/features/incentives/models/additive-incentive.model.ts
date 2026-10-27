/** Largest value per unit the backend accepts (pesos). */
export const MAX_INCENTIVE_PER_UNIT = 100_000;

/** Largest reason length the backend accepts. */
export const MAX_INCENTIVE_REASON_LENGTH = 255;

/**
 * One additive with the incentive that applies on the queried day.
 * Calendar days are "YYYY-MM-DD" (Colombia), never a Date.
 *
 * - `hasEverHadIncentive === false`: show "Sin incentivo".
 * - `currentValue === null` with `hasEverHadIncentive`: only future values exist; show "—" and the next change.
 */
export interface AdditiveIncentiveRow {
  additiveId: number;
  additiveName: string;
  currentValue: number | null;
  currentSince: string | null;
  nextValue: number | null;
  nextSince: string | null;
  hasEverHadIncentive: boolean;
  /** The backend does not send it yet; the "more than half the price" warning comes from the backend. */
  salePrice: number | null;
}

/** Screen label of each status: Programado, Vigente, Anterior. */
export type IncentiveRuleStatus = 'SCHEDULED' | 'CURRENT' | 'PREVIOUS';

export interface IncentiveRuleHistoryItem {
  value: number;
  effectiveFrom: string;
  reason: string | null;
  status: IncentiveRuleStatus;
}

export interface CreateIncentiveRuleRequest {
  additiveId: number;
  /** Whole pesos, 0 to 100.000; 0 stops paying the incentive. */
  valuePerUnit: number;
  effectiveFrom: string;
  /** Optional, up to 255 characters. */
  reason?: string;
}

export interface IncentiveRuleWarning {
  code: 'UNUSUALLY_HIGH_VALUE' | 'VALID_FROM_NOT_FIRST_DAY';
  message: string;
}

/** The four outcomes of creating a rule; components never look at HTTP codes. */
export type CreateIncentiveRuleResult =
  | { outcome: 'CREATED'; message: string }
  | { outcome: 'CREATED_WITH_WARNINGS'; message: string; warnings: IncentiveRuleWarning[] }
  /** Nothing was created because the additive already pays that value on that day. */
  | { outcome: 'NO_CHANGE'; message: string }
  | { outcome: 'ERROR'; message: string; code: string | null };
