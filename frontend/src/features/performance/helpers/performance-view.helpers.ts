import {
  PerformanceCoverage, PerformanceMeasure, PerformanceReason, PerformanceScenario, ScenarioSale, SellerCompliance, SellerComparison
} from '../../../shared/models/dtos/seller-performance.dto';
import { Weekday } from '../../../shared/models/dtos/sales-statistics.dto';
import { addExact, divideExact, WEEKDAYS } from '../../../shared/helpers/sales-statistics.helpers';
import { formatDecimal } from '../../../shared/helpers/sales-target.helpers';
import { multiplyPerformance } from '../../../shared/helpers/seller-performance-reports.helpers';
import {
  DaySaleView, FEW_TURNS, IndexBar, IndexChartModel, ScenarioLine, SellerView
} from '../models/performance-view.model';

const WEEKDAY_ORDER = WEEKDAYS.map(day => day.value);
const WEEKDAY_SHORT: Record<Weekday, string> = {
  MONDAY: 'lun', TUESDAY: 'mar', WEDNESDAY: 'mié', THURSDAY: 'jue', FRIDAY: 'vie', SATURDAY: 'sáb', SUNDAY: 'dom'
};
const MONTHS = ['ene', 'feb', 'mar', 'abr', 'may', 'jun', 'jul', 'ago', 'sep', 'oct', 'nov', 'dic'];

const PLAIN_REASONS: Record<PerformanceReason, string> = {
  HISTORY_INCOMPLETE: 'Todavía faltan jornadas por procesar en este período.',
  NO_CONFIRMED_SELLER_CLOSES: 'No hay cierres de vendedor confirmados en este período.',
  TARGET_NOT_PROVIDED: 'Todavía no hay una meta para comparar.',
  TARGET_PERIOD_MISMATCH: 'La meta que llegó es de otro período.',
  TARGET_MEASURE_MISMATCH: 'La meta que llegó es de otra unidad de venta.',
  ZERO_TARGET: 'La meta es cero, así que no hay porcentaje que calcular.',
  NEGATIVE_OBSERVATIONS_REQUIRE_POLICY: 'Hay ajustes negativos que todavía no tienen una regla de cálculo.',
  SHIFT_CLASSIFICATION_NOT_PROVIDED: 'Todavía no se puede calcular: falta la clasificación de los turnos.',
  MULTIPLIER_NOT_PROVIDED: 'Todavía no se puede calcular: falta el factor de ajuste de este turno.',
  MULTIPLIER_PERIOD_MISMATCH: 'El factor de ajuste que llegó es de otro período.',
  MULTIPLIER_MEASURE_MISMATCH: 'El factor de ajuste que llegó es de otra unidad de venta.',
  INCOMPLETE_CALCULATIONS: 'Faltan parámetros para calcular algunos indicadores.',
  ZERO_BASELINE: 'El promedio de la estación es cero, así que no hay con qué comparar.'
};

/** The reason a figure is missing, in plain words for the person who reads the page. */
export function plainReason(reason: PerformanceReason | null): string {
  return reason === null ? '' : PLAIN_REASONS[reason];
}

/** Gallons are written with two decimals; pesos without them. */
export function displayScale(measure: PerformanceMeasure): number {
  return measure === 'FUEL_GALLONS' ? 2 : 0;
}

export function unitText(measure: PerformanceMeasure): string {
  return measure === 'FUEL_GALLONS' ? 'gal' : 'COP';
}

/** HALF_EVEN rounding of an exact decimal text, only to write it. */
export function roundText(value: string, scale: number): string {
  return divideExact(value, '1', scale) ?? value;
}

/** "439,40": rounded to `scale` decimals and written with the local separators. */
export function formatRounded(value: string, scale: number): string {
  return formatDecimal(roundText(value, scale));
}

/** The seller's index and averages over all the scenarios that can be compared. */
export interface SellerIndex {
  /** Percentage with one decimal, "108.0". */
  percent: string;
  turns: number;
  sellerAverage: string;
  stationAverage: string;
}

/**
 * Index of a seller against the station: what the seller sold divided by what the station average would have sold in
 * those same shifts, Σ(observations × seller average) ÷ Σ(observations × station average). Only scenarios that can be
 * compared count. Null when no scenario can be compared or the station average adds up to zero.
 */
export function sellerIndex(rows: SellerComparison[]): SellerIndex | null {
  const usable = rows.filter(row => row.availability === 'AVAILABLE');
  if (!usable.length) return null;
  let turns = 0, seller = '0', station = '0';
  for (const row of usable) {
    const observations = String(row.observations);
    turns += row.observations;
    seller = addExact(seller, multiplyPerformance(observations, row.sellerAverage));
    station = addExact(station, multiplyPerformance(observations, row.stationAverage));
  }
  const percent = divideExact(multiplyPerformance(seller, '100'), station, 1);
  const sellerAverage = divideExact(seller, String(turns), 2), stationAverage = divideExact(station, String(turns), 2);
  if (percent === null || sellerAverage === null || stationAverage === null) return null;
  return { percent, turns, sellerAverage, stationAverage };
}

function weekdayLabel(day: Weekday): string {
  return WEEKDAYS.find(item => item.value === day)?.label ?? day;
}

function turnsText(count: number): string {
  return `${count} ${count === 1 ? 'turno' : 'turnos'}`;
}

/** "jue, vie y sáb". */
function listDays(days: Weekday[]): string {
  const names = days.map(day => WEEKDAY_SHORT[day]);
  return names.length < 2 ? names.join('') : `${names.slice(0, -1).join(', ')} y ${names[names.length - 1]}`;
}

/**
 * Lines of the detail of one role, ordered from Monday to Sunday. The day of the week is the label; the kind of shift is
 * added only when the same day appears twice (one seller working day and night shifts on the same weekday).
 */
function scenarioLines(rows: SellerComparison[], measure: PerformanceMeasure): ScenarioLine[] {
  const scale = displayScale(measure), unit = unitText(measure);
  const sorted = [...rows].sort((a, b) =>
    WEEKDAY_ORDER.indexOf(a.scenario.weekday) - WEEKDAY_ORDER.indexOf(b.scenario.weekday)
    || a.scenario.dayType.localeCompare(b.scenario.dayType) || a.scenario.period.localeCompare(b.scenario.period)
    || a.scenario.workedCode.localeCompare(b.scenario.workedCode));
  const base = (row: SellerComparison) =>
    `${weekdayLabel(row.scenario.weekday)}${row.scenario.dayType === 'HOLIDAY' ? ' · Festivo' : ''}`;
  const count = (text: (row: SellerComparison) => string) => {
    const counts = new Map<string, number>();
    for (const row of sorted) counts.set(text(row), (counts.get(text(row)) ?? 0) + 1);
    return counts;
  };
  const byDay = count(base);
  const withPeriod = (row: SellerComparison) =>
    byDay.get(base(row))! > 1 ? `${base(row)} · ${row.scenario.period === 'DAY' ? 'de día' : 'de noche'}` : base(row);
  const byPeriod = count(withPeriod);
  const label = (row: SellerComparison) =>
    byPeriod.get(withPeriod(row))! > 1 ? `${withPeriod(row)} · ${row.scenario.workedCode}` : withPeriod(row);
  return sorted.map(row => {
    const comparable = row.availability === 'AVAILABLE' && row.relativePercent !== null;
    const text = label(row);
    return {
      key: `${row.scenario.weekday}|${row.scenario.dayType}|${row.scenario.period}|${row.scenario.workedCode}|${row.scenario.effectiveRole}`,
      label: text,
      observations: row.observations,
      sellerAverage: roundText(row.sellerAverage, scale),
      stationAverage: roundText(row.stationAverage, scale),
      percentText: comparable ? `${formatRounded(row.relativePercent!, 0)} %` : null,
      percentValue: comparable ? Number(row.relativePercent) : null,
      few: row.observations < FEW_TURNS,
      summary: `${text} · ${turnsText(row.observations)} · ${formatRounded(row.sellerAverage, scale)} ${unit} frente a ${formatRounded(row.stationAverage, scale)} de la estación`,
      reason: comparable ? '' : plainReason(row.reason),
      sellerValue: Number(row.sellerAverage),
      stationValue: Number(row.stationAverage)
    };
  });
}

function roleOf(rows: SellerComparison[]): string {
  const turns = (role: string) => rows.filter(row => row.scenario.effectiveRole === role).reduce((sum, row) => sum + row.observations, 0);
  if (!rows.length) return '';
  return turns('TITULAR') >= turns('APOYO') ? 'Titular' : 'Apoyo';
}

function supportSummary(lines: ScenarioLine[], rows: SellerComparison[]): string {
  if (!lines.length) return '';
  const turns = rows.reduce((sum, row) => sum + row.observations, 0);
  const days = [...new Set(rows.map(row => row.scenario.weekday))].sort((a, b) => WEEKDAY_ORDER.indexOf(a) - WEEKDAY_ORDER.indexOf(b));
  return `${turnsText(turns)} (${listDays(days)})${lines.every(line => line.few) ? ' · muy pocos para comparar' : ''}`;
}

/**
 * One view per seller of the report, even the ones that cannot be compared. Sellers are ordered from the highest to the
 * lowest index, then by name; this is a way to read the page, not an official classification.
 */
export function buildSellerViews(
  sellers: SellerCompliance[], comparisons: SellerComparison[], measure: PerformanceMeasure
): SellerView[] {
  const scale = displayScale(measure);
  const views = sellers.map<SellerView>(seller => {
    const rows = comparisons.filter(row => row.workerId === seller.workerId);
    const index = sellerIndex(rows);
    const titularRows = rows.filter(row => row.scenario.effectiveRole === 'TITULAR');
    const supportRows = rows.filter(row => row.scenario.effectiveRole === 'APOYO');
    const support = scenarioLines(supportRows, measure);
    return {
      workerId: seller.workerId,
      name: seller.recordedName,
      roleLabel: roleOf(rows),
      hasData: index !== null,
      turns: index?.turns ?? 0,
      sellerAverage: index ? roundText(index.sellerAverage, scale) : null,
      stationAverage: index ? roundText(index.stationAverage, scale) : null,
      indexText: index ? `${formatDecimal(index.percent)} %` : null,
      indexValue: index ? Number(index.percent) : null,
      sellerValue: index ? Number(index.sellerAverage) : 0,
      stationValue: index ? Number(index.stationAverage) : 0,
      titular: scenarioLines(titularRows, measure),
      support,
      supportSummary: supportSummary(support, supportRows)
    };
  });
  return views.sort((a, b) => (b.indexValue ?? -Infinity) - (a.indexValue ?? -Infinity) || a.name.localeCompare(b.name, 'es'));
}

/** "85 %", "92,5 %": the axis labels use a comma and keep a decimal only when they need it. */
function axisText(value: number): string {
  return `${String(value).replace('.', ',')} %`;
}

/**
 * The divergent chart: the center is 100 %, and the scale is the same on both sides, at least 15 points and a multiple
 * of 5 above the biggest distance.
 */
export function buildIndexChart(views: SellerView[]): IndexChartModel {
  const compared = views.filter(view => view.indexValue !== null);
  const deviation = compared.reduce((max, view) => Math.max(max, Math.abs(view.indexValue! - 100)), 0);
  const limit = Math.max(15, Math.ceil(deviation / 5) * 5);
  const bars = compared.map<IndexBar>(view => {
    const value = view.indexValue!;
    return {
      workerId: view.workerId,
      name: view.name,
      percentText: view.indexText!,
      value,
      up: value >= 100,
      width: Math.min(50, Math.abs(value - 100) / limit * 50),
      ariaLabel: `${view.name}: ${view.indexText} frente a la estación`
    };
  });
  const marks = [100 - limit, 100 - limit / 2, 100, 100 + limit / 2, 100 + limit];
  return {
    bars, limit,
    axis: marks.map((mark, index) => ({
      position: index * 25, emphasis: index === 2,
      text: index === 2 ? '100 % · igual a la estación' : axisText(mark)
    }))
  };
}

/** Position (0 to 100) of a value on the mini lines, the scale being the biggest average on screen. */
export function linePosition(value: number, maximum: number): number {
  return maximum <= 0 ? 0 : Math.max(0, Math.min(100, value / maximum * 100));
}

/** The biggest average among the sellers and their lines, to give every mini line the same scale. */
export function lineScale(views: SellerView[]): number {
  let maximum = 0;
  for (const view of views) {
    maximum = Math.max(maximum, view.sellerValue, view.stationValue);
    for (const line of [...view.titular, ...view.support]) maximum = Math.max(maximum, line.sellerValue, line.stationValue);
  }
  return maximum;
}

/** Sales of the day of every seller with their shift, and the ones that rested at the end. */
export function buildDaySales(
  sellers: SellerCompliance[], comparisons: ScenarioSale[], measure: PerformanceMeasure, resting: { workerId: string; name: string }[]
): { rows: DaySaleView[]; total: string } {
  const scale = displayScale(measure), unit = unitText(measure);
  const working = sellers.map(seller => {
    const scenario = comparisons.find(row => row.workerId === seller.workerId)?.scenario ?? null;
    return { seller, scenario };
  });
  const maximum = working.reduce((max, item) => Math.max(max, Number(item.seller.sales)), 0);
  const rows = working.map<DaySaleView>(({ seller, scenario }) => ({
    workerId: seller.workerId,
    name: seller.recordedName,
    roleLabel: scenario ? (scenario.effectiveRole === 'TITULAR' ? 'Titular' : 'Apoyo') : '',
    shiftCode: scenario?.workedCode ?? null,
    period: scenario?.period ?? null,
    sales: seller.sales,
    salesText: `${formatRounded(seller.sales, scale)} ${unit}`,
    salesValue: Number(seller.sales),
    width: maximum > 0 ? Number(seller.sales) / maximum * 100 : 0,
    resting: false,
    empty: seller.sales === '0'
  })).sort((a, b) => b.salesValue - a.salesValue || a.name.localeCompare(b.name, 'es'));
  const known = new Set(sellers.map(seller => seller.workerId));
  for (const person of resting.filter(item => !known.has(item.workerId))) {
    rows.push({ workerId: person.workerId, name: person.name, roleLabel: '', shiftCode: null, period: null, sales: null,
      salesText: '—', salesValue: 0, width: 0, resting: true, empty: false });
  }
  const total = sellers.reduce((sum, seller) => addExact(sum, seller.sales), '0');
  return { rows, total: `${formatRounded(total, scale)} ${unit}` };
}

/** A scenario in plain words: "Viernes · de noche · titular". */
export function plainScenario(scenario: PerformanceScenario): string {
  return `${weekdayLabel(scenario.weekday)}${scenario.dayType === 'HOLIDAY' ? ' festivo' : ''} · ${scenario.period === 'DAY' ? 'de día' : 'de noche'} · ${scenario.effectiveRole === 'TITULAR' ? 'titular' : 'apoyo'}`;
}

/** "17 oct 2025". */
export function shortDate(day: string): string {
  return `${Number(day.slice(8, 10))} ${monthYear(day)}`;
}

/** "Viernes 20 nov 2026": the day of the week and the date, for the title of a day. */
export function longDayLabel(day: string): string {
  const weekday = WEEKDAY_ORDER[(new Date(`${day}T00:00:00Z`).getUTCDay() + 6) % 7];
  return `${weekdayLabel(weekday)} ${shortDate(day)}`;
}

function monthYear(day: string): string {
  return `${MONTHS[Number(day.slice(5, 7)) - 1]} ${day.slice(0, 4)}`;
}

/** Real range of the days that have closed shifts, "nov 2025 a ago 2026", or null when none has. */
export function dataRangeLabel(coverage: PerformanceCoverage[]): string | null {
  const days = coverage.filter(item => item.status === 'PROCESSED').map(item => item.date).sort();
  if (!days.length) return null;
  const first = days[0], last = days[days.length - 1];
  if (first === last) return `${Number(first.slice(8))} ${monthYear(first)}`;
  return monthYear(first) === monthYear(last) ? monthYear(first) : `${monthYear(first)} a ${monthYear(last)}`;
}
