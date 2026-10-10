package com.edu.unicauca.gasstation.backend.inventory.api.dto;

import com.edu.unicauca.gasstation.backend.inventory.FuelType;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TankResponse {
    private Long id;
    private String code;
    private String name;
    private FuelType fuelType;
    private BigDecimal maxCapacityGallons;
    private BigDecimal maxHeightCm;
    private BigDecimal toleranceCm;
}
