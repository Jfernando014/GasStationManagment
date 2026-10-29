import { ProductCategory } from '../models/dtos/product.dto';
import { FuelType } from '../models/dtos/tank.dto';

/** Names shown in the screens; the enum values stay in English. */
export const FUEL_LABELS: Record<FuelType, string> = {
  MOTOR: 'Gasolina Corriente',
  EXTRA: 'Gasolina Extra',
  DIESEL: 'Diésel ACPM',
  MAX_PRO: 'Diésel Max Pro'
};

export const FUEL_COLORS: Record<FuelType, string> = {
  MOTOR: 'var(--fuel-corriente)',
  EXTRA: 'var(--fuel-extra)',
  DIESEL: 'var(--fuel-diesel)',
  MAX_PRO: 'var(--fuel-maxpro)'
};

export const PRODUCT_CATEGORY_LABELS: Record<ProductCategory, string> = {
  LUBRICANTE: 'Lubricante',
  ADITIVO: 'Aditivo',
  REFRIGERANTE: 'Refrigerante',
  OTRO: 'Otro'
};

/** Plural names for the category filters of the catalog. */
export const PRODUCT_CATEGORY_FILTER_LABELS: Record<ProductCategory, string> = {
  LUBRICANTE: 'Lubricantes',
  ADITIVO: 'Aditivos',
  REFRIGERANTE: 'Refrigerantes',
  OTRO: 'Varios'
};

const COP = new Intl.NumberFormat('es-CO', { minimumFractionDigits: 0, maximumFractionDigits: 0 });
const ONE_DECIMAL = new Intl.NumberFormat('es-CO', { minimumFractionDigits: 1, maximumFractionDigits: 1 });
const TWO_DECIMALS = new Intl.NumberFormat('es-CO', { minimumFractionDigits: 2, maximumFractionDigits: 2 });

/** Colombian pesos without cents, as the printed forms show them. */
export function formatCop(value: number | null | undefined): string {
  return value === null || value === undefined ? '—' : `$ ${COP.format(value)}`;
}

export function formatGallons(value: number | null | undefined, decimals: 1 | 2 = 2): string {
  if (value === null || value === undefined) return '—';
  return (decimals === 1 ? ONE_DECIMAL : TWO_DECIMALS).format(value);
}

export function formatPercent(value: number | null | undefined): string {
  return value === null || value === undefined ? '—' : `${ONE_DECIMAL.format(value)} %`;
}

export function formatDays(value: number | null | undefined): string {
  return value === null || value === undefined ? '—' : `${ONE_DECIMAL.format(value)} días`;
}
