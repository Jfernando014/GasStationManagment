package com.edu.unicauca.gasstation.backend.inventory;

import com.edu.unicauca.gasstation.backend.inventory.domain.models.Product;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.StockLocation;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.StockMovement;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.StockMovementType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public final class StockTestData {

    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 8, 5, 10, 0);

    private StockTestData() {
    }

    public static Product product(Long id, String code, ProductCategory category, boolean active) {
        Product product = new Product();
        product.setId(id);
        product.setCode(code);
        product.setName("Product " + code);
        product.setCategory(category);
        product.setPrice(new BigDecimal("10.00"));
        product.setActive(active);
        return product;
    }

    public static StockMovement movement(Long productId, LocalDate date, StockLocation location,
                                         StockMovementType type, int quantity) {
        StockMovement movement = new StockMovement();
        movement.setProductId(productId);
        movement.setJournalDate(date);
        movement.setLocation(location);
        movement.setMovementType(type);
        movement.setQuantity(quantity);
        movement.setCreatedAt(CREATED_AT);
        return movement;
    }
}