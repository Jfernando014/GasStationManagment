package com.edu.unicauca.gasstation.backend.inventory.domain.models;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Size ranges of the shortage or surplus of a day, in gallons. The cuts are fixed rules of the domain, not
 * configuration.
 *
 * <p>The ranges of the Excel overlap on their edges. Here they are contiguous, decimals included, and each cut
 * belongs to the lower range: [1, 49], (49, 74], (74, 124], (124, 249], (249, 499] and (499, ∞). A variation
 * below 1 gallon belongs to no range.
 */
public enum GallonRange {
    FROM_1_TO_49("49"),
    FROM_50_TO_74("74"),
    FROM_75_TO_124("124"),
    FROM_125_TO_249("249"),
    FROM_250_TO_499("499"),
    FROM_500(null);

    private static final BigDecimal MINIMUM_GALLONS = BigDecimal.ONE;

    /** Highest size of the range, included; null for the open one. */
    private final BigDecimal upperCut;

    GallonRange(String upperCut) {
        this.upperCut = upperCut == null ? null : new BigDecimal(upperCut);
    }

    /**
     * @param gallons size of the variation, without sign
     */
    public static Optional<GallonRange> of(BigDecimal gallons) {
        if (gallons.compareTo(MINIMUM_GALLONS) < 0) {
            return Optional.empty();
        }
        for (GallonRange range : values()) {
            if (range.upperCut == null || gallons.compareTo(range.upperCut) <= 0) {
                return Optional.of(range);
            }
        }
        return Optional.empty();
    }
}
