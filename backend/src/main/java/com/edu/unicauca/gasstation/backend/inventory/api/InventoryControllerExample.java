package com.edu.unicauca.gasstation.backend.inventory.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Example REST Controller for inventory module.
 * Documented with Swagger OpenAPI annotations.
 */
@Tag(name = "Inventory", description = "Operations related to inventory")
@RestController
@RequestMapping("/api/v1/inventory")
public class InventoryControllerExample {

    @Operation(summary = "Get example inventory status")
    @GetMapping("/example")
    public ResponseEntity<String> getExample() {
        return ResponseEntity.ok("Inventory module endpoint is active");
    }
}
