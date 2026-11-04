package com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence.adapters;

import com.edu.unicauca.gasstation.backend.inventory.FuelType;
import com.edu.unicauca.gasstation.backend.inventory.SalesSummary;
import com.edu.unicauca.gasstation.backend.sales.SellerCloseQuery;
import com.edu.unicauca.gasstation.backend.sales.SellerCloseQuery.DaySummary;
import com.edu.unicauca.gasstation.backend.sales.SellerCloseQuery.ProductSaleSummary;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SalesSummaryAdapter - step 2 sales published to Inventory")
class SalesSummaryAdapterTest {

    private static final LocalDate DATE = LocalDate.of(2026, 8, 5);

    @Mock
    private SellerCloseQuery sellerCloseQuery;

    @InjectMocks
    private SalesSummaryAdapter adapter;

    @Test
    @DisplayName("Should return empty when the journal date is null")
    void returnsEmptyWithoutDate() {
        assertThat(adapter.findByJournalDate(null)).isEmpty();
        verifyNoInteractions(sellerCloseQuery);
    }

    @Test
    @DisplayName("Should return empty when step 2 has no closes for the date")
    void returnsEmptyWithoutSales() {
        when(sellerCloseQuery.findDaySummary(DATE)).thenReturn(Optional.empty());

        assertThat(adapter.findByJournalDate(DATE)).isEmpty();
    }

    @Test
    @DisplayName("Should translate gallons and units into Inventory terms")
    void translatesSales() {
        DaySummary day = day(new BigDecimal("120.500"), new BigDecimal("80.000"),
                new BigDecimal("40.000"), new BigDecimal("10.000"),
                List.of(
                        product("80216", 3),
                        product("80217", 2),
                        product("80217", 1)));
        when(sellerCloseQuery.findDaySummary(DATE)).thenReturn(Optional.of(day));

        SalesSummary summary = adapter.findByJournalDate(DATE).orElseThrow();

        assertThat(summary.journalDate()).isEqualTo(DATE);
        assertThat(summary.gallonsByFuel())
                .containsEntry(FuelType.MOTOR, new BigDecimal("120.500"))
                .containsEntry(FuelType.DIESEL, new BigDecimal("80.000"))
                .containsEntry(FuelType.EXTRA, new BigDecimal("40.000"))
                .containsEntry(FuelType.MAX_PRO, new BigDecimal("10.000"));
        assertThat(summary.unitsByProduct())
                .containsEntry("80216", 3)
                .containsEntry("80217", 3);
    }

    @Test
    @DisplayName("Should keep a fuel sold as zero and ignore products without units")
    void keepsZeroGallonsAndSkipsProductsWithoutUnits() {
        DaySummary day = day(BigDecimal.ZERO, new BigDecimal("50.000"), null, BigDecimal.ZERO,
                List.of(product("80216", 0), product(" ", 4), product(null, 2)));
        when(sellerCloseQuery.findDaySummary(DATE)).thenReturn(Optional.of(day));

        SalesSummary summary = adapter.findByJournalDate(DATE).orElseThrow();

        assertThat(summary.gallonsByFuel())
                .containsEntry(FuelType.MOTOR, BigDecimal.ZERO)
                .containsEntry(FuelType.DIESEL, new BigDecimal("50.000"))
                .containsEntry(FuelType.MAX_PRO, BigDecimal.ZERO)
                .doesNotContainKey(FuelType.EXTRA);
        assertThat(summary.unitsByProduct()).isEmpty();
    }

    private DaySummary day(BigDecimal motor, BigDecimal diesel, BigDecimal extra, BigDecimal maxpro,
                           List<ProductSaleSummary> products) {
        return new DaySummary(DATE, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                motor, diesel, extra, maxpro, motor.add(diesel), BigDecimal.ZERO, 0, products);
    }

    private ProductSaleSummary product(String sku, int units) {
        return new ProductSaleSummary(sku, "Producto", "LUBRICANTE", units,
                BigDecimal.ONE, BigDecimal.valueOf(units));
    }
}
