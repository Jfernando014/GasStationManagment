/** First month of the calculation and of the accumulated total (yyyy-MM). Earlier months are rejected. */
export const INCENTIVES_START_MONTH = '2025-11';

export type UnresolvedReason = 'PRODUCT_NOT_FOUND' | 'NO_RULE_IN_FORCE';

/** Units of one additive that earned the same value per unit. Days are "YYYY-MM-DD" (Colombia), never a Date. */
export interface IncentiveLine {
  sku: string;
  productName: string;
  units: number;
  valuePerUnit: number;
  subtotal: number;
  /** First day of the month this value applied. */
  periodStart: string;
  /** Last day of the month this value applied; null when it held until the end of the month. */
  periodEnd: string | null;
}

/** Units sold that could not be turned into pesos; they are in no total. */
export interface UnresolvedUnits {
  sku: string;
  productName: string;
  units: number;
  reason: UnresolvedReason;
}

export interface VendorIncentive {
  document: string;
  /** The document when the backend has no name for the seller. */
  name: string;
  lines: IncentiveLine[];
  unresolved: UnresolvedUnits[];
  monthTotal: number;
  accumulatedTotal: number;
  /** Units since the start of the accumulated total that are missing from it; 0 means it is complete. */
  accumulatedUnresolvedUnits: number;
}

export interface MonthlyIncentives {
  month: string;
  accumulatedFrom: string;
  vendors: VendorIncentive[];
}

/** The three outcomes of asking for a month; components never look at HTTP codes. */
export type MonthResult =
  | { outcome: 'OK'; data: MonthlyIncentives }
  /** The month is in the future, before the start or not a month at all. */
  | { outcome: 'INVALID_MONTH'; message: string }
  | { outcome: 'ERROR'; message: string };

/** A closed day whose additive sales have not been stored yet. */
export interface PendingDay {
  date: string;
  attempts: number;
}

export type ReprocessResult =
  | { outcome: 'PROCESSED' }
  | { outcome: 'ERROR'; message: string };
