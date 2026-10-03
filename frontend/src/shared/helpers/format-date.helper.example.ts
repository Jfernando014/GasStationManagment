/**
 * Example Shared Helper: Formats Date to YYYY-MM-DD string.
 */
export function formatDateExample(date: Date): string {
  const pad = (n: number) => n.toString().padStart(2, '0');
  const year = date.getFullYear();
  const month = pad(date.getMonth() + 1);
  const day = pad(date.getDate());
  return ${year}--;
}
