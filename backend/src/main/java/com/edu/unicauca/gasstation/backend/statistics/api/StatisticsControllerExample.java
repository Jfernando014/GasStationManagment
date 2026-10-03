package com.edu.unicauca.gasstation.backend.statistics.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Example REST Controller for statistics module.
 * Documented with Swagger OpenAPI annotations.
 */
@Tag(name = "Statistics", description = "Operations related to statistics")
@RestController
@RequestMapping("/api/v1/statistics")
public class StatisticsControllerExample {

    @Operation(summary = "Get example statistics status")
    @GetMapping("/example")
    public ResponseEntity<String> getExample() {
        return ResponseEntity.ok("Statistics module endpoint is active");
    }
}
