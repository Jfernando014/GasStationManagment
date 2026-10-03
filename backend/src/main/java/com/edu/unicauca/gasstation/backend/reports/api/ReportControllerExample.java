package com.edu.unicauca.gasstation.backend.reports.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Example REST Controller for reports module.
 * Documented with Swagger OpenAPI annotations.
 */
@Tag(name = "Report", description = "Operations related to reports")
@RestController
@RequestMapping("/api/v1/reports")
public class ReportControllerExample {

    @Operation(summary = "Get example reports status")
    @GetMapping("/example")
    public ResponseEntity<String> getExample() {
        return ResponseEntity.ok("Report module endpoint is active");
    }
}
