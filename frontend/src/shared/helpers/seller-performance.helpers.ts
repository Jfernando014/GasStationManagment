import {
  ComplianceReport, PerformanceAvailability, PerformanceCoverage, PerformanceQuery,
  PerformanceReason, SellerCompliance, SellerPerformanceResponse
} from '../models/dtos/seller-performance.dto';
import { dateRangeError, decimal, difference, divideExact, exactJson } from './sales-statistics.helpers';

const DAY_MS = 86_400_000;
const reasons: Record<PerformanceReason, string> = {
  HISTORY_INCOMPLETE: 'El historial del período aún no está completo. No se muestran totales parciales.',
  NO_CONFIRMED_SELLER_CLOSES: 'No hay cierres de vendedor en el período confirmado.',
  TARGET_NOT_PROVIDED: 'Meta individual no disponible.',
  TARGET_PERIOD_MISMATCH: 'La meta recibida corresponde a otro período.',
  TARGET_MEASURE_MISMATCH: 'La meta recibida corresponde a otra unidad de venta.',
  ZERO_TARGET: 'La meta es cero; el porcentaje no es calculable.',
  NEGATIVE_OBSERVATIONS_REQUIRE_POLICY: 'Hay ajustes negativos pendientes de una regla de cálculo.',
  SHIFT_CLASSIFICATION_NOT_PROVIDED: 'Clasificación histórica de turnos no disponible.',
  MULTIPLIER_NOT_PROVIDED: 'Factor de ajuste no disponible.',
  MULTIPLIER_PERIOD_MISMATCH: 'El factor corresponde a otro período.',
  MULTIPLIER_MEASURE_MISMATCH: 'El factor corresponde a otra unidad.',
  INCOMPLETE_CALCULATIONS: 'Hay indicadores pendientes de parámetros válidos. Consulta el estado de cada vendedor.',
  ZERO_BASELINE: 'El promedio de referencia es cero; la comparación no es calculable.'
};

export function performanceReason(reason: PerformanceReason | null): string {
  return reason === null ? '' : reasons[reason];
}

export function performanceRangeError(from: string, to: string): string {
  const error = dateRangeError(from, to);
  if (error) return error;
  return (Date.parse(`${to}T00:00:00Z`) - Date.parse(`${from}T00:00:00Z`)) / DAY_MS >= 3660
    ? 'El período puede contener como máximo 3660 días.' : '';
}

export function validatePerformanceQuery(query: PerformanceQuery): void {
  const error = performanceRangeError(query.from, query.to);
  if (error) throw new Error(error);
  if (!['FUEL_GALLONS', 'BASKET_COP'].includes(query.measure) || !Number.isInteger(query.outputScale)
      || query.outputScale < 0 || query.outputScale > 12) throw new Error('Métrica o precisión inválida.');
}

function invalid(): never { throw new Error('La respuesta de cumplimiento es inválida o no corresponde a la consulta.'); }
function row(value: unknown): Record<string, unknown> {
  if (!value || typeof value !== 'object' || Array.isArray(value)) return invalid();
  return value as Record<string, unknown>;
}
function reason(value: unknown): PerformanceReason | null {
  if (value === null) return null;
  if (typeof value !== 'string' || !Object.hasOwn(reasons, value)) return invalid();
  return value as PerformanceReason;
}
const optionalDecimal = (value: unknown): string | null => value === null ? null : decimal(value);
function source(value: unknown): string | null {
  if (value === null) return null;
  return typeof value === 'string' && value.trim() ? value : invalid();
}

function seller(value: unknown, scale: number): SellerCompliance {
  const s = row(value), sales = decimal(s['sales']), target = optionalDecimal(s['target']);
  const percent = optionalDecimal(s['percent']), targetSource = source(s['targetSource']);
  const availability = s['availability'], unavailable = reason(s['reason']);
  if (typeof s['workerId'] !== 'string' || !/^[\da-f]{8}(?:-[\da-f]{4}){3}-[\da-f]{12}$/i.test(s['workerId'])
      || /^0{8}(?:-0{4}){3}-0{12}$/.test(s['workerId'])
      || typeof s['recordedName'] !== 'string' || !s['recordedName'].trim()) return invalid();
  let ratio: SellerCompliance['ratio'] = null;
  if (availability === 'AVAILABLE') {
    const r = row(s['ratio']);
    ratio = { numerator: decimal(r['numerator']), denominator: decimal(r['denominator']) };
    if (unavailable !== null || target === null || target === '0' || percent === null || targetSource === null
        || ratio.numerator !== sales || ratio.denominator !== target) return invalid();
    // The displayed percent must describe the exact ratio (HALF_EVEN), including a genuine zero sale.
    // Multiply before rounding: rounding the unscaled ratio first would change half-way cases.
    const [whole, tail = ''] = sales.split('.');
    const multiplied = decimal(`${whole}${tail}e${2 - tail.length}`);
    if (decimal(divideExact(multiplied, target, scale) ?? '') !== percent) return invalid();
  } else if (availability === 'UNAVAILABLE') {
    if (percent !== null || s['ratio'] !== null || unavailable === null
        || !['TARGET_NOT_PROVIDED', 'TARGET_PERIOD_MISMATCH', 'TARGET_MEASURE_MISMATCH', 'ZERO_TARGET'].includes(unavailable)) return invalid();
    if (unavailable === 'TARGET_NOT_PROVIDED') {
      if (target !== null || targetSource !== null) return invalid();
    } else if (target === null || targetSource === null || (unavailable === 'ZERO_TARGET' && target !== '0')) return invalid();
  } else return invalid();
  return { workerId: s['workerId'], recordedName: s['recordedName'], sales, target,
    availability, reason: unavailable, ratio, percent, targetSource };
}

/** Decode only C-14, without coupling its availability to the comparison/normalization parameters. */
export function parseSellerPerformance(text: string, query: PerformanceQuery): SellerPerformanceResponse {
  validatePerformanceQuery(query);
  const data = row(exactJson(text)), report = row(data['compliance']), window = row(report['window']);
  const unit = query.measure === 'FUEL_GALLONS' ? 'GALLONS' : 'COP';
  if (data['source'] !== 'CONFIRMED_SHIFT_SALES_HISTORY' || data['unit'] !== unit
      || data['outputScale'] !== String(query.outputScale) || data['roundingMode'] !== 'HALF_EVEN'
      || window['from'] !== query.from || window['to'] !== query.to || report['measure'] !== query.measure
      || !Array.isArray(report['coverage']) || !Array.isArray(report['sellers'])) return invalid();
  const availability = report['availability'] as PerformanceAvailability, unavailable = reason(report['reason']);
  if (!['AVAILABLE', 'PARTIAL', 'UNAVAILABLE', 'NO_DATA'].includes(availability)) return invalid();
  const first = Date.parse(`${query.from}T00:00:00Z`);
  const count = (Date.parse(`${query.to}T00:00:00Z`) - first) / DAY_MS + 1;
  if (report['coverage'].length !== count) return invalid();
  const coverage = report['coverage'].map<PerformanceCoverage>((value, index) => {
    const c = row(value);
    if (c['date'] !== new Date(first + index * DAY_MS).toISOString().slice(0, 10)
        || !['PROCESSED', 'PROCESSED_EMPTY', 'PENDING', 'ABSENT'].includes(String(c['status']))) return invalid();
    return { date: c['date'] as string, status: c['status'] as PerformanceCoverage['status'] };
  });
  const sellers = report['sellers'].map(value => seller(value, query.outputScale));
  if (new Set(sellers.map(s => s.workerId)).size !== sellers.length) return invalid();
  const incomplete = coverage.some(c => c.status === 'PENDING' || c.status === 'ABSENT');
  const available = sellers.filter(s => s.availability === 'AVAILABLE').length;
  if (incomplete) {
    if (availability !== 'UNAVAILABLE' || unavailable !== 'HISTORY_INCOMPLETE' || sellers.length) return invalid();
  } else if (sellers.length) {
    const expected = available === sellers.length ? 'AVAILABLE' : available ? 'PARTIAL' : 'UNAVAILABLE';
    if (!coverage.some(c => c.status === 'PROCESSED') || availability !== expected
        || unavailable !== (expected === 'AVAILABLE' ? null : 'INCOMPLETE_CALCULATIONS')) return invalid();
  } else if (availability === 'NO_DATA') {
    if (unavailable !== 'NO_CONFIRMED_SELLER_CLOSES' || coverage.some(c => c.status !== 'PROCESSED_EMPTY')) return invalid();
  } else if (availability !== 'UNAVAILABLE' || unavailable !== 'NEGATIVE_OBSERVATIONS_REQUIRE_POLICY'
      || !coverage.some(c => c.status === 'PROCESSED')) return invalid();
  const compliance: ComplianceReport = { window: { from: query.from, to: query.to }, measure: query.measure,
    availability, reason: unavailable, coverage, sellers };
  return { source: 'CONFIRMED_SHIFT_SALES_HISTORY', unit, outputScale: query.outputScale, roundingMode: 'HALF_EVEN', compliance };
}

/** A mismatched target is not the target of the requested period, even when supplied by the source. */
export function applicableTarget(seller: SellerCompliance): string | null {
  return seller.reason === 'TARGET_PERIOD_MISMATCH' || seller.reason === 'TARGET_MEASURE_MISMATCH' ? null : seller.target;
}

/** Convert only a bounded decorative coordinate; keep the displayed percentage as exact text. */
export function complianceBarValue(percent: string | null): number | null {
  return percent === null ? null : !difference(percent, '150').startsWith('-') ? 150 : Number(percent);
}
