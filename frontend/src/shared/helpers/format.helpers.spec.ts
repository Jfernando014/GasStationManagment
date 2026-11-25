import { todayCalendarDay, yesterdayCalendarDay } from './format.helpers';

describe('format.helpers calendar day helpers', () => {
  it('formats todayCalendarDay in local calendar YYYY-MM-DD', () => {
    expect(todayCalendarDay(new Date(2026, 10, 25, 14, 30))).toBe('2026-11-25');
  });

  it('returns the previous calendar day for a standard date', () => {
    expect(yesterdayCalendarDay(new Date(2026, 10, 25, 14, 30))).toBe('2026-11-24');
  });

  it('handles month boundaries without UTC drift', () => {
    expect(yesterdayCalendarDay(new Date(2026, 2, 1, 0, 5))).toBe('2026-02-28');
    expect(yesterdayCalendarDay(new Date(2024, 2, 1, 0, 5))).toBe('2024-02-29');
  });

  it('handles year boundaries cleanly', () => {
    expect(yesterdayCalendarDay(new Date(2026, 0, 1, 1, 0))).toBe('2025-12-31');
  });
});
