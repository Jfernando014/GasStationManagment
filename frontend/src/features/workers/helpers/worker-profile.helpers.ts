import { capitalize, monthLabel, ShiftTone, shiftTone } from '../../../shared/helpers/shift-display.helpers';
import { formatShortDate } from '../../../shared/helpers/format.helpers';
import { ShiftCode } from '../../../shared/models/dtos/shift-catalog.dto';
import { PerformanceWeekday } from '../../../shared/models/dtos/worker-profile.dto';

/** Rows with fewer shifts than this are shown in gray: the average is not reliable yet. */
export const MIN_RELIABLE_SHIFTS = 3;

const MONTH_PATTERN = /^\d{4}-(0[1-9]|1[0-2])$/;

const WEEKDAY_NAMES = ['domingo', 'lunes', 'martes', 'miércoles', 'jueves', 'viernes', 'sábado'];

export const PERFORMANCE_WEEKDAY_LABELS: Record<PerformanceWeekday, string> = {
  MONDAY: 'Lunes',
  TUESDAY: 'Martes',
  WEDNESDAY: 'Miércoles',
  THURSDAY: 'Jueves',
  FRIDAY: 'Viernes',
  SATURDAY: 'Sábado',
  SUNDAY: 'Domingo',
  HOLIDAY: 'Festivo'
};

const gallonsFormat = new Intl.NumberFormat('es-CO', { minimumFractionDigits: 2, maximumFractionDigits: 2 });

/** True for a month written as yyyy-MM. */
export function isValidMonth(value: string | null | undefined): value is string {
  return !!value && MONTH_PATTERN.test(value);
}

/** "Agosto 2026". */
export function monthTitle(month: string): string {
  return capitalize(monthLabel(month).replace(' de ', ' '));
}

/** "agosto". */
export function monthName(month: string): string {
  return monthLabel(month).split(' de ')[0];
}

/** Days the first week of the month starts after Monday (Monday = 0, Sunday = 6). */
export function leadingBlankDays(month: string): number {
  const [year, monthNumber] = month.split('-').map(Number);
  const weekday = new Date(Date.UTC(year, monthNumber - 1, 1)).getUTCDay();
  return (weekday + 6) % 7;
}

/**
 * Color family of a code. It comes from the catalog (role, period and whether it needs support) so a new code is
 * styled without touching this file; the name of the code is only the fallback when the catalog has no data for it.
 */
export function toneForCode(code: string | null | undefined, info: ShiftCode | undefined): ShiftTone {
  if (!code) return 'vacio';
  if (!info) return shiftTone(code);
  if (!info.role) return 'descanso';
  if (info.role.name === 'APOYO') return 'apoyo';
  if (info.period === 'NIGHT') return 'noche';
  if (!info.requiresSupport) return 'dia';
  const firstHour = info.schedule.segments[0]?.startHour ?? 0;
  return firstHour <= 6 ? 'dia6' : 'nueve-seis';
}

/** Gallons with the comma as decimal separator: 845.66 -> "845,66". */
export function formatGallons(value: number): string {
  return gallonsFormat.format(value);
}

/** "109 %". */
export function formatPercent(value: number): string {
  return `${Math.round(value)} %`;
}

/** Calendar day yyyy-MM-dd as "nov 2025". */
export function formatMonthYearShort(day: string): string {
  return formatShortDate(day).split(' ').slice(1).join(' ');
}

/** Calendar day yyyy-MM-dd as "sábado 28 mar 2026". It works on the text, so the time zone cannot move the day. */
export function formatWeekdayDate(day: string): string {
  const [year, month, date] = day.split('-').map(Number);
  const weekday = WEEKDAY_NAMES[new Date(Date.UTC(year, month - 1, date)).getUTCDay()];
  return `${weekday} ${formatShortDate(day)}`;
}
