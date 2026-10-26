package com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "autonomy_threshold")
@Getter
@Setter
@NoArgsConstructor
public class AutonomyThresholdEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "valid_from", nullable = false, unique = true)
    private LocalDate validFrom;

    @Column(name = "warning_days", nullable = false, precision = 4, scale = 1)
    private BigDecimal warningDays;

    @Column(name = "critical_days", nullable = false, precision = 4, scale = 1)
    private BigDecimal criticalDays;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}