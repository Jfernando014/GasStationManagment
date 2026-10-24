package com.edu.unicauca.gasstation.backend.inventory.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class TankMeasurementRequest {

    public static final String READINGS_REQUIRED = "At least one tank reading is required";

    @Schema(description = "Readings of one or more tanks (maximum one per tank)")
    @NotEmpty(message = READINGS_REQUIRED)
    @Valid
    private List<TankReadingRequest> readings;
}