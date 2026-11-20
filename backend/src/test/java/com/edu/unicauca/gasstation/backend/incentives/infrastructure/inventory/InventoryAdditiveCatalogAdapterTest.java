package com.edu.unicauca.gasstation.backend.incentives.infrastructure.inventory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.edu.unicauca.gasstation.backend.incentives.domain.ports.AdditiveInfo;
import com.edu.unicauca.gasstation.backend.inventory.ProductCategory;
import com.edu.unicauca.gasstation.backend.inventory.ProductInfo;
import com.edu.unicauca.gasstation.backend.inventory.ProductQuery;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * The code that sales reports as SKU is the business code of the inventory product. The adapter turns it into an
 * additive of the incentives module, and a product of any other category is "not found" for incentives.
 */
@ExtendWith(MockitoExtension.class)
class InventoryAdditiveCatalogAdapterTest {

    private static final ProductInfo GASOLINE_ADDITIVE = new ProductInfo(11L, "RUTA75-GAS",
            "Aditivo para gasolina Ruta 75", ProductCategory.ADITIVO, true, new BigDecimal("4500.00"));
    private static final ProductInfo DISCONTINUED_ADDITIVE = new ProductInfo(20L, "VIEJO",
            "Aditivo descontinuado", ProductCategory.ADITIVO, false, new BigDecimal("3000.00"));
    private static final ProductInfo LUBRICANT = new ProductInfo(30L, "LUB-15W40", "Aceite 15W40",
            ProductCategory.LUBRICANTE, true, new BigDecimal("45000.00"));

    @Mock
    private ProductQuery productQuery;

    @InjectMocks
    private InventoryAdditiveCatalogAdapter adapter;

    @Test
    void theSkuOfASaleIsResolvedToTheAdditiveOfThatCode() {
        when(productQuery.findByCode("RUTA75-GAS")).thenReturn(Optional.of(GASOLINE_ADDITIVE));

        Optional<AdditiveInfo> found = adapter.findByCode("RUTA75-GAS");

        assertThat(found).contains(new AdditiveInfo(11L, "Aditivo para gasolina Ruta 75", true,
                new BigDecimal("4500.00")));
    }

    @Test
    void theCodeIsPassedToInventoryAsItComesBecauseInventoryTrimsIt() {
        when(productQuery.findByCode(" RUTA75-GAS ")).thenReturn(Optional.of(GASOLINE_ADDITIVE));

        assertThat(adapter.findByCode(" RUTA75-GAS ")).map(AdditiveInfo::id).contains(11L);
        verify(productQuery).findByCode(" RUTA75-GAS ");
    }

    @Test
    void anSkuOfAnotherCategoryIsNotAnAdditive() {
        when(productQuery.findByCode("LUB-15W40")).thenReturn(Optional.of(LUBRICANT));

        assertThat(adapter.findByCode("LUB-15W40")).isEmpty();
    }

    @Test
    void anSkuNobodyKnowsIsNotFound() {
        when(productQuery.findByCode("NO-EXISTE")).thenReturn(Optional.empty());

        assertThat(adapter.findByCode("NO-EXISTE")).isEmpty();
    }

    @Test
    void aDiscontinuedAdditiveIsStillFoundByCodeSoItsOldSalesKeepTheirValue() {
        when(productQuery.findByCode("VIEJO")).thenReturn(Optional.of(DISCONTINUED_ADDITIVE));

        assertThat(adapter.findByCode("VIEJO")).hasValueSatisfying(additive -> {
            assertThat(additive.id()).isEqualTo(20L);
            assertThat(additive.active()).isFalse();
        });
    }

    @Test
    void anAdditiveIsFoundByIdWithItsPrice() {
        when(productQuery.findById(11L)).thenReturn(Optional.of(GASOLINE_ADDITIVE));

        assertThat(adapter.findById(11L)).hasValueSatisfying(additive -> {
            assertThat(additive.name()).isEqualTo("Aditivo para gasolina Ruta 75");
            assertThat(additive.price()).isEqualByComparingTo("4500");
            assertThat(additive.active()).isTrue();
        });
    }

    @Test
    void aProductOfAnotherCategoryIsNotFoundById() {
        when(productQuery.findById(30L)).thenReturn(Optional.of(LUBRICANT));

        assertThat(adapter.findById(30L)).isEmpty();
    }

    @Test
    void aProductIdNobodyKnowsIsNotFound() {
        when(productQuery.findById(999L)).thenReturn(Optional.empty());

        assertThat(adapter.findById(999L)).isEmpty();
    }

    @Test
    void theListAsksInventoryOnlyForTheActiveAdditivesAndKeepsItsOrder() {
        ProductInfo octane = new ProductInfo(12L, "RUTA75-OCT", "Elevador de octanaje Ruta 75",
                ProductCategory.ADITIVO, true, new BigDecimal("13500.00"));
        when(productQuery.findByCategory(ProductCategory.ADITIVO, true)).thenReturn(List.of(GASOLINE_ADDITIVE, octane));

        List<AdditiveInfo> additives = adapter.listActiveAdditives();

        assertThat(additives).extracting(AdditiveInfo::id).containsExactly(11L, 12L);
        verify(productQuery).findByCategory(ProductCategory.ADITIVO, true);
        verifyNoMoreInteractions(productQuery);
    }

    @Test
    void withoutActiveAdditivesTheListIsEmpty() {
        when(productQuery.findByCategory(ProductCategory.ADITIVO, true)).thenReturn(List.of());

        assertThat(adapter.listActiveAdditives()).isEmpty();
    }
}
