import { SellerComparison } from '../../../shared/models/dtos/seller-performance.dto';
import {
  buildIndexChart, buildSellerViews, dataRangeLabel, linePosition, longDayLabel, plainReason, sellerIndex, shortDate
} from './performance-view.helpers';

function row(partial: Partial<SellerComparison> & { weekday?: 'MONDAY' | 'FRIDAY'; role?: 'TITULAR' | 'APOYO' }): SellerComparison {
  return {
    workerId: 'w1', recordedName: 'Ana Milena Villamil',
    scenario: { weekday: partial.weekday ?? 'FRIDAY', period: 'DAY', dayType: 'REGULAR', workedCode: 'D1', effectiveRole: partial.role ?? 'TITULAR' },
    sales: '0', observations: 1, availability: 'AVAILABLE', reason: null, sellerAverage: '100', stationSales: '0',
    stationObservations: 1, stationAverage: '100', relativeAverage: null, relativePercent: '100', ...partial
  } as SellerComparison;
}

describe('performance view helpers', () => {
  it('weights the index by shifts: Σ(turnos × promedio) ÷ Σ(turnos × promedio de la estación)', () => {
    const index = sellerIndex([
      row({ observations: 28, sellerAverage: '439.40', stationAverage: '389.89' }),
      row({ observations: 2, sellerAverage: '100', stationAverage: '200', weekday: 'MONDAY' })
    ]);
    // (28 × 439,40 + 2 × 100) ÷ (28 × 389,89 + 2 × 200) = 12503,2 ÷ 11316,92
    expect(index).toEqual({ percent: '110.5', turns: 30, sellerAverage: '416.77', stationAverage: '377.23' });
  });

  it('ignores scenarios that cannot be compared and returns null when none can', () => {
    expect(sellerIndex([row({ availability: 'UNAVAILABLE', reason: 'ZERO_BASELINE' })])).toBeNull();
    expect(sellerIndex([row({ stationAverage: '0' })])).toBeNull();
    expect(sellerIndex([row({ observations: 3, sellerAverage: '110', stationAverage: '100' }),
      row({ availability: 'UNAVAILABLE', observations: 50, sellerAverage: '1', stationAverage: '100', weekday: 'MONDAY' })]
    )?.percent).toBe('110.0');
  });

  it('draws a symmetric scale of at least 15 points, rounded to multiples of 5, sorted from highest to lowest', () => {
    const sellers = [
      { workerId: 'a', recordedName: 'Ana', sales: '0', target: null, percent: null, ratio: null, availability: 'AVAILABLE', reason: null, targetSource: null },
      { workerId: 'b', recordedName: 'Beto', sales: '0', target: null, percent: null, ratio: null, availability: 'AVAILABLE', reason: null, targetSource: null }
    ];
    const views = buildSellerViews(sellers as never, [
      row({ workerId: 'a', recordedName: 'Ana', sellerAverage: '108', stationAverage: '100' }),
      row({ workerId: 'b', recordedName: 'Beto', sellerAverage: '77', stationAverage: '100' })
    ], 'FUEL_GALLONS');
    expect(views.map(view => view.name)).toEqual(['Ana', 'Beto']);
    const chart = buildIndexChart(views);
    expect(chart.limit).toBe(25);
    expect(chart.bars.map(bar => bar.up)).toEqual([true, false]);
    expect(chart.bars[0].width).toBeCloseTo(16, 5);
    expect(chart.bars[0].ariaLabel).toBe('Ana: 108,0 % frente a la estación');
    expect(chart.axis.map(mark => mark.text)).toEqual(['75 %', '87,5 %', '100 % · igual a la estación', '112,5 %', '125 %']);
    expect(buildIndexChart([]).limit).toBe(15);
  });

  it('writes the reasons in plain words and the dates the way the page shows them', () => {
    expect(plainReason('SHIFT_CLASSIFICATION_NOT_PROVIDED')).toBe('Todavía no se puede calcular: falta la clasificación de los turnos.');
    expect(plainReason(null)).toBe('');
    expect(shortDate('2025-10-17')).toBe('17 oct 2025');
    expect(longDayLabel('2026-11-20')).toBe('Viernes 20 nov 2026');
    expect(linePosition(150, 100)).toBe(100);
    expect(linePosition(5, 0)).toBe(0);
  });

  it('gives the real range of the closed days, not the range that was asked for', () => {
    const day = (date: string, status: 'PROCESSED' | 'PENDING' | 'PROCESSED_EMPTY' | 'ABSENT') => ({ date, status });
    expect(dataRangeLabel([day('2025-10-16', 'PENDING'), day('2025-11-03', 'PROCESSED'), day('2026-08-20', 'PROCESSED'), day('2026-09-01', 'ABSENT')]))
      .toBe('nov 2025 a ago 2026');
    expect(dataRangeLabel([day('2026-08-20', 'PROCESSED')])).toBe('20 ago 2026');
    expect(dataRangeLabel([day('2026-08-20', 'PENDING')])).toBeNull();
  });
});
