import { addExact, dateRangeError, difference, divideExact, monthRange, parseStatistics, participation, serializeObservations } from './sales-statistics.helpers';
import { basketSellers, parseBasketStatistics, priceValidator } from './statistics-contracts.helpers';
import { FormControl } from '@angular/forms';
import { averagesFixture, basketFixture, observationRequest } from '../../features/statistics/statistics-test-data';

describe('SCRUM-84 exact statistics and contracts', () => {
  it('sums, subtracts and calculates percentages without losing monetary precision', () => {
    expect(addExact('999999999999999999.123456789012', '0.000000000001')).toBe('999999999999999999.123456789013');
    expect(difference('2.1', '3.25')).toBe('-1.15'); expect(participation('-20', '100')).toBe('-20.00');
    expect(participation('0', '0')).toBeNull(); expect(divideExact('1.005', '1')).toBe('1.00'); expect(divideExact('1.015', '1')).toBe('1.02');
  });
  it('validates inclusive ranges and real calendar month boundaries', () => {
    expect(monthRange('2024-02')).toEqual({ from: '2024-02-01', to: '2024-02-29' });
    expect(monthRange('2026-02').to).toBe('2026-02-28'); expect(dateRangeError('2026-02-30', '2026-03-01')).not.toBe('');
    expect(dateRangeError('2026-08-31', '2026-08-01')).not.toBe(''); expect(() => monthRange('2026-13')).toThrow();
  });
  it('preserves zero observations and rejects duplicates and out of range records', () => {
    expect(serializeObservations({ ...observationRequest, observations: [{ ...observationRequest.observations[0], value: '0' }] })).toContain('"value":0');
    expect(() => serializeObservations({ ...observationRequest, observations: [observationRequest.observations[0], observationRequest.observations[0]] })).toThrow();
    expect(() => serializeObservations({ ...observationRequest, to: '2026-08-01' })).toThrow();
  });
  it('normalizes unavailable legacy totals to null and rejects a fabricated provenance', () => {
    const data = averagesFixture(); data.series[1] = { ...data.series[1], available: false, average: null, total: '0', observations: 0, exactAverage: null };
    expect(parseStatistics(JSON.stringify(data), observationRequest).series[1].total).toBeNull();
    expect(() => parseStatistics(JSON.stringify({ ...data, source: 'REAL_HISTORICAL_SALES' }), observationRequest)).toThrow();
  });
  it.each(['NO_DATA', 'UNAVAILABLE'] as const)('does not convert %s to a zero sale', status => {
    const response = parseBasketStatistics(JSON.stringify(basketFixture(status)));
    expect(response.summary).toBeNull(); expect(basketSellers(response)).toEqual([]); expect(response.charts.availability).toBe(status);
  });
  it('aggregates categories per seller without counting each shift twice', () => {
    const response = basketFixture('AVAILABLE'); response.workedShiftsAvailability = 'AVAILABLE';
    response.groups[0].workedShifts = 2;
    response.groups.push({ ...response.groups[0], category: 'ADITIVO', amount: '100.5' });
    response.summary!.amount = '201';
    const sellers = basketSellers(response); expect(sellers).toHaveLength(1); expect(sellers[0].amount).toBe('201');
    expect(sellers[0].workedShifts).toBe(2); expect(sellers[0].average).toBe('100.50'); expect(sellers[0].participation).toBe('100.00');
  });
  it('validates optional costs independently from strictly positive selling prices', () => {
    expect(priceValidator()(new FormControl('0'))).not.toBeNull(); expect(priceValidator(true)(new FormControl('0'))).toBeNull();
    expect(priceValidator()(new FormControl('1.001'))).not.toBeNull(); expect(priceValidator()(new FormControl('10000000000'))).not.toBeNull();
  });
});
