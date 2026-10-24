/** Contract fixtures for tests only. Never imported by application components or services. */
import { BasketStatisticsResponse } from '../../shared/models/dtos/basket-statistics.dto';
import { ExplicitStatisticsRequest, SalesStatisticsResponse } from '../../shared/models/dtos/sales-statistics.dto';
import { PriceComparison } from '../../shared/models/dtos/competition.dto';

export const observationRequest: ExplicitStatisticsRequest = { from: '2026-08-01', to: '2026-08-31', category: 'FUEL_AGGREGATE', outputScale: 2,
  observations: [{ date: '2026-08-03', journey: 'DAY', value: '100' }, { date: '2026-08-03', journey: 'NIGHT', value: '50' }] };
export function averagesFixture(): SalesStatisticsResponse {
  return { ...observationRequest, unit: 'GALLONS', source: 'CLIENT_SUPPLIED_OBSERVATIONS', roundingMode: 'HALF_EVEN', series: [
    { label: 'Diurna', journey: 'DAY', dayOfWeek: null, total: '100', average: '100', exactAverage: { numerator: '100', denominator: '1' }, available: true, observations: 1 },
    { label: 'Nocturna', journey: 'NIGHT', dayOfWeek: null, total: '50', average: '50', exactAverage: { numerator: '50', denominator: '1' }, available: true, observations: 1 }
  ] };
}
export function basketFixture(status: 'AVAILABLE' | 'NO_DATA' | 'UNAVAILABLE' = 'UNAVAILABLE'): BasketStatisticsResponse {
  return { from: '2026-08-01', to: '2026-08-31', workerId: null, category: null, period: 'DAY', moneyUnit: 'COP', quantityUnit: 'PRODUCT_UNITS', workedShiftsAvailability: 'UNAVAILABLE', workedShiftsReason: 'WORKED_SHIFTS_SOURCE_NOT_CONNECTED',
    summary: status === 'AVAILABLE' ? { amount: '100.5', quantity: '2', saleLines: 2, workedShifts: null, averageAmountPerWorkedShift: null } : null,
    groups: status === 'AVAILABLE' ? [{ workerId: '00000000-0000-0000-0000-000000000001', workerName: 'Vendedor de prueba', category: 'LUBRICANTE', amount: '100.5', quantity: '2', saleLines: 2, participationPercent: '100', workedShifts: null, averageAmountPerWorkedShift: null }] : [],
    charts: { source: 'REAL_HISTORICAL_SALES', availability: status, reason: status === 'AVAILABLE' ? null : status === 'NO_DATA' ? 'NO_RECORDED_SALES_FOR_FILTERS' : 'SALES_SOURCE_NOT_CONNECTED', from: '2026-08-01', to: '2026-08-31', outputScale: 2, roundingMode: 'HALF_EVEN', decimalPolicy: 'EXACT_TOTALS_HALF_EVEN_DERIVED_VALUES', series: [] } };
}
export function comparisonFixture(): PriceComparison {
  return { asOf: '2026-08-03T12:00:00Z', bellavistaDate: '2026-08-03', mode: 'CURRENT_LATEST', unit: 'COP_PER_GALLON', activeCompetitorsOnly: true,
    fuels: [{ fuelType: 'MOTOR', bellavistaPrice: '100', bellavistaValidFrom: '2026-08-01', competition: { competitorCount: 1, average: '90', minimum: '90' }, versusAverage: { absoluteCopPerGallon: '10', percentage: '11.111111' }, versusMinimum: { absoluteCopPerGallon: '10', percentage: '11.111111' },
      observations: [{ id: '1', competitorId: '1', fuelType: 'MOTOR', pricePerGallon: '90', observedAt: '2026-08-02T15:00:00Z', createdAt: '2026-08-02T15:00:00Z' }] }] };
}
