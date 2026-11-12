import {
  AdjustmentDto, CategoryHoursDto, FortnightDto, SheetDto, SheetWorkerDto, SummaryDto
} from '../models/overtime.dto';
import { weekdayName } from '../helpers/overtime-display.helpers';
import {
  CategoryHours, Fortnight, OvertimeAdjustment, OvertimeCategory, OvertimeSheet, OvertimeSummary, OvertimeWorker,
  OVERTIME_CATEGORIES
} from '../models/overtime.model';

export function toCategoryHours(dto: CategoryHoursDto): CategoryHours {
  return { RDF: dto.rdf, RNO: dto.rno, RNF: dto.rnf, HEDO: dto.hedo, HENO: dto.heno, HEDF: dto.hedf, HENF: dto.henf };
}

function toFortnight(dto: FortnightDto): Fortnight {
  return { month: dto.month, number: dto.fortnight === 2 ? 2 : 1, from: dto.from, to: dto.to };
}

function toWorker(dto: SheetWorkerDto): OvertimeWorker {
  return { id: dto.workerId, name: dto.workerName, role: dto.role };
}

function toAdjustment(dto: AdjustmentDto): OvertimeAdjustment {
  const category = OVERTIME_CATEGORIES.includes(dto.category as OvertimeCategory)
    ? dto.category as OvertimeCategory : 'HEDO';
  return {
    id: dto.id, category, direction: dto.direction, hours: dto.hours, reason: dto.reason,
    createdAt: dto.createdAt, createdBy: dto.createdBy
  };
}

export function toSheet(dto: SheetDto): OvertimeSheet {
  return {
    worker: toWorker(dto.worker),
    fortnight: toFortnight(dto.fortnight),
    days: dto.days.map(day => ({
      date: day.date,
      weekday: weekdayName(day.dayOfWeek),
      holiday: day.holiday,
      workedCode: day.workedCode,
      grossHours: day.grossHours,
      hours: toCategoryHours(day),
      parametersFrom: day.parametersValidFrom
    })),
    subtotals: toCategoryHours(dto.subtotals),
    grossHours: dto.grossHours,
    fixedHedo: { applies: dto.fixedHedo.applies, hours: dto.fixedHedo.hours },
    adjustments: dto.adjustments.map(toAdjustment),
    adjustmentsNet: toCategoryHours(dto.adjustmentsNet),
    totalToPay: toCategoryHours(dto.totalToPay),
    incomplete: dto.incomplete,
    noveltiesUnconfirmed: dto.noveltiesUnconfirmed,
    affectedDates: dto.affectedDates,
    parameterVersions: [...dto.parameterVersions].sort()
  };
}

export function toSummary(dto: SummaryDto): OvertimeSummary {
  return {
    fortnight: toFortnight(dto.fortnight),
    workers: dto.workers.map(row => ({
      worker: toWorker(row.worker),
      grossHours: row.grossHours,
      totalToPay: toCategoryHours(row.totalToPay),
      incomplete: row.incomplete,
      noveltiesUnconfirmed: row.noveltiesUnconfirmed
    })),
    affectedDates: dto.affectedDates
  };
}
