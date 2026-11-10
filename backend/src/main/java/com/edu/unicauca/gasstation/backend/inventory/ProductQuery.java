package com.edu.unicauca.gasstation.backend.inventory;

import java.util.List;
import java.util.Optional;

/**
 * Public read query over the product catalog, published for other modules.
 *
 * <p>{@code incentives} uses it to resolve the units sold by code into a product id and to check
 * that the product exists and is an additive. It reads the real catalog, so the temporary
 * in-memory catalog can be retired.
 */
public interface ProductQuery {

    /**
     * Returns any product, active or not, by its database id.
     */
    Optional<ProductInfo> findById(Long productId);

    /**
     * Returns any product, active or not, by its code. The code is trimmed before searching.
     */
    Optional<ProductInfo> findByCode(String code);

    /**
     * Returns the products of a category ordered by name. When {@code active} is null every product
     * of the category is returned; otherwise only the ones matching the flag.
     */
    List<ProductInfo> findByCategory(ProductCategory category, Boolean active);
}
