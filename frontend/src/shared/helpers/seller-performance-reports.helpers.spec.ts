import { fullPerformanceFixture, unavailablePerformanceFixture } from './performance-report.test-data';
import { performanceQuery } from './seller-performance.test-data';
import { parsePerformanceReport } from './seller-performance-reports.helpers';

describe('SCRUM-108 C-09 contract', () => {
  const parse = (data: ReturnType<typeof fullPerformanceFixture>) => parsePerformanceReport(JSON.stringify(data), performanceQuery);
  it('preserves equivalent cohorts, exact values, 100 = station and explicit normalization provenance', () => {
    const data = parse(fullPerformanceFixture());
    expect(data.comparison.sellers[0].relativePercent).toBe('100');
    expect(data.comparison.sellers[0].sellerAverage).toBe('120');
    expect(data.normalization.sellers[0].normalizedSales).toBe('180');
    expect(data.normalization.sellers[0].multiplierSource).toBe('approved-test-source');
  });
  it('accepts equivalent JSON coverage regardless of property order', () => {
    const fixture = fullPerformanceFixture();
    fixture.comparison.coverage = fixture.comparison.coverage.map(day => ({ status: day.status, date: day.date }));
    expect(parse(fixture).comparison.availability).toBe('AVAILABLE');
  });
  it.each(['SHIFT_CLASSIFICATION_NOT_PROVIDED', 'NO_CONFIRMED_SELLER_CLOSES', 'HISTORY_INCOMPLETE', 'NEGATIVE_OBSERVATIONS_REQUIRE_POLICY'])('preserves %s without partial totals or fake parameters', reason => {
    const data = parse(unavailablePerformanceFixture(performanceQuery, reason));
    expect(data.comparison.reason).toBe(reason); expect(data.normalization.sellers).toEqual([]);
  });
  it('supports ZERO_BASELINE without making division by zero equal 0%', () => {
    const fixture = fullPerformanceFixture();
    Object.assign(fixture.compliance.sellers[0], { sales: '0', percent: '0', ratio: { numerator: '0', denominator: '100' } });
    Object.assign(fixture.comparison.sellers[0], { sales: '0', sellerAverage: '0', stationSales: '0', stationAverage: '0',
      relativeAverage: null, relativePercent: null, availability: 'UNAVAILABLE', reason: 'ZERO_BASELINE' });
    fixture.comparison.availability = 'UNAVAILABLE'; fixture.comparison.reason = 'INCOMPLETE_CALCULATIONS';
    Object.assign(fixture.normalization.sellers[0], { sales: '0', normalizedSales: '0' });
    expect(parse(fixture).comparison.sellers[0].relativePercent).toBeNull();
    expect(parse(fixture).normalization.sellers[0].normalizedSales).toBe('0');
  });
  it.each(['MULTIPLIER_NOT_PROVIDED', 'MULTIPLIER_PERIOD_MISMATCH', 'MULTIPLIER_MEASURE_MISMATCH'])('keeps actual sales when %s prevents normalization', reason => {
    const fixture = fullPerformanceFixture();
    fixture.normalization.availability = 'UNAVAILABLE'; fixture.normalization.reason = 'INCOMPLETE_CALCULATIONS';
    Object.assign(fixture.normalization.sellers[0], { availability: 'UNAVAILABLE', reason, normalizedSales: null,
      ...(reason === 'MULTIPLIER_NOT_PROVIDED' ? { multiplier: null, multiplierSource: null } : {}) });
    const data = parse(fixture);
    expect(data.normalization.sellers[0].sales).toBe('120'); expect(data.normalization.sellers[0].normalizedSales).toBeNull();
    expect(data.comparison.availability).toBe('AVAILABLE');
  });
  it('supports PARTIAL while keeping day and night for the same worker separate', () => {
    const fixture = fullPerformanceFixture();
    fixture.comparison.sellers[0].scenario.period = fixture.normalization.sellers[0].scenario.period = 'DAY';
    fixture.comparison.sellers.push({ ...fixture.comparison.sellers[0], scenario: { ...fixture.comparison.sellers[0].scenario, period: 'NIGHT' } });
    fixture.normalization.sellers.push({ ...fixture.normalization.sellers[0], scenario: { ...fixture.normalization.sellers[0].scenario, period: 'NIGHT' },
      availability: 'UNAVAILABLE', reason: 'MULTIPLIER_NOT_PROVIDED', normalizedSales: null, multiplier: null, multiplierSource: null });
    fixture.normalization.availability = 'PARTIAL'; fixture.normalization.reason = 'INCOMPLETE_CALCULATIONS';
    Object.assign(fixture.compliance.sellers[0], { sales: '240', percent: '240', ratio: { numerator: '240', denominator: '100' } });
    const data = parse(fixture);
    expect(data.normalization.availability).toBe('PARTIAL'); expect(data.normalization.sellers).toHaveLength(2);
  });
  it.each(['coverage', 'window', 'measure', 'baseline', 'scenario', 'counts', 'average', 'relative', 'normalized', 'source', 'duplicate', 'population', 'unit'])('rejects inconsistent %s', field => {
    const f = fullPerformanceFixture();
    switch (field) {
      case 'coverage': f.normalization.coverage = []; break;
      case 'window': f.comparison.window.from = '2026-08-01'; break;
      case 'measure': f.normalization.measure = 'BASKET_COP'; break;
      case 'baseline': f.comparison.baselineDefinition = 'ACTIVE_WORKERS'; break;
      case 'scenario': f.comparison.sellers[0].scenario.dayType = 'UNKNOWN'; break;
      case 'counts': f.comparison.sellers[0].stationObservations = 0; break;
      case 'average': f.comparison.sellers[0].sellerAverage = '999'; break;
      case 'relative': f.comparison.sellers[0].relativePercent = '0'; break;
      case 'normalized': f.normalization.sellers[0].normalizedSales = '0'; break;
      case 'source': f.normalization.sellers[0].multiplierSource = null; break;
      case 'duplicate': f.comparison.sellers.push(f.comparison.sellers[0]); break;
      case 'population': f.comparison.sellers[0].workerId = '00000000-0000-0000-0000-000000000001'; break;
      case 'unit': f.unit = 'COP'; break;
    }
    expect(() => parse(f)).toThrow();
  });
});
