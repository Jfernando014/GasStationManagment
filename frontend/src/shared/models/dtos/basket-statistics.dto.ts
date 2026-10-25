/** Mirrors origin/feature/back/sales-statistics-scrum83 (d9a447b). */
export type StatisticsAvailability = 'AVAILABLE' | 'NO_DATA' | 'UNAVAILABLE';
export type BasketCategory = 'LUBRICANTE' | 'ADITIVO' | 'REFRIGERANTE' | 'OTRO';
export interface BasketGroup {
  workerId: string; workerName: string | null; category: BasketCategory; amount: string; quantity: string;
  saleLines: number; participationPercent: string | null; workedShifts: number | null; averageAmountPerWorkedShift: string | null;
}
export interface BasketSummary { amount: string; quantity: string; saleLines: number; workedShifts: number | null; averageAmountPerWorkedShift: string | null }
export interface ReportingChartSeries {
  id: string; label: string; chartType: 'BAR' | 'LINE'; metric: string; unit: string; workerId: string | null; category: BasketCategory | null;
  points: { key: string; periodStart: string | null; value: string; observations: number; change: string | null; changePercent: string | null }[];
}
export interface ReportingCharts {
  source: 'REAL_HISTORICAL_SALES'; availability: StatisticsAvailability; reason: string | null;
  from: string; to: string; outputScale: number; roundingMode: string; decimalPolicy: string;
  series: ReportingChartSeries[];
}
export interface BasketStatisticsResponse {
  from: string; to: string; workerId: string | null; category: BasketCategory | null; period: 'DAY' | 'WEEK' | 'MONTH';
  moneyUnit: 'COP'; quantityUnit: 'PRODUCT_UNITS'; workedShiftsAvailability: StatisticsAvailability; workedShiftsReason: string | null;
  summary: BasketSummary | null; groups: BasketGroup[]; charts: ReportingCharts;
}
export interface BasketSeller {
  workerId: string; name: string; document: string | null; amount: string; quantity: string; workedShifts: number | null;
  average: string | null; participation: string | null; groups: BasketGroup[];
}
