package com.edu.unicauca.gasstation.backend.workers.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Example REST Controller for workers module.
 * Documented with Swagger OpenAPI annotations.
 */
@Tag(name = "Worker", description = "Operations related to workers")
@RestController
@RequestMapping("/api/v1/workers")
public class WorkerControllerExample {

    @Operation(summary = "Get example workers status")
    @GetMapping("/example")
    public ResponseEntity<String> getExample() {
        return ResponseEntity.ok("Worker module endpoint is active");
    }
}
