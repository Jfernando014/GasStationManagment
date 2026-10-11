package com.edu.unicauca.gasstation.backend.inventory.api;

import com.edu.unicauca.gasstation.backend.inventory.ProductCategory;
import com.edu.unicauca.gasstation.backend.inventory.api.dto.ProductRequest;
import com.edu.unicauca.gasstation.backend.inventory.api.dto.ProductResponse;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.Product;
import com.edu.unicauca.gasstation.backend.inventory.domain.services.ProductService;
import com.edu.unicauca.gasstation.backend.inventory.infrastructure.mappers.ProductMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/inventory/products")
@Tag(name = "Products", description = "Registration and consultation of the store product catalog")
public class ProductController {
    private final ProductService productService;
    private final ProductMapper productMapper;

    public ProductController(ProductService productService, ProductMapper productMapper) {
        this.productService = productService;
        this.productMapper = productMapper;
    }

    @Operation(summary = "Register a product")
    @ApiResponse(responseCode = "201", description = "Product registered")
    @ApiResponse(responseCode = "400", description = "Invalid or missing fields")
    @ApiResponse(responseCode = "409", description = "A product with the same code already exists")
    @PostMapping
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest product) {
        Product create = productService.create(productMapper.toDomain(product));
        return ResponseEntity.status(HttpStatus.CREATED).body(productMapper.toResponse(create));
    }

    @Operation(summary = "List products ordered by name",
            description = "Optionally filtered by category and/or active status")
    @ApiResponse(responseCode = "200", description = "Products found")
    @ApiResponse(responseCode = "400", description = "Invalid category or active value")
    @GetMapping
    public List<ProductResponse> findAll(
            @Parameter(description = "Filter by category") @RequestParam(required = false) ProductCategory category,
            @Parameter(description = "Filter by active status") @RequestParam(required = false) Boolean active) {
        return productMapper.toResponseList(productService.findAll(category, active));
    }

    @Operation(summary = "Get a product by id")
    @ApiResponse(responseCode = "200", description = "Product found")
    @ApiResponse(responseCode = "404", description = "Product not found")
    @GetMapping("/{id}")
    public ProductResponse findById(@PathVariable Long id) {
        return productMapper.toResponse(productService.findById(id));
    }

    @Operation(summary = "Update a product",
            description = "All fields are editable, including the code and the active status")
    @ApiResponse(responseCode = "200", description = "Product updated")
    @ApiResponse(responseCode = "400", description = "Invalid or missing fields")
    @ApiResponse(responseCode = "404", description = "Product not found")
    @ApiResponse(responseCode = "409", description = "Another product already uses the code")
    @PutMapping("/{id}")
    public ProductResponse update(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        return productMapper.toResponse(productService.update(id, productMapper.toDomain(request)));
    }
}
