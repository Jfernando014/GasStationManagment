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

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "tank_measurement")
@Getter
@Setter
@NoArgsConstructor
public class TankMeasurementEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "journal_date", nullable = false, unique = true)
    private LocalDate journalDate;

    @Column(name = "measured_at", nullable = false)
    private LocalDateTime measuredAt;
}