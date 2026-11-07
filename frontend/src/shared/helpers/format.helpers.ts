const SHORT_MONTHS = ['ene', 'feb', 'mar', 'abr', 'may', 'jun', 'jul', 'ago', 'sep', 'oct', 'nov', 'dic'];

const LONG_MONTHS = [
  'enero', 'febrero', 'marzo', 'abril', 'mayo', 'junio',
  'julio', 'agosto', 'septiembre', 'octubre', 'noviembre', 'diciembre'
];

const CALENDAR_DAY = /^(\d{4})-(\d{2})-(\d{2})$/;
const DATE_TIME = /^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})/;

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

/**
 * Local date-time "2026-11-06T18:06:27" as "6 nov 2026, 18:06".
 * It works on the text, so the hour cannot shift with the time zone of the browser.
 */
export function formatDateTime(value: string): string {
  const parts = DATE_TIME.exec(value);
  if (!parts) return value;
  return `${Number(parts[3])} ${SHORT_MONTHS[Number(parts[2]) - 1] ?? parts[2]} ${parts[1]}, ${parts[4]}:${parts[5]}`;
}

/** Today's calendar day in the local time zone, as "YYYY-MM-DD". */
export function todayCalendarDay(): string {
  const now = new Date();
  const month = `${now.getMonth() + 1}`.padStart(2, '0');
  const day = `${now.getDate()}`.padStart(2, '0');
  return `${now.getFullYear()}-${month}-${day}`;
}
