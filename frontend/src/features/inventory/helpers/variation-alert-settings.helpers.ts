import { AbstractControl, FormControl, FormGroup, ValidationErrors } from '@angular/forms';

import { valueOn } from '../../../shared/helpers/dated-value.helpers';
import { CALENDAR_DAY } from '../../../shared/helpers/form-control.helpers';
import { formatInputDate, formatShortDate } from '../../../shared/helpers/format.helpers';
import { addMonths, capitalize, monthLabel, monthOf } from '../../../shared/helpers/shift-display.helpers';
import {
  VariationAlertSettingRequest,
  VariationAlertSettingResponse
} from '../../../shared/models/dtos/variation-alert-setting.dto';

export type ShortageField =
  | 'shortageLimit1To49'
  | 'shortageLimit50To74'
  | 'shortageLimit75To124'
  | 'shortageLimit125To249'
  | 'shortageLimit250To499'
  | 'shortageLimit500OrMore';

/** Sizes of a shortage, from the smallest to the largest. A larger one cannot allow more days. */
export const SHORTAGE_RANGES: readonly { field: ShortageField; label: string }[] = [
  { field: 'shortageLimit1To49', label: '1–49 gal' },
  { field: 'shortageLimit50To74', label: '50–74 gal' },
  { field: 'shortageLimit75To124', label: '75–124 gal' },
  { field: 'shortageLimit125To249', label: '125–249 gal' },
  { field: 'shortageLimit250To499', label: '250–499 gal' },
  { field: 'shortageLimit500OrMore', label: '500+ gal' }
];

export const MIN_ESCALATION_DAYS = 1;
export const MAX_ESCALATION_DAYS = 10;

/** Values proposed by the station; "Usar valores recomendados" fills the form with them. */
export const RECOMMENDED_VALUES: Omit<VariationAlertSettingRequest, 'validFrom'> = {
  cumulativeLimitPercentage: 0.5,
  escalationDays: 3,
  shortageLimit1To49: 12,
  shortageLimit50To74: 8,
  shortageLimit75To124: 6,
  shortageLimit125To249: 4,
  shortageLimit250To499: 2,
  shortageLimit500OrMore: 1
};

/* ---------- Form ---------- */

export type VariationSettingForm = ReturnType<typeof createVariationSettingForm>;
export type VariationSettingFormValue = ReturnType<VariationSettingForm['getRawValue']>;

/** The controls are named as the fields of the request, so the errors of the server reach each of them. */
export function createVariationSettingForm() {
  const text = (validator: (control: AbstractControl<string>) => ValidationErrors | null) =>
    new FormControl('', { nonNullable: true, validators: validator });
  return new FormGroup({
    validFrom: text(validFromValidator),
    cumulativeLimitPercentage: text(percentageValidator),
    escalationDays: new FormControl(RECOMMENDED_VALUES.escalationDays, { nonNullable: true }),
    shortageLimit1To49: text(shortageLimitValidator),
    shortageLimit50To74: text(shortageLimitValidator),
    shortageLimit75To124: text(shortageLimitValidator),
    shortageLimit125To249: text(shortageLimitValidator),
    shortageLimit250To499: text(shortageLimitValidator),
    shortageLimit500OrMore: text(shortageLimitValidator)
  });
}

/** The saved values as they are written in the fields, without the date. */
export function toFormValues(
  values: Omit<VariationAlertSettingRequest, 'validFrom'>
): Omit<VariationSettingFormValue, 'validFrom'> {
  return {
    cumulativeLimitPercentage: formatLimitPercentage(values.cumulativeLimitPercentage),
    escalationDays: values.escalationDays,
    shortageLimit1To49: String(values.shortageLimit1To49),
    shortageLimit50To74: String(values.shortageLimit50To74),
    shortageLimit75To124: String(values.shortageLimit75To124),
    shortageLimit125To249: String(values.shortageLimit125To249),
    shortageLimit250To499: String(values.shortageLimit250To499),
    shortageLimit500OrMore: String(values.shortageLimit500OrMore)
  };
}

/** Call it only with a valid form: the whole numbers travel as whole numbers. */
export function toRequest(value: VariationSettingFormValue): VariationAlertSettingRequest {
  return {
    validFrom: value.validFrom,
    cumulativeLimitPercentage: parseLimitPercentage(value.cumulativeLimitPercentage) ?? 0,
    escalationDays: value.escalationDays,
    shortageLimit1To49: Number(value.shortageLimit1To49),
    shortageLimit50To74: Number(value.shortageLimit50To74),
    shortageLimit75To124: Number(value.shortageLimit75To124),
    shortageLimit125To249: Number(value.shortageLimit125To249),
    shortageLimit250To499: Number(value.shortageLimit250To499),
    shortageLimit500OrMore: Number(value.shortageLimit500OrMore)
  };
}

/** Number typed with comma or dot, or null when the text is not a number. */
export function parseLimitPercentage(text: string): number | null {
  const clean = text.replace(/[\s%]/g, '');
  return /^(\d+([.,]\d*)?|[.,]\d+)$/.test(clean) ? Number(clean.replace(',', '.')) : null;
}

function validFromValidator(control: AbstractControl<string>): ValidationErrors | null {
  if (!control.value) return { validFrom: 'La fecha de vigencia es obligatoria' };
  return CALENDAR_DAY.test(control.value) ? null : { validFrom: 'Escribe la fecha completa desde la que rige' };
}

function percentageValidator(control: AbstractControl<string>): ValidationErrors | null {
  const fail = (message: string) => ({ percentage: message });
  if (!control.value.trim()) return fail('El porcentaje límite acumulado es obligatorio');
  const value = parseLimitPercentage(control.value);
  if (value === null) return fail('Escribe un número, por ejemplo 0,5');
  const [, decimals = ''] = control.value.replace(/[\s%]/g, '').split(/[.,]/);
  if (decimals.replace(/0+$/, '').length > 4) return fail('El porcentaje límite acumulado admite máximo 4 decimales');
  return value > 0 && value <= 100
    ? null
    : fail('El porcentaje límite acumulado debe ser mayor que 0 y menor o igual a 100');
}

/** Short messages: each field is narrow. */
function shortageLimitValidator(control: AbstractControl<string>): ValidationErrors | null {
  if (!control.value) return { shortageLimit: 'Obligatorio' };
  const value = Number(control.value);
  return Number.isInteger(value) && value >= 1 ? null : { shortageLimit: 'Mínimo 1' };
}

export interface RangeOrderCheck {
  /** True for each range that allows more days than the one before it. */
  invalid: boolean[];
  /** Explains the first of them. */
  message: string | null;
}

export function checkRangeOrder(limits: readonly string[]): RangeOrderCheck {
  let message: string | null = null;
  const invalid = limits.map((text, index) => {
    const previous = limits[index - 1];
    if (!previous || !text || Number(text) <= Number(previous)) return false;
    message ??= `${SHORTAGE_RANGES[index].label} no puede tener más días que ${SHORTAGE_RANGES[index - 1].label} `
      + `(máximo ${Number(previous)})`;
    return true;
  });
  return { invalid, message };
}

/* ---------- Texts ---------- */

/** "0,50", "0,45" or "0,1234": at least two decimals and at most four. */
export function formatLimitPercentage(value: number): string {
  return new Intl.NumberFormat('es-CO', { minimumFractionDigits: 2, maximumFractionDigits: 4 }).format(value);
}

function limitsText(setting: VariationAlertSettingResponse, separator: string): string {
  return SHORTAGE_RANGES.map(range => setting[range.field]).join(separator);
}

/** "0,45 % · 4 días · 12 / 8 / 6 / 4 / 2 / 1" */
export function settingSummary(setting: VariationAlertSettingResponse, separator = ' / '): string {
  const days = setting.escalationDays === 1 ? '1 día' : `${setting.escalationDays} días`;
  return `${formatLimitPercentage(setting.cumulativeLimitPercentage)} % · ${days} · ${limitsText(setting, separator)}`;
}

/* ---------- Months each setting covers ---------- */

/**
 * Months ("yyyy-MM") a setting rules. A month uses the setting that rules on its last day, or today for the month
 * in progress. `from` is null for the base (every earlier month) and `to` is null for the latest one.
 */
export interface MonthCoverage {
  from: string | null;
  to: string | null;
  /** Another setting of the same month replaces it before it rules a whole month. */
  empty: boolean;
}

/** First month a setting can rule: one that starts later in the month in progress waits for the next one. */
function firstMonthOf(validFrom: string, today: string): string {
  const month = monthOf(validFrom);
  return month === monthOf(today) && validFrom > today ? addMonths(month, 1) : month;
}

/** `ascending` holds every setting, the oldest (the base) first. */
export function monthCoverage(
  ascending: readonly VariationAlertSettingResponse[],
  index: number,
  today: string
): MonthCoverage {
  const next = ascending[index + 1];
  const from = index === 0 ? null : firstMonthOf(ascending[index].validFrom, today);
  const nextFrom = next ? firstMonthOf(next.validFrom, today) : null;
  const to = nextFrom ? addMonths(nextFrom, -1) : null;
  return { from, to, empty: from !== null && to !== null && to < from };
}

/** "may 2026" */
function shortMonth(month: string): string {
  return formatShortDate(`${month}-01`).replace(/^1 /, '');
}

export function coverageLabel({ from, to, empty }: MonthCoverage): string {
  if (empty) return 'sin meses: la reemplaza otra';
  if (from === null) return to === null ? 'todos los meses' : `hasta ${shortMonth(to)}`;
  if (to === null) return `${shortMonth(from)} en adelante`;
  if (from === to) return shortMonth(from);
  const start = from.slice(0, 4) === to.slice(0, 4) ? shortMonth(from).split(' ')[0] : shortMonth(from);
  return `${start} – ${shortMonth(to)}`;
}

/** The setting that rules today. */
export function currentSetting(
  settings: readonly VariationAlertSettingResponse[],
  today: string
): VariationAlertSettingResponse | null {
  return valueOn(settings, today);
}

/** Longest list of months that is written one by one. */
const MAX_LISTED_MONTHS = 6;

/** Which months go back to the previous setting when this one is deleted. It never applies to the base. */
export function deleteMessage(
  ascending: readonly VariationAlertSettingResponse[],
  index: number,
  today: string
): string {
  const previous = ascending[index - 1];
  if (!previous) return '';
  const { from, to, empty } = monthCoverage(ascending, index, today);
  if (empty || from === null) {
    return 'Ningún mes cambia: otra vigencia posterior ya la reemplaza.';
  }
  const name = index === 1 ? 'la configuración base' : `la configuración del ${formatInputDate(previous.validFrom)}`;
  const target = `${name} (${settingSummary(previous, '/')})`;
  if (to === null) {
    return `Desde ${monthLabel(from)} en adelante, los meses pasarán a usar ${target} y sus estados se recalcularán.`;
  }
  if (from === to) {
    return `${capitalize(monthLabel(from))} pasará a usar ${target} y sus estados se recalcularán.`;
  }

  const months: string[] = [];
  for (let month = from; month <= to && months.length <= MAX_LISTED_MONTHS; month = addMonths(month, 1)) {
    months.push(month);
  }
  const listed = months.length <= MAX_LISTED_MONTHS && from.slice(0, 4) === to.slice(0, 4);
  const names = months.map(month => monthLabel(month).split(' ')[0]);
  const subject = listed
    ? `${capitalize(names.slice(0, -1).join(', '))} y ${names.at(-1)} de ${to.slice(0, 4)}`
    : `Los meses de ${monthLabel(from)} a ${monthLabel(to)}`;
  return `${subject} pasarán a usar ${target} y sus estados se recalcularán.`;
}
