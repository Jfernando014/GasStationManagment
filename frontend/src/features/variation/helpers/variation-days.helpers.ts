import { addDays } from '../../../shared/helpers/shift-display.helpers';
import { ShownAlertLevel } from '../../../shared/models/dtos/alert-level.dto';
import { DailyVariationDto, FuelMonthlyVariationDto } from '../models/monthly-variation.dto';
import {
  Figure, UNKNOWN, dayAndMonth, gallons, monthTitleWord, percentFigure, signedFigure, weekday
} from './variation-format.helpers';

/** Where a day without data can be completed: a step of the journal of a date. */
export interface JournalLink {
  label: string;
  route: string;
  date: string;
}

export interface DayCells {
  initial: string;
  entries: string;
  hasEntries: boolean;
  sold: string;
  theoretical: string;
  measured: string;
  variance: Figure;
  percentage: Figure;
  tolerance: string;
}

export interface AccumulatedCells {
  sold: string;
  variance: Figure;
  /** Colored only beyond the limit. */
  percentage: Figure;
}

/** A day ready to show, either calculated or with the reason why it could not be. */
export interface DayRow {
  date: string;
  /** "02" */
  day: string;
  weekday: string;
  /** "2 abr" */
  shortDate: string;
  calculated: boolean;
  outOfTolerance: boolean;
  /** Null on a day without data. */
  cells: DayCells | null;
  accumulated: AccumulatedCells | null;
  /** Level of the month up to this day; null before the first calculated one. */
  level: ShownAlertLevel | null;
  /** The level comes from an earlier day, because this one has no data. */
  carried: boolean;
  reason: string;
  /** The figures a day without data does have, e.g. "Inicial 8.626,24 · Vendido 420,55". */
  known: string;
  link: JournalLink | null;
}

export interface TotalsRow {
  month: string;
  calculatedDays: number;
  outOfToleranceDays: number;
  entries: string;
  sold: string;
  variance: Figure;
  percentage: Figure;
  accumulatedSold: string;
  level: ShownAlertLevel;
}

/** The days that have already happened: the rest of a month in progress has nothing to show yet. */
export function elapsedDays(fuel: FuelMonthlyVariationDto, today: string): DailyVariationDto[] {
  return fuel.days.filter(day => day.date <= today);
}

/** Sales of the calculated days, the base of the percentage of the month. */
export function calculatedSold(fuel: FuelMonthlyVariationDto): number | null {
  const last = fuel.days.find(day => day.date === fuel.summary.lastCalculatedDate);
  return last?.cumulativeSoldGallons ?? null;
}

/** Why a day has no figures and which step of which journal completes it. */
function missingData(day: DailyVariationDto): { reason: string; link: JournalLink } {
  const measurement = (date: string, label: string): JournalLink => ({ label, route: '/jornada/inventario', date });
  const previousDay = () => measurement(addDays(day.date, -1), 'Completar Paso 5 del día anterior');

  switch (day.status) {
    case 'NO_PREVIOUS_MEASUREMENT':
      return { reason: 'Falta la medición del día anterior', link: previousDay() };
    case 'NO_SALES_DATA':
      return {
        reason: 'Sin ventas registradas',
        link: { label: 'Completar Paso 2', route: '/jornada/vendedores', date: day.date }
      };
    default:
      // With the measurement of the day complete, the incomplete one is the previous day's.
      return day.measuredGallons === null
        ? { reason: 'Medición incompleta', link: measurement(day.date, 'Completar Paso 5') }
        : { reason: 'Medición anterior incompleta', link: previousDay() };
  }
}

function knownFigures(day: DailyVariationDto): string {
  const parts: [string, number | null][] = [
    ['Inicial', day.initialGallons],
    ['Entradas', day.entriesGallons || null],
    ['Vendido', day.soldGallons],
    ['Medido', day.measuredGallons]
  ];
  return parts.filter(([, value]) => value !== null).map(([name, value]) => `${name} ${gallons(value)}`).join(' · ');
}

export function dayRows(days: DailyVariationDto[], limit: number): DayRow[] {
  let lastLevel: ShownAlertLevel | null = null;
  return days.map(day => {
    const calculated = day.status === 'OK';
    if (calculated && day.cumulativeStatus) lastLevel = day.cumulativeStatus;
    const base = {
      date: day.date,
      day: day.date.slice(8, 10),
      weekday: weekday(day.date),
      shortDate: dayAndMonth(day.date),
      calculated
    };

    if (!calculated) {
      const { reason, link } = missingData(day);
      return {
        ...base, outOfTolerance: false, cells: null, accumulated: null, level: lastLevel, carried: true,
        reason, known: knownFigures(day), link
      };
    }

    const accumulatedPercentage = percentFigure(day.cumulativePercentage);
    const beyondLimit = Math.abs(day.cumulativePercentage ?? 0) > limit;
    return {
      ...base,
      outOfTolerance: day.outOfTolerance === true,
      cells: {
        initial: gallons(day.initialGallons),
        entries: gallons(day.entriesGallons),
        hasEntries: !!day.entriesGallons,
        sold: gallons(day.soldGallons),
        theoretical: gallons(day.theoreticalGallons),
        measured: gallons(day.measuredGallons),
        variance: signedFigure(day.varianceGallons),
        percentage: percentFigure(day.variancePercentage),
        tolerance: day.toleranceGallons === null ? UNKNOWN : `±${gallons(day.toleranceGallons)}`
      },
      accumulated: {
        sold: gallons(day.cumulativeSoldGallons),
        variance: signedFigure(day.cumulativeVarianceGallons),
        percentage: { text: accumulatedPercentage.text, tone: beyondLimit ? accumulatedPercentage.tone : 'even' }
      },
      level: day.cumulativeStatus,
      carried: false,
      reason: '',
      known: '',
      link: null
    };
  });
}

export function totalsRow(fuel: FuelMonthlyVariationDto, month: string): TotalsRow {
  const { totals, summary } = fuel;
  return {
    month: monthTitleWord(month),
    calculatedDays: summary.calculatedDays,
    outOfToleranceDays: summary.outOfToleranceDays,
    entries: gallons(totals.entriesGallons),
    sold: gallons(totals.soldGallons),
    variance: signedFigure(totals.varianceGallons),
    percentage: percentFigure(totals.variancePercentage),
    accumulatedSold: gallons(calculatedSold(fuel)),
    level: summary.cumulativeStatus ?? 'SIN_DATOS'
  };
}
