package com.edu.unicauca.gasstation.backend.sales.api;

import com.edu.unicauca.gasstation.backend.sales.application.MonthlyReconciliationService;
import com.edu.unicauca.gasstation.backend.sales.domain.reconciliation.MonthlyReconciliation;
import com.edu.unicauca.gasstation.backend.sales.domain.reconciliation.ReconciliationValidationException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Monthly reconciliation",
        description = "C-05: Credibanco, QR and Puntos Colombia of a month with the commission")
@RequestMapping("/api/v1/sales/reconciliation/monthly")
@RequiredArgsConstructor
public class MonthlyReconciliationController {

    private final MonthlyReconciliationService service;

    @GetMapping("/{month}")
    @Operation(summary = "Monthly table of Credibanco, QR and Puntos Colombia",
            description = "month = yyyy-MM. One row per calendar day, including days without data, month totals, and "
                    + "Credibanco commission. Read only; the rate effective on the month's last day is used.")
    public MonthlyReconciliation get(@PathVariable String month) {
        return service.get(parse(month));
    }

    private static YearMonth parse(String month) {
        try {
            return YearMonth.parse(month);
        } catch (DateTimeParseException e) {
            throw new ReconciliationValidationException("month", "El mes debe tener el formato yyyy-MM");
        }
    }
}
