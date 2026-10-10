package com.edu.unicauca.gasstation.backend.sales.api;

import com.edu.unicauca.gasstation.backend.sales.api.dto.BalanceResponseDto;
import com.edu.unicauca.gasstation.backend.sales.api.dto.SavingsFundMovementRequestDto;
import com.edu.unicauca.gasstation.backend.sales.api.dto.SavingsFundMovementResponseDto;
import com.edu.unicauca.gasstation.backend.sales.domain.services.SavingsFundService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Savings Fund", description = "Daily contributions, payments and balance of the savings fund (C-06)")
@RestController
@RequestMapping("/api/v1/sales/savings-fund")
public class SavingsFundController {

    private final SavingsFundService savingsFundService;

    public SavingsFundController(SavingsFundService savingsFundService) {
        this.savingsFundService = savingsFundService;
    }

    @Operation(summary = "Register the daily contribution (one per date)")
    @ApiResponse(responseCode = "201", description = "Contribution registered")
    @ApiResponse(responseCode = "400", description = "Invalid data")
    @ApiResponse(responseCode = "409", description = "That date already has a contribution")
    @PostMapping("/contributions")
    public ResponseEntity<SavingsFundMovementResponseDto> registerContribution(
            @Valid @RequestBody SavingsFundMovementRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(SavingsFundMovementResponseDto.from(
                savingsFundService.registerContribution(request.date(), request.amount(), request.description())));
    }

    @Operation(summary = "Register a payment taken from the fund")
    @ApiResponse(responseCode = "201", description = "Payment registered")
    @ApiResponse(responseCode = "400", description = "Invalid data")
    @ApiResponse(responseCode = "409", description = "Payment exceeds the fund balance")
    @PostMapping("/payments")
    public ResponseEntity<SavingsFundMovementResponseDto> registerPayment(
            @Valid @RequestBody SavingsFundMovementRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(SavingsFundMovementResponseDto.from(
                savingsFundService.registerPayment(request.date(), request.amount(), request.description())));
    }

    @Operation(summary = "Fund balance at a date (includes that date)")
    @ApiResponse(responseCode = "200", description = "Balance returned")
    @GetMapping("/balance")
    public ResponseEntity<BalanceResponseDto> getBalance(
            @Parameter(description = "Date (yyyy-MM-dd)", example = "2026-07-31")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(new BalanceResponseDto(date, savingsFundService.getBalanceAt(date)));
    }

    @Operation(summary = "List all fund movements, newest first")
    @ApiResponse(responseCode = "200", description = "Movements returned")
    @GetMapping("/movements")
    public ResponseEntity<List<SavingsFundMovementResponseDto>> listMovements() {
        return ResponseEntity.ok(savingsFundService.listMovements().stream()
                .map(SavingsFundMovementResponseDto::from).toList());
    }
}
