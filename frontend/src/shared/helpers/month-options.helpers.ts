import { addMonths, capitalize, monthLabel } from './shift-display.helpers';

/** "agosto" from "2026-08". */
export function monthName(month: string): string {
  return monthLabel(month).split(' ')[0];
}

/** "Agosto 2026", with "· hasta hoy" for the month in progress. */
export function monthOptionLabel(month: string, current: string): string {
  const [name, , year] = monthLabel(month).split(' ');
  return `${capitalize(name)} ${year}${month === current ? ' · hasta hoy' : ''}`;
}

/** Every month from `start` to `current` ("yyyy-MM"), the newest first, as options of a select. */
export function monthOptions(start: string, current: string): { value: string; label: string }[] {
  const options: { value: string; label: string }[] = [];
  for (let month = current; month >= start; month = addMonths(month, -1)) {
    options.push({ value: month, label: monthOptionLabel(month, current) });
  }
  return options;
}
