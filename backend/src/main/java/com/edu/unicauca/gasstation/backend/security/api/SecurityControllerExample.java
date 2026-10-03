package com.edu.unicauca.gasstation.backend.security.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Example REST Controller for security module.
 * Documented with Swagger OpenAPI annotations.
 */
@Tag(name = "Security", description = "Operations related to security")
@RestController
@RequestMapping("/api/v1/security")
public class SecurityControllerExample {

    @Operation(summary = "Get example security status")
    @GetMapping("/example")
    public ResponseEntity<String> getExample() {
        return ResponseEntity.ok("Security module endpoint is active");
    }
}
