/** Worker role as returned by the backend (RoleResponse). */
export interface RoleOption {
  id: string;
  /** TITULAR or APOYO. */
  name: string;
  dispenser: number;
}

/** DAY or NIGHT; null for DESCANSO. */
export type ShiftPeriod = 'DAY' | 'NIGHT';

/** One block of working hours (ShiftSchedule.Segment). endHour lower than startHour ends the next day. */
export interface ShiftScheduleSegment {
  startHour: number;
  endHour: number;
}

/** Working segments and total hours of a shift code (ShiftSchedule). */
export interface ShiftSchedule {
  segments: ShiftScheduleSegment[];
  totalHours: number;
}

/** Shift code of the catalog (ShiftCodeResponse). */
export interface ShiftCode {
  id: string;
  code: string;
  name: string;
  /** Null for DESCANSO. */
  role: RoleOption | null;
  /** DAY or NIGHT; null for DESCANSO. */
  period: ShiftPeriod | null;
  schedule: ShiftSchedule;
  /** True for titular codes that need a support worker (DIA6, 9-6). */
  requiresSupport: boolean;
  /** Only on support codes: id of the titular code it pairs with (12-7 pairs with DIA6). */
  pairedWithId: string | null;
  active: boolean;
}
