import { formatShortDate } from '../../../shared/helpers/format.helpers';
import { fuelName } from '../../../shared/helpers/fuel-price.helpers';
import { monthName, monthOptionLabel } from '../../../shared/helpers/month-options.helpers';
import { capitalize } from '../../../shared/helpers/shift-display.helpers';
import { FuelType } from '../../../shared/models/dtos/tank.dto';

/** Shown for every figure that is not known. */
export const UNKNOWN = '—';

const MINUS = '−';
const WEEKDAYS = ['dom', 'lun', 'mar', 'mié', 'jue', 'vie', 'sáb'];

/** Which side a difference falls on: it picks the color, while the sign and the words say it too. */
export type VarianceTone = 'short' | 'over' | 'even';

/** A figure ready to show with the side it falls on. */
export interface Figure {
  text: string;
  tone: VarianceTone;
}

/** "8.626,24": dot for thousands, comma for decimals, no sign. */
export function formatAmount(value: number, decimals = 2): string {
  const [whole, fraction] = Math.abs(value).toFixed(decimals).split('.');
  const grouped = whole.replace(/\B(?=(\d{3})+(?!\d))/g, '.');
  return fraction ? `${grouped},${fraction}` : grouped;
}

/** -1, 0 or 1 for the value as it will be written, so "−0,00" never shows. */
function writtenSign(value: number, decimals: number): number {
  return Math.sign(Number(value.toFixed(decimals)));
}

/** Gallons of a stock or a sale: a minus only when it is below zero. */
export function gallons(value: number | null): string {
  if (value === null) return UNKNOWN;
  return `${writtenSign(value, 2) < 0 ? MINUS : ''}${formatAmount(value)}`;
}

/** "+3,85", "−285,00" or "0,00". */
export function signed(value: number | null): string {
  if (value === null) return UNKNOWN;
  const sign = writtenSign(value, 2);
  return `${sign > 0 ? '+' : sign < 0 ? MINUS : ''}${formatAmount(value)}`;
}

/** "−4,60 %" */
export function signedPercent(value: number | null): string {
  return value === null ? UNKNOWN : `${signed(value)} %`;
}

/** "0,50 %": a limit, without sign. */
export function limitPercent(value: number): string {
  return `${formatAmount(value, Number(value.toFixed(2)) === value ? 2 : 4)} %`;
}

export function varianceTone(value: number | null): VarianceTone {
  if (value === null) return 'even';
  const sign = writtenSign(value, 2);
  return sign < 0 ? 'short' : sign > 0 ? 'over' : 'even';
}

export function signedFigure(value: number | null): Figure {
  return { text: signed(value), tone: varianceTone(value) };
}

export function percentFigure(value: number | null): Figure {
  return { text: signedPercent(value), tone: varianceTone(value) };
}

export function plural(count: number, one: string, many: string): string {
  return `${count} ${count === 1 ? one : many}`;
}

/** "28 abr" */
export function dayAndMonth(date: string): string {
  return formatShortDate(date, false);
}

export function weekday(date: string): string {
  const [year, month, day] = date.split('-').map(Number);
  return WEEKDAYS[new Date(Date.UTC(year, month - 1, day)).getUTCDay()];
}

/** "abril" */
export function monthWord(month: string): string {
  return monthName(month);
}

/** "Abril" */
export function monthTitleWord(month: string): string {
  return capitalize(monthName(month));
}

/** "Abril 2026" */
export function monthAndYear(month: string): string {
  return monthOptionLabel(month, '');
}

/** Name of a fuel as this screen writes it. */
export function variationFuelName(type: FuelType): string {
  return type === 'MAX_PRO' ? 'Max Pro' : fuelName(type);
}
