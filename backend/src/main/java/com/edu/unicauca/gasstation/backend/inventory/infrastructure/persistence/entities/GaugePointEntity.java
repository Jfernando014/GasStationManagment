package com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "gauge_point",
        uniqueConstraints = @UniqueConstraint(name = "uq_gauge_point_table_height",
                columnNames = {"gauge_table_id", "height_cm"}))
@Getter
@Setter
@NoArgsConstructor
public class GaugePointEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "gauge_table_id", nullable = false)
    private Long gaugeTableId;

    @Column(name = "height_cm", nullable = false, precision = 5, scale = 1)
    private BigDecimal heightCm;

    @Column(name = "volume_gallons", nullable = false, precision = 8, scale = 2)
    private BigDecimal volumeGallons;
}