package com.edu.unicauca.gasstation.backend.sales.api;

import com.edu.unicauca.gasstation.backend.sales.api.dto.ElectronicPaymentsStatus;
import com.edu.unicauca.gasstation.backend.sales.api.dto.ElectronicVoucherRequest;
import com.edu.unicauca.gasstation.backend.sales.application.ElectronicPaymentsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.UUID;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Electronic payments", description = "Step 3: Credibanco, QR (Redeban) and Puntos Colombia")
@RequestMapping("/api/v1/sales/electronic-payments")
@RequiredArgsConstructor
public class ElectronicPaymentsController {
    private final ElectronicPaymentsService service;

    @GetMapping("/{date}")
    @Operation(summary = "Complete electronic-payment reconciliation status for one journal date")
    public ElectronicPaymentsStatus getStatus(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return service.getStatus(date);
    }

    @PutMapping("/{date}/vendors/{workerId}/vouchers")
    @Operation(summary = "Save a vendor's six Credibanco voucher amounts")
    public ElectronicPaymentsStatus saveVouchers(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @PathVariable UUID workerId,
            @Valid @RequestBody ElectronicVoucherRequest request,
            Authentication authentication) {
        SalesAdminAuthorization.requireAdmin(authentication);
        return service.saveVouchers(date, workerId, request);
    }
}
