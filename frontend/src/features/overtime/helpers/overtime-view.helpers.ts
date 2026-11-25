import { DotValueItem } from '../../../atomic-design/molecules/dot-value-list/dot-value-list.component';
import { addDays, capitalize } from '../../../shared/helpers/shift-display.helpers';
import {
  AffectedDate, CategoryHours, IncompleteReason, OVERTIME_CATEGORIES, OvertimeCategory
} from '../models/overtime.model';
import { CATEGORY_HELP, EXTRA_CATEGORIES, formatHours, SURCHARGE_CATEGORIES } from './overtime-display.helpers';

/** "8 h", "−2 h". */
export function hoursShort(hours: number): string {
  return `${formatHours(hours)} h`;
}

/** "Titular" from TITULAR; empty when the role is not known. */
export function roleLabel(role: string | null): string {
  return role ? capitalize(role.toLowerCase()) : '';
}

export function sumHours(hours: CategoryHours, categories: readonly OvertimeCategory[]): number {
  return categories.reduce((total, category) => total + hours[category], 0);
}

/** Category by category sum of several lines. */
export function addCategoryHours(list: CategoryHours[]): CategoryHours {
  return Object.fromEntries(OVERTIME_CATEGORIES.map(category =>
    [category, list.reduce((total, hours) => total + hours[category], 0)])) as CategoryHours;
}

function concept(category: OvertimeCategory, tone: DotValueItem['tone'], hours: number): DotValueItem {
  return {
    key: category,
    tone,
    value: hoursShort(hours),
    label: CATEGORY_HELP[category].short,
    tooltip: `${category} · ${CATEGORY_HELP[category].name.toLowerCase()}`,
    negative: hours < 0
  };
}

/** Surcharges that are not zero. */
export function surchargeItems(hours: CategoryHours): DotValueItem[] {
  return SURCHARGE_CATEGORIES.filter(category => hours[category] !== 0)
    .map(category => concept(category, 'blue', hours[category]));
}

/**
 * Overtime that is not zero. `fixedHours`, already part of the HEDO of `hours`, is written apart as "(fijas)"; what
 * is left of HEDO (days and adjustments, which may be below zero) follows it.
 */
export function extraItems(hours: CategoryHours, fixedHours = 0): DotValueItem[] {
  const items: DotValueItem[] = [];
  if (fixedHours !== 0) {
    items.push({
      key: 'fixed',
      tone: 'orange',
      value: hoursShort(fixedHours),
      label: `${CATEGORY_HELP.HEDO.short} (fijas)`,
      tooltip: `HEDO · ${CATEGORY_HELP.HEDO.name.toLowerCase()} · fijas de la quincena`,
      negative: false
    });
  }
  for (const category of EXTRA_CATEGORIES) {
    const value = hours[category] - (category === 'HEDO' ? fixedHours : 0);
    if (value !== 0) items.push(concept(category, 'orange', value));
  }
  return items;
}

/**
 * Where the journal of a day stands. `no-hours` is a closed journal whose hours were not calculated; `unknown`, a
 * past day while the history of journals is not available.
 */
export type JournalState = 'closed' | 'no-hours' | 'open' | 'today' | 'future' | 'unknown';

export interface JournalDay {
  date: string;
  state: JournalState;
  /** Why the hours of the day cannot be trusted; null when they can. */
  reason: IncompleteReason | null;
}

/** The day has no calculated hours: it was never kept, or keeping it failed. */
export function lacksHours(reason: IncompleteReason | null): boolean {
  return reason === 'MISSING_RECORD' || reason === 'PENDING';
}

/**
 * The history of journals says which ones are closed (`closed`, null while it is not available); the sheet says
 * whether the hours of the day are there.
 */
export function journalState(
  date: string, reason: IncompleteReason | null, today: string, closed: ReadonlySet<string> | null
): JournalState {
  if (closed?.has(date)) return lacksHours(reason) ? 'no-hours' : 'closed';
  if (date > today) return 'future';
  if (date === today) return 'today';
  return closed ? 'open' : 'unknown';
}

/** Every day of the fortnight with the state of its journal. */
export function journalDays(
  from: string, to: string, affected: AffectedDate[], today: string, closed: ReadonlySet<string> | null
): JournalDay[] {
  const reasons = new Map(affected.map(item => [item.date, item.reason]));
  const days: JournalDay[] = [];
  for (let date = from; date <= to; date = addDays(date, 1)) {
    const reason = reasons.get(date) ?? null;
    days.push({ date, state: journalState(date, reason, today, closed), reason });
  }
  return days;
}
