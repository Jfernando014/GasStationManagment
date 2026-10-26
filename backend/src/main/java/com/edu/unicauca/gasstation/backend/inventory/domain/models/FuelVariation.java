package com.edu.unicauca.gasstation.backend.inventory.domain.models;

import com.edu.unicauca.gasstation.backend.inventory.FuelType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class FuelVariation {
    private final FuelType fuelType;
    private final VariationStatus status;
    private final List<Long> tankIds;
    private final BigDecimal initialGallons;
    private final BigDecimal entriesGallons;
    private final BigDecimal soldGallons;
    /**
     * initial + entries - sold. Null unless the status is OK.
     */
    private final BigDecimal theoreticalGallons;
    private final BigDecimal measuredGallons;
    /**
     * measured - theoretical. Positive means surplus, negative means shortage. Null unless the status is OK.
     */
    private final BigDecimal varianceGallons;
    /**
     * Sum of the tolerances of the fuel's tanks, converted to gallons. Null unless the status is OK.
     */
    private final BigDecimal toleranceGallons;
    private final boolean outOfTolerance;
}
