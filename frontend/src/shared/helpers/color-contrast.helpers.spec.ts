import {
  DEFAULT_CURATED_PALETTE,
  contrastRatio,
  deriveSoftSurface,
  hasSafeLightSurfaceContrast,
  normalizeHexColor,
  readableTextOn,
  validateEditableHex
} from './color-contrast.helpers';

describe('color-contrast.helpers', () => {
  it('normalizes trimmed lowercase hex codes with or without leading hash', () => {
    expect(normalizeHexColor('  #0284c7 ')).toBe('#0284C7');
    expect(normalizeHexColor('ff6b00')).toBe('#FF6B00');
    expect(normalizeHexColor('#FFF')).toBeNull();
    expect(normalizeHexColor('not-a-color')).toBeNull();
  });

  it('accepts every curated swatch and rejects near-white unreadable colors', () => {
    for (const swatch of DEFAULT_CURATED_PALETTE) {
      expect(hasSafeLightSurfaceContrast(swatch.hex)).toBe(true);
      expect(validateEditableHex(swatch.hex)).toBeNull();
    }

    expect(hasSafeLightSurfaceContrast('#FFFFFF')).toBe(false);
    expect(hasSafeLightSurfaceContrast('#F8FAFC')).toBe(false);
    expect(validateEditableHex('#FFFFFF')).toContain('demasiado claro');
    expect(validateEditableHex('#ZZZ')).toContain('hexadecimal válido');
  });

  it('derives WCAG AA legible soft surfaces and solid foreground text for every curated swatch', () => {
    for (const swatch of DEFAULT_CURATED_PALETTE) {
      const surface = deriveSoftSurface(swatch.hex);
      expect(contrastRatio(surface.strong, surface.subtle)).toBeGreaterThanOrEqual(4.5);
      expect(readableTextOn(swatch.hex)).toMatch(/^#(FFFFFF|0B192C)$/);
    }

    expect(readableTextOn('#0B192C')).toBe('#FFFFFF');
    expect(readableTextOn('#F59E0B')).toBe('#0B192C');
  });
});
