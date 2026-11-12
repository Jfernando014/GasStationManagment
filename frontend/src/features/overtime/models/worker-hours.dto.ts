/** Backend contract of /api/v1/workers/hours (WorkerHoursDtos). Only what the screen reads. */

export type HoursSyncStatusDto = 'PROCESSED' | 'PENDING';

export interface HoursSyncDto {
  date: string;
  status: HoursSyncStatusDto;
  /** False when the day was saved from the grid, before its novelties could be applied. */
  noveltiesApplied: boolean;
  attempts: number;
  lastError: string | null;
}

export interface WorkerDayHoursDto {
  workerId: string;
  workerName: string;
  workedCode: string | null;
  gridCode: string;
  totalHours: number;
  absent: boolean;
  /** NONE, REPLACEMENT, ABSENCE, SHIFT_CHANGE or SWAP. */
  noveltyType: string;
  replacedWorkerName: string | null;
  reason: string | null;
}

export interface DayHoursDto {
  sync: HoursSyncDto;
  workers: WorkerDayHoursDto[];
}
