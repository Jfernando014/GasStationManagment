package com.edu.unicauca.gasstation.backend.inventory.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Measured level of a tank")
public class TankStockResponse {

    private Long tankId;
    private String tankCode;
    private BigDecimal heightCm;

    @Schema(description = "Net gallons (gauge table volume minus the dead volume)")
    private BigDecimal gallons;
}