package com.edu.unicauca.gasstation.backend.inventory.domain.services;

import com.edu.unicauca.gasstation.backend.inventory.ProductCategory;
import com.edu.unicauca.gasstation.backend.inventory.ProductInfo;
import com.edu.unicauca.gasstation.backend.inventory.ProductQuery;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.Product;
import com.edu.unicauca.gasstation.backend.inventory.domain.repositories.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Implements the public product query published to other modules.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductQueryService implements ProductQuery {

    private final ProductRepository productRepository;

    @Override
    public Optional<ProductInfo> findById(Long productId) {
        if (productId == null) {
            return Optional.empty();
        }
        return productRepository.findById(productId).map(ProductQueryService::toInfo);
    }

    @Override
    public Optional<ProductInfo> findByCode(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        return productRepository.findByCode(code.trim()).map(ProductQueryService::toInfo);
    }

    @Override
    public List<ProductInfo> findByCategory(ProductCategory category, Boolean active) {
        if (category == null) {
            return List.of();
        }
        return productRepository.findAll(category, active).stream()
                .map(ProductQueryService::toInfo)
                .toList();
    }

    private static ProductInfo toInfo(Product product) {
        return new ProductInfo(
                product.getId(),
                product.getCode(),
                product.getName(),
                product.getCategory(),
                Boolean.TRUE.equals(product.getActive()),
                product.getPrice()
        );
    }
}
