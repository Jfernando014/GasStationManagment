package com.edu.unicauca.gasstation.backend.inventory.api.dto;

import com.edu.unicauca.gasstation.backend.inventory.domain.models.AlertLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class FuelVariationSummaryResponse {

    @Schema(description = "Cumulative status of the last day with status OK; null without one")
    private AlertLevel cumulativeStatus;

    private LocalDate lastCalculatedDate;
    private int calculatedDays;
    private int outOfToleranceDays;
}
