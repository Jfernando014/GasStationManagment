package com.edu.unicauca.gasstation.backend.incentives.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Example REST Controller for incentives module.
 * Documented with Swagger OpenAPI annotations.
 */
@Tag(name = "Incentive", description = "Operations related to incentives")
@RestController
@RequestMapping("/api/v1/incentives")
public class IncentiveControllerExample {

    @Operation(summary = "Get example incentives status")
    @GetMapping("/example")
    public ResponseEntity<String> getExample() {
        return ResponseEntity.ok("Incentive module endpoint is active");
    }
}
