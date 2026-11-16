export type PerformanceMeasure = 'FUEL_GALLONS' | 'BASKET_COP';
export type PerformanceAvailability = 'AVAILABLE' | 'PARTIAL' | 'UNAVAILABLE' | 'NO_DATA';
export type PerformanceReason =
  | 'HISTORY_INCOMPLETE' | 'NO_CONFIRMED_SELLER_CLOSES' | 'TARGET_NOT_PROVIDED'
  | 'TARGET_PERIOD_MISMATCH' | 'TARGET_MEASURE_MISMATCH' | 'ZERO_TARGET'
  | 'NEGATIVE_OBSERVATIONS_REQUIRE_POLICY' | 'SHIFT_CLASSIFICATION_NOT_PROVIDED'
  | 'MULTIPLIER_NOT_PROVIDED' | 'MULTIPLIER_PERIOD_MISMATCH' | 'MULTIPLIER_MEASURE_MISMATCH'
  | 'INCOMPLETE_CALCULATIONS' | 'ZERO_BASELINE';

export interface PerformanceWindow { from: string; to: string }
export interface PerformanceQuery extends PerformanceWindow {
  measure: PerformanceMeasure;
  outputScale: number;
}
export interface PerformanceCoverage {
  date: string;
  status: 'PROCESSED' | 'PROCESSED_EMPTY' | 'PENDING' | 'ABSENT';
}
export interface SellerCompliance {
  workerId: string;
  recordedName: string;
  sales: string;
  target: string | null;
  availability: 'AVAILABLE' | 'UNAVAILABLE';
  reason: PerformanceReason | null;
  ratio: { numerator: string; denominator: string } | null;
  percent: string | null;
  targetSource: string | null;
}
export interface ComplianceReport {
  window: PerformanceWindow;
  measure: PerformanceMeasure;
  availability: PerformanceAvailability;
  reason: PerformanceReason | null;
  coverage: PerformanceCoverage[];
  sellers: SellerCompliance[];
}

/** Independent C-14 projection; FullSellerPerformanceResponse adds C-09 without coupling availability. */
export interface SellerPerformanceResponse {
  source: 'CONFIRMED_SHIFT_SALES_HISTORY';
  unit: 'GALLONS' | 'COP';
  outputScale: number;
  roundingMode: 'HALF_EVEN';
  compliance: ComplianceReport;
}

export interface PerformanceScenario {
  weekday: 'MONDAY' | 'TUESDAY' | 'WEDNESDAY' | 'THURSDAY' | 'FRIDAY' | 'SATURDAY' | 'SUNDAY';
  period: 'DAY' | 'NIGHT';
  dayType: 'REGULAR' | 'HOLIDAY';
  workedCode: string;
  effectiveRole: 'TITULAR' | 'APOYO';
}
export interface ScenarioSale {
  workerId: string;
  recordedName: string;
  scenario: PerformanceScenario;
  sales: string;
  observations: number;
  availability: 'AVAILABLE' | 'UNAVAILABLE';
  reason: PerformanceReason | null;
}
export interface SellerComparison extends ScenarioSale {
  sellerAverage: string;
  stationSales: string;
  stationObservations: number;
  stationAverage: string;
  relativeAverage: { numerator: string; denominator: string } | null;
  /** 100 means equal to the station; this is not a percentage difference. */
  relativePercent: string | null;
}
export interface NormalizedSale extends ScenarioSale {
  multiplier: string | null;
  normalizedSales: string | null;
  multiplierSource: string | null;
}
export interface ScenarioReport<T extends ScenarioSale> extends Omit<ComplianceReport, 'sellers'> { sellers: T[] }
export interface FullSellerPerformanceResponse extends SellerPerformanceResponse {
  comparison: ScenarioReport<SellerComparison> & { baselineDefinition: string };
  normalization: ScenarioReport<NormalizedSale>;
}
