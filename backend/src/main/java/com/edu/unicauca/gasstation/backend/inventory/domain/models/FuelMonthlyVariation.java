package com.edu.unicauca.gasstation.backend.inventory.domain.models;

import com.edu.unicauca.gasstation.backend.inventory.FuelType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class FuelMonthlyVariation {
    private final FuelType fuelType;
    private final List<DailyVariationEntry> days;
    private final MonthlyVariationTotals totals;
    /** Sales target of the month; null while there is none. */
    private final BigDecimal targetGallons;
    /** target - sold. Null when either is unknown. */
    private final BigDecimal remainingGallons;
    private final List<ShortageRangeCount> shortageRanges;
    private final List<SurplusRangeCount> surplusRanges;
    private final FuelVariationSummary summary;
}
