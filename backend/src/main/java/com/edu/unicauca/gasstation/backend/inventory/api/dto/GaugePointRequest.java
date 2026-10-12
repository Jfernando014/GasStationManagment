package com.edu.unicauca.gasstation.backend.inventory.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class GaugePointRequest {

    public static final String HEIGHT_REQUIRED = "The height is required";
    public static final String VOLUME_REQUIRED = "The volumen is  required";

    @Schema(example = "55.0", description = "Height in cm, maximum a decimal")
    @NotNull(message = HEIGHT_REQUIRED)
    private BigDecimal heightCm;

    @Schema(example = "559", description = "volumen in gallons")
    @NotNull(message = VOLUME_REQUIRED)
    private BigDecimal volumeGallons;
}