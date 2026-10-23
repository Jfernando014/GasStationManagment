package com.edu.unicauca.gasstation.backend.sales.api;

import com.edu.unicauca.gasstation.backend.inventory.ProductCategory;
import com.edu.unicauca.gasstation.backend.sales.api.dto.*;
import com.edu.unicauca.gasstation.backend.sales.application.*;
import com.edu.unicauca.gasstation.backend.sales.domain.calculation.StatisticsPeriod;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/sales/statistics")
@RequiredArgsConstructor
public class SalesReportingController {
    private final BasketStatisticsService baskets;
    private final SalesChartsService charts;

    @GetMapping("/basket")
    @Operation(summary = "Basket sales by seller/category", description = "ADMIN only. Real historical sales exclusively. UNAVAILABLE without a complete connected source; never substitutes targets or scheduled shifts.")
    public BasketStatisticsResponse basket(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) UUID workerId, @RequestParam(required = false) ProductCategory category,
            @RequestParam(defaultValue = "DAY") StatisticsPeriod period,
            @RequestParam(defaultValue = "2") int outputScale, Authentication authentication) {
        SalesAdminAuthorization.requireAdmin(authentication);
        return baskets.read(from, to, workerId, category, period, outputScale);
    }

    @PostMapping("/trends/explicit")
    @Operation(summary = "Observed period totals and journey averages", description = "ADMIN only. Client observations, not verified historical sales. Emits observed buckets only; change compares consecutive observed totals.")
    public StatisticsChartResponse trends(@Valid @RequestBody ExplicitTrendRequest request, Authentication authentication) {
        SalesAdminAuthorization.requireAdmin(authentication);
        var sales = request.sales();
        return charts.observations(sales.from(), sales.to(), sales.category(), sales.toDomain(), request.period(), sales.outputScale());
    }

    @GetMapping("/targets/{id}")
    @Operation(summary = "Chart configured target", description = "ADMIN only. TARGET_PROJECTION, never completed sales or compliance.")
    public StatisticsChartResponse target(@PathVariable long id, @RequestParam(defaultValue = "2") int outputScale, Authentication authentication) {
        SalesAdminAuthorization.requireAdmin(authentication);
        return charts.target(id, outputScale);
    }

    @PostMapping("/targets/{id}/distribution")
    @Operation(summary = "Chart target distribution by date and worker", description = "ADMIN only. Reuses SCRUM-40 calculation with explicit assignments. Assignments do not certify attendance. See the existing distribution endpoint for full allocation and coverage diagnostics.")
    public StatisticsChartResponse distribution(@PathVariable long id, @Valid @RequestBody CalculateDistributionRequest request, Authentication authentication) {
        SalesAdminAuthorization.requireAdmin(authentication);
        return charts.distribution(id, request.profileId(), request.outputScale(), request.toDomainAssignments());
    }
}
