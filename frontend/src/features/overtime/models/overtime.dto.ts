/** Backend contract of /api/v1/workers/overtime-* (OvertimeDtos). Hours are whole numbers. */

export interface CategoryHoursDto {
  rdf: number;
  rno: number;
  rnf: number;
  hedo: number;
  heno: number;
  hedf: number;
  henf: number;
}

export interface FortnightDto {
  /** "yyyy-MM". */
  month: string;
  fortnight: number;
  from: string;
  to: string;
}

export interface SheetWorkerDto {
  workerId: string;
  workerName: string;
  role: string | null;
}

/** One day of a sheet: the hours of the shift that started that day, with the parameters in force that day. */
export interface OvertimeDayDto extends CategoryHoursDto {
  date: string;
  /** MONDAY … SUNDAY. */
  dayOfWeek: string;
  holiday: boolean;
  workedCode: string | null;
  grossHours: number;
  parametersValidFrom: string;
}

export interface FixedHedoDto {
  applies: boolean;
  hours: number;
}

export type AdjustmentDirectionDto = 'ADD' | 'SUBTRACT';

export interface AdjustmentDto {
  id: number;
  workerId: string;
  fortnight: FortnightDto;
  category: string;
  direction: AdjustmentDirectionDto;
  hours: number;
  reason: string;
  createdAt: string;
  createdBy: string | null;
}

export type IncompleteReasonDto = 'MISSING_RECORD' | 'PENDING' | 'NOVELTIES_NOT_APPLIED';

export interface AffectedDateDto {
  date: string;
  reason: IncompleteReasonDto;
}

export interface SheetDto {
  worker: SheetWorkerDto;
  fortnight: FortnightDto;
  days: OvertimeDayDto[];
  subtotals: CategoryHoursDto;
  grossHours: number;
  fixedHedo: FixedHedoDto;
  adjustments: AdjustmentDto[];
  adjustmentsNet: CategoryHoursDto;
  totalToPay: CategoryHoursDto;
  incomplete: boolean;
  noveltiesUnconfirmed: boolean;
  affectedDates: AffectedDateDto[];
  /** First day of every version of the parameters used in the fortnight. */
  parameterVersions: string[];
}

export interface SummaryRowDto {
  worker: SheetWorkerDto;
  grossHours: number;
  subtotals: CategoryHoursDto;
  fixedHedo: FixedHedoDto;
  adjustmentsNet: CategoryHoursDto;
  totalToPay: CategoryHoursDto;
  incomplete: boolean;
  noveltiesUnconfirmed: boolean;
}

export interface SummaryDto {
  fortnight: FortnightDto;
  workers: SummaryRowDto[];
  incomplete: boolean;
  noveltiesUnconfirmed: boolean;
  affectedDates: AffectedDateDto[];
}

export interface CreateAdjustmentRequestDto {
  month: string;
  fortnight: number;
  category: string;
  direction: AdjustmentDirectionDto;
  hours: number;
  reason: string;
}

/** One version of the parameters in GET /overtime-parameters/history, the newest first. */
export interface ParameterVersionDto {
  id: number;
  validFrom: string;
  /** Last day it applies; null when no later version exists. */
  validTo: string | null;
  /** True for the version that applies today. */
  current: boolean;
  nightStartHour: number;
  restHours: number;
  restMinShiftHours: number;
  fixedHedoHours: number;
  fixedHedoRoles: string[];
  reason: string | null;
  createdBy: string | null;
  createdAt: string;
}

export interface CreateParametersRequestDto {
  validFrom: string;
  nightStartHour: number;
  restHours: number;
  restMinShiftHours: number;
  fixedHedoHours: number;
  fixedHedoRoles: string[];
  reason?: string;
}
