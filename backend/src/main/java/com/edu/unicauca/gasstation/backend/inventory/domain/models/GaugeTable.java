package com.edu.unicauca.gasstation.backend.inventory.domain.models;

import com.edu.unicauca.gasstation.backend.inventory.exception.HeightExceedsGaugeException;
import com.edu.unicauca.gasstation.backend.inventory.exception.InvalidGaugeTableException;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
public class GaugeTable {
    public static final int MIN_POINTS = 2;
    private static final int VOLUME_SCALE = 2;
    private static final int MAX_HEIGHT_INTEGER_DIGITS = 4;
    private static final int MAX_HEIGHT_DECIMALS = 1;
    private static final int MAX_VOLUME_INTEGER_DIGITS = 6;
    private static final int MAX_VOLUME_DECIMALS = 2;

    private Long id;
    private String name;
    private List<GaugePoint> points = new ArrayList<>();

    public void validate() {
        if (points == null || points.size() < MIN_POINTS) {
            throw new InvalidGaugeTableException("points",
                    "The gauge table must have at least " + MIN_POINTS + " points");
        }
        for (int i = 0; i < points.size(); i++) {
            GaugePoint current = points.get(i);
            String field = "points[" + i + "]";

            if (current.getHeightCm() == null || current.getVolumeGallons() == null) {
                throw new InvalidGaugeTableException(field, "The height and volume are obligated");
            }
            if (current.getHeightCm().signum() < 0 || current.getVolumeGallons().signum() < 0) {
                throw new InvalidGaugeTableException(field, "The height and volume must be positive");
            }
            if (exceeds(current.getHeightCm(), MAX_HEIGHT_INTEGER_DIGITS, MAX_HEIGHT_DECIMALS)) {
                throw new InvalidGaugeTableException(field,
                        "The height allows for a maximum of " + MAX_HEIGHT_INTEGER_DIGITS + " integer and "
                                + MAX_HEIGHT_DECIMALS + " decimal");
            }
            if (exceeds(current.getVolumeGallons(), MAX_VOLUME_INTEGER_DIGITS, MAX_VOLUME_DECIMALS)) {
                throw new InvalidGaugeTableException(field,
                        "The volume allows for a maximum of " + MAX_VOLUME_INTEGER_DIGITS + " integer and "
                                + MAX_VOLUME_DECIMALS + " decimals");
            }
            if (i == 0) {
                continue;
            }
            GaugePoint previous = points.get(i - 1);
            if (current.getHeightCm().compareTo(previous.getHeightCm()) <= 0) {
                throw new InvalidGaugeTableException(field, "The height " + current.getHeightCm().toPlainString()
                        + " cm must be greater than that in the previous point ("
                        + previous.getHeightCm().toPlainString() + " cm)");
            }
            if (current.getVolumeGallons().compareTo(previous.getVolumeGallons()) < 0) {
                throw new InvalidGaugeTableException(field, "The volume " + current.getVolumeGallons().toPlainString()
                        + " gal  cannot be less than that in the previous point ("
                        + previous.getVolumeGallons().toPlainString() + " gal)");
            }
        }
    }

    public BigDecimal volumeAt(BigDecimal heightCm) {
        Objects.requireNonNull(heightCm, "heightCm");
        if (heightCm.signum() < 0) {
            throw new IllegalArgumentException("heightCm must not be negative");
        }
        GaugePoint first = points.get(0);
        GaugePoint last = points.get(points.size() - 1);

        if (heightCm.compareTo(first.getHeightCm()) < 0) {
            return BigDecimal.ZERO;
        }
        if (heightCm.compareTo(last.getHeightCm()) > 0) {
            throw new HeightExceedsGaugeException(last.getHeightCm());
        }
        for (int i = 1; i < points.size(); i++) {
            GaugePoint upper = points.get(i);
            if (heightCm.compareTo(upper.getHeightCm()) <= 0) {
                return interpolate(points.get(i - 1), upper, heightCm);
            }
        }
        return last.getVolumeGallons();
    }

    public BigDecimal minHeightCm() {
        return points.get(0).getHeightCm();
    }

    public BigDecimal maxHeightCm() {
        return points.get(points.size() - 1).getHeightCm();
    }

    public BigDecimal maxVolumeGallons() {
        return points.get(points.size() - 1).getVolumeGallons();
    }

    private BigDecimal interpolate(GaugePoint lower, GaugePoint upper, BigDecimal heightCm) {
        BigDecimal heightSpan = upper.getHeightCm().subtract(lower.getHeightCm());
        BigDecimal volumeSpan = upper.getVolumeGallons().subtract(lower.getVolumeGallons());
        BigDecimal fraction = heightCm.subtract(lower.getHeightCm()).divide(heightSpan, MathContext.DECIMAL64);
        return lower.getVolumeGallons().add(volumeSpan.multiply(fraction)).setScale(VOLUME_SCALE, RoundingMode.HALF_UP);
    }

    private static boolean exceeds(BigDecimal value, int maxIntegerDigits, int maxDecimals) {
        BigDecimal normalized = value.stripTrailingZeros();
        int decimals = Math.max(normalized.scale(), 0);
        int integerDigits = normalized.precision() - normalized.scale();
        return decimals > maxDecimals || integerDigits > maxIntegerDigits;
    }
}
