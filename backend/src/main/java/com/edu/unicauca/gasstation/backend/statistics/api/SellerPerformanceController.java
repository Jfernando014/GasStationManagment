package com.edu.unicauca.gasstation.backend.statistics.api;

import com.edu.unicauca.gasstation.backend.statistics.application.SellerPerformanceService;
import com.edu.unicauca.gasstation.backend.statistics.domain.calculation.SellerPerformance.*;
import io.swagger.v3.oas.annotations.Operation;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/statistics/seller-performance")
public class SellerPerformanceController {
    private final SellerPerformanceService service;

    public SellerPerformanceController(SellerPerformanceService service) { this.service = service; }

    @GetMapping
    @Operation(summary = "Seller performance from confirmed shift history", description = "ADMIN only. Inclusive operational dates. Returns compliance, equivalent observed-shift comparisons and explicit normalization. Missing assigned targets, historical classifications or approved multipliers remain UNAVAILABLE. No client sales or parameter uploads.")
    public Report read(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam Measure measure,
            @RequestParam(defaultValue = "2") int outputScale, Authentication authentication) {
        requireAdmin(authentication);
        return service.report(new Window(from, to), measure, outputScale);
    }

    // Method security is not enabled globally; enforce the same ADMIN authority convention locally.
    private static void requireAdmin(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || new AuthenticationTrustResolverImpl().isAnonymous(authentication)) {
            throw new AuthenticationCredentialsNotFoundException("Authentication required");
        }
        if (authentication.getAuthorities().stream().noneMatch(a -> "ADMIN".equals(a.getAuthority()))) {
            throw new AccessDeniedException("ADMIN authority required");
        }
    }
}
