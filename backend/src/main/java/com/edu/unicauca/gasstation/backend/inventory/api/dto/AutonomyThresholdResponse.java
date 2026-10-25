package com.edu.unicauca.gasstation.backend.inventory.api.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class AutonomyThresholdResponse {

    private Long id;
    private LocalDate validFrom;
    private BigDecimal warningDays;
    private BigDecimal criticalDays;
    private LocalDateTime createdAt;
}