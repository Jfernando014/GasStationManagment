package com.edu.unicauca.gasstation.backend.inventory.domain.models;

import com.edu.unicauca.gasstation.backend.inventory.FuelType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@NoArgsConstructor
@Getter
@Setter
public class Tank {

    private Long id;

    private String code;

    private String name;

    private FuelType fuelType;

    private BigDecimal maxCapacityGallons;

    private BigDecimal maxHeightCm;

    private BigDecimal toleranceCm;


}
