import { DivergingBar } from '../../../atomic-design/atoms/diverging-bar-chart/diverging-bar-chart.component';
import { TrendPoint } from '../../../atomic-design/atoms/trend-band-chart/trend-band-chart.component';
import { formatLongDate } from '../../../shared/helpers/format.helpers';
import { ShownAlertLevel } from '../../../shared/models/dtos/alert-level.dto';
import {
  DailyVariationDto, FuelMonthlyVariationDto, GallonRange, MonthlyVariationDto
} from '../models/monthly-variation.dto';
import { DayRow, TotalsRow, calculatedSold, dayRows, elapsedDays, totalsRow } from './variation-days.helpers';
import {
  UNKNOWN, VarianceTone, dayAndMonth, formatAmount, gallons, limitPercent, monthWord, plural, signed, signedPercent,
  variationFuelName, varianceTone
} from './variation-format.helpers';
import { Streak, accumulatedLine, currentStreak, fuelLevel } from './variation-summary.helpers';

export interface StatusView {
  level: ShownAlertLevel;
  /** "al 28 abr" */
  asOf: string;
  title: string;
  text: string;
  /** What to do, only when it is time to ask for help or to act. */
  advice: string;
  streak: string;
}

export interface Metric {
  label: string;
  /** Empty when there is nothing to put as a figure; the tile shows `unset` instead. */
  value: string;
  unset?: string;
  note: string;
  tone: VarianceTone;
}

export interface ChartView {
  bars: DivergingBar[];
  line: TrendPoint[];
  slots: number;
  limit: number;
  level: ShownAlertLevel;
  /** Labels under the charts, from the first to the last day. */
  axis: string[];
  barsLabel: string;
  lineLabel: string;
}

export interface RangeRow {
  range: GallonRange;
  name: string;
  count: number;
  limit: number;
  text: string;
  shortText: string;
  level: ShownAlertLevel;
  tone: 'ok' | 'warning' | 'alarm';
  /** Value at the end of the bar. */
  scaleMax: number;
}

export interface RangesView {
  rows: RangeRow[];
  surplus: string;
  shortSurplus: string;
}

export interface FuelDetailView {
  name: string;
  status: StatusView;
  metrics: Metric[];
  /** Four indicators that group the same figures for a phone. */
  compactMetrics: Metric[];
  chart: ChartView;
  ranges: RangesView;
  rows: DayRow[];
  totals: TotalsRow;
}

const RANGE_NAMES: Record<GallonRange, { short: string; words: string }> = {
  FROM_1_TO_49: { short: '1–49 gal', words: '1 a 49 gal' },
  FROM_50_TO_74: { short: '50–74', words: '50 a 74 gal' },
  FROM_75_TO_124: { short: '75–124', words: '75 a 124 gal' },
  FROM_125_TO_249: { short: '125–249', words: '125 a 249 gal' },
  FROM_250_TO_499: { short: '250–499', words: '250 a 499 gal' },
  FROM_500: { short: '500 o más', words: '500 gal o más' }
};

/** Entry dates that are written one by one before they become "del … al …". */
const MAX_LISTED_DATES = 4;

/* ---------- Status card ---------- */

function advice(level: ShownAlertLevel, short: boolean): string {
  if (level !== 'AYUDA' && level !== 'ALARMA') return '';
  const compare = 'compara la medición electrónica con la de varilla.';
  if (!short) {
    return `pide revisar la calibración de los surtidores y que las recepciones y las ventas estén bien registradas; ${compare}`;
  }
  return level === 'ALARMA'
    ? `pide revisar la calibración de los surtidores y posibles fugas en el tanque; ${compare}`
    : `pide que revisen surtidores y tanques; ${compare}`;
}

function statusView(
  fuel: FuelMonthlyVariationDto,
  days: DailyVariationDto[],
  report: MonthlyVariationDto,
  streak: Streak
): StatusView {
  const name = variationFuelName(fuel.fuelType);
  const month = monthWord(report.month);
  const level = fuelLevel(fuel);
  const limit = limitPercent(report.alertSetting.cumulativeLimitPercentage);
  const { summary, totals } = fuel;
  const asOf = summary.lastCalculatedDate ? `al ${dayAndMonth(summary.lastCalculatedDate)}` : '';
  const calculated = plural(summary.calculatedDays, 'día calculado', 'días calculados');

  if (level === 'SIN_DATOS' || totals.varianceGallons === null) {
    return {
      level: 'SIN_DATOS',
      asOf,
      title: `Todavía no hay días calculados de ${name} en ${month}.`,
      text: 'Para calcular un día se necesitan su medición, la del día anterior y las ventas.',
      advice: '',
      streak: ''
    };
  }

  if (level === 'OK') {
    const alwaysWithin = days.every(day => day.cumulativeStatus === null || day.cumulativeStatus === 'OK');
    const nearZero = Math.abs(totals.variancePercentage ?? 0) <= report.alertSetting.cumulativeLimitPercentage / 5;
    const within = alwaysWithin
      ? `El acumulado nunca salió del ±${limit} en ${calculated}`
      : `El acumulado está dentro del ±${limit} permitido, aunque antes estuvo fuera`;
    const tolerance = summary.outOfToleranceDays === 0
      ? ' y ningún día quedó fuera de tolerancia.'
      : `. ${plural(summary.outOfToleranceDays, 'día quedó', 'días quedaron')} fuera de tolerancia.`;
    return {
      level,
      asOf,
      title: `${name} va bien: la diferencia del mes ${nearZero ? 'es casi cero' : 'está dentro de lo permitido'}.`,
      text: `${within}${tolerance}`,
      advice: '',
      streak: 'Sin días fuera'
    };
  }

  const short = totals.varianceGallons < 0;
  const kind = short ? 'faltante' : 'sobrante';
  const inARow = plural(streak.days, 'día calculado', 'días calculados seguidos');
  const over = `acumulado está por encima del ${limit} permitido: ${inARow}.`;
  const since = streak.since ? `Desde el ${formatLongDate(streak.since)} el ${kind} ${over}` : `El ${kind} ${over}`;
  return {
    level,
    asOf,
    title: `${short ? 'Faltan' : 'Sobran'} ${formatAmount(totals.varianceGallons)} galones de ${name} en ${month}.`,
    text: `${since} Un día sin datos no corta la racha.`,
    advice: advice(level, short),
    streak: `${inARow} fuera`
  };
}

/* ---------- Indicators ---------- */

/** "9 y 23 abr" */
function listDates(dates: string[]): string {
  const last = dayAndMonth(dates[dates.length - 1]);
  const earlier = dates.slice(0, -1).map(date => String(Number(date.slice(8, 10))));
  return earlier.length ? `${earlier.join(', ')} y ${last}` : last;
}

function receptionsNote(days: DailyVariationDto[]): string {
  const dates = days.filter(day => (day.entriesGallons ?? 0) > 0).map(day => day.date);
  if (dates.length === 0) return 'Sin recepciones registradas';
  const when = dates.length > MAX_LISTED_DATES
    ? `del ${Number(dates[0].slice(8, 10))} al ${dayAndMonth(dates[dates.length - 1])}`
    : listDates(dates);
  return `${plural(dates.length, 'recepción', 'recepciones')} (${when})`;
}

function withUnit(value: number | null): string {
  return value === null ? UNKNOWN : `${gallons(value)} gal`;
}

function targetMetric(fuel: FuelMonthlyVariationDto, note: string): Metric {
  const label = 'Meta del mes';
  if (fuel.targetGallons === null) {
    return { label, value: '', unset: 'Meta sin definir', note, tone: 'even' };
  }
  const remaining = fuel.remainingGallons;
  let progress = 'Sin ventas para compararla';
  if (remaining !== null) {
    progress = remaining > 0
      ? `Faltan ${formatAmount(remaining)} gal por vender`
      : `Meta cumplida: ${formatAmount(remaining)} gal por encima`;
  }
  return { label, value: withUnit(fuel.targetGallons), note: progress, tone: 'even' };
}

function metrics(
  fuel: FuelMonthlyVariationDto,
  days: DailyVariationDto[]
): { full: Metric[]; compact: Metric[] } {
  const { totals, summary } = fuel;
  const base = calculatedSold(fuel);
  const calculated = plural(summary.calculatedDays, 'día calculado', 'días calculados');
  const over = (daysText: string) => base === null || totals.variancePercentage === null
    ? 'Sin días calculados'
    : `${signedPercent(totals.variancePercentage)} sobre ${gallons(base)} gal de ${daysText}`;
  const variance: Metric = {
    label: 'Variación del mes',
    value: totals.varianceGallons === null ? UNKNOWN : `${signed(totals.varianceGallons)} gal`,
    note: over(calculated),
    tone: varianceTone(totals.varianceGallons)
  };
  const average = totals.averageDailySoldGallons;
  const sold: Metric = {
    label: 'Vendido',
    value: withUnit(totals.soldGallons),
    note: average === null ? 'Sin días con ventas' : `Promedio ${gallons(average)} gal por día con ventas`,
    tone: 'even'
  };
  const entries: Metric = {
    label: 'Entradas', value: withUnit(totals.entriesGallons), note: receptionsNote(days), tone: 'even'
  };
  const measuredOn = totals.lastMeasuredDate ? dayAndMonth(totals.lastMeasuredDate) : '';
  const inventory: Metric = {
    label: 'Último inventario medido',
    value: withUnit(totals.lastMeasuredGallons),
    note: measuredOn ? `Medido el ${measuredOn}` : 'Sin mediciones',
    tone: 'even'
  };
  const lastDay = summary.lastCalculatedDate ? ` · último calculado ${dayAndMonth(summary.lastCalculatedDate)}` : '';
  const calculatedDays: Metric = {
    label: 'Días calculados',
    value: `${summary.calculatedDays} de ${days.length}`,
    note: `${summary.outOfToleranceDays} fuera de tolerancia${lastDay}`,
    tone: 'even'
  };

  const outOfTolerance = `${summary.outOfToleranceDays} de ${plural(summary.calculatedDays, 'día', 'días')} `
    + 'fuera de tolerancia';
  return {
    full: [
      variance, sold, entries, inventory, calculatedDays,
      targetMetric(fuel, 'Cuando se carguen las metas por combustible verás cuánto falta por vender.')
    ],
    compact: [
      { ...variance, note: over(plural(summary.calculatedDays, 'día', 'días')) },
      { ...sold, note: average === null ? 'Sin días con ventas' : `${gallons(average)} gal/día` },
      {
        ...inventory,
        label: 'Último inventario',
        note: [measuredOn, totals.entriesGallons === null ? '' : `entradas ${gallons(totals.entriesGallons)}`]
          .filter(Boolean).join(' · ')
      },
      targetMetric(fuel, outOfTolerance)
    ]
  };
}

/* ---------- Charts and ranges ---------- */

function chartView(
  fuel: FuelMonthlyVariationDto,
  days: DailyVariationDto[],
  report: MonthlyVariationDto,
  streak: Streak
): ChartView {
  const name = variationFuelName(fuel.fuelType);
  const line = accumulatedLine(days, [
    days.find(day => day.status === 'OK')?.date ?? null, fuel.summary.lastCalculatedDate, streak.since
  ]);
  const total = fuel.days.length;
  // First day, one per week and the last one.
  const axis = [fuel.days[0], fuel.days[7], fuel.days[14], fuel.days[21], fuel.days[total - 1]]
    .map((day, index) => index === 0 || index === 4 ? dayAndMonth(day.date) : String(Number(day.date.slice(8, 10))));
  const short = days.filter(day => day.status === 'OK' && (day.varianceGallons ?? 0) < 0).length;
  const over = days.filter(day => day.status === 'OK' && (day.varianceGallons ?? 0) > 0).length;
  const missing = days.filter(day => day.status !== 'OK').length;
  return {
    bars: days.map((day, index) => {
      const value = day.status === 'OK' ? day.varianceGallons : null;
      return {
        index,
        value,
        title: `${dayAndMonth(day.date)}: ${value === null ? 'sin datos' : `${signed(value)} gal`}`
      };
    }),
    line,
    slots: total,
    limit: report.alertSetting.cumulativeLimitPercentage,
    level: fuelLevel(fuel),
    axis,
    barsLabel: `Diferencia diaria de ${name} en galones: ${plural(short, 'día', 'días')} con faltante, `
      + `${over} con sobrante y ${missing} sin datos`,
    lineLabel: `Porcentaje acumulado de ${name}: ${signedPercent(fuel.totals.variancePercentage)} al cierre de lo `
      + `calculado, con ±${limitPercent(report.alertSetting.cumulativeLimitPercentage)} permitido`
  };
}

function rangesView(fuel: FuelMonthlyVariationDto): RangesView {
  const tones = { OK: 'ok', REVISAR: 'warning', AYUDA: 'warning', ALARMA: 'alarm' } as const;
  const withSurplus = fuel.surplusRanges.filter(range => range.count > 0);
  const total = withSurplus.reduce((sum, range) => sum + range.count, 0);
  const parts = withSurplus.length === 1
    ? `de ${RANGE_NAMES[withSurplus[0].range].words}`
    : withSurplus.map(range => `${RANGE_NAMES[range.range].words}: ${range.count}`).join(' · ');
  return {
    rows: fuel.shortageRanges.map(({ range, count, limit, status }) => ({
      range,
      name: RANGE_NAMES[range].short,
      count,
      limit,
      text: `${count} de ${plural(limit, 'día', 'días')}`,
      shortText: `${count} de ${limit}`,
      level: status ?? 'SIN_DATOS',
      tone: status ? tones[status] : 'ok',
      scaleMax: Math.max(count, limit) * 1.15
    })),
    surplus: total === 0
      ? 'Ningún día tuvo sobrante de 1 galón o más.'
      : `Días con sobrante: ${total} (${parts}). Se muestran solo como información; no tienen límite.`,
    shortSurplus: total === 0
      ? 'Sin días con sobrante de 1 galón o más.'
      : `Días con sobrante: ${total} (solo informativo)`
  };
}

export function fuelDetailView(
  fuel: FuelMonthlyVariationDto,
  report: MonthlyVariationDto,
  today: string
): FuelDetailView {
  const days = elapsedDays(fuel, today);
  const streak = currentStreak(days);
  const { full, compact } = metrics(fuel, days);
  return {
    name: variationFuelName(fuel.fuelType),
    status: statusView(fuel, days, report, streak),
    metrics: full,
    compactMetrics: compact,
    chart: chartView(fuel, days, report, streak),
    ranges: rangesView(fuel),
    rows: dayRows(days, report.alertSetting.cumulativeLimitPercentage),
    totals: totalsRow(fuel, report.month)
  };
}
