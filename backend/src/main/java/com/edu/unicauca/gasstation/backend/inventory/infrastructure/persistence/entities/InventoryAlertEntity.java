package com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.entities;

import com.edu.unicauca.gasstation.backend.inventory.FuelType;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.AlertStatus;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.AlertType;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.AutonomyLevel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "inventory_alert")
@Getter
@Setter
@NoArgsConstructor
public class InventoryAlertEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "alert_type", nullable = false, length = 20)
    private AlertType alertType;

    @Enumerated(EnumType.STRING)
    @Column(name = "fuel_type", nullable = false, length = 20)
    private FuelType fuelType;

    @Enumerated(EnumType.STRING)
    @Column(name = "level", nullable = false, length = 10)
    private AutonomyLevel level;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private AlertStatus status;

    @Column(name = "autonomy_days", nullable = false, precision = 8, scale = 2)
    private BigDecimal autonomyDays;

    @Column(name = "opened_on", nullable = false)
    private LocalDate openedOn;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "attended_at")
    private LocalDateTime attendedAt;

    @Column(name = "attend_note", length = 200)
    private String attendNote;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;
}
