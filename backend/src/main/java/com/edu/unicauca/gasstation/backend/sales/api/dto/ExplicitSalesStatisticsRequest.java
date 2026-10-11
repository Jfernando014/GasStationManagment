package com.edu.unicauca.gasstation.backend.sales.api.dto;

import com.edu.unicauca.gasstation.backend.sales.domain.calculation.SalesJourneyStatistics.*;
import com.edu.unicauca.gasstation.backend.sales.domain.models.SalesTargetCategory;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Client-supplied consolidated observations, never a query of server historical sales. */
public record ExplicitSalesStatisticsRequest(@NotNull LocalDate from, @NotNull LocalDate to,
        @NotNull SalesTargetCategory category, @NotNull @Min(0) @Max(12) Integer outputScale,
        @NotNull List<@NotNull @Valid Observation> observations) {
    public record Observation(@NotNull LocalDate date, @NotNull Journey journey,
                              @NotNull @DecimalMin("0") BigDecimal value) { }

    public List<JourneySale> toDomain() {
        if (to.isBefore(from)) throw new IllegalArgumentException("Invalid sales date range");
        return observations.stream().map(row -> {
            if (row.date().isBefore(from) || row.date().isAfter(to))
                throw new IllegalArgumentException("Observation outside requested date range");
            return new JourneySale(row.date(), row.journey(), row.value());
        }).toList();
    }
}
