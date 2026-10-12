package com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.adapters;

import com.edu.unicauca.gasstation.backend.inventory.ProductCategory;
import com.edu.unicauca.gasstation.backend.inventory.ProductTestData;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.Product;
import com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.mappers.ProductPersistenceMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Import({ProductRepositoryAdapter.class, ProductPersistenceMapper.class})
@Testcontainers
class ProductRepositoryAdapterTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private ProductRepositoryAdapter adapter;

    @Test
    void saveAssignsIdAndPersistsAllFields() {
        Product saved = adapter.save(ProductTestData.product(null, "80216", "Terpel Celerity 4T Cuarto"));

        assertThat(saved.getId()).isNotNull();
        Product found = adapter.findById(saved.getId()).orElseThrow();
        assertThat(found.getCode()).isEqualTo("80216");
        assertThat(found.getName()).isEqualTo("Terpel Celerity 4T Cuarto");
        assertThat(found.getCategory()).isEqualTo(ProductCategory.LUBRICANTE);
        assertThat(found.getPrice()).isEqualByComparingTo("28500");
        assertThat(found.getCost()).isEqualByComparingTo("21924");
        assertThat(found.getActive()).isTrue();
    }

    @Test
    void saveAllowsAnUnknownCost() {
        Product product = ProductTestData.product(null, "80216", "Producto sin costo");
        product.setCost(null);

        Product saved = adapter.save(product);

        assertThat(adapter.findById(saved.getId()).orElseThrow().getCost()).isNull();
    }

    @Test
    void saveUpdatesAnExistingProductWithoutCreatingAnotherOne() {
        Product saved = adapter.save(ProductTestData.product(null, "80216", "Nombre anterior"));
        saved.setName("Nombre nuevo");
        saved.setActive(false);

        adapter.save(saved);

        assertThat(adapter.findAll(null, null)).hasSize(1);
        Product found = adapter.findById(saved.getId()).orElseThrow();
        assertThat(found.getName()).isEqualTo("Nombre nuevo");
        assertThat(found.getActive()).isFalse();
    }

    @Test
    void saveRejectsDuplicatedCode() {
        adapter.save(ProductTestData.product(null, "80216", "Producto uno"));

        assertThatThrownBy(() -> adapter.save(ProductTestData.product(null, "80216", "Producto dos")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void existsByCodeIsTrueEvenForInactiveProducts() {
        saveProduct("80216", "Producto", ProductCategory.LUBRICANTE, false);

        assertThat(adapter.existsByCode("80216")).isTrue();
        assertThat(adapter.existsByCode("00000")).isFalse();
    }

    @Test
    void existsByCodeAndIdNotIgnoresTheProductItself() {
        Product first = saveProduct("80216", "Producto uno", ProductCategory.LUBRICANTE, true);
        Product second = saveProduct("80262", "Producto dos", ProductCategory.LUBRICANTE, true);

        assertThat(adapter.existsByCodeAndIdNot("80216", first.getId())).isFalse();
        assertThat(adapter.existsByCodeAndIdNot("80216", second.getId())).isTrue();
    }

    // ---------- findAll: las 4 combinaciones de filtros (null = no filtrar) ----------

    @Test
    void findAllWithoutFiltersReturnsEveryProductOrderedByName() {
        saveCatalog();

        assertThat(names(adapter.findAll(null, null)))
                .containsExactly("Aceite A", "Aceite B", "Aditivo C");
    }

    @Test
    void findAllFiltersByCategoryOnly() {
        saveCatalog();

        assertThat(names(adapter.findAll(ProductCategory.LUBRICANTE, null)))
                .containsExactly("Aceite A", "Aceite B");
    }

    @Test
    void findAllFiltersByActiveOnly() {
        saveCatalog();

        assertThat(names(adapter.findAll(null, true))).containsExactly("Aceite A", "Aditivo C");
        assertThat(names(adapter.findAll(null, false))).containsExactly("Aceite B");
    }

    @Test
    void findAllFiltersByCategoryAndActive() {
        saveCatalog();

        assertThat(names(adapter.findAll(ProductCategory.LUBRICANTE, true))).containsExactly("Aceite A");
        assertThat(adapter.findAll(ProductCategory.ADITIVO, false)).isEmpty();
    }

    // ---------- helpers ----------

    private void saveCatalog() {
        saveProduct("1001", "Aceite A", ProductCategory.LUBRICANTE, true);
        saveProduct("1002", "Aceite B", ProductCategory.LUBRICANTE, false);
        saveProduct("1003", "Aditivo C", ProductCategory.ADITIVO, true);
    }

    private Product saveProduct(String code, String name, ProductCategory category, boolean active) {
        Product product = ProductTestData.product(null, code, name);
        product.setCategory(category);
        product.setActive(active);
        return adapter.save(product);
    }

    private List<String> names(List<Product> products) {
        return products.stream().map(product -> product.getName()).toList();
    }
}