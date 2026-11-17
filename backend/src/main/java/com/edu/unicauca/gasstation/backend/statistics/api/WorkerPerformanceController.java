package com.edu.unicauca.gasstation.backend.statistics.api;

import com.edu.unicauca.gasstation.backend.statistics.application.WorkerPerformanceService;
import com.edu.unicauca.gasstation.backend.statistics.domain.calculation.WorkerPerformance.Report;
import java.util.Objects;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Historical worker performance endpoint backed by confirmed shift sales history. */
@RestController
@RequestMapping("/api/v1/statistics/workers")
public class WorkerPerformanceController {
    private final WorkerPerformanceService performance;

    public WorkerPerformanceController(WorkerPerformanceService performance) {
        this.performance = Objects.requireNonNull(performance, "performance");
    }

    @GetMapping("/{workerId}/performance")
    public Report getPerformance(@PathVariable UUID workerId, Authentication authentication) {
        requireAdmin(authentication);
        return performance.report(workerId);
    }

    private static void requireAdmin(Authentication authentication) {
        if (authentication == null || authentication.getAuthorities().stream()
                .noneMatch(authority -> "ADMIN".equals(authority.getAuthority()))) {
            throw new AccessDeniedException("Only ADMIN can inspect worker performance");
        }
    }
}
