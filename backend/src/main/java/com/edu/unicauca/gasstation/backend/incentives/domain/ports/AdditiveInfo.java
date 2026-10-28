package com.edu.unicauca.gasstation.backend.incentives.domain.ports;

import com.edu.unicauca.gasstation.backend.inventory.ProductCategory;
import java.math.BigDecimal;

/** Catalog product as incentives needs it; {@code price} is the sale price of one unit. */
public record AdditiveInfo(Long id, String name, ProductCategory category, boolean active, BigDecimal price) {
}
