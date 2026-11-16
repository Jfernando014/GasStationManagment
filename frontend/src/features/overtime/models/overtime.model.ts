/** First month the screens offer; the hours exist only from the first closed journal on. */
export const OVERTIME_START_MONTH = '2025-11';

/** Longest and shortest reason the backend accepts, for adjustments and for versions of the parameters. */
export const MIN_REASON_LENGTH = 3;
export const MAX_REASON_LENGTH = 255;

/** Surcharges (RDF, RNO, RNF) first, then the overtime hours. */
export const OVERTIME_CATEGORIES = ['RDF', 'RNO', 'RNF', 'HEDO', 'HENO', 'HEDF', 'HENF'] as const;
export type OvertimeCategory = typeof OVERTIME_CATEGORIES[number];

/** Whole hours of each category. A total to pay can be negative. */
export type CategoryHours = Record<OvertimeCategory, number>;

/** Days are "YYYY-MM-DD" (Colombia), never a Date. */
export interface Fortnight {
  /** "yyyy-MM". */
  month: string;
  number: 1 | 2;
  from: string;
  to: string;
}

export interface OvertimeWorker {
  id: string;
  name: string;
  /** TITULAR or APOYO; null when Turnos does not know it. */
  role: string | null;
}

export interface OvertimeDay {
  date: string;
  /** "dom", "lun" … */
  weekday: string;
  holiday: boolean;
  /** Shift code worked; null when the worker rested or the day has no record. */
  workedCode: string | null;
  grossHours: number;
  hours: CategoryHours;
  /** First day of the version of the parameters this day was calculated with. */
  parametersFrom: string;
}

export interface OvertimeAdjustment {
  id: number;
  category: OvertimeCategory;
  direction: 'ADD' | 'SUBTRACT';
  hours: number;
  reason: string;
  createdAt: string;
  /** Who registered it; null when the backend does not know. */
  createdBy: string | null;
}

/** Why the hours of a day cannot be trusted in a fortnight. */
export type IncompleteReason = 'MISSING_RECORD' | 'PENDING' | 'NOVELTIES_NOT_APPLIED';

export interface AffectedDate {
  date: string;
  reason: IncompleteReason;
}

export interface FixedHedo {
  /** True when the role of the worker has the fixed HEDO switch on. */
  applies: boolean;
  hours: number;
}

export interface OvertimeSheet {
  worker: OvertimeWorker;
  fortnight: Fortnight;
  days: OvertimeDay[];
  subtotals: CategoryHours;
  grossHours: number;
  fixedHedo: FixedHedo;
  adjustments: OvertimeAdjustment[];
  adjustmentsNet: CategoryHours;
  totalToPay: CategoryHours;
  incomplete: boolean;
  noveltiesUnconfirmed: boolean;
  affectedDates: AffectedDate[];
  /** First day of each version of the parameters used, oldest first. */
  parameterVersions: string[];
}

export interface OvertimeSummaryRow {
  worker: OvertimeWorker;
  grossHours: number;
  totalToPay: CategoryHours;
  incomplete: boolean;
  noveltiesUnconfirmed: boolean;
}

export interface OvertimeSummary {
  fortnight: Fortnight;
  workers: OvertimeSummaryRow[];
  affectedDates: AffectedDate[];
}

/** The three outcomes of asking for something; components never look at HTTP codes. */
export type LoadResult<T> =
  | { outcome: 'OK'; data: T }
  /** The worker or the day does not exist (or the link is malformed). */
  | { outcome: 'NOT_FOUND'; message: string }
  | { outcome: 'ERROR'; message: string };

/** Result of an action: it either worked or it says why not. `field` is where the message belongs. */
export type ActionResult =
  | { outcome: 'OK' }
  | { outcome: 'ERROR'; message: string; field: string | null };

export interface NewAdjustment {
  category: OvertimeCategory;
  direction: 'ADD' | 'SUBTRACT';
  hours: number;
  reason: string;
}

/** A closed day: what happened to its hours and, when they were saved, who worked. */
export type DayStatus = 'PENDING' | 'PROCESSED' | 'NOT_REGISTERED';

export interface DayWorker {
  workerId: string;
  name: string;
  /** Shift worked; null when the worker was on the grid but did not work. */
  workedCode: string | null;
  /** Code on the grid that day. */
  gridCode: string;
  hours: number;
  absent: boolean;
  /** Novelty text, e.g. "Reemplazo de Ana"; null when there is none. */
  novelty: string | null;
}

export interface DayHours {
  date: string;
  status: DayStatus;
  /** False when the hours were saved from the grid, without the novelties of the day. */
  noveltiesApplied: boolean;
  attempts: number;
  workers: DayWorker[];
}

export type ReprocessResult =
  | { outcome: 'PROCESSED' }
  | { outcome: 'ERROR'; message: string };

/** One version of the parameters, from the oldest (number 1) to the newest. */
export interface ParameterVersion {
  id: number;
  /** Position in time, 1 for the first one ever created. */
  number: number;
  validFrom: string;
  validTo: string | null;
  /** The one that applies today; the backend says it, it is not deduced from `validTo`. */
  current: boolean;
  /** It starts after today. */
  scheduled: boolean;
  nightStartHour: number;
  restHours: number;
  restMinShiftHours: number;
  fixedHedoHours: number;
  fixedHedoRoles: string[];
  reason: string | null;
  createdBy: string | null;
  createdAt: string;
}

export interface NewParameterVersion {
  validFrom: string;
  nightStartHour: number;
  restHours: number;
  restMinShiftHours: number;
  fixedHedoHours: number;
  fixedHedoRoles: string[];
  reason: string;
}
