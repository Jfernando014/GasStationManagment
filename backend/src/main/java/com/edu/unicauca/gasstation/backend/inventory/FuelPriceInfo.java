package com.edu.unicauca.gasstation.backend.inventory;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@AllArgsConstructor
public class FuelPriceInfo {
    private final FuelType fuelType;
    private final BigDecimal pricePerGallon;
    private final LocalDate validFrom;
}
