package com.edu.unicauca.gasstation.backend.inventory.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TankReadingResponse {

    private Long tankId;
    private BigDecimal heightCm;

    @Schema(description = "Net gallons: gauge table volume minus the dead volume")
    private BigDecimal volumeGallons;
}