import { PerformanceQuery } from '../models/dtos/seller-performance.dto';

/** Test-only HTTP fixtures. Never imported by the application. */
export const performanceQuery: PerformanceQuery = {
  from: '2026-08-03', to: '2026-08-03', measure: 'FUEL_GALLONS', outputScale: 2
};
export function performanceFixture(query = performanceQuery) {
  const from = Date.parse(`${query.from}T00:00:00Z`), to = Date.parse(`${query.to}T00:00:00Z`);
  return {
    source: 'CONFIRMED_SHIFT_SALES_HISTORY', unit: query.measure === 'FUEL_GALLONS' ? 'GALLONS' : 'COP',
    outputScale: query.outputScale, roundingMode: 'HALF_EVEN',
    compliance: {
      window: { from: query.from, to: query.to }, measure: query.measure, availability: 'AVAILABLE', reason: null as string | null,
      coverage: Array.from({ length: (to - from) / 86_400_000 + 1 }, (_, index) => ({
        date: new Date(from + index * 86_400_000).toISOString().slice(0, 10), status: 'PROCESSED'
      })),
      sellers: [{ workerId: '10000000-0000-0000-0000-000000000001', recordedName: 'Ángela Pérez',
        sales: '120', target: '100' as string | null, percent: '120.00' as string | null,
        ratio: { numerator: '120', denominator: '100' } as { numerator: string; denominator: string } | null,
        availability: 'AVAILABLE', reason: null as string | null, targetSource: 'assigned-target-v1' as string | null }]
    },
    comparison: { availability: 'UNAVAILABLE', reason: 'SHIFT_CLASSIFICATION_NOT_PROVIDED' },
    normalization: { availability: 'UNAVAILABLE', reason: 'SHIFT_CLASSIFICATION_NOT_PROVIDED' }
  };
}
export function emptyPerformanceFixture(query = performanceQuery) {
  const data = performanceFixture(query);
  data.compliance.sellers = [];
  data.compliance.availability = 'NO_DATA';
  data.compliance.reason = 'NO_CONFIRMED_SELLER_CLOSES';
  data.compliance.coverage.forEach(day => day.status = 'PROCESSED_EMPTY');
  return data;
}
export function missingTargetSeller() {
  return { ...performanceFixture().compliance.sellers[0], workerId: '20000000-0000-0000-0000-000000000002',
    recordedName: 'Vendedor histórico inactivo', sales: '0', target: null, percent: null, ratio: null,
    availability: 'UNAVAILABLE', reason: 'TARGET_NOT_PROVIDED', targetSource: null };
}
