import {
  ColorPresetDto,
  ColorThemeDto,
  FuelColorKey,
  ResolvedColorEntryDto,
  ShiftColorKey
} from '../models/dtos/color-settings.dto';

const HEX_PATTERN = /^#[0-9A-F]{6}$/;

/** Minimum WCAG 2.1 contrast ratio against pure white (#FFFFFF), matching backend ColorHex. */
export const MIN_CONTRAST_AGAINST_WHITE = 1.8;

export const DEFAULT_FUEL_COLORS: Record<FuelColorKey, string> = {
  MOTOR: '#0284C7',
  EXTRA: '#6366F1',
  DIESEL: '#10B981',
  MAX_PRO: '#F59E0B'
};

export const DEFAULT_SHIFT_COLORS: Record<ShiftColorKey, string> = {
  DIA: '#FF6B00',
  DIA6: '#F59E0B',
  NUEVE_SEIS: '#EA580C',
  NOCHE: '#0B192C',
  APOYO: '#0284C7',
  DESCANSO: '#64748B'
};

export const DEFAULT_CURATED_PALETTE: ColorPresetDto[] = [
  { name: 'Naranja Energía', hex: '#FF6B00' },
  { name: 'Naranja Intenso', hex: '#EA580C' },
  { name: 'Ámbar Profundo', hex: '#D97706' },
  { name: 'Ámbar Vivo', hex: '#F59E0B' },
  { name: 'Azul Petróleo', hex: '#0284C7' },
  { name: 'Azul Rey', hex: '#2563EB' },
  { name: 'Azul Midnight', hex: '#0B192C' },
  { name: 'Verde Azulado', hex: '#0F766E' },
  { name: 'Esmeralda', hex: '#10B981' },
  { name: 'Verde Bosque', hex: '#16A34A' },
  { name: 'Índigo', hex: '#6366F1' },
  { name: 'Violeta', hex: '#7C3AED' },
  { name: 'Púrpura', hex: '#9333EA' },
  { name: 'Magenta', hex: '#DB2777' },
  { name: 'Carmesí', hex: '#DC2626' },
  { name: 'Pizarra', hex: '#64748B' }
];

const DEFAULT_FUEL_ENTRIES: ResolvedColorEntryDto[] = [
  { key: 'MOTOR', label: 'Corriente', colorHex: DEFAULT_FUEL_COLORS.MOTOR, defaultHex: DEFAULT_FUEL_COLORS.MOTOR, customized: false },
  { key: 'EXTRA', label: 'Extra', colorHex: DEFAULT_FUEL_COLORS.EXTRA, defaultHex: DEFAULT_FUEL_COLORS.EXTRA, customized: false },
  { key: 'DIESEL', label: 'Diésel', colorHex: DEFAULT_FUEL_COLORS.DIESEL, defaultHex: DEFAULT_FUEL_COLORS.DIESEL, customized: false },
  { key: 'MAX_PRO', label: 'Maxpro', colorHex: DEFAULT_FUEL_COLORS.MAX_PRO, defaultHex: DEFAULT_FUEL_COLORS.MAX_PRO, customized: false }
];

const DEFAULT_SHIFT_ENTRIES: ResolvedColorEntryDto[] = [
  { key: 'DIA', label: 'Día', colorHex: DEFAULT_SHIFT_COLORS.DIA, defaultHex: DEFAULT_SHIFT_COLORS.DIA, customized: false },
  { key: 'DIA6', label: 'Día 6 h', colorHex: DEFAULT_SHIFT_COLORS.DIA6, defaultHex: DEFAULT_SHIFT_COLORS.DIA6, customized: false },
  { key: 'NUEVE_SEIS', label: '9–6', colorHex: DEFAULT_SHIFT_COLORS.NUEVE_SEIS, defaultHex: DEFAULT_SHIFT_COLORS.NUEVE_SEIS, customized: false },
  { key: 'NOCHE', label: 'Noche', colorHex: DEFAULT_SHIFT_COLORS.NOCHE, defaultHex: DEFAULT_SHIFT_COLORS.NOCHE, customized: false },
  { key: 'APOYO', label: 'Apoyo', colorHex: DEFAULT_SHIFT_COLORS.APOYO, defaultHex: DEFAULT_SHIFT_COLORS.APOYO, customized: false },
  { key: 'DESCANSO', label: 'Descanso', colorHex: DEFAULT_SHIFT_COLORS.DESCANSO, defaultHex: DEFAULT_SHIFT_COLORS.DESCANSO, customized: false }
];

export const DEFAULT_COLOR_THEME: ColorThemeDto = {
  fuels: DEFAULT_FUEL_ENTRIES,
  shifts: DEFAULT_SHIFT_ENTRIES,
  workers: {},
  palette: DEFAULT_CURATED_PALETTE
};

export interface DerivedSurfaceTokens {
  subtle: string;
  strong: string;
  border: string;
  onSolid: '#FFFFFF' | '#0B192C';
}

/** Normalizes a #RRGGBB hex string to uppercase, or returns null when malformed. */
export function normalizeHexColor(raw: string | null | undefined): string | null {
  if (!raw) return null;
  const trimmed = raw.trim().toUpperCase();
  const withHash = trimmed.startsWith('#') ? trimmed : `#${trimmed}`;
  return HEX_PATTERN.test(withHash) ? withHash : null;
}

export function isValidHexColor(raw: string | null | undefined): boolean {
  return normalizeHexColor(raw) !== null;
}

/** Validates format and minimum contrast on light surfaces; returns Spanish error or null when valid. */
export function validateEditableHex(raw: string | null | undefined): string | null {
  const normalized = normalizeHexColor(raw);
  if (!normalized) return 'Usa un código hexadecimal válido (#RRGGBB).';
  if (!hasSafeLightSurfaceContrast(normalized)) {
    return 'Color demasiado claro; elige un tono con más contraste.';
  }
  return null;
}

export function parseRgb(hex: string): [number, number, number] {
  return [
    parseInt(hex.slice(1, 3), 16),
    parseInt(hex.slice(3, 5), 16),
    parseInt(hex.slice(5, 7), 16)
  ];
}

export function formatRgb(r: number, g: number, b: number): string {
  const clamp = (n: number) => Math.max(0, Math.min(255, Math.round(n))).toString(16).padStart(2, '0').toUpperCase();
  return `#${clamp(r)}${clamp(g)}${clamp(b)}`;
}

function linearChannel(channel: number): number {
  const srgb = channel / 255;
  return srgb <= 0.03928 ? srgb / 12.92 : Math.pow((srgb + 0.055) / 1.055, 2.4);
}

/** WCAG 2.1 relative luminance in [0, 1]. */
export function relativeLuminance(hex: string): number {
  const [r, g, b] = parseRgb(hex);
  return 0.2126 * linearChannel(r) + 0.7152 * linearChannel(g) + 0.0722 * linearChannel(b);
}

/** WCAG 2.1 contrast ratio between two #RRGGBB colors (1 to 21). */
export function contrastRatio(hexA: string, hexB: string): number {
  const l1 = relativeLuminance(hexA);
  const l2 = relativeLuminance(hexB);
  const lighter = Math.max(l1, l2);
  const darker = Math.min(l1, l2);
  return (lighter + 0.05) / (darker + 0.05);
}

export function hasSafeLightSurfaceContrast(hex: string): boolean {
  const normalized = normalizeHexColor(hex);
  return normalized !== null && contrastRatio(normalized, '#FFFFFF') >= MIN_CONTRAST_AGAINST_WHITE;
}

/** Picks white or deep navy text for maximum WCAG readability over a solid colored surface. */
export function readableTextOn(backgroundHex: string): '#FFFFFF' | '#0B192C' {
  const normalized = normalizeHexColor(backgroundHex);
  if (!normalized) return '#FFFFFF';
  return contrastRatio(normalized, '#FFFFFF') >= contrastRatio(normalized, '#0B192C')
    ? '#FFFFFF'
    : '#0B192C';
}

function mixColors(hexA: string, hexB: string, weightA: number): string {
  const [r1, g1, b1] = parseRgb(hexA);
  const [r2, g2, b2] = parseRgb(hexB);
  const w = Math.max(0, Math.min(1, weightA));
  return formatRgb(r1 * w + r2 * (1 - w), g1 * w + g2 * (1 - w), b1 * w + b2 * (1 - w));
}

/**
 * Derives flat, WCAG-AA legible subtle/strong/border tokens from any base color so shift chips and tinted pills
 * stay crisp without gradients.
 */
export function deriveSoftSurface(hex: string): DerivedSurfaceTokens {
  const normalized = normalizeHexColor(hex) ?? '#0B192C';
  const subtle = mixColors(normalized, '#FFFFFF', 0.12);
  const border = mixColors(normalized, '#FFFFFF', 0.34);

  let strong = normalized;
  let shadeWeight = 0.08;
  while (contrastRatio(strong, subtle) < 5.2 && shadeWeight <= 0.85) {
    strong = mixColors('#0B192C', normalized, shadeWeight);
    shadeWeight += 0.08;
  }

  return {
    subtle,
    strong,
    border,
    onSolid: readableTextOn(normalized)
  };
}
