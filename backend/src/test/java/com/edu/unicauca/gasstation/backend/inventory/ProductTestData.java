package com.edu.unicauca.gasstation.backend.inventory;

import com.edu.unicauca.gasstation.backend.inventory.domain.models.Product;

import java.math.BigDecimal;

public final class ProductTestData {

    private ProductTestData() {
    }

    public static Product product(Long id, String code, String name) {
        Product product = new Product();
        product.setId(id);
        product.setCode(code);
        product.setName(name);
        product.setCategory(ProductCategory.LUBRICANTE);
        product.setPrice(new BigDecimal("28500"));
        product.setCost(new BigDecimal("21924"));
        product.setActive(true);
        return product;
    }
}