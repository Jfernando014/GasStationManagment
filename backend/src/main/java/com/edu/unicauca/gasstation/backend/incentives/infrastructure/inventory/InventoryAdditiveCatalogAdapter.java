package com.edu.unicauca.gasstation.backend.incentives.infrastructure.inventory;

import com.edu.unicauca.gasstation.backend.incentives.domain.ports.AdditiveCatalogPort;
import com.edu.unicauca.gasstation.backend.incentives.domain.ports.AdditiveInfo;
import com.edu.unicauca.gasstation.backend.inventory.ProductCategory;
import com.edu.unicauca.gasstation.backend.inventory.ProductInfo;
import com.edu.unicauca.gasstation.backend.inventory.ProductQuery;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Serves the additive catalog from the product query that inventory publishes. */
@Component
@RequiredArgsConstructor
public class InventoryAdditiveCatalogAdapter implements AdditiveCatalogPort {

    private final ProductQuery productQuery;

    // A product of another category is not an additive here, so it is reported as not found
    @Override
    public Optional<AdditiveInfo> findById(Long productId) {
        return productQuery.findById(productId).filter(InventoryAdditiveCatalogAdapter::isAdditive)
                .map(InventoryAdditiveCatalogAdapter::toInfo);
    }

    @Override
    public Optional<AdditiveInfo> findByCode(String code) {
        return productQuery.findByCode(code).filter(InventoryAdditiveCatalogAdapter::isAdditive)
                .map(InventoryAdditiveCatalogAdapter::toInfo);
    }

    @Override
    public List<AdditiveInfo> listActiveAdditives() {
        return productQuery.findByCategory(ProductCategory.ADITIVO, true).stream()
                .map(InventoryAdditiveCatalogAdapter::toInfo)
                .toList();
    }

    private static boolean isAdditive(ProductInfo product) {
        return product.category() == ProductCategory.ADITIVO;
    }

    private static AdditiveInfo toInfo(ProductInfo product) {
        return new AdditiveInfo(product.id(), product.name(), product.active(), product.price());
    }
}
