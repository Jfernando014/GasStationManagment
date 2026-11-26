package com.edu.unicauca.gasstation.backend.inventory.api.dto;

import com.edu.unicauca.gasstation.backend.inventory.FuelType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Monthly variation of a fuel")
public class FuelMonthlyVariationResponse {

    private FuelType fuelType;
    private List<DailyVariationResponse> days;
    private MonthlyVariationTotalsResponse totals;

    @Schema(description = "Sales target of the month in gallons; null while there is none")
    private BigDecimal targetGallons;

    @Schema(description = "Target minus sold. Null when either is unknown")
    private BigDecimal remainingGallons;

    private List<ShortageRangeResponse> shortageRanges;
    private List<SurplusRangeResponse> surplusRanges;
    private FuelVariationSummaryResponse summary;
}
