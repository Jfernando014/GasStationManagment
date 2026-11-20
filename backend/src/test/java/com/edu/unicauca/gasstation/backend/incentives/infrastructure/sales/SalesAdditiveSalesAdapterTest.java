package com.edu.unicauca.gasstation.backend.incentives.infrastructure.sales;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.when;

import com.edu.unicauca.gasstation.backend.incentives.domain.models.AdditiveSaleUnits;
import com.edu.unicauca.gasstation.backend.sales.SellerCloseQuery;
import com.edu.unicauca.gasstation.backend.sales.SellerCloseQuery.ProductSaleSummary;
import com.edu.unicauca.gasstation.backend.sales.SellerCloseQuery.VendorCloseSummary;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** What the additive units of a closed date are made of, read from the seller closes that sales publishes. */
@ExtendWith(MockitoExtension.class)
class SalesAdditiveSalesAdapterTest {

    private static final LocalDate DATE = LocalDate.of(2026, 4, 18);

    @Mock
    private SellerCloseQuery sellerCloseQuery;

    @InjectMocks
    private SalesAdditiveSalesAdapter adapter;

    private static ProductSaleSummary product(String sku, String name, String category, int units) {
        return new ProductSaleSummary(sku, name, category, units, BigDecimal.valueOf(4_500),
                BigDecimal.valueOf(4_500L * units));
    }

    private static VendorCloseSummary close(String document, String name, String shift,
                                            ProductSaleSummary... products) {
        BigDecimal zero = BigDecimal.ZERO;
        return new VendorCloseSummary(DATE, document, name, shift, 1, zero, zero, zero, zero, zero, zero, zero, zero,
                zero, zero, zero, zero, zero, "OK", List.of(products));
    }

    private void givenCloses(VendorCloseSummary... closes) {
        when(sellerCloseQuery.findVendorClosesByDate(DATE)).thenReturn(List.of(closes));
    }

    @Test
    void aDateWithoutSellerClosesHasNoUnits() {
        givenCloses();

        assertThat(adapter.findByDate(DATE)).isEmpty();
    }

    @Test
    void eachSellerAndAdditiveGetsItsOwnRowWithTheDateOfTheJournal() {
        givenCloses(
                close("1001", "ANA MILENA VILLAMIL", "DIA",
                        product("RUTA75-GAS", "Aditivo para gasolina Ruta 75", "ADITIVO", 6),
                        product("RUTA75-OCT", "Elevador de octanaje Ruta 75", "ADITIVO", 7)),
                close("1002", "CARLOS ANDRÉS PAREDES", "NOCHE",
                        product("RUTA75-GAS", "Aditivo para gasolina Ruta 75", "ADITIVO", 16)));

        List<AdditiveSaleUnits> units = adapter.findByDate(DATE);

        assertThat(units).extracting(AdditiveSaleUnits::journalDate, AdditiveSaleUnits::vendorDocument,
                        AdditiveSaleUnits::sku, AdditiveSaleUnits::units)
                .containsExactlyInAnyOrder(
                        tuple(DATE, "1001", "RUTA75-GAS", 6),
                        tuple(DATE, "1001", "RUTA75-OCT", 7),
                        tuple(DATE, "1002", "RUTA75-GAS", 16));
    }

    @Test
    void theSameSellerWithSeveralClosesInTheDayAddsTheUnitsOfTheSameSku() {
        givenCloses(
                close("1001", "ANA MILENA VILLAMIL", "DIA",
                        product("RUTA75-GAS", "Aditivo para gasolina Ruta 75", "ADITIVO", 4)),
                close("1001", "ANA MILENA VILLAMIL", "NOCHE",
                        product("RUTA75-GAS", "Aditivo para gasolina Ruta 75", "ADITIVO", 3),
                        product("RUTA75-OCT", "Elevador de octanaje Ruta 75", "ADITIVO", 2)));

        List<AdditiveSaleUnits> units = adapter.findByDate(DATE);

        assertThat(units).extracting(AdditiveSaleUnits::sku, AdditiveSaleUnits::units)
                .containsExactlyInAnyOrder(tuple("RUTA75-GAS", 7), tuple("RUTA75-OCT", 2));
    }

    @Test
    void theSameSkuOfTwoDifferentSellersIsNotMerged() {
        givenCloses(
                close("1001", "ANA MILENA VILLAMIL", "DIA",
                        product("RUTA75-GAS", "Aditivo para gasolina Ruta 75", "ADITIVO", 4)),
                close("1002", "CARLOS ANDRÉS PAREDES", "DIA",
                        product("RUTA75-GAS", "Aditivo para gasolina Ruta 75", "ADITIVO", 5)));

        assertThat(adapter.findByDate(DATE)).hasSize(2);
    }

    @Test
    void onlyAdditivesCountAndTheCategoryIsComparedIgnoringCaseAndSpaces() {
        givenCloses(close("1001", "ANA MILENA VILLAMIL", "DIA",
                product("RUTA75-GAS", "Aditivo para gasolina Ruta 75", "  aditivo ", 6),
                product("LUB-15W40", "Aceite 15W40", "LUBRICANTE", 10),
                product("AGUA", "Agua", "OTRO", 3)));

        assertThat(adapter.findByDate(DATE)).extracting(AdditiveSaleUnits::sku).containsExactly("RUTA75-GAS");
    }

    @Test
    void additivesWithZeroOrNegativeUnitsAreLeftOut() {
        givenCloses(close("1001", "ANA MILENA VILLAMIL", "DIA",
                product("RUTA75-GAS", "Aditivo para gasolina Ruta 75", "ADITIVO", 0),
                product("RUTA75-OCT", "Elevador de octanaje Ruta 75", "ADITIVO", -2),
                product("SUPER-PLUS", "Super concentrado plus", "ADITIVO", 1)));

        assertThat(adapter.findByDate(DATE)).extracting(AdditiveSaleUnits::sku).containsExactly("SUPER-PLUS");
    }

    @Test
    void aCloseWithoutSellerDocumentIsIgnored() {
        givenCloses(
                close(null, "SIN DOCUMENTO", "DIA", product("RUTA75-GAS", "Aditivo", "ADITIVO", 6)),
                close("   ", "DOCUMENTO EN BLANCO", "DIA", product("RUTA75-GAS", "Aditivo", "ADITIVO", 6)),
                close("1001", "ANA MILENA VILLAMIL", "DIA", product("RUTA75-GAS", "Aditivo", "ADITIVO", 1)));

        assertThat(adapter.findByDate(DATE)).extracting(AdditiveSaleUnits::vendorDocument).containsExactly("1001");
    }

    @Test
    void aProductWithoutCategoryOrWithoutSkuIsIgnored() {
        givenCloses(close("1001", "ANA MILENA VILLAMIL", "DIA",
                product("RUTA75-GAS", "Sin categoría", null, 6),
                product("RUTA75-OCT", "Categoría en blanco", "  ", 6),
                product(null, "Sin SKU", "ADITIVO", 6),
                product("  ", "SKU en blanco", "ADITIVO", 6),
                product("SUPER-PLUS", "Super concentrado plus", "ADITIVO", 2)));

        assertThat(adapter.findByDate(DATE)).extracting(AdditiveSaleUnits::sku).containsExactly("SUPER-PLUS");
    }

    @Test
    void documentAndSkuAreStrippedSoTheSameSellerAndAdditiveAreNotSplit() {
        givenCloses(
                close(" 1001 ", "ANA MILENA VILLAMIL", "DIA",
                        product(" RUTA75-GAS ", "Aditivo para gasolina Ruta 75", "ADITIVO", 4)),
                close("1001", "ANA MILENA VILLAMIL", "NOCHE",
                        product("RUTA75-GAS", "Aditivo para gasolina Ruta 75", "ADITIVO", 3)));

        assertThat(adapter.findByDate(DATE)).singleElement().satisfies(row -> {
            assertThat(row.vendorDocument()).isEqualTo("1001");
            assertThat(row.sku()).isEqualTo("RUTA75-GAS");
            assertThat(row.units()).isEqualTo(7);
        });
    }

    @Test
    void aBlankNameFallsBackToTheDocumentOfTheSellerAndTheSkuOfTheProduct() {
        givenCloses(close("1001", "  ", "DIA", product("RUTA75-GAS", null, "ADITIVO", 4)));

        assertThat(adapter.findByDate(DATE)).singleElement().satisfies(row -> {
            assertThat(row.vendorName()).isEqualTo("1001");
            assertThat(row.productName()).isEqualTo("RUTA75-GAS");
        });
    }

    @Test
    void namesAreStripped() {
        givenCloses(close("1001", "  ANA MILENA VILLAMIL ", "DIA",
                product("RUTA75-GAS", " Aditivo para gasolina Ruta 75 ", "ADITIVO", 4)));

        assertThat(adapter.findByDate(DATE)).singleElement().satisfies(row -> {
            assertThat(row.vendorName()).isEqualTo("ANA MILENA VILLAMIL");
            assertThat(row.productName()).isEqualTo("Aditivo para gasolina Ruta 75");
        });
    }
}
