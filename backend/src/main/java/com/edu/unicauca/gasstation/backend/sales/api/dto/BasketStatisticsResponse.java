package com.edu.unicauca.gasstation.backend.sales.api.dto;

import com.edu.unicauca.gasstation.backend.inventory.ProductCategory;
import com.edu.unicauca.gasstation.backend.sales.domain.calculation.StatisticsPeriod;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record BasketStatisticsResponse(LocalDate from, LocalDate to, UUID workerId, ProductCategory category,
        StatisticsPeriod period, String moneyUnit, String quantityUnit,
        StatisticsChartResponse.Availability workedShiftsAvailability, String workedShiftsReason,
        Summary summary, List<Group> groups, StatisticsChartResponse charts) {
    public record Summary(BigDecimal amount, BigDecimal quantity, int saleLines, Long workedShifts,
                          BigDecimal averageAmountPerWorkedShift) { }
    public record Group(UUID workerId, String workerName, ProductCategory category,
                        BigDecimal amount, BigDecimal quantity, int saleLines, BigDecimal participationPercent,
                        Long workedShifts, BigDecimal averageAmountPerWorkedShift) { }
    public BasketStatisticsResponse { groups = List.copyOf(groups); }
}
