package com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.entities;

import com.edu.unicauca.gasstation.backend.inventory.FuelType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "tank",
        uniqueConstraints = @UniqueConstraint(name = "uq_tank_code", columnNames = "code"))
@NoArgsConstructor
@Getter
@Setter
public class TankEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 10)
    private String code;

    @Column(nullable = false, length = 80)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FuelType fuelType;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal maxCapacityGallons;

    @Column(nullable = false, precision = 6, scale = 2)
    private BigDecimal maxHeightCm;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal toleranceCm;
}
