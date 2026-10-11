import { AbstractControl, ValidationErrors } from '@angular/forms';
import { SalesTargetResponse } from '../models/dtos/sales-target.dto';

/** Plain decimal input; comma is accepted as a decimal separator, never as grouping. */
export function decimalText(input: string): string | null {
  const value = input.trim().replace(',', '.');
  if (!/^\d+(?:\.\d+)?$/.test(value)) return null;
  const [integer, fraction = ''] = value.split('.');
  const whole = integer.replace(/^0+(?=\d)/, '');
  const tail = fraction.replace(/0+$/, '');
  if (whole.length > 18 || tail.length > 12) return null;
  return tail ? `${whole}.${tail}` : whole;
}

export function targetDecimalValidator(control: AbstractControl): ValidationErrors | null {
  if (control.value === '' || control.value === null) return null;
  return typeof control.value === 'string' && decimalText(control.value) !== null
    ? null
    : { decimal: 'Ingresa un número no negativo menor que 10¹⁸, con hasta 12 decimales exactos. Sin separadores de miles.' };
}

function scaled(value: string): bigint {
  const [whole, fraction = ''] = value.split('.');
  return BigInt(whole) * 10n ** 12n + BigInt(fraction.padEnd(12, '0'));
}

/** Prototype margin = maximumSale - annual target, with exact decimal subtraction. */
export function targetMargin(value: string, maximum: string): string | null {
  const base = decimalText(value), max = decimalText(maximum);
  if (base === null || max === null) return null;
  const margin = scaled(max) - scaled(base);
  const absolute = margin < 0n ? -margin : margin;
  const fraction = (absolute % 10n ** 12n).toString().padStart(12, '0').replace(/0+$/, '');
  return `${margin < 0n ? '-' : '+'}${absolute / 10n ** 12n}${fraction ? '.' + fraction : ''}`;
}

/** Presentation only: the prototype rounds a stored monthly budget / calendar days. */
export function monthlyDailyPreview(value: string, days: number): string {
  const denominator = BigInt(days) * 10n ** 12n;
  return ((scaled(value) * 2n + denominator) / (2n * denominator)).toString();
}

export function formatDecimal(value: string | null): string {
  if (value === null) return 'Sin datos';
  const [whole, fraction = ''] = value.split('.');
  return whole.replace(/\B(?=(\d{3})+(?!\d))/g, '.') + (fraction ? ',' + fraction : '');
}

/** Jackson may serialize small BigDecimals as 1E-12 (including zero as 0E-12). */
function jsonDecimal(value: unknown): string | null {
  if (typeof value !== 'string') return null;
  if (!/[eE]/.test(value)) return decimalText(value);
  const match = /^(\d+)(?:\.(\d+))?[eE]([+-]?\d+)$/.exec(value);
  if (!match) return null;
  const exponent = Number(match[3]);
  if (!Number.isInteger(exponent) || Math.abs(exponent) > 100) return null;
  const digits = match[1] + (match[2] ?? '');
  const point = match[1].length + exponent;
  const expanded = point <= 0 ? '0.' + '0'.repeat(-point) + digits
    : point >= digits.length ? digits + '0'.repeat(point - digits.length)
    : digits.slice(0, point) + '.' + digits.slice(point);
  return decimalText(expanded);
}

/** Quote numeric tokens outside JSON strings before parsing, preserving BigDecimal and Long. */
export function parseSalesTargets(text: string): SalesTargetResponse[] {
  const decoded: unknown = JSON.parse(text.replace(/"(?:\\.|[^"\\])*"|-?\d+(?:\.\d+)?(?:[eE][+-]?\d+)?/g,
    token => token.startsWith('"') ? token : JSON.stringify(token)));
  const rows = Array.isArray(decoded) ? decoded : [decoded];
  return rows.map((row: unknown) => {
    if (typeof row !== 'object' || row === null) throw new Error('Respuesta de metas inválida');
    const data = row as Record<string, unknown>;
    const year = Number(data['year']);
    const month = data['month'] === null ? null : Number(data['month']);
    const value = jsonDecimal(data['value']);
    const maximum = jsonDecimal(data['maximumSale']);
    const category = data['category'], unit = data['unit'];
    if (typeof data['year'] !== 'string' || !Number.isInteger(year) || typeof data['id'] !== 'string' || !/^\d+$/.test(data['id']) ||
        (month !== null && (!Number.isInteger(month) || month < 1 || month > 12)) || value === null ||
        (data['maximumSale'] !== null && maximum === null) || typeof data['createdAt'] !== 'string' ||
        !((category === 'FUEL_AGGREGATE' && unit === 'GALLONS') || (category === 'BASKET' && unit === 'COP'))) {
      throw new Error('Respuesta de metas inválida');
    }
    return { id: data['id'], year, month, category, unit, value, maximumSale: maximum,
      createdAt: data['createdAt'], ...(typeof data['message'] === 'string' ? { message: data['message'] } : {}) };
  });
}

export function parseSalesTarget(text: string): SalesTargetResponse {
  const targets = parseSalesTargets(text);
  if (targets.length !== 1) throw new Error('Respuesta de meta inválida');
  return targets[0];
}
