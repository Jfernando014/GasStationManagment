package com.edu.unicauca.gasstation.backend.competition.infrastructure.persistence.entities;

import com.edu.unicauca.gasstation.backend.inventory.FuelType;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "competitor_price_observation")
@Getter @Setter @NoArgsConstructor
public class ObservationEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "competitor_id", nullable = false, updatable = false)
    private Long competitorId;
    @Enumerated(EnumType.STRING)
    @Column(name = "fuel_type", nullable = false, length = 20, updatable = false)
    private FuelType fuelType;
    @Column(name = "price_per_gallon", nullable = false, precision = 12, scale = 2, updatable = false)
    private BigDecimal pricePerGallon;
    @Column(name = "observed_at", nullable = false, updatable = false)
    private Instant observedAt;
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;
}
