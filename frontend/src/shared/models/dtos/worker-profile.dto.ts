/** What a novelty meant for the worker, seen from the worker (WorkerMonthNovelty.kind). */
export type WorkerNoveltyKind = 'ABSENT' | 'COVERING' | 'SHIFT_CHANGE' | 'SWAP';

/** Novelty of a day seen from the worker. */
export interface WorkerMonthNovelty {
  kind: WorkerNoveltyKind;
  /** Code the worker had in the schedule that day; DESCANSO without a row. */
  originalCode: string;
  /** Code worked instead; null when absent. */
  finalCode: string | null;
  reason: string;
  /** A code of another role was accepted on purpose. */
  exceptionalCoverage: boolean;
}

/** One day of the month of a worker (WorkerMonthDay). */
export interface WorkerMonthDay {
  /** Calendar day, yyyy-MM-dd. */
  date: string;
  /** Code of the schedule; null when the worker has no row that day. */
  gridCode: string | null;
  /** Code actually worked with the novelties applied; null when absent. */
  workedCode: string | null;
  /** Hours of the schedule code; 0 without a row. */
  hours: number;
  note: string | null;
  novelties: WorkerMonthNovelty[];
}

/** A working code and the number of days it has in the month. */
export interface WorkerMonthCodeCount {
  code: string;
  count: number;
}

/** Totals of the month, calculated over the schedule by the server. */
export interface WorkerMonthSummary {
  scheduledHours: number;
  scheduledShifts: number;
  restDays: number;
  shiftsByCode: WorkerMonthCodeCount[];
  /** Times the worker covered a colleague. */
  replacementsMade: number;
  /** Days the worker did not work, with or without a replacement. */
  absences: number;
}

/** Month of one worker (WorkerMonthResponse). */
export interface WorkerMonth {
  workerId: string;
  /** yyyy-MM. */
  month: string;
  /** One element per day of the month. */
  days: WorkerMonthDay[];
  summary: WorkerMonthSummary;
}

export type PerformanceWeekday =
  | 'MONDAY'
  | 'TUESDAY'
  | 'WEDNESDAY'
  | 'THURSDAY'
  | 'FRIDAY'
  | 'SATURDAY'
  | 'SUNDAY'
  | 'HOLIDAY';

/** Performance of a worker in one role on one kind of day. */
export interface RolePerformance {
  shifts: number;
  /** Gallons per shift; null without shifts. */
  averageGallons: number | null;
  /** 100 = same as the station average for that kind of shift and day; null without shifts. */
  performancePercent: number | null;
}

export interface WeekdayPerformance {
  weekday: PerformanceWeekday;
  asHolder: RolePerformance;
  asSupport: RolePerformance;
}

/** Best sale of the worker in one shift. */
export interface MaxSale {
  gallons: number;
  /** Calendar day, yyyy-MM-dd. */
  date: string;
  shiftCode: string;
}

export interface ClosedShiftsCount {
  total: number;
  asHolder: number;
  asSupport: number;
}

/** Historical performance of a worker, from the closed journals. */
export interface WorkerPerformance {
  /** First closed journal considered; null when there is none. */
  periodFrom: string | null;
  periodTo: string | null;
  closedShifts: ClosedShiftsCount;
  byWeekday: WeekdayPerformance[];
  maxSale: MaxSale | null;
}
