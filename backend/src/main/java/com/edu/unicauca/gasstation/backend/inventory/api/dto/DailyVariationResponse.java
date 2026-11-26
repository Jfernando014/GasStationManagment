package com.edu.unicauca.gasstation.backend.inventory.api.dto;

import com.edu.unicauca.gasstation.backend.inventory.domain.models.AlertLevel;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.VariationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "A calendar day of the monthly variation. Null means unknown, never zero")
public class DailyVariationResponse {

    private LocalDate date;
    private VariationStatus status;
    private BigDecimal initialGallons;
    private BigDecimal entriesGallons;
    private BigDecimal soldGallons;
    private BigDecimal theoreticalGallons;
    private BigDecimal measuredGallons;

    @Schema(description = "Measured minus theoretical. Positive = surplus, negative = shortage")
    private BigDecimal varianceGallons;

    @Schema(description = "Variance / sold x 100. Null when nothing was sold")
    private BigDecimal variancePercentage;

    private BigDecimal toleranceGallons;
    private Boolean outOfTolerance;

    @Schema(description = "Sales of the days with status OK up to this one. Null unless this day is OK")
    private BigDecimal cumulativeSoldGallons;

    private BigDecimal cumulativeVarianceGallons;
    private BigDecimal cumulativePercentage;

    @Schema(description = "OK means no alert")
    private AlertLevel cumulativeStatus;
}
