package com.edu.unicauca.gasstation.backend.sales.api.dto;

import com.edu.unicauca.gasstation.backend.sales.domain.calculation.SalesJourneyStatistics.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.DayOfWeek;

public record CompareSalesScenariosRequest(@NotNull @Valid ExplicitSalesStatisticsRequest sales,
        @NotNull @Valid ScenarioRequest first, @NotNull @Valid ScenarioRequest second) {
    public record ScenarioRequest(@NotNull DayOfWeek dayOfWeek, @NotNull Journey journey) {
        public Scenario toDomain() { return new Scenario(dayOfWeek, journey); }
    }
}
