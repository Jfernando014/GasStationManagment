package com.edu.unicauca.gasstation.backend.sales.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Example REST Controller for sales module.
 * Documented with Swagger OpenAPI annotations.
 */
@Tag(name = "Sale", description = "Operations related to sales")
@RestController
@RequestMapping("/api/v1/sales")
public class SaleControllerExample {

    @Operation(summary = "Get example sales status")
    @GetMapping("/example")
    public ResponseEntity<String> getExample() {
        return ResponseEntity.ok("Sale module endpoint is active");
    }
}
