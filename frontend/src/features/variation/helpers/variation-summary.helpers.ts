import { TrendPoint } from '../../../atomic-design/atoms/trend-band-chart/trend-band-chart.component';
import { TagVariant } from '../../../atomic-design/atoms/tag/tag.component';
import { FUELS } from '../../../shared/helpers/fuel-price.helpers';
import { formatInputDate, formatLongDate } from '../../../shared/helpers/format.helpers';
import { AlertLevel, ShownAlertLevel } from '../../../shared/models/dtos/alert-level.dto';
import { FuelType } from '../../../shared/models/dtos/tank.dto';
import { VariationAlertSettingResponse } from '../../../shared/models/dtos/variation-alert-setting.dto';
import { DailyVariationDto, FuelMonthlyVariationDto, MonthlyVariationDto } from '../models/monthly-variation.dto';
import { elapsedDays } from './variation-days.helpers';
import {
  UNKNOWN, VarianceTone, dayAndMonth, formatAmount, gallons, limitPercent, monthAndYear, monthTitleWord, monthWord,
  plural, signed, signedPercent, variationFuelName, varianceTone
} from './variation-format.helpers';

/** How each fuel is named in the address. */
export const FUEL_SLUGS: Record<FuelType, string> = {
  MOTOR: 'corriente',
  DIESEL: 'diesel',
  EXTRA: 'extra',
  MAX_PRO: 'maxpro'
};

const TAB_TONES: Partial<Record<ShownAlertLevel, TagVariant>> = { REVISAR: 'amber', AYUDA: 'orange', ALARMA: 'red' };

const SEVERITY: Record<ShownAlertLevel, number> = { SIN_DATOS: 0, OK: 0, REVISAR: 1, AYUDA: 2, ALARMA: 3 };

export function fuelLevel(fuel: FuelMonthlyVariationDto): ShownAlertLevel {
  return fuel.summary.cumulativeStatus ?? 'SIN_DATOS';
}

/** Dot of the tab of a fuel: only when its month needs attention. */
export function tabTone(level: ShownAlertLevel): TagVariant | undefined {
  return TAB_TONES[level];
}

/** Whether not a single day of the month has any figure. */
export function isEmptyMonth(report: MonthlyVariationDto, today: string): boolean {
  return report.fuels.every(fuel => elapsedDays(fuel, today).every(day =>
    day.initialGallons === null && day.soldGallons === null && day.measuredGallons === null && !day.entriesGallons));
}

export interface Streak {
  /** Calculated days in a row, up to the last one, with the month beyond the limit. */
  days: number;
  /** First of them; null when the data does not tell since when the month is on the same side. */
  since: string | null;
}

/** A day without data neither breaks nor advances the streak, as the server counts it. */
export function currentStreak(days: DailyVariationDto[]): Streak {
  const judged = days.filter(day => day.status === 'OK' && day.cumulativeStatus !== null);
  const lastWithin = judged.map(day => day.cumulativeStatus).lastIndexOf('OK');
  const run = judged.slice(lastWithin + 1);
  if (run.length === 0) return { days: 0, since: null };
  // If the month went from missing to exceeding without coming back inside, "since" would be misleading.
  const sides = new Set(run.map(day => varianceTone(day.cumulativeVarianceGallons)));
  return { days: run.length, since: sides.size === 1 ? run[0].date : null };
}

/* ---------- Summary tab ---------- */

export interface LevelCount {
  level: ShownAlertLevel;
  text: string;
}

export interface Headline {
  before: string;
  /** Fuel the sentence is about, written in bold; empty when it is about all of them. */
  fuel: string;
  after: string;
  slug: string | null;
}

export interface FuelCard {
  type: FuelType;
  slug: string;
  name: string;
  color: string;
  level: ShownAlertLevel;
  /** Accumulated percentage of the month; null without calculated days. */
  percentage: number | null;
  tone: VarianceTone;
  detail: string;
  line: TrendPoint[];
  slots: number;
  limit: number;
  lineLabel: string;
  inventory: string;
  inventoryDate: string;
}

export interface LevelMeaning {
  level: AlertLevel;
  text: string;
}

export interface SummaryView {
  counts: LevelCount[];
  headline: Headline;
  cards: FuelCard[];
  legendIntro: string;
  legend: LevelMeaning[];
}

const COUNT_TEXTS: [ShownAlertLevel, (count: number) => string][] = [
  ['ALARMA', count => `${count} en alarma`],
  ['AYUDA', count => `${count} para pedir ayuda`],
  ['REVISAR', count => `${count} para revisar`],
  ['OK', count => `${count} bien`],
  ['SIN_DATOS', count => `${count} sin datos`]
];

/** The accumulated percentage after each calculated day. */
export function accumulatedLine(days: DailyVariationDto[], marked: (string | null)[] = []): TrendPoint[] {
  return days.flatMap((day, index) => day.status === 'OK' && day.cumulativePercentage !== null
    ? [{
        index,
        value: day.cumulativePercentage,
        title: `${dayAndMonth(day.date)}: ${signedPercent(day.cumulativePercentage)} acumulado`,
        marked: marked.includes(day.date)
      }]
    : []);
}

/** "faltan 285,00 gal" or "sobran 3,85 gal" */
function differenceWords(variance: number): string {
  return `${variance < 0 ? 'faltan' : 'sobran'} ${formatAmount(variance)} gal`;
}

function headline(report: MonthlyVariationDto, today: string): Headline {
  const month = report.month;
  const [worst] = report.fuels
    .filter(fuel => SEVERITY[fuelLevel(fuel)] > 0)
    .sort((a, b) => SEVERITY[fuelLevel(b)] - SEVERITY[fuelLevel(a)]
      || Math.abs(b.totals.variancePercentage ?? 0) - Math.abs(a.totals.variancePercentage ?? 0));

  if (worst) {
    const { since } = currentStreak(elapsedDays(worst, today));
    const from = since ? ` desde el ${formatLongDate(since)}` : '';
    const variance = worst.totals.varianceGallons;
    const difference = variance === null ? 'la diferencia está fuera de lo permitido' : differenceWords(variance);
    return {
      before: fuelLevel(worst) === 'REVISAR'
        ? `En ${monthWord(month)} hay que revisar `
        : `${monthTitleWord(month)} va mal en `,
      fuel: variationFuelName(worst.fuelType),
      after: `: ${difference}${from}.`,
      slug: FUEL_SLUGS[worst.fuelType]
    };
  }

  const judged = report.fuels.filter(fuel => fuelLevel(fuel) === 'OK').length;
  let sentence = `${monthTitleWord(month)} va bien: la diferencia de cada combustible está dentro de lo permitido.`;
  if (judged === 0) {
    sentence = `Todavía no hay días calculados en ${monthWord(month)}: completa las mediciones y las ventas.`;
  } else if (judged < report.fuels.length) {
    sentence = `${monthTitleWord(month)} va bien en los combustibles con días calculados.`;
  }
  return { before: sentence, fuel: '', after: '', slug: null };
}

function fuelCard(fuel: FuelMonthlyVariationDto, report: MonthlyVariationDto, today: string): FuelCard {
  const { totals, summary } = fuel;
  const level = fuelLevel(fuel);
  const variance = totals.varianceGallons;
  const calculated = plural(summary.calculatedDays, 'día calculado', 'días calculados');
  // Inside the limit the difference is only stated; beyond it, with the word that says which way.
  const word = level === 'OK' || level === 'SIN_DATOS' || !variance ? '' : variance < 0 ? ' faltan' : ' sobran';
  const name = variationFuelName(fuel.fuelType);
  return {
    type: fuel.fuelType,
    slug: FUEL_SLUGS[fuel.fuelType],
    name,
    color: FUELS.find(item => item.type === fuel.fuelType)?.color ?? 'var(--text-light)',
    level,
    percentage: totals.variancePercentage,
    tone: level === 'OK' ? 'even' : varianceTone(totals.variancePercentage),
    detail: variance === null ? 'Sin días calculados' : `${signed(variance)} gal${word} · ${calculated}`,
    line: accumulatedLine(elapsedDays(fuel, today)),
    slots: fuel.days.length,
    limit: report.alertSetting.cumulativeLimitPercentage,
    lineLabel: totals.variancePercentage === null
      ? `${name}: sin porcentaje acumulado en el mes`
      : `Porcentaje acumulado de ${name} en el mes, hoy ${signedPercent(totals.variancePercentage)}`,
    inventory: totals.lastMeasuredGallons === null ? UNKNOWN : `${gallons(totals.lastMeasuredGallons)} gal`,
    inventoryDate: totals.lastMeasuredDate ? dayAndMonth(totals.lastMeasuredDate) : ''
  };
}

/** What each level means, with the days and the limit of the rules of the month. */
export function levelMeanings(setting: VariationAlertSettingResponse): LevelMeaning[] {
  const days = setting.escalationDays;
  const recent = days === 1
    ? 'Se salió del límite hace poco (1 día). Revisa las mediciones de ese día.'
    : `Se salió del límite hace poco (1 a ${days} días). Revisa las mediciones de esos días.`;
  return [
    { level: 'OK', text: 'La diferencia del mes está dentro de lo permitido. No hay que hacer nada.' },
    { level: 'REVISAR', text: recent },
    {
      level: 'AYUDA',
      text: `Lleva más de ${plural(days, 'día', 'días')} fuera. Pide que revisen surtidores y tanques.`
    },
    {
      level: 'ALARMA',
      text: `Lleva más de ${days * 2} días fuera. Puede haber una fuga o un error de calibración: actúa hoy.`
    }
  ];
}

export function summaryView(report: MonthlyVariationDto, today: string): SummaryView {
  const levels = report.fuels.map(fuelLevel);
  return {
    counts: COUNT_TEXTS.flatMap(([level, text]) => {
      const count = levels.filter(item => item === level).length;
      return count ? [{ level, text: text(count) }] : [];
    }),
    headline: headline(report, today),
    cards: report.fuels.map(fuel => fuelCard(fuel, report, today)),
    legendIntro: 'Se mide sobre el % acumulado del mes; la franja verde de las gráficas es lo permitido '
      + `(±${limitPercent(report.alertSetting.cumulativeLimitPercentage)}).`,
    legend: levelMeanings(report.alertSetting)
  };
}

/* ---------- Rules chip ---------- */

export interface RulesView {
  button: string;
  title: string;
  rows: { name: string; value: string }[];
  note: string;
}

export function rulesView(report: MonthlyVariationDto): RulesView {
  const setting = report.alertSetting;
  const since = formatInputDate(setting.validFrom);
  return {
    button: `Reglas vigentes desde ${since}`,
    title: `Reglas usadas en ${monthAndYear(report.month).toLowerCase()}`,
    rows: [
      { name: 'Límite del % acumulado', value: limitPercent(setting.cumulativeLimitPercentage) },
      { name: 'Días seguidos para escalar', value: String(setting.escalationDays) },
      {
        name: 'Días permitidos por rango',
        value: [
          setting.shortageLimit1To49, setting.shortageLimit50To74, setting.shortageLimit75To124,
          setting.shortageLimit125To249, setting.shortageLimit250To499, setting.shortageLimit500OrMore
        ].join(' · ')
      }
    ],
    note: `Rigen desde el ${since}.`
  };
}
