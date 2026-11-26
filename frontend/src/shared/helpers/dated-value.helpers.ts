import { TagVariant } from '../../atomic-design/atoms/tag/tag.component';

/** A value that rules from `validFrom` ("YYYY-MM-DD") until a later one of the same series starts. */
export interface DatedValue {
  validFrom: string;
}

export type DatedValueStatus = 'current' | 'previous' | 'scheduled';

/** How each state of a value is labelled; the text says it, the color only helps. */
export const DATED_VALUE_STATUS: Record<DatedValueStatus, { label: string; tone: TagVariant }> = {
  current: { label: 'Vigente', tone: 'green' },
  previous: { label: 'Anterior', tone: 'gray' },
  scheduled: { label: 'Programado', tone: 'blue' }
};

export function newestFirst<T extends DatedValue>(values: readonly T[]): T[] {
  return [...values].sort((a, b) => b.validFrom.localeCompare(a.validFrom));
}

/** The value that rules on a day: the latest one that had started by then. */
export function valueOn<T extends DatedValue>(values: readonly T[], day: string): T | null {
  return newestFirst(values).find(value => value.validFrom <= day) ?? null;
}

/** State of a value of one series, where `current` is the one that rules today. */
export function valueStatus(value: DatedValue, current: DatedValue | null, today: string): DatedValueStatus {
  if (value.validFrom > today) return 'scheduled';
  return value.validFrom === current?.validFrom ? 'current' : 'previous';
}
