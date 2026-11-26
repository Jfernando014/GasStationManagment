const MAX_TYPED_DECIMALS = 4;
const NO_BREAK_SPACE = String.fromCharCode(160);

/** "27,84 %": comma for decimals, without trailing zeros. The space does not break, so the sign never wraps alone. */
export function formatPercentage(value: number, maxDecimals = 2): string {
  return `${formatPercentageInput(value, maxDecimals)}${NO_BREAK_SPACE}%`;
}

/** The same figure as it is typed in a field, without the sign. */
export function formatPercentageInput(value: number, maxDecimals = MAX_TYPED_DECIMALS): string {
  return new Intl.NumberFormat('es-CO', { maximumFractionDigits: maxDecimals }).format(value);
}

/** Reads a percentage from 0 to 100 with up to 4 decimals: "2,65", "2.65" or "25 %". */
export function parsePercentage(
  text: string,
  allowZero = true
): { value: number; error: null } | { value: null; error: string } {
  const fail = (error: string) => ({ value: null, error });
  const clean = text.replace(/[\s%]/g, '');
  if (clean === '') return fail('Escribe el porcentaje.');
  if (!/^\d+([.,]\d*)?$/.test(clean)) return fail('Escribe un número entre 0 y 100, por ejemplo 27,5.');

  const [, decimals = ''] = clean.split(/[.,]/);
  if (decimals.length > MAX_TYPED_DECIMALS) return fail('Usa máximo 4 decimales.');
  const value = Number(clean.replace(',', '.'));
  if (value > 100) return fail('El porcentaje no puede ser mayor que 100.');
  return value > 0 || allowZero ? { value, error: null } : fail('El porcentaje debe ser mayor que cero.');
}
