package com.edu.unicauca.gasstation.backend.competition.api.dto;

import com.edu.unicauca.gasstation.backend.inventory.FuelType;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;

public final class CompetitionRequests {
    private CompetitionRequests() { }
    public record CompetitorRequest(@NotBlank @Size(max = 150) String name,
            @NotBlank @Size(max = 100) String brand, @NotBlank @Size(max = 255) String location) { }
    public record StatusRequest(@NotNull Boolean active) { }
    public record ObservationRequest(@NotNull @Positive Long competitorId, @NotNull FuelType fuelType,
            @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal pricePerGallon,
            @NotNull @PastOrPresent Instant observedAt) { }
    public record SimulationRequest(@NotNull FuelType fuelType,
            @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal proposedPrice,
            @DecimalMin("0") @Digits(integer = 10, fraction = 2) BigDecimal acquisitionCost,
            @PastOrPresent Instant asOf) { }
}
