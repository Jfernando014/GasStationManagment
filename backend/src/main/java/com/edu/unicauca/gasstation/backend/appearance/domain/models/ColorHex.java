package com.edu.unicauca.gasstation.backend.appearance.domain.models;

import com.edu.unicauca.gasstation.backend.appearance.exception.InsufficientColorContrastException;
import com.edu.unicauca.gasstation.backend.appearance.exception.InvalidColorHexException;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Validated 6-digit hexadecimal color ({@code #RRGGBB}) with guaranteed minimum visibility on light surfaces.
 *
 * <p>Rejects near-white and washed-out colors whose WCAG 2.1 contrast ratio against {@code #FFFFFF} falls below
 * {@link #MIN_CONTRAST_AGAINST_WHITE}, while accepting every saturated operational color (such as amber {@code #F59E0B}
 * and emerald {@code #10B981}).
 */
public final class ColorHex {

    private static final Pattern HEX_PATTERN = Pattern.compile("^#[0-9A-F]{6}$");

    /**
     * Minimum WCAG 2.1 contrast ratio against white (#FFFFFF). Allows saturated operational colors (amber #F59E0B is
     * 2.15, emerald #10B981 is 2.54) while rejecting illegible whites and pale pastels.
     */
    public static final double MIN_CONTRAST_AGAINST_WHITE = 1.80;

    private final String value;

    private ColorHex(String value) {
        this.value = value;
    }

    /** Parses, normalizes to uppercase {@code #RRGGBB}, and validates legibility contrast against white. */
    public static ColorHex of(String raw) {
        if (raw == null) {
            throw new InvalidColorHexException("null");
        }
        String normalized = raw.strip().toUpperCase(Locale.ROOT);
        if (!HEX_PATTERN.matcher(normalized).matches()) {
            throw new InvalidColorHexException(raw);
        }
        if (contrastAgainstWhite(normalized) < MIN_CONTRAST_AGAINST_WHITE) {
            throw new InsufficientColorContrastException(normalized);
        }
        return new ColorHex(normalized);
    }

    /** Restores an already persisted uppercase hex without re-checking historical contrast thresholds. */
    public static ColorHex restore(String stored) {
        String normalized = Objects.requireNonNull(stored, "colorHex").strip().toUpperCase(Locale.ROOT);
        if (!HEX_PATTERN.matcher(normalized).matches()) {
            throw new InvalidColorHexException(stored);
        }
        return new ColorHex(normalized);
    }

    public String value() {
        return value;
    }

    /** WCAG 2.1 relative luminance in {@code [0, 1]} for a {@code #RRGGBB} string. */
    public static double relativeLuminance(String hex) {
        int red = Integer.parseInt(hex.substring(1, 3), 16);
        int green = Integer.parseInt(hex.substring(3, 5), 16);
        int blue = Integer.parseInt(hex.substring(5, 7), 16);
        return 0.2126 * linearChannel(red) + 0.7152 * linearChannel(green) + 0.0722 * linearChannel(blue);
    }

    /** WCAG 2.1 contrast ratio between {@code hex} and pure white ({@code #FFFFFF}). */
    public static double contrastAgainstWhite(String hex) {
        return (1.0 + 0.05) / (relativeLuminance(hex) + 0.05);
    }

    private static double linearChannel(int channel) {
        double srgb = channel / 255.0;
        return srgb <= 0.03928 ? srgb / 12.92 : Math.pow((srgb + 0.055) / 1.055, 2.4);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ColorHex that && value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return value;
    }
}
