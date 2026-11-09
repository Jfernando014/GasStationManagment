package com.edu.unicauca.gasstation.backend.inventory.domain.services;

import com.edu.unicauca.gasstation.backend.inventory.ProductCategory;
import com.edu.unicauca.gasstation.backend.inventory.ProductInfo;
import com.edu.unicauca.gasstation.backend.inventory.ProductTestData;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.Product;
import com.edu.unicauca.gasstation.backend.inventory.domain.repositories.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductQueryServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductQueryService productQueryService;

    @Test
    void findByIdPublishesAllTheFields() {
        when(productRepository.findById(2L)).thenReturn(Optional.of(additive(2L, "ADITIVO-FUEL")));

        ProductInfo info = productQueryService.findById(2L).orElseThrow();

        assertThat(info.id()).isEqualTo(2L);
        assertThat(info.code()).isEqualTo("ADITIVO-FUEL");
        assertThat(info.name()).isEqualTo("Aditivo para combustible");
        assertThat(info.category()).isEqualTo(ProductCategory.ADITIVO);
        assertThat(info.active()).isTrue();
        assertThat(info.price()).isEqualByComparingTo("18000");
    }

    @Test
    void findByIdReturnsEmptyForAnUnknownOrMissingId() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThat(productQueryService.findById(99L)).isEmpty();
        assertThat(productQueryService.findById(null)).isEmpty();
    }

    @Test
    void findByCodeTrimsTheCodeAndPublishesTheProduct() {
        when(productRepository.findByCode("ADITIVO-FUEL")).thenReturn(Optional.of(additive(2L, "ADITIVO-FUEL")));

        assertThat(productQueryService.findByCode("  ADITIVO-FUEL  "))
                .get()
                .extracting(ProductInfo::id)
                .isEqualTo(2L);
        verify(productRepository).findByCode("ADITIVO-FUEL");
    }

    @Test
    void findByCodeReturnsEmptyForABlankOrUnknownCode() {
        when(productRepository.findByCode("NOPE")).thenReturn(Optional.empty());

        assertThat(productQueryService.findByCode("NOPE")).isEmpty();
        assertThat(productQueryService.findByCode("  ")).isEmpty();
        assertThat(productQueryService.findByCode(null)).isEmpty();
    }

    @Test
    void findByCategoryPassesTheFiltersAndPublishesTheProducts() {
        when(productRepository.findAll(ProductCategory.ADITIVO, true))
                .thenReturn(List.of(additive(2L, "ADITIVO-FUEL")));

        List<ProductInfo> products = productQueryService.findByCategory(ProductCategory.ADITIVO, true);

        assertThat(products).hasSize(1);
        assertThat(products.get(0).category()).isEqualTo(ProductCategory.ADITIVO);
        verify(productRepository).findAll(ProductCategory.ADITIVO, true);
    }

    @Test
    void findByCategoryWithoutActiveFilterPassesNull() {
        when(productRepository.findAll(ProductCategory.ADITIVO, null)).thenReturn(List.of());

        assertThat(productQueryService.findByCategory(ProductCategory.ADITIVO, null)).isEmpty();
        verify(productRepository).findAll(ProductCategory.ADITIVO, null);
    }

    @Test
    void findByCategoryWithoutACategoryReturnsEmpty() {
        assertThat(productQueryService.findByCategory(null, true)).isEmpty();
    }

    private static Product additive(Long id, String code) {
        Product product = ProductTestData.product(id, code, "Aditivo para combustible");
        product.setCategory(ProductCategory.ADITIVO);
        product.setPrice(new BigDecimal("18000"));
        return product;
    }
}
