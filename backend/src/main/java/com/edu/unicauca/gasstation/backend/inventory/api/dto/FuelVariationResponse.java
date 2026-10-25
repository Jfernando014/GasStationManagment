package com.edu.unicauca.gasstation.backend.inventory.api.dto;

import com.edu.unicauca.gasstation.backend.inventory.FuelType;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.VariationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Variation of a fuel on a journal date")
public class FuelVariationResponse {

    private FuelType fuelType;

    @Schema(description = "OK when the variation could be calculated; otherwise the reason it could not")
    private VariationStatus status;

    private List<Long> tankIds;
    private BigDecimal initialGallons;
    private BigDecimal entriesGallons;
    private BigDecimal soldGallons;

    @Schema(description = "Initial + entries - sold. Null unless the status is OK")
    private BigDecimal theoreticalGallons;

    private BigDecimal measuredGallons;

    @Schema(description = "Measured minus theoretical gallons. Positive = surplus, negative = shortage. "
            + "Null unless the status is OK")
    private BigDecimal varianceGallons;

    @Schema(description = "Sum of the tolerances of the fuel's tanks, in gallons. Null unless the status is OK")
    private BigDecimal toleranceGallons;

    @Schema(description = "True when the absolute variance exceeds the tolerance")
    private boolean outOfTolerance;
}