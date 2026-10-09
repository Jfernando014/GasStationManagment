package com.edu.unicauca.gasstation.backend.inventory.api.dto;

import com.edu.unicauca.gasstation.backend.inventory.domain.models.FuelType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FuelPriceResponse {
    private Long id;
    private FuelType fuelType;
    private BigDecimal pricePerGallon;
    private LocalDate validFrom;
}
