package com.edu.unicauca.gasstation.backend.inventory.domain.services;

import com.edu.unicauca.gasstation.backend.inventory.ProductCategory;
import com.edu.unicauca.gasstation.backend.inventory.ProductTestData;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.Product;
import com.edu.unicauca.gasstation.backend.inventory.domain.repositories.ProductRepository;
import com.edu.unicauca.gasstation.backend.inventory.exception.DuplicateProductCodeException;
import com.edu.unicauca.gasstation.backend.inventory.exception.ProductNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    void createTrimsCodeAndNameAndSaves() {
        Product product = ProductTestData.product(null, "  80216 ", "  Terpel Celerity 4T Cuarto ");
        when(productRepository.existsByCode("80216")).thenReturn(false);
        when(productRepository.save(product)).thenReturn(product);

        Product created = productService.create(product);

        assertThat(created.getCode()).isEqualTo("80216");
        assertThat(created.getName()).isEqualTo("Terpel Celerity 4T Cuarto");
        verify(productRepository).save(product);
    }

    @Test
    void createThrowsWhenCodeAlreadyExists() {
        Product product = ProductTestData.product(null, "80216", "Producto");
        when(productRepository.existsByCode("80216")).thenReturn(true);

        assertThatThrownBy(() -> productService.create(product))
                .isInstanceOf(DuplicateProductCodeException.class)
                .hasMessageContaining("80216");
        verify(productRepository, never()).save(any());
    }

    @Test
    void createThrowsWhenCodeIsUsedByAnInactiveProduct() {
        // existsByCode no distingue activos de inactivos: el código sigue ocupado
        Product product = ProductTestData.product(null, "80216", "Producto");
        when(productRepository.existsByCode("80216")).thenReturn(true);

        assertThatThrownBy(() -> productService.create(product))
                .isInstanceOf(DuplicateProductCodeException.class);
    }

    @Test
    void findByIdReturnsProductWhenItExists() {
        Product product = ProductTestData.product(1L, "80216", "Producto");
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        assertThat(productService.findById(1L)).isSameAs(product);
    }

    @Test
    void findByIdThrowsWhenProductDoesNotExist() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.findById(99L))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void findAllPassesTheFiltersToTheRepository() {
        Product product = ProductTestData.product(1L, "80216", "Producto");
        when(productRepository.findAll(ProductCategory.LUBRICANTE, true)).thenReturn(List.of(product));

        assertThat(productService.findAll(ProductCategory.LUBRICANTE, true)).containsExactly(product);
    }

    @Test
    void findAllWithoutFiltersPassesNulls() {
        when(productRepository.findAll(null, null)).thenReturn(List.of());

        assertThat(productService.findAll(null, null)).isEmpty();
        verify(productRepository).findAll(null, null);
    }

    @Test
    void updateChangesAllFieldsIncludingCodeAndActive() {
        Product existing = ProductTestData.product(1L, "80216", "Nombre anterior");
        Product changes = ProductTestData.product(null, " 80217 ", " Nombre nuevo ");
        changes.setCategory(ProductCategory.ADITIVO);
        changes.setPrice(new BigDecimal("30500"));
        changes.setCost(null);
        changes.setActive(false);
        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(productRepository.existsByCodeAndIdNot("80217", 1L)).thenReturn(false);
        when(productRepository.save(existing)).thenReturn(existing);

        Product updated = productService.update(1L, changes);

        assertThat(updated.getId()).isEqualTo(1L);
        assertThat(updated.getCode()).isEqualTo("80217");
        assertThat(updated.getName()).isEqualTo("Nombre nuevo");
        assertThat(updated.getCategory()).isEqualTo(ProductCategory.ADITIVO);
        assertThat(updated.getPrice()).isEqualByComparingTo("30500");
        assertThat(updated.getCost()).isNull();
        assertThat(updated.getActive()).isFalse();
    }

    @Test
    void updateKeepingTheSameCodeIsNotADuplicate() {
        Product existing = ProductTestData.product(1L, "80216", "Producto");
        Product changes = ProductTestData.product(null, "80216", "Producto renombrado");
        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(productRepository.existsByCodeAndIdNot("80216", 1L)).thenReturn(false);
        when(productRepository.save(existing)).thenReturn(existing);

        assertThat(productService.update(1L, changes).getName()).isEqualTo("Producto renombrado");
    }

    @Test
    void updateThrowsWhenAnotherProductUsesTheCode() {
        Product existing = ProductTestData.product(1L, "80216", "Producto");
        Product changes = ProductTestData.product(null, "80262", "Producto");
        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(productRepository.existsByCodeAndIdNot("80262", 1L)).thenReturn(true);

        assertThatThrownBy(() -> productService.update(1L, changes))
                .isInstanceOf(DuplicateProductCodeException.class)
                .hasMessageContaining("80262");
        verify(productRepository, never()).save(any());
    }

    @Test
    void updateThrowsWhenProductDoesNotExist() {
        Product changes = ProductTestData.product(null, "80216", "Producto");
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.update(99L, changes))
                .isInstanceOf(ProductNotFoundException.class);
        verify(productRepository, never()).save(any());
    }
}