package com.edu.unicauca.gasstation.backend.incentives.infrastructure.temporary;

import com.edu.unicauca.gasstation.backend.incentives.domain.ports.AdditiveCatalogPort;
import com.edu.unicauca.gasstation.backend.incentives.domain.ports.AdditiveInfo;
import com.edu.unicauca.gasstation.backend.inventory.ProductCategory;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** TEMPORARY. Made-up catalog until inventory publishes a product query; ids match no real product. */
class TemporaryAdditiveCatalog implements AdditiveCatalogPort {

    private static final List<AdditiveInfo> PRODUCTS = List.of(
            new AdditiveInfo(9001L, "Aditivo de prueba Alfa", ProductCategory.ADITIVO, true, new BigDecimal("12000")),
            new AdditiveInfo(9002L, "Aditivo de prueba Beta", ProductCategory.ADITIVO, true, new BigDecimal("18000")),
            new AdditiveInfo(9003L, "Aditivo de prueba Gamma", ProductCategory.ADITIVO, true, new BigDecimal("9500")),
            new AdditiveInfo(9004L, "Aditivo de prueba inactivo", ProductCategory.ADITIVO, false, new BigDecimal("15000")),
            new AdditiveInfo(9005L, "Lubricante de prueba", ProductCategory.LUBRICANTE, true, new BigDecimal("45000")));

    @Override
    public Optional<AdditiveInfo> findById(Long productId) {
        return PRODUCTS.stream().filter(product -> product.id().equals(productId)).findFirst();
    }

    @Override
    public List<AdditiveInfo> listActiveAdditives() {
        return PRODUCTS.stream()
                .filter(product -> product.category() == ProductCategory.ADITIVO && product.active())
                .sorted(Comparator.comparing(AdditiveInfo::name))
                .toList();
    }
}
