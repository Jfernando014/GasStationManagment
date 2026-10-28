package com.edu.unicauca.gasstation.backend.sales.api;

import com.edu.unicauca.gasstation.backend.sales.api.dto.CredibancoVendorRequest;
import com.edu.unicauca.gasstation.backend.sales.api.dto.PreviousMonthRequest;
import com.edu.unicauca.gasstation.backend.sales.application.CredibancoReconciliationService;
import com.edu.unicauca.gasstation.backend.sales.domain.reconciliation.CredibancoDayResult;
import com.edu.unicauca.gasstation.backend.sales.domain.reconciliation.PeriodTotals;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@Tag(name = "Credibanco reconciliation", description = "HU-34: vouchers by franchise per vendor against the planilla")
@RequestMapping("/api/v1/sales/reconciliation/credibanco")
@RequiredArgsConstructor
public class CredibancoReconciliationController {
    private final CredibancoReconciliationService service;

    @GetMapping("/{date}")
    @Operation(summary = "Result of the day",
            description = "Per-vendor check, TOTAL EDS (Mastercard + Visa), accounting report, month to date, previous month and commission.")
    public CredibancoDayResult get(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return service.get(date);
    }

    @PutMapping("/{date}/vendors/{workerId}")
    @Operation(summary = "Register a vendor's vouchers and planilla figures",
            description = "ADMIN only. Replaces the vendor's row of the day and returns the recalculated day. "
                    + "A discrepancy never blocks: it is reported with the exact difference.")
    public CredibancoDayResult saveVendor(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                          @PathVariable UUID workerId,
                                          @Valid @RequestBody CredibancoVendorRequest request,
                                          Authentication authentication) {
        SalesAdminAuthorization.requireAdmin(authentication);
        return service.saveVendor(date, request.toDomain(workerId));
    }

    @DeleteMapping("/{date}/vendors/{workerId}")
    @Operation(summary = "Remove a vendor from the day", description = "ADMIN only. Returns the recalculated day.")
    public CredibancoDayResult removeVendor(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                            @PathVariable UUID workerId, Authentication authentication) {
        SalesAdminAuthorization.requireAdmin(authentication);
        return service.removeVendor(date, workerId);
    }

    @PutMapping("/previous-month/{month}")
    @Operation(summary = "Opening figures of a month (VALORES MES ANTERIOR)",
            description = "ADMIN only. month = yyyy-MM, the month the figures belong to. Used while that month has no daily rows.")
    public ResponseEntity<Void> saveBaseline(@PathVariable String month, @Valid @RequestBody PreviousMonthRequest request,
                                             Authentication authentication) {
        SalesAdminAuthorization.requireAdmin(authentication);
        YearMonth parsed;
        try {
            parsed = YearMonth.parse(month);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("El mes debe tener el formato yyyy-MM");
        }
        service.saveBaseline(parsed, PeriodTotals.of(request.mastercard(), request.visa(), request.amex(),
                request.qr(), "BASELINE"));
        return ResponseEntity.noContent().build();
    }
}
