package com.edu.unicauca.gasstation.backend.competition.domain.models;

import com.edu.unicauca.gasstation.backend.inventory.FuelType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

public record PriceObservation(Long id, Long competitorId, FuelType fuelType,
                               BigDecimal pricePerGallon, Instant observedAt, Instant createdAt) {
    public PriceObservation {
        if (competitorId == null || competitorId <= 0) throw new IllegalArgumentException("Invalid competitorId");
        Objects.requireNonNull(fuelType, "fuelType");
        Objects.requireNonNull(observedAt, "observedAt");
        PriceCalculations.money(pricePerGallon, false);
    }
}
