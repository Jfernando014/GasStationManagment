package com.edu.unicauca.gasstation.backend.sales.domain.models;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.UUID;

public record VendorCloseProduct(
        UUID id,
        String sku,
        String productName,
        String category,
        int unitsSold,
        BigDecimal unitPrice,
        BigDecimal totalAmount
) {
    public VendorCloseProduct {
        Objects.requireNonNull(sku, "sku is required");
        Objects.requireNonNull(productName, "productName is required");
        Objects.requireNonNull(category, "category is required");
        unitPrice = (unitPrice != null ? unitPrice : BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        totalAmount = (totalAmount != null
                ? totalAmount
                : unitPrice.multiply(BigDecimal.valueOf(unitsSold))
        ).setScale(2, RoundingMode.HALF_UP);
    }
}
