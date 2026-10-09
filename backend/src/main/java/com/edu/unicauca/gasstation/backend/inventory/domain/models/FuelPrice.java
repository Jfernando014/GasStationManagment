package com.edu.unicauca.gasstation.backend.inventory.domain.models;

import com.edu.unicauca.gasstation.backend.inventory.FuelType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "fuel_price",
        uniqueConstraints = @UniqueConstraint(name = "uq_fuel_price_fuel_type_valid_from",
                columnNames = {"fuel_type", "valid_from"}))
@NoArgsConstructor
@Getter
@Setter
public class FuelPrice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FuelType fuelType;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal pricePerGallon;

    @Column(nullable = false)
    private LocalDate validFrom;
}
