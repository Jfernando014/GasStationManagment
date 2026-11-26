package com.edu.unicauca.gasstation.backend.inventory.api;

import com.edu.unicauca.gasstation.backend.inventory.api.dto.MonthlyVariationResponse;
import com.edu.unicauca.gasstation.backend.inventory.domain.services.MonthlyFuelVariationService;
import com.edu.unicauca.gasstation.backend.inventory.exception.InvalidVariationMonthException;
import com.edu.unicauca.gasstation.backend.inventory.infrastructure.mappers.MonthlyVariationMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.DateTimeException;
import java.time.YearMonth;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/v1/inventory/variations/monthly")
@RequiredArgsConstructor
@Tag(name = "Fuel variation", description = "Theoretical stock against measured stock per fuel")
public class MonthlyFuelVariationController {

    private static final Pattern MONTH_FORMAT = Pattern.compile("\\d{4}-\\d{2}");

    private final MonthlyFuelVariationService variationService;
    private final MonthlyVariationMapper mapper;

    @GetMapping
    @Operation(summary = "Daily control of a month per fuel, with totals and alerts",
            description = "One entry per calendar day. A figure without data is null.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The four fuels"),
            @ApiResponse(responseCode = "400", description = "Missing or invalid month"),
            @ApiResponse(responseCode = "404", description = "No alert setting had started on the reference date")
    })
    public MonthlyVariationResponse findByMonth(
            @Parameter(description = "Month (yyyy-MM)", example = "2026-08")
            @RequestParam String month) {
        return mapper.toResponse(variationService.calculate(parse(month)));
    }

    private static YearMonth parse(String month) {
        if (!MONTH_FORMAT.matcher(month).matches()) {
            throw new InvalidVariationMonthException();
        }
        try {
            return YearMonth.parse(month);
        } catch (DateTimeException e) {
            throw new InvalidVariationMonthException();
        }
    }
}
