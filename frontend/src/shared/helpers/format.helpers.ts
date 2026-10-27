const SHORT_MONTHS = ['ene', 'feb', 'mar', 'abr', 'may', 'jun', 'jul', 'ago', 'sep', 'oct', 'nov', 'dic'];

const LONG_MONTHS = [
  'enero', 'febrero', 'marzo', 'abril', 'mayo', 'junio',
  'julio', 'agosto', 'septiembre', 'octubre', 'noviembre', 'diciembre'
];

const CALENDAR_DAY = /^(\d{4})-(\d{2})-(\d{2})$/;

/** Colombian pesos without decimals and with a dot as thousands separator: 1200 -> "$1.200". */
export function formatCop(amount: number): string {
  const rounded = Math.round(amount);
  const digits = Math.abs(rounded).toString().replace(/\B(?=(\d{3})+(?!\d))/g, '.');
  return `${rounded < 0 ? '-' : ''}$${digits}`;
}

/**
 * Calendar day "YYYY-MM-DD" as "1 sep 2026". It works on the text, never on a Date,
 * so the day cannot shift with the time zone of the browser.
 */
export function formatShortDate(day: string, withYear = true): string {
  const parts = CALENDAR_DAY.exec(day);
  if (!parts) return day;
  const text = `${Number(parts[3])} ${SHORT_MONTHS[Number(parts[2]) - 1] ?? parts[2]}`;
  return withYear ? `${text} ${parts[1]}` : text;
}

/** Calendar day "YYYY-MM-DD" as "1 de septiembre", without the year. */
export function formatLongDate(day: string): string {
  const parts = CALENDAR_DAY.exec(day);
  if (!parts) return day;
  return `${Number(parts[3])} de ${LONG_MONTHS[Number(parts[2]) - 1] ?? parts[2]}`;
}

/** Calendar day "YYYY-MM-DD" as "dd/mm/aaaa", the way date fields show it. */
export function formatInputDate(day: string): string {
  const parts = CALENDAR_DAY.exec(day);
  return parts ? `${parts[3]}/${parts[2]}/${parts[1]}` : day;
}
