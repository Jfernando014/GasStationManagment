import { ParamMap } from '@angular/router';

import { formatShortDate } from '../../../shared/helpers/format.helpers';
import { capitalize, lastDayOfMonth, shortDateLabel } from '../../../shared/helpers/shift-display.helpers';
import {
  CategoryHours, Fortnight, OVERTIME_CATEGORIES, OVERTIME_START_MONTH, OvertimeCategory
} from '../models/overtime.model';

const MONTH_PARAM = /^\d{4}-(0[1-9]|1[0-2])$/;

/** What each sigla means, for the tooltip of its column. */
export const CATEGORY_HELP: Record<OvertimeCategory, { name: string; help: string }> = {
  RDF: { name: 'Recargo dominical o festivo', help: 'Horas diurnas trabajadas en domingo o festivo.' },
  RNO: { name: 'Recargo nocturno ordinario', help: 'Horas de noche trabajadas en un día ordinario.' },
  RNF: { name: 'Recargo nocturno festivo', help: 'Horas de noche trabajadas en domingo o festivo.' },
  HEDO: { name: 'Hora extra diurna ordinaria', help: 'Hoy solo cambia con las HEDO fijas de la quincena y con los ajustes.' },
  HENO: { name: 'Hora extra nocturna ordinaria', help: 'Hoy solo cambia con los ajustes.' },
  HEDF: { name: 'Hora extra diurna festiva', help: 'Hoy solo cambia con los ajustes.' },
  HENF: { name: 'Hora extra nocturna festiva', help: 'Hoy solo cambia con los ajustes.' }
};

/** The sigla of each column with the text of its tooltip. */
export const CATEGORY_HEADS = OVERTIME_CATEGORIES.map(category => ({
  category,
  tooltip: `${CATEGORY_HELP[category].name}. ${CATEGORY_HELP[category].help}`
}));

const WEEKDAY_BY_NAME: Record<string, string> = {
  MONDAY: 'lun', TUESDAY: 'mar', WEDNESDAY: 'mié', THURSDAY: 'jue', FRIDAY: 'vie', SATURDAY: 'sáb', SUNDAY: 'dom'
};

/** "lun" from MONDAY. */
export function weekdayName(dayOfWeek: string): string {
  return WEEKDAY_BY_NAME[dayOfWeek] ?? dayOfWeek.slice(0, 3).toLowerCase();
}

/** Whole hours with a real minus sign: -4 → "−4". */
export function formatHours(hours: number): string {
  return hours < 0 ? `−${Math.abs(hours)}` : String(hours);
}

/** Same as `formatHours` with "+" for a positive number; used for adjustments. */
export function formatSignedHours(hours: number): string {
  return hours > 0 ? `+${hours}` : formatHours(hours);
}

/** A valid "yyyy-MM" or null. */
export function parseMonthParam(value: string | null): string | null {
  return value !== null && MONTH_PARAM.test(value) ? value : null;
}

export function parseFortnightParam(value: string | null): 1 | 2 | null {
  return value === '1' ? 1 : value === '2' ? 2 : null;
}

/** The fortnight that contains a day: the 1st goes to the 15th, the 2nd from the 16th on. */
export function fortnightOf(day: string): { month: string; number: 1 | 2 } {
  return { month: day.slice(0, 7), number: Number(day.slice(8, 10)) <= 15 ? 1 : 2 };
}

/**
 * Month and fortnight of the address (`?mes=2026-08&quincena=1`). Anything missing, invalid or out of range falls back
 * to the fortnight in progress (or to the 1st fortnight of another month).
 */
export function fortnightFromQuery(query: ParamMap, today: string): { month: string; number: 1 | 2 } {
  const current = fortnightOf(today);
  const asked = parseMonthParam(query.get('mes'));
  const month = asked !== null && asked >= OVERTIME_START_MONTH && asked <= current.month ? asked : current.month;
  const number = parseFortnightParam(query.get('quincena')) ?? (month === current.month ? current.number : 1);
  return { month, number };
}

/** Last day of the fortnight, to label the second one: "16 al 30". */
export function fortnightEnd(month: string, number: 1 | 2): string {
  return number === 1 ? `${month}-15` : lastDayOfMonth(month);
}

/** "16 al 30" for the buttons of the fortnight picker. */
export function fortnightButtonLabel(month: string, number: 1 | 2): string {
  return number === 1 ? '1 al 15' : `16 al ${Number(fortnightEnd(month, 2).slice(8, 10))}`;
}

/** "1 al 15 ago 2026". */
export function fortnightLabel(fortnight: Pick<Fortnight, 'from' | 'to'>): string {
  return `${Number(fortnight.from.slice(8, 10))} al ${formatShortDate(fortnight.to)}`;
}

/** "sáb 22 ago". */
export function dayLabel(date: string): string {
  return `${shortDateLabel(date).slice(0, 3)} ${formatShortDate(date, false)}`;
}

/** Categories whose total is below zero, in the order of the sheet. */
export function negativeCategories(hours: CategoryHours): OvertimeCategory[] {
  return (Object.keys(hours) as OvertimeCategory[]).filter(category => hours[category] < 0);
}

/** The values of a version of the parameters that can change from one version to the next. */
export interface ParameterValues {
  nightStartHour: number;
  restHours: number;
  restMinShiftHours: number;
  fixedHedoHours: number;
  fixedHedoRoles: string[];
}

/** A value that differs between two versions. */
export interface ParameterChange {
  label: string;
  from: string;
  to: string;
}

export function hoursText(hours: number): string {
  return `${hours} ${hours === 1 ? 'hora' : 'horas'}`;
}

/** "19:00". */
export function hourOfDay(hour: number): string {
  return `${String(hour).padStart(2, '0')}:00`;
}

/** "Titular y Apoyo", or "ninguno" when no role is chosen. */
export function rolesText(roles: string[]): string {
  return roles.length === 0 ? 'ninguno' : [...roles].sort().map(role => capitalize(role.toLowerCase())).join(' y ');
}

/** What `after` changes with respect to `before`; a value that is not a number yet (while typing) is not a change. */
export function parameterChanges(before: ParameterValues, after: ParameterValues): ParameterChange[] {
  const changes: ParameterChange[] = [];
  const differs = (a: number, b: number) => !Number.isNaN(b) && a !== b;
  if (differs(before.nightStartHour, after.nightStartHour)) {
    changes.push({ label: 'La noche empieza', from: hourOfDay(before.nightStartHour), to: hourOfDay(after.nightStartHour) });
  }
  if (differs(before.restHours, after.restHours)) {
    changes.push({ label: 'Descanso', from: hoursText(before.restHours), to: hoursText(after.restHours) });
  }
  if (differs(before.restMinShiftHours, after.restMinShiftHours)) {
    changes.push({
      label: 'Mínimo del turno para descontar el descanso',
      from: hoursText(before.restMinShiftHours), to: hoursText(after.restMinShiftHours)
    });
  }
  if (differs(before.fixedHedoHours, after.fixedHedoHours)) {
    changes.push({ label: 'HEDO fijas por quincena', from: hoursText(before.fixedHedoHours), to: hoursText(after.fixedHedoHours) });
  }
  if ([...before.fixedHedoRoles].sort().join() !== [...after.fixedHedoRoles].sort().join()) {
    changes.push({ label: 'Roles con HEDO fijas', from: rolesText(before.fixedHedoRoles), to: rolesText(after.fixedHedoRoles) });
  }
  return changes;
}
