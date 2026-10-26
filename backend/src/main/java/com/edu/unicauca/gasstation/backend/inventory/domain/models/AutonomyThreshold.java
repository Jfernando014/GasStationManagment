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
public class AutonomyThreshold {

    private Long id;
    private LocalDate validFrom;
    private BigDecimal warningDays;
    private BigDecimal criticalDays;
    private LocalDateTime createdAt;
}