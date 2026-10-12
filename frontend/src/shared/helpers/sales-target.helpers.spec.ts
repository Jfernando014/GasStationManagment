import { FormControl } from '@angular/forms';
import { decimalText, formatDecimal, monthlyDailyPreview, parseSalesTargets, targetDecimalValidator, targetMargin } from './sales-target.helpers';

describe('C-14 exact decimals', () => {
  it('accepts zero, comma decimals and extra trailing zeros within NUMERIC(30,12)', () => {
    expect(decimalText('0')).toBe('0');
    expect(decimalText('00012,345600000000000')).toBe('12.3456');
    expect(decimalText('999999999999999999.999999999999')).toBe('999999999999999999.999999999999');
  });
  it('rejects negative values, grouping, overflow and lossy scale', () => {
    for (const value of ['-1', '1.000,25', 'NaN', '1000000000000000000', '0.0000000000001']) {
      expect(targetDecimalValidator(new FormControl(value))).not.toBeNull();
    }
  });
  it('calculates the prototype margin exactly, allowing a negative margin', () => {
    expect(targetMargin('999999999999999999.123456789012', '999999999999999999.123456789013')).toBe('+0.000000000001');
    expect(targetMargin('1', '0')).toBe('-1');
    expect(targetMargin('', '1')).toBeNull();
    expect(formatDecimal('+12345.6789')).toBe('+12.345,6789');
  });
  it('rounds only the monthly daily presentation, using calendar days', () => {
    expect(monthlyDailyPreview('15', 30)).toBe('1');
    expect(monthlyDailyPreview('14.999999999999', 30)).toBe('0');
  });
  it('preserves JSON Long IDs, BigDecimals and scientific notation in responses', () => {
    const raw = '[{"id":9223372036854775807,"year":2026,"month":null,"category":"FUEL_AGGREGATE","unit":"GALLONS","value":999999999999999999.123456789012,"maximumSale":1E-12,"createdAt":"2026-10-11T12:00:00Z","message":"Metas 2026"}]';
    const [target] = parseSalesTargets(raw);
    expect(target.id).toBe('9223372036854775807');
    expect(target.value).toBe('999999999999999999.123456789012');
    expect(target.maximumSale).toBe('0.000000000001');
    expect(target.message).toBe('Metas 2026');
    expect(parseSalesTargets(raw.replace('1E-12', '0E-12'))[0].maximumSale).toBe('0');
  });
  it('rejects unavailable or incompatible API bodies instead of enabling registration', () => {
    for (const raw of ['null', '{}', '{"detail":"Not found"}']) expect(() => parseSalesTargets(raw)).toThrow();
    expect(parseSalesTargets('[]')).toEqual([]);
  });
});
