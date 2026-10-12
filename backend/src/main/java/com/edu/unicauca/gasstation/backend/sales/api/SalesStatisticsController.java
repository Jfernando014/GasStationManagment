package com.edu.unicauca.gasstation.backend.sales.api;

import com.edu.unicauca.gasstation.backend.sales.api.dto.*;
import com.edu.unicauca.gasstation.backend.sales.domain.calculation.SalesJourneyStatistics;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/sales")
public class SalesStatisticsController {
    private final SalesJourneyStatistics statistics = new SalesJourneyStatistics();

    @PostMapping("/averages/explicit")
    @Operation(summary = "Calculate day/night averages from explicit observations", description = "ADMIN only. Uses client data, not server sales history. DAY=diurna, NIGHT=nocturna. Empty observations produce unavailable averages.")
    public SalesStatisticsResponse averages(@Valid @RequestBody ExplicitSalesStatisticsRequest request, Authentication authentication) {
        SalesAdminAuthorization.requireAdmin(authentication);
        return SalesStatisticsResponse.from(statistics.averages(request.from(), request.to(), request.category(), request.toDomain()), request.outputScale());
    }

    @PostMapping("/scenarios/compare/explicit")
    @Operation(summary = "Compare two weekday/journey scenarios from explicit observations", description = "ADMIN only. Returns labelled chart data, not a graph or a server-history query.")
    public SalesStatisticsResponse compare(@Valid @RequestBody CompareSalesScenariosRequest request, Authentication authentication) {
        SalesAdminAuthorization.requireAdmin(authentication);
        var sales = request.sales();
        return SalesStatisticsResponse.from(statistics.compare(sales.from(), sales.to(), sales.category(),
                request.first().toDomain(), request.second().toDomain(), sales.toDomain()), sales.outputScale());
    }
}
