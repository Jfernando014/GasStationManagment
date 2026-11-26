package com.edu.unicauca.gasstation.backend.inventory.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Fuel variation of a month")
public class MonthlyVariationResponse {

    @Schema(example = "2026-08")
    private String month;

    @Schema(description = "Last day of the month, or today while it is in progress")
    private LocalDate referenceDate;

    @Schema(description = "Setting in force on the reference date, applied to the whole month")
    private VariationAlertSettingResponse alertSetting;

    private List<FuelMonthlyVariationResponse> fuels;
}
