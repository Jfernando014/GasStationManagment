import { decimalText, formatDecimal } from './sales-target.helpers';
import { ExplicitStatisticsRequest, SalesStatisticsResponse, StatisticsSummary, Weekday } from '../models/dtos/sales-statistics.dto';

export const WEEKDAYS: { value: Weekday; label: string }[] = [
  { value: 'MONDAY', label: 'Lunes' }, { value: 'TUESDAY', label: 'Martes' },
  { value: 'WEDNESDAY', label: 'Miércoles' }, { value: 'THURSDAY', label: 'Jueves' },
  { value: 'FRIDAY', label: 'Viernes' }, { value: 'SATURDAY', label: 'Sábado' }, { value: 'SUNDAY', label: 'Domingo' }
];
export const formatStatistics = (value: string | null): string => value === null ? '—' : formatDecimal(value);
export function localDate(date = new Date()): string {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
}
export function validDate(value: string): boolean {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(value) || value.startsWith('0000')) return false;
  const date = new Date(`${value}T12:00:00Z`);
  return !Number.isNaN(date.getTime()) && date.toISOString().slice(0, 10) === value;
}
export function dateRangeError(from: string, to: string): string {
  if (!validDate(from) || !validDate(to)) return 'Selecciona fechas inicial y final válidas.';
  return from > to ? 'La fecha inicial debe ser menor o igual a la fecha final.' : '';
}
export function weekday(date: string): Weekday {
  return WEEKDAYS[(new Date(`${date}T12:00:00Z`).getUTCDay() + 6) % 7].value;
}

/** Preserve JSON number tokens before decoding; only counts/scales become JS numbers. */
export function exactJson(text: string): unknown {
  return JSON.parse(text.replace(/"(?:\\.|[^"\\])*"|-?\d+(?:\.\d+)?(?:[eE][+-]?\d+)?/g,
    token => token.startsWith('"') ? token : JSON.stringify(token)));
}
export function decimal(value: unknown): string {
  if (typeof value !== 'string') throw new Error('Respuesta estadística inválida');
  const match = /^(\d+)(?:\.(\d+))?(?:[eE]([+-]?\d+))?$/.exec(value);
  if (!match || value.length > 200 || Math.abs(Number(match[3] ?? 0)) > 100) throw new Error('Importe estadístico inválido');
  const digits = match[1] + (match[2] ?? '');
  const point = match[1].length + Number(match[3] ?? 0);
  const expanded = point <= 0 ? '0.' + '0'.repeat(-point) + digits : point >= digits.length
    ? digits + '0'.repeat(point - digits.length) : digits.slice(0, point) + '.' + digits.slice(point);
  const [whole, fraction = ''] = expanded.split('.');
  const tail = fraction.replace(/0+$/, '');
  return whole.replace(/^0+(?=\d)/, '') + (tail ? '.' + tail : '');
}

export function signedDecimal(value: unknown): string | null {
  if (value === null) return null;
  if (typeof value !== 'string') throw new Error('Importe inválido');
  return value.startsWith('-') ? '-' + decimal(value.slice(1)) : decimal(value);
}

export function monthRange(month: string): { from: string; to: string } {
  if (!/^(?!0000)\d{4}-(0[1-9]|1[0-2])$/.test(month)) throw new Error('Selecciona un período válido.');
  const year = Number(month.slice(0, 4)), number = Number(month.slice(5));
  const leap = year % 4 === 0 && (year % 100 !== 0 || year % 400 === 0);
  const days = [31, leap ? 29 : 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31][number - 1];
  return { from: `${month}-01`, to: `${month}-${days}` };
}

export function addExact(a: string, b: string): string { return difference(a, b === '0' ? '0' : '-' + b); }

/** Divide exact decimal strings with HALF_EVEN rounding; null means undefined denominator. */
export function divideExact(a: string, b: string, scale = 2): string | null {
  const precision = Math.max(a.split('.')[1]?.length ?? 0, b.split('.')[1]?.length ?? 0);
  const integer = (value: string): bigint => {
    const negative = value.startsWith('-');
    const [whole, tail = ''] = value.replace(/^[+-]/, '').split('.');
    return BigInt(whole + tail.padEnd(precision, '0')) * (negative ? -1n : 1n);
  };
  const denominator = integer(b); if (denominator === 0n) return null;
  const numerator = integer(a) * 10n ** BigInt(scale);
  const negative = (numerator < 0n) !== (denominator < 0n);
  const n = numerator < 0n ? -numerator : numerator, d = denominator < 0n ? -denominator : denominator;
  let quotient = n / d; const remainder = n % d;
  if (remainder * 2n > d || (remainder * 2n === d && quotient % 2n !== 0n)) quotient++;
  const digits = quotient.toString().padStart(scale + 1, '0');
  return (negative && quotient !== 0n ? '-' : '') + (scale ? digits.slice(0, -scale) + '.' + digits.slice(-scale) : digits);
}

export function participation(amount: string, total: string): string | null {
  const [whole, tail = ''] = amount.replace(/^[+-]/, '').split('.');
  const scale = tail.length;
  const digits = (BigInt(whole + tail) * 100n).toString().padStart(scale + 1, '0');
  const multiplied = (amount.startsWith('-') ? '-' : '') + (scale ? digits.slice(0, -scale) + '.' + digits.slice(-scale) : digits);
  return divideExact(multiplied, total, 2);
}
function object(value: unknown): Record<string, unknown> {
  if (value === null || typeof value !== 'object' || Array.isArray(value)) throw new Error('Respuesta estadística inválida');
  return value as Record<string, unknown>;
}
export function parseStatistics(text: string, request: ExplicitStatisticsRequest): SalesStatisticsResponse {
  const row = object(exactJson(text));
  const unit = request.category === 'BASKET' ? 'COP' : 'GALLONS';
  if (row['source'] !== 'CLIENT_SUPPLIED_OBSERVATIONS' || row['category'] !== request.category || row['unit'] !== unit ||
      row['from'] !== request.from || row['to'] !== request.to || Number(row['outputScale']) !== request.outputScale ||
      row['roundingMode'] !== 'HALF_EVEN' || !Array.isArray(row['series']) || row['series'].length !== 2)
    throw new Error('La respuesta no corresponde al análisis solicitado.');
  const series = row['series'].map<StatisticsSummary>((value: unknown) => {
    const data = object(value);
    const journey = data['journey'], day = data['dayOfWeek'];
    const count = Number(data['observations']);
    if ((journey !== 'DAY' && journey !== 'NIGHT') || typeof data['label'] !== 'string' ||
        typeof data['available'] !== 'boolean' || !Number.isSafeInteger(count) || count < 0 ||
        (day !== null && !WEEKDAYS.some(d => d.value === day))) throw new Error('Serie estadística inválida');
    const available = data['available'];
    if ((available && count === 0) || (!available && (count !== 0 || data['average'] !== null || data['exactAverage'] !== null)))
      throw new Error('Disponibilidad estadística inconsistente');
    const fraction = available ? object(data['exactAverage']) : null;
    if (fraction && (typeof fraction['numerator'] !== 'string' || !/^\d+$/.test(fraction['numerator']) ||
        typeof fraction['denominator'] !== 'string' || !/^[1-9]\d*$/.test(fraction['denominator']))) throw new Error('Promedio exacto inválido');
    return { label: data['label'], dayOfWeek: day as Weekday | null, journey, available, observations: count,
      total: available ? decimal(data['total']) : null, average: available ? decimal(data['average']) : null,
      exactAverage: fraction ? { numerator: fraction['numerator'] as string, denominator: fraction['denominator'] as string } : null };
  });
  return { from: request.from, to: request.to, category: request.category, unit, source: 'CLIENT_SUPPLIED_OBSERVATIONS',
    outputScale: request.outputScale, roundingMode: 'HALF_EVEN', series };
}

export function serializeObservations(request: ExplicitStatisticsRequest): string {
  const rangeError = dateRangeError(request.from, request.to);
  if (rangeError) throw new Error(rangeError);
  if (!['BASKET', 'FUEL_AGGREGATE'].includes(request.category) || !Number.isInteger(request.outputScale) || request.outputScale < 0 || request.outputScale > 12)
    throw new Error('Categoría o precisión inválida');
  const seen = new Set<string>();
  const observations = request.observations.map(row => {
    const value = decimalText(row.value);
    const key = `${row.date}/${row.journey}`;
    if (!validDate(row.date) || row.date < request.from || row.date > request.to || !['DAY', 'NIGHT'].includes(row.journey) || value === null || seen.has(key))
      throw new Error('Revisa fechas, jornadas e importes. Solo se admite un total por fecha y jornada.');
    seen.add(key);
    return `${JSON.stringify({ date: row.date, journey: row.journey }).slice(0, -1)},"value":${value}}`;
  });
  return `${JSON.stringify({ from: request.from, to: request.to, category: request.category, outputScale: request.outputScale }).slice(0, -1)},"observations":[${observations.join(',')}]}`;
}

/** Exact decimal difference for display; no money passes through binary floating point. */
export function difference(a: string, b: string): string {
  const scale = Math.max(a.split('.')[1]?.length ?? 0, b.split('.')[1]?.length ?? 0);
  const integer = (value: string): bigint => {
    const negative = value.startsWith('-'); const [whole, tail = ''] = value.replace(/^[+-]/, '').split('.');
    return BigInt(whole + tail.padEnd(scale, '0')) * (negative ? -1n : 1n);
  };
  const result = integer(a) - integer(b);
  const digits = (result < 0n ? -result : result).toString().padStart(scale + 1, '0');
  const tail = scale ? digits.slice(-scale).replace(/0+$/, '') : '';
  return (result < 0n ? '-' : '') + (scale ? digits.slice(0, -scale) : digits) + (tail ? '.' + tail : '');
}
/** A bounded display-coordinate ratio. Number conversion is only for SVG geometry. */
export function chartRatio(value: string, maximum: string): number {
  const scale = Math.max(value.split('.')[1]?.length ?? 0, maximum.split('.')[1]?.length ?? 0);
  const integer = (s: string): bigint => { const [a, b = ''] = s.split('.'); return BigInt(a + b.padEnd(scale, '0')); };
  const max = integer(maximum);
  return max === 0n ? 0 : Number(integer(value) * 100000n / max) / 100000;
}
