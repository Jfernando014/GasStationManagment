package com.edu.unicauca.gasstation.backend.sales.api;

import com.edu.unicauca.gasstation.backend.sales.api.dto.AdministrativeExpenseRequestDto;
import com.edu.unicauca.gasstation.backend.sales.api.dto.AdministrativeExpenseResponseDto;
import com.edu.unicauca.gasstation.backend.sales.domain.services.AdministrativeExpenseService;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Administrative Expenses", description = "Administrative expenses by fortnight or date (HU-36)")
@RestController
@RequestMapping("/api/v1/sales/administrative-expenses")
public class AdministrativeExpenseController {

    private final AdministrativeExpenseService service;

    public AdministrativeExpenseController(AdministrativeExpenseService service) {
        this.service = service;
    }

    @Operation(summary = "Register an administrative expense")
    @ApiResponse(responseCode = "201", description = "Expense registered")
    @PostMapping
    public ResponseEntity<AdministrativeExpenseResponseDto> register(
            @Valid @RequestBody AdministrativeExpenseRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(AdministrativeExpenseResponseDto.from(
                        service.register(request.date(), request.amount(), request.description())));
    }

    @Operation(summary = "List administrative expenses, optionally filtered by fortnight or date range")
    @ApiResponse(responseCode = "200", description = "Expenses returned")
    @GetMapping
    public ResponseEntity<List<AdministrativeExpenseResponseDto>> list(
            @Parameter(description = "Year for fortnight filter", example = "2026")
            @RequestParam(required = false) Integer year,
            @Parameter(description = "Month for fortnight filter (1-12)", example = "8")
            @RequestParam(required = false) Integer month,
            @Parameter(description = "Fortnight filter, 1 or 2", example = "1")
            @RequestParam(required = false) Integer fortnight,
            @Parameter(description = "Initial date for range filter (yyyy-MM-dd)", example = "2026-08-01")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @Parameter(description = "Final date for range filter (yyyy-MM-dd)", example = "2026-08-15")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return ResponseEntity.ok(service.listExpenses(year, month, fortnight, fromDate, toDate).stream()
                .map(AdministrativeExpenseResponseDto::from).toList());
    }

    @Operation(summary = "Delete an administrative expense by id")
    @ApiResponse(responseCode = "204", description = "Expense deleted")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
