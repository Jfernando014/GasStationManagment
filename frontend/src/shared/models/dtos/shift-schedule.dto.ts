import type { ShiftPeriod } from './shift-catalog.dto';

/** Shift of a worker on one day (ScheduleDay). */
export interface ScheduleDay {
  date: string;
  shiftCode: string;
  /** Reason for the change; null when there is none. */
  note: string | null;
}

/** Scheduled days of one worker in the requested range (WorkerSchedule). */
export interface ScheduleWorkerRow {
  workerId: string;
  fullName: string;
  /** Ascending order; a missing day is not scheduled. */
  days: ScheduleDay[];
}

/** Coverage of one day over every active worker (DayCoverage). */
export interface DailyCoverage {
  date: string;
  dayCovered: boolean;
  nightCovered: boolean;
  supportCovered: boolean;
  covered: boolean;
}

/** Schedule of a date range with its daily coverage (ScheduleResponse). */
export interface ScheduleResponse {
  from: string;
  to: string;
  /** Only active workers with at least one shift in the range, after the filters. */
  workers: ScheduleWorkerRow[];
  /** Over every active worker, regardless of the filters. */
  coverage: DailyCoverage[];
}

/** Body of PUT /api/shifts/schedule/assignments. */
export interface AssignShiftRequest {
  workerId: string;
  date: string;
  shiftCode: string;
  note?: string | null;
}

/** Body of PUT /api/shifts/schedule/assignments/range. At most 62 days, from today on. */
export interface AssignRangeRequest {
  workerId: string;
  from: string;
  to: string;
  shiftCode: string;
  note?: string | null;
  leaveSundaysAsRest: boolean;
}

/** Result of changing the schedule (ScheduleUpdateResponse). uncoveredDates is a warning, not an error. */
export interface ScheduleUpdateResponse {
  saved: number;
  uncoveredDates: string[];
}

export type { ShiftPeriod };
