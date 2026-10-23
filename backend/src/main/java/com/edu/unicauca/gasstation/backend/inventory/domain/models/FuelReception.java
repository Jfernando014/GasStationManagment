package com.edu.unicauca.gasstation.backend.inventory.domain.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class FuelReception {

    private Long id;
    private Long tankId;
    private LocalDate receptionDate;
    private BigDecimal gallons;
    private String supplier;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}