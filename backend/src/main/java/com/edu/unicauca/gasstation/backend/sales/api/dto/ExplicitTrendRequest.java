package com.edu.unicauca.gasstation.backend.sales.api.dto;

import com.edu.unicauca.gasstation.backend.sales.domain.calculation.StatisticsPeriod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record ExplicitTrendRequest(@NotNull @Valid ExplicitSalesStatisticsRequest sales,
                                   @NotNull StatisticsPeriod period) { }
