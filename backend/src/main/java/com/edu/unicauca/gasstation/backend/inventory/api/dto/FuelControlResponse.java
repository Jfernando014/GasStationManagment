package com.edu.unicauca.gasstation.backend.inventory.api.dto;

import com.edu.unicauca.gasstation.backend.inventory.FuelType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Variation and autonomy of a fuel on a journal date")
public class FuelControlResponse {

    private FuelType fuelType;
    private FuelVariationResponse variation;
    private FuelAutonomyResponse autonomy;

    @Schema(description = "Journal date plus the whole days of autonomy. Null unless the autonomy was calculated")
    private LocalDate estimatedDepletionDate;
}