import { FuelValueDto, FuelValueKind } from '../models/dtos/fuel-price.dto';
import { FuelType } from '../models/dtos/tank.dto';
import { newestFirst } from './dated-value.helpers';

export interface FuelIdentity {
  type: FuelType;
  name: string;
  color: string;
}

/** The fuels in the order the station reads them. */
export const FUELS: readonly FuelIdentity[] = [
  { type: 'MOTOR', name: 'Corriente', color: 'var(--fuel-corriente)' },
  { type: 'DIESEL', name: 'Diésel', color: 'var(--fuel-diesel)' },
  { type: 'EXTRA', name: 'Extra', color: 'var(--fuel-extra)' },
  { type: 'MAX_PRO', name: 'Maxpro', color: 'var(--fuel-maxpro)' }
];

/** How each kind of value is called inside a sentence. */
export const FUEL_VALUE_LABELS: Record<FuelValueKind, string> = {
  sale: 'precio de venta',
  purchase: 'costo de compra'
};

export type FuelTrendDirection = 'up' | 'down' | 'same' | 'first';

export interface FuelTrend {
  direction: FuelTrendDirection;
  text: string;
}

/** One side (sale or purchase) of a fuel card: the value that rules today and how it compares with the one before. */
export interface FuelSide {
  value: number | null;
  trend: FuelTrend | null;
  /** Day of the first scheduled value, only when nothing rules today. */
  scheduledFrom: string | null;
}

export interface FuelSummary extends FuelIdentity {
  sale: FuelSide;
  purchase: FuelSide;
  /** Sale price minus purchase cost per gallon; null when one of them is missing. */
  margin: number | null;
  /** Sale prices up to today, oldest first, for the small line. */
  saleLine: number[];
}

const MAX_LINE_POINTS = 8;
const MAX_INTEGER_DIGITS = 10;

export function fuelName(type: FuelType): string {
  return FUELS.find(fuel => fuel.type === type)?.name ?? type;
}

/** Pesos with a dot for thousands and a comma for cents; cents show when asked or when there are any. */
export function formatGallonMoney(amount: number, alwaysCents = false): string {
  const cents = Math.round(Math.abs(amount) * 100);
  const whole = Math.floor(cents / 100).toString().replace(/\B(?=(\d{3})+(?!\d))/g, '.');
  const rest = cents % 100;
  const decimals = alwaysCents || rest !== 0 ? `,${String(rest).padStart(2, '0')}` : '';
  return `${amount < 0 && cents !== 0 ? '-' : ''}$${whole}${decimals}`;
}

/** Costs always carry two decimals; sale prices only when they have them. */
export function formatFuelValue(kind: FuelValueKind, amount: number): string {
  return formatGallonMoney(amount, kind === 'purchase');
}

/** The same figure as it is typed in a field, without the peso sign. */
export function formatFuelValueInput(kind: FuelValueKind, amount: number): string {
  return formatFuelValue(kind, amount).replace('$', '');
}

/**
 * Reads what was typed as a value per gallon: "15.036,31", "15036,31", "15036.31" or "15.036".
 * A dot followed by groups of three digits separates thousands. Zero is only valid with `allowZero`.
 */
export function parseGallonValue(
  text: string,
  allowZero = false
): { value: number; error: null } | { value: null; error: string } {
  const fail = (error: string) => ({ value: null, error });
  const tooLow = allowZero ? 'El valor no puede ser negativo.' : 'El valor debe ser mayor que cero.';
  const clean = text.replace(/[\s$]/g, '');
  if (clean === '') return fail('Escribe el valor por galón.');
  if (clean.startsWith('-')) return fail(tooLow);

  let plain: string;
  if (/^(\d{1,3}(\.\d{3})+|\d+),\d*$/.test(clean)) plain = clean.replace(/\./g, '').replace(',', '.');
  else if (/^[1-9]\d{0,2}(\.\d{3})+$/.test(clean)) plain = clean.replace(/\./g, '');
  else if (/^\d+(\.\d*)?$/.test(clean)) plain = clean;
  else return fail('Escribe solo números, por ejemplo 15.036,31.');

  const [integer, decimals = ''] = plain.split('.');
  if (decimals.length > 2) return fail('Usa máximo 2 decimales.');
  if (integer.replace(/^0+/, '').length > MAX_INTEGER_DIGITS) return fail('El valor es demasiado grande.');
  const value = Number(plain);
  return value > 0 || allowZero ? { value, error: null } : fail(tooLow);
}

/** Difference between two pesos amounts without the noise of floating point. */
export function gallonMargin(sale: number, purchase: number): number {
  return Math.round((sale - purchase) * 100) / 100;
}

function trendOf(kind: FuelValueKind, current: number, previous: number | null): FuelTrend {
  if (previous === null) return { direction: 'first', text: 'Primer valor cargado' };
  const change = gallonMargin(current, previous);
  if (change === 0) return { direction: 'same', text: 'Sin cambio frente al anterior' };
  const amount = formatFuelValue(kind, Math.abs(change));
  return change > 0
    ? { direction: 'up', text: `${amount} más que el anterior` }
    : { direction: 'down', text: `${amount} menos que el anterior` };
}

function sideOf(kind: FuelValueKind, values: readonly FuelValueDto[], today: string): FuelSide {
  const sorted = newestFirst(values);
  const index = sorted.findIndex(value => value.validFrom <= today);
  if (index < 0) {
    return { value: null, trend: null, scheduledFrom: sorted.at(-1)?.validFrom ?? null };
  }
  const current = sorted[index].pricePerGallon;
  return {
    value: current,
    trend: trendOf(kind, current, sorted[index + 1]?.pricePerGallon ?? null),
    scheduledFrom: null
  };
}

export function summarizeFuel(
  fuel: FuelIdentity,
  sales: readonly FuelValueDto[],
  purchases: readonly FuelValueDto[],
  today: string
): FuelSummary {
  const ownSales = sales.filter(value => value.fuelType === fuel.type);
  const sale = sideOf('sale', ownSales, today);
  const purchase = sideOf('purchase', purchases.filter(value => value.fuelType === fuel.type), today);
  return {
    ...fuel,
    sale,
    purchase,
    margin: sale.value !== null && purchase.value !== null ? gallonMargin(sale.value, purchase.value) : null,
    saleLine: newestFirst(ownSales)
      .filter(value => value.validFrom <= today)
      .slice(0, MAX_LINE_POINTS)
      .reverse()
      .map(value => value.pricePerGallon)
  };
}
