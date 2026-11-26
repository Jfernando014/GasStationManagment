package com.edu.unicauca.gasstation.backend.inventory.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class MonthlyVariationTotalsResponse {

    @Schema(description = "Entries of every recorded day")
    private BigDecimal entriesGallons;

    @Schema(description = "Sales of every recorded day")
    private BigDecimal soldGallons;

    @Schema(description = "Variance of the days with status OK")
    private BigDecimal varianceGallons;

    @Schema(description = "Variance / sales of the days with status OK x 100")
    private BigDecimal variancePercentage;

    @Schema(description = "Average over the days on which something was sold")
    private BigDecimal averageDailySoldGallons;

    private BigDecimal lastMeasuredGallons;
    private LocalDate lastMeasuredDate;
}
