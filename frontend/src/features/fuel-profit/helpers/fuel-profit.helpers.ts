import { formatCop, formatShortDate } from '../../../shared/helpers/format.helpers';
import { FUELS, FuelIdentity } from '../../../shared/helpers/fuel-price.helpers';
import { formatGallons } from '../../../shared/helpers/inventory-display.helpers';
import { monthName } from '../../../shared/helpers/month-options.helpers';
import { capitalize } from '../../../shared/helpers/shift-display.helpers';
import { FuelType } from '../../../shared/models/dtos/tank.dto';
import { FuelProfitDto, FuelProfitReportDto, MissingDatum } from '../models/fuel-profit.dto';

export const UNDEFINED_FIGURE = 'Sin definir';

const PRICES_ROUTE = '/configuracion/precios-combustible';

interface DatumText {
  /** "el costo de compra", inside a sentence. */
  name: string;
  /** Text of the action that fixes it. */
  action: string;
  /** Kind of value to show in the prices screen; null when it is fixed in "Datos del cálculo". */
  priceKind: 'venta' | 'compra' | null;
}

const MISSING_DATA: Record<MissingDatum, DatumText> = {
  SALE_PRICE: { name: 'precio de venta', action: 'Cargar precio de venta', priceKind: 'venta' },
  PURCHASE_COST: { name: 'costo de compra', action: 'Cargar costo de compra', priceKind: 'compra' },
  FREIGHT_PER_GALLON: { name: 'flete por galón', action: 'Cargar flete', priceKind: null },
  FINANCIAL_COST_PER_GALLON: { name: 'costo financiero por galón', action: 'Cargar costo financiero', priceKind: null }
};

/** Where a missing figure is fixed: the prices screen, or the "Datos del cálculo" tab of this one. */
export type ProfitFix =
  | { place: 'prices'; label: string; link: string; queryParams: { combustible: FuelType; valor: 'venta' | 'compra' } }
  | { place: 'inputs'; label: string };

export interface MissingNote {
  datum: MissingDatum;
  /** "costo de compra desde el 1 jun" */
  text: string;
  fix: ProfitFix;
}

export function fixFor(datum: MissingDatum, fuelType: FuelType): ProfitFix {
  const { action, priceKind } = MISSING_DATA[datum];
  return priceKind
    ? { place: 'prices', label: action, link: PRICES_ROUTE, queryParams: { combustible: fuelType, valor: priceKind } }
    : { place: 'inputs', label: action };
}

export function missingNotes(fuel: FuelProfitDto): MissingNote[] {
  return fuel.missingData.map(({ datum, since }) => ({
    datum,
    text: `${MISSING_DATA[datum].name} desde el ${formatShortDate(since, false)}`,
    fix: fixFor(datum, fuel.fuelType)
  }));
}

export function fuelIdentity(type: FuelType): FuelIdentity {
  return FUELS.find(fuel => fuel.type === type) ?? { type, name: type, color: 'var(--text-light)' };
}

/** "Extra y Maxpro" */
export function listNames(names: string[]): string {
  return names.length < 2 ? names.join('') : `${names.slice(0, -1).join(', ')} y ${names[names.length - 1]}`;
}

/** What the month lacks, one sentence per missing figure, and the action that fixes the first one. */
export function incompleteSummary(report: FuelProfitReportDto): { text: string; fix: ProfitFix | null } {
  const groups = new Map<MissingDatum, { since: string; fuels: FuelType[] }>();
  for (const fuel of report.fuels) {
    for (const { datum, since } of fuel.missingData) {
      const group = groups.get(datum) ?? { since, fuels: [] };
      group.since = since < group.since ? since : group.since;
      group.fuels.push(fuel.fuelType);
      groups.set(datum, group);
    }
  }
  // The figures that the same fuels lack are told in one sentence.
  const sentences = new Map<string, { fuels: FuelType[]; data: string[] }>();
  for (const [datum, group] of groups) {
    const key = group.fuels.join();
    const sentence = sentences.get(key) ?? { fuels: group.fuels, data: [] };
    sentence.data.push(`el ${MISSING_DATA[datum].name} desde el ${formatShortDate(group.since, false)}`);
    sentences.set(key, sentence);
  }
  const lacking = [...sentences.values()].map(({ fuels, data }) => {
    const who = fuels.length === report.fuels.length
      ? 'todos los combustibles'
      : listNames(fuels.map(type => fuelIdentity(type).name));
    return `A ${who} le${fuels.length > 1 ? 's' : ''} falta ${listNames(data)}.`;
  });
  const first = [...groups][0];
  return {
    text: `${lacking.join(' ')} Sin ${groups.size > 1 ? 'esos datos' : 'ese dato'} no se puede sumar el total.`,
    fix: first ? fixFor(first[0], first[1].fuels[0]) : null
  };
}

/** Pesos without cents; a dash when the figure cannot be calculated. */
export function money(value: number | null): string {
  return value === null ? '—' : formatCop(value);
}

/** The commission is written as what it takes away. */
export function commissionText(value: number): string {
  return Math.round(value) === 0 ? formatCop(0) : `−${formatCop(value)}`;
}

export function gallonsText(value: number | null): string {
  return value === null ? '—' : `${formatGallons(value, 1)} gal`;
}

/** "Junio" */
export function monthTitle(month: string): string {
  return capitalize(monthName(month));
}

const toCents = (value: number) => Math.round(value * 100) / 100;

/**
 * How much the commission of the complete fuels would change with a typed card percentage. It rounds each fuel as
 * the backend does. Null when any figure needed is missing.
 */
export function commissionChange(report: FuelProfitReportDto, percentage: number): number | null {
  const complete = report.fuels.filter(fuel => fuel.complete);
  const rate = report.card.commissionRate;
  if (complete.length === 0 || !Number.isFinite(rate)) return null;
  let change = 0;
  for (const fuel of complete) {
    if (fuel.sales === null || fuel.commission === null) return null;
    change += toCents(fuel.sales * (percentage / 100) * rate) - fuel.commission;
  }
  return toCents(change);
}

/**
 * Change of the commission of the complete fuels between two reports of a month. Null when the two cannot be
 * compared fuel by fuel.
 */
export function commissionDifference(before: FuelProfitReportDto, after: FuelProfitReportDto): number | null {
  const complete = after.fuels.filter(fuel => fuel.complete);
  if (complete.length === 0) return null;
  let change = 0;
  for (const fuel of complete) {
    const previous = before.fuels.find(item => item.fuelType === fuel.fuelType);
    if (!previous?.complete || previous.commission === null || fuel.commission === null) return null;
    change += fuel.commission - previous.commission;
  }
  return toCents(change);
}
