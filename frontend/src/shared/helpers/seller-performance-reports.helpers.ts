import {
  FullSellerPerformanceResponse, NormalizedSale, PerformanceQuery, PerformanceReason, PerformanceScenario,
  ScenarioReport, ScenarioSale, SellerComparison, SellerPerformanceResponse
} from '../models/dtos/seller-performance.dto';
import { addExact, decimal, difference, divideExact, exactJson, WEEKDAYS } from './sales-statistics.helpers';
import { parseSellerPerformance } from './seller-performance.helpers';

const BASELINE = 'ALL_CONFIRMED_EQUIVALENT_SELLER_SHIFTS_IN_REQUESTED_WINDOW_INCLUDING_SELLER';
function invalid(): never { throw new Error('La respuesta de rendimiento es inválida o no corresponde a la consulta.'); }
function row(value: unknown): Record<string, unknown> {
  return value && typeof value === 'object' && !Array.isArray(value) ? value as Record<string, unknown> : invalid();
}
const optionalDecimal = (v: unknown) => v === null ? null : decimal(v);
function count(value: unknown): number {
  const result = Number(value);
  return typeof value === 'string' && /^[1-9]\d*$/.test(value) && Number.isSafeInteger(result) ? result : invalid();
}
function source(value: unknown): string | null {
  return value === null ? null : typeof value === 'string' && value.trim() ? value : invalid();
}
export function multiplyPerformance(a: string, b: string): string {
  const [aw, af = ''] = a.split('.'), [bw, bf = ''] = b.split('.');
  return decimal(`${BigInt(aw + af) * BigInt(bw + bf)}e-${af.length + bf.length}`);
}
export function scenarioKey(sale: Pick<ScenarioSale, 'workerId' | 'scenario'>): string {
  const s = sale.scenario;
  return JSON.stringify([sale.workerId, s.weekday, s.period, s.dayType, s.workedCode, s.effectiveRole]);
}
export function scenarioLabel(s: PerformanceScenario): string {
  return `${WEEKDAYS.find(d => d.value === s.weekday)?.label} · ${s.dayType === 'HOLIDAY' ? 'Festivo' : 'Regular'} · ${s.period === 'DAY' ? 'Día' : 'Noche'} · ${s.workedCode} · ${s.effectiveRole === 'TITULAR' ? 'Titular' : 'Apoyo'}`;
}
function base(value: unknown, response: SellerPerformanceResponse): ScenarioSale {
  const r = row(value), s = row(r['scenario']);
  if (!response.compliance.sellers.some(person => person.workerId === r['workerId'])
      || typeof r['recordedName'] !== 'string' || !r['recordedName'].trim()
      || !WEEKDAYS.some(d => d.value === s['weekday']) || !['DAY', 'NIGHT'].includes(String(s['period']))
      || !['REGULAR', 'HOLIDAY'].includes(String(s['dayType'])) || typeof s['workedCode'] !== 'string' || !s['workedCode'].trim()
      || !['TITULAR', 'APOYO'].includes(String(s['effectiveRole']))
      || !['AVAILABLE', 'UNAVAILABLE'].includes(String(r['availability']))) return invalid();
  return { workerId: r['workerId'] as string, recordedName: r['recordedName'], scenario: s as unknown as PerformanceScenario,
    sales: decimal(r['sales']), observations: count(r['observations']),
    availability: r['availability'] as ScenarioSale['availability'], reason: r['reason'] as PerformanceReason | null };
}
function comparison(value: unknown, response: SellerPerformanceResponse): SellerComparison {
  const r = row(value), sale = base(value, response);
  const stationSales = decimal(r['stationSales']), stationObservations = count(r['stationObservations']);
  const sellerAverage = decimal(r['sellerAverage']), stationAverage = decimal(r['stationAverage']);
  const relativePercent = optionalDecimal(r['relativePercent']);
  if (stationObservations < sale.observations || difference(stationSales, sale.sales).startsWith('-')
      || decimal(divideExact(sale.sales, String(sale.observations), response.outputScale)) !== sellerAverage
      || decimal(divideExact(stationSales, String(stationObservations), response.outputScale)) !== stationAverage) return invalid();
  let relativeAverage: SellerComparison['relativeAverage'] = null;
  if (sale.availability === 'AVAILABLE') {
    const ratio = row(r['relativeAverage']);
    relativeAverage = { numerator: decimal(ratio['numerator']), denominator: decimal(ratio['denominator']) };
    if (sale.reason !== null || stationSales === '0' || relativePercent === null
        || relativeAverage.numerator !== multiplyPerformance(sale.sales, String(stationObservations))
        || relativeAverage.denominator !== multiplyPerformance(stationSales, String(sale.observations))
        || decimal(divideExact(multiplyPerformance(relativeAverage.numerator, '100'), relativeAverage.denominator, response.outputScale)) !== relativePercent) return invalid();
  } else if (sale.reason !== 'ZERO_BASELINE' || stationSales !== '0' || r['relativeAverage'] !== null || relativePercent !== null) return invalid();
  return { ...sale, sellerAverage, stationSales, stationObservations, stationAverage, relativeAverage, relativePercent };
}
function normalization(value: unknown, response: SellerPerformanceResponse): NormalizedSale {
  const r = row(value), sale = base(value, response);
  const multiplier = optionalDecimal(r['multiplier']), normalizedSales = optionalDecimal(r['normalizedSales']), multiplierSource = source(r['multiplierSource']);
  if (multiplier === '0') return invalid();
  if (sale.availability === 'AVAILABLE') {
    if (sale.reason !== null || multiplier === null || multiplierSource === null || normalizedSales === null
        || normalizedSales !== multiplyPerformance(sale.sales, multiplier)) return invalid();
  } else {
    if (normalizedSales !== null || !['MULTIPLIER_NOT_PROVIDED', 'MULTIPLIER_PERIOD_MISMATCH', 'MULTIPLIER_MEASURE_MISMATCH'].includes(String(sale.reason))) return invalid();
    if (sale.reason === 'MULTIPLIER_NOT_PROVIDED') {
      if (multiplier !== null || multiplierSource !== null) return invalid();
    } else if (multiplier === null || multiplierSource === null) return invalid();
  }
  return { ...sale, multiplier, normalizedSales, multiplierSource };
}
function report<T extends ScenarioSale>(value: unknown, response: SellerPerformanceResponse, parse: (value: unknown, response: SellerPerformanceResponse) => T): ScenarioReport<T> {
  const r = row(value), window = row(r['window']), c = response.compliance;
  if (window['from'] !== c.window.from || window['to'] !== c.window.to || r['measure'] !== c.measure
      || !Array.isArray(r['coverage']) || r['coverage'].length !== c.coverage.length
      || !r['coverage'].every((value, index) => { const day = row(value); return day['date'] === c.coverage[index].date && day['status'] === c.coverage[index].status; })
      || !Array.isArray(r['sellers'])) return invalid();
  const sellers = r['sellers'].map(v => parse(v, response));
  if (new Set(sellers.map(scenarioKey)).size !== sellers.length) return invalid();
  if (sellers.length) {
    const available = sellers.filter(s => s.availability === 'AVAILABLE').length;
    const expected = available === sellers.length ? 'AVAILABLE' : available ? 'PARTIAL' : 'UNAVAILABLE';
    if (r['availability'] !== expected || r['reason'] !== (expected === 'AVAILABLE' ? null : 'INCOMPLETE_CALCULATIONS')
        || c.reason === 'HISTORY_INCOMPLETE' || c.reason === 'NEGATIVE_OBSERVATIONS_REQUIRE_POLICY') return invalid();
    // Scenario rows must cover exactly the same recorded sellers/sales, without dropping zero-sale sellers.
    for (const seller of c.sellers) {
      const rows = sellers.filter(s => s.workerId === seller.workerId);
      if (!rows.length || rows.reduce((total, s) => addExact(total, s.sales), '0') !== seller.sales) return invalid();
    }
  } else {
    const expectedReason = c.sellers.length ? 'SHIFT_CLASSIFICATION_NOT_PROVIDED' : c.reason;
    if (r['availability'] !== (expectedReason === 'NO_CONFIRMED_SELLER_CLOSES' ? 'NO_DATA' : 'UNAVAILABLE')
        || r['reason'] !== expectedReason) return invalid();
  }
  return { window: c.window, measure: c.measure, coverage: c.coverage, availability: r['availability'] as ScenarioReport<T>['availability'], reason: r['reason'] as PerformanceReason | null, sellers };
}
/** Full C-09 projection; C-14 keeps its independent parser and read() method. */
export function parsePerformanceReport(text: string, query: PerformanceQuery): FullSellerPerformanceResponse {
  const response = parseSellerPerformance(text, query), r = row(exactJson(text)), comp = row(r['comparison']);
  if (comp['baselineDefinition'] !== BASELINE) return invalid();
  const compared = report(comp, response, comparison), normalized = report(r['normalization'], response, normalization);
  if (compared.sellers.length !== normalized.sellers.length) return invalid();
  const equivalents = new Map(compared.sellers.map(s => [scenarioKey(s), s]));
  const cohorts = new Map<string, SellerComparison>();
  for (const sale of compared.sellers) {
    const key = scenarioKey({ ...sale, workerId: '' }), previous = cohorts.get(key);
    if (previous && (previous.stationSales !== sale.stationSales || previous.stationObservations !== sale.stationObservations)) return invalid();
    cohorts.set(key, sale);
  }
  for (const sale of normalized.sellers) {
    const equivalent = equivalents.get(scenarioKey(sale));
    if (!equivalent || equivalent.sales !== sale.sales || equivalent.observations !== sale.observations) return invalid();
  }
  return { ...response, comparison: { ...compared, baselineDefinition: BASELINE }, normalization: normalized };
}
