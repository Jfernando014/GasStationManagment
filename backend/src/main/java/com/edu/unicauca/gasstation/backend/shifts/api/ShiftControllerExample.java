package com.edu.unicauca.gasstation.backend.shifts.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Example REST Controller for shifts module.
 * Documented with Swagger OpenAPI annotations.
 */
@Tag(name = "Shift", description = "Operations related to shifts")
@RestController
@RequestMapping("/api/v1/shifts")
public class ShiftControllerExample {

    @Operation(summary = "Get example shifts status")
    @GetMapping("/example")
    public ResponseEntity<String> getExample() {
        return ResponseEntity.ok("Shift module endpoint is active");
    }
}
