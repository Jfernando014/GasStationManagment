package com.edu.unicauca.gasstation.backend.sales.api;

import com.edu.unicauca.gasstation.backend.sales.api.dto.FuelProfitCardSettingRequest;
import com.edu.unicauca.gasstation.backend.sales.application.FuelProfitService;
import com.edu.unicauca.gasstation.backend.sales.domain.profit.CardPercentageMode;
import com.edu.unicauca.gasstation.backend.sales.domain.profit.FuelProfitCardSetting;
import com.edu.unicauca.gasstation.backend.sales.domain.profit.FuelProfitReport;
import com.edu.unicauca.gasstation.backend.sales.domain.reconciliation.ReconciliationValidationException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@Tag(name = "Fuel profit", description = "Monthly profit per fuel and its card commission")
@RequestMapping("/api/v1/sales/fuel-profit")
@RequiredArgsConstructor
public class FuelProfitController {

    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");

    private final FuelProfitService service;

    @GetMapping
    @Operation(summary = "Profit per fuel of a month",
            description = "month = yyyy-MM. A figure that cannot be calculated is null. Read only; the commission "
                    + "rate effective on the month's last day is used.")
    public FuelProfitReport report(@RequestParam String month) {
        return service.report(parse(month), LocalDate.now(BOGOTA));
    }

    @GetMapping("/card-setting/{month}")
    @Operation(summary = "Card percentage setting of a month",
            description = "A month without a registered setting is in REAL mode.")
    public FuelProfitCardSetting cardSetting(@PathVariable String month) {
        return service.cardSetting(parse(month));
    }

    @PutMapping("/card-setting/{month}")
    @Operation(summary = "Set the card percentage setting of a month", description = "ADMIN only.")
    public FuelProfitCardSetting saveCardSetting(@PathVariable String month,
                                                 @Valid @RequestBody FuelProfitCardSettingRequest request,
                                                 Authentication authentication) {
        SalesAdminAuthorization.requireAdmin(authentication);
        return service.saveCardSetting(new FuelProfitCardSetting(parse(month).toString(),
                CardPercentageMode.valueOf(request.mode()), request.manualPercentage()));
    }

    private static YearMonth parse(String month) {
        try {
            return YearMonth.parse(month);
        } catch (DateTimeParseException e) {
            throw new ReconciliationValidationException("month", "El mes debe tener el formato yyyy-MM");
        }
    }
}
