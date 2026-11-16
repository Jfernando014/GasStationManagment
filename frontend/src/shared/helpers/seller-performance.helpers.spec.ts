import { applicableTarget, complianceBarValue, parseSellerPerformance, performanceRangeError } from './seller-performance.helpers';
import { emptyPerformanceFixture, missingTargetSeller, performanceFixture, performanceQuery } from './seller-performance.test-data';

describe('SCRUM-108 seller performance parser', () => {
  it('decodes compliance independently of unavailable C-09 calculations', () => {
    const data = parseSellerPerformance(JSON.stringify(performanceFixture()), performanceQuery);
    expect(data.compliance.availability).toBe('AVAILABLE');
    expect(data.compliance.sellers[0].percent).toBe('120');
  });
  it('preserves numeric JSON tokens and HALF_EVEN percentages without binary float rounding', () => {
    const fixture = performanceFixture();
    fixture.compliance.sellers[0] = { ...fixture.compliance.sellers[0], sales: '999999999999999999.123456789012',
      target: '999999999999999999.123456789012', percent: '100', ratio: {
        numerator: '999999999999999999.123456789012', denominator: '999999999999999999.123456789012'
      } };
    const json = JSON.stringify(fixture).replace(/"999999999999999999\.123456789012"/g, '999999999999999999.123456789012');
    expect(parseSellerPerformance(json, performanceQuery).compliance.sellers[0].sales).toBe('999999999999999999.123456789012');
    const seller = fixture.compliance.sellers[0];
    Object.assign(seller, { sales: '0.00005', target: '1', percent: '0.00', ratio: { numerator: '0.00005', denominator: '1' } });
    expect(parseSellerPerformance(JSON.stringify(fixture).replace(/"0\.00005"/g, '5E-5'), performanceQuery).compliance.sellers[0].percent).toBe('0');
    seller.percent = '0.01';
    expect(() => parseSellerPerformance(JSON.stringify(fixture), performanceQuery)).toThrow();
  });
  it('keeps exact backend order, zero sales, and null targets in a partial report', () => {
    const fixture = performanceFixture();
    fixture.compliance.sellers.push(missingTargetSeller());
    fixture.compliance.availability = 'PARTIAL';
    fixture.compliance.reason = 'INCOMPLETE_CALCULATIONS';
    const report = parseSellerPerformance(JSON.stringify(fixture), performanceQuery).compliance;
    expect(report.sellers.map(s => s.recordedName)).toEqual(['Ángela Pérez', 'Vendedor histórico inactivo']);
    expect(report.sellers[1].sales).toBe('0');
    expect(report.sellers[1].percent).toBeNull();
    const seller = fixture.compliance.sellers[0];
    Object.assign(seller, { sales: '0', percent: '0', ratio: { numerator: '0', denominator: '100' } });
    expect(parseSellerPerformance(JSON.stringify(fixture), performanceQuery).compliance.sellers[0].percent).toBe('0');
  });
  it.each(['PENDING', 'ABSENT'])('preserves %s coverage without inventing partial totals', status => {
    const fixture = emptyPerformanceFixture();
    fixture.compliance.availability = 'UNAVAILABLE';
    fixture.compliance.reason = 'HISTORY_INCOMPLETE';
    fixture.compliance.coverage[0].status = status;
    expect(parseSellerPerformance(JSON.stringify(fixture), performanceQuery).compliance.coverage[0].status).toBe(status);
    fixture.compliance.sellers.push(performanceFixture().compliance.sellers[0]);
    expect(() => parseSellerPerformance(JSON.stringify(fixture), performanceQuery)).toThrow();
  });
  it('distinguishes NO_DATA from uncalculable negative adjustments', () => {
    const fixture = emptyPerformanceFixture();
    expect(parseSellerPerformance(JSON.stringify(fixture), performanceQuery).compliance.availability).toBe('NO_DATA');
    fixture.compliance.availability = 'UNAVAILABLE';
    fixture.compliance.reason = 'NEGATIVE_OBSERVATIONS_REQUIRE_POLICY';
    fixture.compliance.coverage[0].status = 'PROCESSED';
    expect(parseSellerPerformance(JSON.stringify(fixture), performanceQuery).compliance.sellers).toEqual([]);
  });
  it.each(['TARGET_NOT_PROVIDED', 'TARGET_PERIOD_MISMATCH', 'TARGET_MEASURE_MISMATCH', 'ZERO_TARGET'])('retains unavailable rows for %s', reason => {
    const fixture = performanceFixture();
    fixture.compliance.sellers = [{ ...missingTargetSeller(), reason,
      target: reason === 'TARGET_NOT_PROVIDED' ? null : '0', targetSource: reason === 'TARGET_NOT_PROVIDED' ? null : 'source' }];
    fixture.compliance.availability = 'UNAVAILABLE';
    fixture.compliance.reason = 'INCOMPLETE_CALCULATIONS';
    const seller = parseSellerPerformance(JSON.stringify(fixture), performanceQuery).compliance.sellers[0];
    expect(seller.percent).toBeNull();
    expect(applicableTarget(seller)).toBe(reason === 'ZERO_TARGET' ? '0' : null);
  });
  it.each(['unit', 'source', 'range', 'scale', 'reason', 'coverage', 'percent', 'availability', 'duplicate', 'missingNull'])('rejects inconsistent %s', field => {
    const fixture = performanceFixture();
    switch (field) {
      case 'unit': fixture.unit = 'COP'; break;
      case 'source': fixture.source = 'CLIENT_SUPPLIED_OBSERVATIONS'; break;
      case 'range': fixture.compliance.window.to = '2026-08-04'; break;
      case 'scale': fixture.outputScale = 3; break;
      case 'reason': fixture.compliance.reason = 'TARGET_NOT_PROVIDED'; break;
      case 'coverage': fixture.compliance.coverage = []; break;
      case 'percent': fixture.compliance.sellers[0].percent = '99'; break;
      case 'availability': fixture.compliance.availability = 'NO_DATA'; break;
      case 'duplicate': fixture.compliance.sellers.push(fixture.compliance.sellers[0]); break;
      case 'missingNull': return expect(() => parseSellerPerformance(JSON.stringify(fixture).replace('"reason":null,', ''), performanceQuery)).toThrow();
    }
    expect(() => parseSellerPerformance(JSON.stringify(fixture), performanceQuery)).toThrow();
  });
  it('validates operational-date ranges and bounds only decorative bar values', () => {
    expect(performanceRangeError('2026-02-30', '2026-03-01')).not.toBe('');
    expect(performanceRangeError('2026-08-04', '2026-08-03')).not.toBe('');
    expect(performanceRangeError('2020-01-01', '2031-01-01')).toContain('3660');
    expect(complianceBarValue(null)).toBeNull();
    expect(complianceBarValue('0')).toBe(0);
    expect(complianceBarValue('999999999999999999999999')).toBe(150);
  });
});
