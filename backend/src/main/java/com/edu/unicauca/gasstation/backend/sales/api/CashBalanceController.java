package com.edu.unicauca.gasstation.backend.sales.api;

import com.edu.unicauca.gasstation.backend.sales.api.dto.CashBalanceStepResponseDto;
import com.edu.unicauca.gasstation.backend.sales.api.dto.DominusBalanceRequestDto;
import com.edu.unicauca.gasstation.backend.sales.application.CashBalanceStepService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Cash balance", description = "Step 4 of the journal: cash balance against Dominus (C-06)")
@RestController
@RequestMapping("/api/v1/sales/cash-balance")
public class CashBalanceController {

    private final CashBalanceStepService service;

    public CashBalanceController(CashBalanceStepService service) {
        this.service = service;
    }

    @Operation(summary = "Cash balance table of a day and its difference against Dominus",
            description = "difference = Dominus accumulated balance - planilla balance, where the planilla balance is "
                    + "the cash movements balance plus the station cash (sellers' close, step 2) of every day since "
                    + "the opening day. Negative means that the planilla has more cash than Dominus. The values that "
                    + "do not exist yet come as null.")
    @GetMapping("/{date}")
    public ResponseEntity<CashBalanceStepResponseDto> get(
            @Parameter(description = "Date of the journal (yyyy-MM-dd)", example = "2026-08-01")
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(CashBalanceStepResponseDto.from(service.evaluate(date)));
    }

    @Operation(summary = "Store the Dominus accumulated balance of a day",
            description = "ADMIN only. Replaces the balance already stored. Not allowed once the journal is closed "
                    + "or step 4 is completed.")
    @PutMapping("/{date}/dominus")
    public ResponseEntity<CashBalanceStepResponseDto> saveDominus(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Valid @RequestBody DominusBalanceRequestDto request, Authentication authentication) {
        SalesAdminAuthorization.requireAdmin(authentication);
        return ResponseEntity.ok(CashBalanceStepResponseDto.from(
                service.saveDominusBalance(date, request.accumulatedBalance())));
    }
}
