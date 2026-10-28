import { ShiftScheduleSegment } from '../models/dtos/shift-catalog.dto';

/** Zona de la estación: el backend decide "hoy" con esta misma zona. */
const STATION_TIME_ZONE = 'America/Bogota';
/** El día operativo arranca a las 06:00 y termina a las 06:00 del día siguiente. */
const OPERATIONAL_DAY_START_HOUR = 6;

const WEEKDAYS_SHORT = ['D', 'L', 'M', 'M', 'J', 'V', 'S'];
const WEEKDAYS_NAME = ['dom', 'lun', 'mar', 'mié', 'jue', 'vie', 'sáb'];
const MONTH_NAMES = [
  'enero', 'febrero', 'marzo', 'abril', 'mayo', 'junio',
  'julio', 'agosto', 'septiembre', 'octubre', 'noviembre', 'diciembre'
];

/** Posición del ciclo 2x2 el primer día a generar: 0 y 1 DIA, 2 y 3 NOCHE, 4 y 5 DESCANSO. */
export const PHASE_LABELS = ['1.er día', '2.º día', '1.ª noche', '2.ª noche', '1.er descanso', '2.º descanso'];
export const ROTATION_CYCLE = ['DIA', 'DIA', 'NOCHE', 'NOCHE', 'DESCANSO', 'DESCANSO'];

/** Familia visual de un código de turno; decide el color de la celda o la ficha. */
export type ShiftTone = 'dia' | 'dia6' | 'nueve-seis' | 'noche' | 'descanso' | 'apoyo' | 'vacio';

export function shiftTone(code: string | null | undefined): ShiftTone {
  switch (code) {
    case null:
    case undefined:
    case '':
      return 'vacio';
    case 'DIA':
      return 'dia';
    case 'DIA6':
      return 'dia6';
    case '9-6':
      return 'nueve-seis';
    case 'NOCHE':
      return 'noche';
    case 'DESCANSO':
      return 'descanso';
    default:
      return 'apoyo';
  }
}

const SHORT_CODES: Record<string, string> = {
  DIA: 'D',
  DIA6: 'D6',
  NOCHE: 'N',
  DESCANSO: '–',
  '6-9y5-9': '6·5',
  '6-9y2-6': '6·2',
  '2-9pm': '2-9',
  '6-9am': '6-9',
  '2-6pm': '2-6'
};

/** Texto corto para celdas estrechas; el código completo va en la etiqueta accesible. */
export function shortCode(code: string | null | undefined): string {
  if (!code) return '';
  return SHORT_CODES[code] ?? code;
}

/** Fecha de hoy (yyyy-MM-dd) en la zona de la estación, igual que el backend al decidir qué días ya pasaron. */
export function stationToday(now: Date = new Date()): string {
  return formatInZone(now);
}

/** Día operativo en curso: antes de las 06:00 todavía corre el día operativo anterior. */
export function operationalToday(now: Date = new Date()): string {
  return formatInZone(new Date(now.getTime() - OPERATIONAL_DAY_START_HOUR * 3_600_000));
}

function formatInZone(date: Date): string {
  const parts = new Intl.DateTimeFormat('en-CA', {
    timeZone: STATION_TIME_ZONE,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  }).formatToParts(date);
  const get = (type: string) => parts.find(p => p.type === type)?.value ?? '';
  return `${get('year')}-${get('month')}-${get('day')}`;
}

/** Fecha calendario sin hora: se trabaja en UTC para que la zona del navegador no mueva el día. */
function toUtcDate(date: string): Date {
  const [y, m, d] = date.split('-').map(Number);
  return new Date(Date.UTC(y, m - 1, d));
}

function fromUtcDate(date: Date): string {
  return date.toISOString().slice(0, 10);
}

export function addDays(date: string, days: number): string {
  const d = toUtcDate(date);
  d.setUTCDate(d.getUTCDate() + days);
  return fromUtcDate(d);
}

export function daysBetween(from: string, to: string): number {
  return Math.round((toUtcDate(to).getTime() - toUtcDate(from).getTime()) / 86_400_000);
}

export function monthOf(date: string): string {
  return date.slice(0, 7);
}

export function addMonths(month: string, months: number): string {
  const [y, m] = month.split('-').map(Number);
  const d = new Date(Date.UTC(y, m - 1 + months, 1));
  return d.toISOString().slice(0, 7);
}

export function firstDayOfMonth(month: string): string {
  return `${month}-01`;
}

export function lastDayOfMonth(month: string): string {
  return addDays(firstDayOfMonth(addMonths(month, 1)), -1);
}

export function monthDays(month: string): string[] {
  const last = lastDayOfMonth(month);
  const days: string[] = [];
  for (let d = firstDayOfMonth(month); d <= last; d = addDays(d, 1)) {
    days.push(d);
  }
  return days;
}

export function dateRange(from: string, to: string): string[] {
  const days: string[] = [];
  for (let d = from; d <= to; d = addDays(d, 1)) {
    days.push(d);
  }
  return days;
}

export function weekdayShort(date: string): string {
  return WEEKDAYS_SHORT[toUtcDate(date).getUTCDay()];
}

export function isSunday(date: string): boolean {
  return toUtcDate(date).getUTCDay() === 0;
}

export function dayOfMonth(date: string): number {
  return Number(date.slice(8, 10));
}

/** "septiembre de 2026" */
export function monthLabel(month: string): string {
  const [y, m] = month.split('-').map(Number);
  return `${MONTH_NAMES[m - 1]} de ${y}`;
}

/** "mié 05/08" */
export function shortDateLabel(date: string): string {
  return `${WEEKDAYS_NAME[toUtcDate(date).getUTCDay()]} ${date.slice(8, 10)}/${date.slice(5, 7)}`;
}

/** "miércoles 5 de agosto de 2026" */
export function longDateLabel(date: string): string {
  const d = toUtcDate(date);
  const weekday = d.toLocaleDateString('es-CO', { weekday: 'long', timeZone: 'UTC' });
  return `${weekday} ${d.getUTCDate()} de ${MONTH_NAMES[d.getUTCMonth()]} de ${d.getUTCFullYear()}`;
}

function hourLabel(hour: number): string {
  return `${String(hour % 24).padStart(2, '0')}:00`;
}

/** "06:00–18:00" o "06:00–09:00 y 17:00–21:00"; "—" para DESCANSO. */
export function segmentsLabel(segments: ShiftScheduleSegment[]): string {
  if (!segments.length) return '—';
  return segments.map(s => `${hourLabel(s.startHour)}–${hourLabel(s.endHour)}`).join(' y ');
}

/** Posición de un tramo sobre la barra del día operativo (06:00 → 06:00), en porcentaje. */
export function segmentOnOperationalDay(segment: ShiftScheduleSegment): { left: number; width: number } {
  const start = (segment.startHour - OPERATIONAL_DAY_START_HOUR + 24) % 24;
  const length = (segment.endHour - segment.startHour + 24) % 24 || 24;
  return { left: (start / 24) * 100, width: (Math.min(length, 24 - start) / 24) * 100 };
}

/** Solo la primera letra en mayúscula ("Octubre de 2026"), sin tocar el resto como haría text-transform. */
export function capitalize(text: string): string {
  return text ? text.charAt(0).toLocaleUpperCase('es-CO') + text.slice(1) : text;
}

/**
 * Primer nombre y primer apellido, para columnas estrechas: "Ana Milena Villamil González" → "Ana Villamil".
 * Con tres palabras se asume un solo nombre ("Carlos Andrés Paredes" → "Carlos Paredes").
 */
export function shortName(fullName: string): string {
  const parts = fullName.trim().split(/\s+/);
  return parts.length >= 3 ? `${parts[0]} ${parts[2]}` : fullName;
}

/** Respeta prefers-reduced-motion para las animaciones que se lanzan desde código. */
export function prefersReducedMotion(): boolean {
  return typeof window !== 'undefined' && !!window.matchMedia?.('(prefers-reduced-motion: reduce)').matches;
}
