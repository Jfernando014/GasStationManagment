package com.edu.unicauca.gasstation.backend.sales.api;

import com.edu.unicauca.gasstation.backend.sales.api.dto.SalesParameterRequest;
import com.edu.unicauca.gasstation.backend.sales.api.dto.SalesParameterResponse;
import com.edu.unicauca.gasstation.backend.sales.application.SalesParameterService;
import com.edu.unicauca.gasstation.backend.sales.domain.models.SalesParameterKey;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@Tag(name = "Sales parameters", description = "Configuration owned by Ventas, with effective date")
@RequestMapping("/api/v1/sales/parameters")
@RequiredArgsConstructor
public class SalesParameterController {
    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");
    private final SalesParameterService service;

    @GetMapping("/{key}")
    @Operation(summary = "Value in force on a date",
            description = "key: daily-savings | credibanco-commission-rate. Date defaults to today (Bogotá).")
    public SalesParameterResponse effective(@PathVariable String key,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return SalesParameterResponse.from(service.effective(SalesParameterKey.fromSlug(key),
                date == null ? LocalDate.now(BOGOTA) : date));
    }

    @GetMapping("/{key}/history")
    @Operation(summary = "Every value of the parameter, newest first")
    public List<SalesParameterResponse> history(@PathVariable String key) {
        return service.history(SalesParameterKey.fromSlug(key)).stream().map(SalesParameterResponse::from).toList();
    }

    @PutMapping("/{key}")
    @Operation(summary = "Set a value from a date",
            description = "ADMIN only. Registering the same date again replaces that value. The commission is a fraction (2,65 % = 0.0265).")
    public SalesParameterResponse register(@PathVariable String key, @Valid @RequestBody SalesParameterRequest request,
                                           Authentication authentication) {
        SalesAdminAuthorization.requireAdmin(authentication);
        return SalesParameterResponse.from(service.register(SalesParameterKey.fromSlug(key), request.value(),
                request.validFrom()));
    }
}
