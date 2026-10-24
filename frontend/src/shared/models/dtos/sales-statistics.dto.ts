import { SalesTargetCategory, SalesTargetUnit } from './sales-target.dto';

export type SalesJourney = 'DAY' | 'NIGHT';
export type StatisticsTab = 'promedios' | 'escenarios' | 'canastas' | 'competencia';
export type Weekday = 'MONDAY' | 'TUESDAY' | 'WEDNESDAY' | 'THURSDAY' | 'FRIDAY' | 'SATURDAY' | 'SUNDAY';
export interface SalesObservation { date: string; journey: SalesJourney; value: string }
export interface ExplicitStatisticsRequest {
  from: string; to: string; category: SalesTargetCategory; outputScale: number;
  observations: SalesObservation[];
}
export interface SalesScenario { dayOfWeek: Weekday; journey: SalesJourney }
export interface CompareStatisticsRequest { sales: ExplicitStatisticsRequest; first: SalesScenario; second: SalesScenario }
export interface StatisticsSummary {
  label: string; dayOfWeek: Weekday | null; journey: SalesJourney;
  /** An unavailable legacy total of zero is normalized to null. */
  total: string | null; observations: number; available: boolean; average: string | null;
  exactAverage: { numerator: string; denominator: string } | null;
}
export interface SalesStatisticsResponse {
  from: string; to: string; category: SalesTargetCategory; unit: SalesTargetUnit;
  source: 'CLIENT_SUPPLIED_OBSERVATIONS'; outputScale: number; roundingMode: 'HALF_EVEN';
  series: StatisticsSummary[];
}
export interface StatisticsChartPoint { label: string; value: string }
export interface StatisticsChartSeries { label: string; color: string; points: StatisticsChartPoint[] }
