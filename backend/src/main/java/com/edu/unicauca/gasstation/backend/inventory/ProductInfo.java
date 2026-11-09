package com.edu.unicauca.gasstation.backend.inventory;

import java.math.BigDecimal;

/**
 * Product as other modules need it.
 *
 * <p>It is intentionally a public value object: consumers such as {@code incentives} map it into
 * their own records, so {@code inventory} never depends on them.
 *
 * @param id       database identifier
 * @param code     business code; sales sends this code when closing a journal
 * @param name     display name
 * @param category product category
 * @param active   whether the product can still be sold
 * @param price    sale price of one unit
 */
public record ProductInfo(
        Long id,
        String code,
        String name,
        ProductCategory category,
        boolean active,
        BigDecimal price
) {}
