package com.edu.unicauca.gasstation.backend.inventory.domain.models;

import com.edu.unicauca.gasstation.backend.inventory.FuelType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;


@NoArgsConstructor
@Getter
@Setter
public class FuelPrice {

    private Long id;

    private FuelType fuelType;

    private BigDecimal pricePerGallon;

    private LocalDate validFrom;
}
