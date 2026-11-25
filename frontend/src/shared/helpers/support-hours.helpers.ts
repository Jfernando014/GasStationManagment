import { ShiftCode, ShiftScheduleSegment } from '../models/dtos/shift-catalog.dto';

/** Hours offered to start a block: the operational day begins at 06:00. */
export const SUPPORT_START_HOURS = Array.from({ length: 18 }, (_, i) => i + 6);
/** Hours offered to end a block; 24 is midnight, a support shift never crosses it. */
export const SUPPORT_END_HOURS = Array.from({ length: 18 }, (_, i) => i + 7);
export const MAX_SUPPORT_RANGES = 2;

const CUSTOM_CODE = /AP(?:_\d{1,2}-\d{1,2}){1,2}/g;

/** A block of hours while it is being chosen: either end can still be empty. */
export interface HourDraft {
  startHour: number | null;
  endHour: number | null;
}

/** True for the generated code of a support shift typed as hours (`AP_10-17`). */
export function isCustomShiftCode(code: string | null | undefined): boolean {
  return !!code && code.startsWith('AP_');
}

/** Replaces every custom code written inside a text, for example a message of the server. */
export function replaceCustomCodes(text: string, replace: (code: string) => string): string {
  return text.replace(CUSTOM_CODE, replace);
}

/** "10–17" or "6–9 y 17–24": the short form for chips and narrow cells. */
export function compactHoursLabel(segments: ShiftScheduleSegment[]): string {
  return segments.map(s => `${s.startHour}–${s.endHour}`).join(' y ');
}

/** Same rules as the backend. One message per block, null when the block is fine. */
export function hourRangeProblems(ranges: HourDraft[], showMissing: boolean): (string | null)[] {
  return ranges.map((range, i) => {
    const { startHour, endHour } = range;
    if (startHour === null || endHour === null) {
      return showMissing ? 'Elige la hora de inicio y la de fin.' : null;
    }
    if (!Number.isInteger(startHour) || !Number.isInteger(endHour)) return 'Solo horas en punto.';
    if (endHour <= startHour) return 'La hora de fin debe ser mayor que la de inicio.';
    const previousEnd = i > 0 ? ranges[i - 1].endHour : null;
    if (previousEnd !== null && startHour <= previousEnd) {
      return `El segundo tramo debe empezar después de las ${previousEnd}:00: no puede ir pegado al primero.`;
    }
    if (ranges.length === 1 && startHour === 0 && endHour === 24) return 'Un turno no puede cubrir las 24 horas.';
    return null;
  });
}

/** The blocks ready to send; null while something is missing or invalid. */
export function completeSegments(ranges: HourDraft[]): ShiftScheduleSegment[] | null {
  if (hourRangeProblems(ranges, true).some(problem => problem !== null)) return null;
  return ranges.map(r => ({ startHour: r.startHour as number, endHour: r.endHour as number }));
}

export function sameSegments(a: ShiftScheduleSegment[], b: ShiftScheduleSegment[]): boolean {
  return a.length === b.length && a.every((s, i) => s.startHour === b[i].startHour && s.endHour === b[i].endHour);
}

/** Standard support shifts of the catalog that can be used to fill the hour selectors. */
export function quickSupportShifts(catalog: ShiftCode[]): ShiftCode[] {
  return catalog.filter(c => {
    const segments = c.schedule?.segments ?? [];
    return c.active && c.role?.name === 'APOYO' && segments.length > 0 && completeSegments(segments) !== null
      && segments.every(s => SUPPORT_START_HOURS.includes(s.startHour));
  });
}

/**
 * Code the server will save for those hours: the standard support code with exactly those hours, otherwise the
 * generated one. Used to show the change before the server answers.
 */
export function codeForHours(segments: ShiftScheduleSegment[], catalog: ShiftCode[]): string {
  const standard = quickSupportShifts(catalog).find(c => sameSegments(c.schedule.segments, segments));
  return standard?.code ?? `AP${segments.map(s => `_${s.startHour}-${s.endHour}`).join('')}`;
}
