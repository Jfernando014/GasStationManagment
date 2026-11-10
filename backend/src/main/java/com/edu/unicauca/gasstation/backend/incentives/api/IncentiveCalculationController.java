package com.edu.unicauca.gasstation.backend.incentives.api;

import com.edu.unicauca.gasstation.backend.incentives.api.IncentiveCalculationDtos.MonthlyIncentivesResponse;
import com.edu.unicauca.gasstation.backend.incentives.api.IncentiveCalculationDtos.VendorMonthResponse;
import com.edu.unicauca.gasstation.backend.incentives.domain.services.IncentiveCalculationService;
import io.swagger.v3.oas.annotations.Parameter;
import java.time.YearMonth;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Monthly incentive calculation endpoints; read only, they need a session. */
@RestController
@RequestMapping("/api/v1/incentives/monthly")
@RequiredArgsConstructor
public class IncentiveCalculationController implements IncentiveCalculationApi {

    private final IncentiveCalculationService service;

    @Override
    @GetMapping("/{month}")
    public MonthlyIncentivesResponse calculateMonth(
            @Parameter(description = "Mes a calcular (yyyy-MM)", example = "2026-03")
            @PathVariable @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
        return MonthlyIncentivesResponse.from(service.calculateMonth(month));
    }

    @Override
    @GetMapping("/{month}/vendors/{vendorDocument}")
    public VendorMonthResponse calculateVendor(
            @Parameter(description = "Mes a calcular (yyyy-MM)", example = "2026-03")
            @PathVariable @DateTimeFormat(pattern = "yyyy-MM") YearMonth month,
            @Parameter(description = "Cédula del vendedor, como la registra Ventas", example = "1000000001")
            @PathVariable String vendorDocument) {
        return VendorMonthResponse.from(service.calculateVendor(month, vendorDocument));
    }
}
