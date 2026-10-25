import type { ShiftScheduleSegment } from './shift-catalog.dto';

/** State of step 1 (day staff) of the journal of a date (Step1Status). UNKNOWN is never produced today, but the
 *  client must still tolerate it in case the temporary journal port changes. */
export type Step1Status = 'NO_JOURNAL' | 'PENDING' | 'CONFIRMED' | 'UNKNOWN';

/** Type of novelty that can be registered (RegisterNoveltyRequest.type). */
export type NoveltyType = 'REPLACEMENT' | 'ABSENCE' | 'SHIFT_CHANGE' | 'SWAP';

/** Novelty of a member of the day as it comes back on the response; NONE when there is none. */
export type MemberNoveltyType = NoveltyType | 'NONE';

/** One worker who works the day (DayStaffStepResponse.Member). */
export interface DayStaffMember {
  workerId: string;
  fullName: string;
  /** Code actually worked. */
  workedCode: string;
  /** Code in the grid; equals workedCode without novelty. */
  gridCode: string;
  segments: ShiftScheduleSegment[];
  totalHours: number;
  /** Role of the day, from the worked code (TITULAR or APOYO). */
  dayRole: string;
  /** 1 for titular, 3 for apoyo. */
  dispenser: number;
  support: boolean;
  noveltyType: MemberNoveltyType;
  /** Replaced worker (REPLACEMENT) or other worker (SWAP); null otherwise. */
  otherWorkerId: string | null;
  otherWorkerName: string | null;
  noveltyReason: string | null;
}

/** One worker on DESCANSO (DayStaffStepResponse.Resting). */
export interface DayStaffResting {
  workerId: string;
  fullName: string;
}

/** One worker taken out of the day by a novelty (DayStaffStepResponse.Absentee). */
export interface DayStaffAbsentee {
  workerId: string;
  fullName: string;
  gridCode: string;
  type: MemberNoveltyType;
  reason: string;
}

/** One active novelty of the day, in registration order (DayStaffStepResponse.Novelty). */
export interface DayStaffNovelty {
  id: string;
  type: NoveltyType;
  workerId: string;
  counterpartWorkerId: string | null;
  /** Grid code of workerId. */
  originalShiftCode: string;
  /** Code actually worked; null in ABSENCE. */
  finalShiftCode: string | null;
  exceptionalCoverage: boolean;
  reason: string;
  createdAt: string;
}

/** One warning of the day; never blocks the confirmation. */
export interface DayStaffWarning {
  code: string;
  message: string;
}

/** Step 1 of a journal: who works, who rests, who is absent, the active novelties and the warnings. */
export interface DayStaffStepResponse {
  date: string;
  step1Status: Step1Status;
  /** False when the journal does not exist or is closed: the step is read only. */
  editable: boolean;
  members: DayStaffMember[];
  resting: DayStaffResting[];
  absentees: DayStaffAbsentee[];
  novelties: DayStaffNovelty[];
  warnings: DayStaffWarning[];
}

/** Body of POST /api/shifts/day-staff/{date}/novelties, and each pending novelty of the confirmation. */
export interface RegisterNoveltyRequest {
  type: NoveltyType;
  /** Absent worker (REPLACEMENT, ABSENCE), worker whose code changes (SHIFT_CHANGE) or first worker of the swap. */
  workerId: string;
  /** Replacement (REPLACEMENT) or second worker of the swap (SWAP); omit otherwise. */
  counterpartWorkerId?: string | null;
  /** Required in SHIFT_CHANGE; in REPLACEMENT the absent worker's code is used when omitted; ignored otherwise. */
  shiftCode?: string | null;
  /** Accepts a code of a role different from the worker's role. */
  exceptionalCoverage: boolean;
  /** 3 to 255 characters. */
  reason: string;
}

/** Optional body of POST /api/shifts/day-staff/{date}/confirmation. */
export interface ConfirmDayStaffRequest {
  /** Novelties not registered yet; omit when they were registered before. */
  novelties?: RegisterNoveltyRequest[];
}
