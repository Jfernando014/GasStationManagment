import { PerformanceQuery } from '../models/dtos/seller-performance.dto';
import { performanceFixture, performanceQuery } from './seller-performance.test-data';

/** Test fixtures only; no application fallback. */
export function fullPerformanceFixture(query: PerformanceQuery = performanceQuery) {
  const data = performanceFixture(query);
  const common = { workerId: data.compliance.sellers[0].workerId, recordedName: 'Ángela Pérez',
    scenario: { weekday: 'MONDAY', period: 'NIGHT', dayType: 'REGULAR', workedCode: 'NOCHE', effectiveRole: 'TITULAR' },
    sales: '120', observations: 1, availability: 'AVAILABLE', reason: null as string | null };
  return { ...data, comparison: {
    window: { from: query.from, to: query.to }, measure: query.measure, availability: 'AVAILABLE', reason: null as string | null,
    baselineDefinition: 'ALL_CONFIRMED_EQUIVALENT_SELLER_SHIFTS_IN_REQUESTED_WINDOW_INCLUDING_SELLER', coverage: data.compliance.coverage,
    sellers: [{ ...common, sellerAverage: '120', stationSales: '120', stationObservations: 1, stationAverage: '120',
      relativeAverage: { numerator: '120', denominator: '120' } as { numerator: string; denominator: string } | null, relativePercent: '100' as string | null }]
  }, normalization: {
    window: { from: query.from, to: query.to }, measure: query.measure, availability: 'AVAILABLE', reason: null as string | null,
    coverage: data.compliance.coverage,
    sellers: [{ ...common, multiplier: '1.5' as string | null, normalizedSales: '180' as string | null, multiplierSource: 'approved-test-source' as string | null }]
  } };
}
export function unavailablePerformanceFixture(query = performanceQuery, reason = 'SHIFT_CLASSIFICATION_NOT_PROVIDED') {
  const data = fullPerformanceFixture(query);
  data.comparison.availability = data.normalization.availability = 'UNAVAILABLE';
  data.comparison.reason = data.normalization.reason = reason;
  data.comparison.sellers = []; data.normalization.sellers = [];
  if (reason === 'NO_CONFIRMED_SELLER_CLOSES' || reason === 'HISTORY_INCOMPLETE' || reason === 'NEGATIVE_OBSERVATIONS_REQUIRE_POLICY') {
    data.compliance.sellers = [];
    data.compliance.availability = reason === 'NO_CONFIRMED_SELLER_CLOSES' ? 'NO_DATA' : 'UNAVAILABLE';
    data.compliance.reason = reason;
    if (reason === 'NO_CONFIRMED_SELLER_CLOSES') {
      data.comparison.availability = data.normalization.availability = 'NO_DATA';
      data.compliance.coverage.forEach(c => c.status = 'PROCESSED_EMPTY');
    } else if (reason === 'HISTORY_INCOMPLETE') data.compliance.coverage[0].status = 'PENDING';
  }
  return data;
}
