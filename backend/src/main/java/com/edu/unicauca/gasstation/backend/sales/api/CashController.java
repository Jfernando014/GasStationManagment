package com.edu.unicauca.gasstation.backend.sales.api;

import com.edu.unicauca.gasstation.backend.sales.api.dto.BalanceResponseDto;
import com.edu.unicauca.gasstation.backend.sales.api.dto.CashMovementRequestDto;
import com.edu.unicauca.gasstation.backend.sales.api.dto.CashMovementResponseDto;
import com.edu.unicauca.gasstation.backend.sales.domain.services.CashService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Cash", description = "Cash movements and cash (Dominus) balance by date (C-06)")
@RestController
@RequestMapping("/api/v1/sales/cash")
public class CashController {

    private final CashService cashService;

    public CashController(CashService cashService) {
        this.cashService = cashService;
    }

    @Operation(summary = "Register a cash movement (initial balance, deposit or adjustment)")
    @ApiResponse(responseCode = "201", description = "Movement registered",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = CashMovementResponseDto.class)))
    @ApiResponse(responseCode = "400", description = "Invalid request body or amount for the type",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping("/movements")
    public ResponseEntity<CashMovementResponseDto> register(@Valid @RequestBody CashMovementRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(CashMovementResponseDto.from(
                cashService.register(request.type(), request.date(), request.amount(), request.description())));
    }

    @Operation(summary = "Cash (Dominus) balance at a date (includes that date)")
    @GetMapping("/balance")
    public ResponseEntity<BalanceResponseDto> getBalance(
            @Parameter(description = "Date (yyyy-MM-dd)", example = "2026-08-01")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(new BalanceResponseDto(date, cashService.getBalanceAt(date)));
    }

    @Operation(summary = "List all cash movements, newest first")
    @GetMapping("/movements")
    public ResponseEntity<List<CashMovementResponseDto>> listMovements() {
        return ResponseEntity.ok(cashService.listMovements().stream()
                .map(CashMovementResponseDto::from).toList());
    }
}
