package com.edu.unicauca.gasstation.backend.sales.domain.calculation;

import com.edu.unicauca.gasstation.backend.sales.domain.services.SalesTargetStoragePolicy;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.*;

/** Immutable, explicitly sourced parameter snapshot. No workbook values are production defaults. */
public record DistributionProfile(String name, String source, List<BigDecimal> hourlyWeights,
        Map<DayOfWeek, BigDecimal> historicalDailyGallons, Set<String> supportShiftCodes,
        BigDecimal titularShare, BigDecimal supportShare, BigDecimal referenceTitularShare, BigDecimal referenceSupportShare) {
    public DistributionProfile {
        if (name == null || name.isBlank() || source == null || source.isBlank())
            throw new IllegalArgumentException("Profile name and source are required");
        if (name.length() > 100 || source.length() > 500) throw new IllegalArgumentException("Profile text too long");
        hourlyWeights = List.copyOf(hourlyWeights);
        historicalDailyGallons = Map.copyOf(historicalDailyGallons);
        supportShiftCodes = Set.copyOf(supportShiftCodes);
        if (hourlyWeights.size() != 24) throw new IllegalArgumentException("Exactly 24 hourly weights required (0..23)");
        if (historicalDailyGallons.size() != 7) throw new IllegalArgumentException("All seven weekdays required");
        if (hourlyWeights.stream().map(DistributionProfile::checked).reduce(BigDecimal.ZERO, BigDecimal::add).signum() <= 0)
            throw new IllegalArgumentException("Positive hourly weight sum required");
        for (var day : DayOfWeek.values()) checked(Objects.requireNonNull(historicalDailyGallons.get(day)));
        if (historicalDailyGallons.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add).signum() <= 0)
            throw new IllegalArgumentException("Positive history sum required");
        validateShares(titularShare, supportShare); validateShares(referenceTitularShare, referenceSupportShare);
        for (var code : supportShiftCodes)
            if (code.isBlank() || code.length() > 30) throw new IllegalArgumentException("Invalid support shift code");
    }
    private static BigDecimal checked(BigDecimal value) {
        Objects.requireNonNull(value);
        if (value.signum() < 0) throw new IllegalArgumentException("Negative parameter");
        SalesTargetStoragePolicy.validate(value);
        return value;
    }
    private static void validateShares(BigDecimal titular, BigDecimal support) {
        checked(titular); checked(support);
        if (titular.add(support).compareTo(BigDecimal.ONE) != 0)
            throw new IllegalArgumentException("Titular and support shares must sum to one");
    }
}
