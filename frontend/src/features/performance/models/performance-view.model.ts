/** First day with confirmed history (from the sheets of the client); "Todo el historial" starts here. */
export const HISTORY_START = '2025-10-17';

/** Below this many shifts a scenario is too small to compare, so it is shown in gray. */
export const FEW_TURNS = 3;

export type PerformanceMode = 'dia' | 'mes' | 'historial';

export type PerformanceTab = 'estacion' | 'ajustado';

/** One line of the detail of a seller: a day of the week (and a kind of shift when it is needed to tell them apart). */
export interface ScenarioLine {
  key: string;
  /** "Viernes", "Viernes · Festivo", "Viernes · de noche". */
  label: string;
  observations: number;
  /** Average of the seller and of the station, as decimal text with two decimals. */
  sellerAverage: string;
  stationAverage: string;
  /** Written index, "109 %"; null when the scenario cannot be compared. */
  percentText: string | null;
  percentValue: number | null;
  /** Too few shifts to compare. */
  few: boolean;
  /** "Viernes · 28 turnos · 439,40 gal frente a 389,89 de la estación". */
  summary: string;
  /** Why it cannot be compared, in plain words; empty when it can. */
  reason: string;
  sellerValue: number;
  stationValue: number;
}

export interface SellerView {
  workerId: string;
  name: string;
  /** "Titular" or "Apoyo": the role in which the seller has more shifts. */
  roleLabel: string;
  /** False when no scenario of the seller could be compared. */
  hasData: boolean;
  turns: number;
  sellerAverage: string | null;
  stationAverage: string | null;
  indexText: string | null;
  indexValue: number | null;
  sellerValue: number;
  stationValue: number;
  titular: ScenarioLine[];
  support: ScenarioLine[];
  /** "3 turnos (jue, vie y sáb) · muy pocos para comparar". */
  supportSummary: string;
}

/** A bar of the divergent chart. */
export interface IndexBar {
  workerId: string;
  name: string;
  percentText: string;
  value: number;
  up: boolean;
  /** Width of the bar as a percentage of half the track. */
  width: number;
  ariaLabel: string;
}

export interface IndexChartModel {
  bars: IndexBar[];
  /** Points above and below 100 the track reaches. */
  limit: number;
  /** Positions (0 to 100) and text of the five marks of the axis. */
  axis: { position: number; text: string; emphasis: boolean }[];
}

/** A seller's day: what was sold in the shift of the day. */
export interface DaySaleView {
  workerId: string;
  name: string;
  roleLabel: string;
  shiftCode: string | null;
  period: 'DAY' | 'NIGHT' | null;
  /** Sales as decimal text; null for someone who rested. */
  sales: string | null;
  salesText: string;
  salesValue: number;
  /** Width of the bar, 0 to 100. */
  width: number;
  resting: boolean;
  /** Has a shift but no sales. */
  empty: boolean;
}
