package com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence.adapters;

import com.edu.unicauca.gasstation.backend.inventory.FuelType;
import com.edu.unicauca.gasstation.backend.sales.SellerCloseQuery;
import com.edu.unicauca.gasstation.backend.sales.SellerCloseQuery.DaySummary;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SalesHistoryAdapter - same weekday gallons for the autonomy")
class SalesHistoryAdapterTest {

    private static final LocalDate DATE = LocalDate.of(2026, 8, 5);

    @Mock
    private SellerCloseQuery sellerCloseQuery;

    @InjectMocks
    private SalesHistoryAdapter adapter;

    @Test
    @DisplayName("Should return no history without date or weeks")
    void returnsEmptyWithoutDateOrWeeks() {
        assertThat(adapter.averageGallonsForSameWeekday(null, 4)).isEmpty();
        assertThat(adapter.averageGallonsForSameWeekday(DATE, 0)).isEmpty();
        verifyNoInteractions(sellerCloseQuery);
    }

    @Test
    @DisplayName("Should average the same weekday over the most recent weeks")
    void averagesTheSameWeekday() {
        when(sellerCloseQuery.findDaySummary(DATE.minusWeeks(1)))
                .thenReturn(Optional.of(day(new BigDecimal("100.000"), new BigDecimal("40.000"))));
        when(sellerCloseQuery.findDaySummary(DATE.minusWeeks(2)))
                .thenReturn(Optional.of(day(new BigDecimal("200.000"), new BigDecimal("60.000"))));
        when(sellerCloseQuery.findDaySummary(DATE.minusWeeks(3)))
                .thenReturn(Optional.empty());
        when(sellerCloseQuery.findDaySummary(DATE.minusWeeks(4)))
                .thenReturn(Optional.of(day(new BigDecimal("300.000"), new BigDecimal("20.000"))));

        Map<FuelType, BigDecimal> averages = adapter.averageGallonsForSameWeekday(DATE, 4);

        assertThat(averages)
                .containsEntry(FuelType.MOTOR, new BigDecimal("200.000"))
                .containsEntry(FuelType.DIESEL, new BigDecimal("40.000"));
    }

    @Test
    @DisplayName("Should omit fuels without any day of data")
    void omitsFuelsWithoutData() {
        when(sellerCloseQuery.findDaySummary(DATE.minusWeeks(1)))
                .thenReturn(Optional.of(day(new BigDecimal("120.000"), null)));

        Map<FuelType, BigDecimal> averages = adapter.averageGallonsForSameWeekday(DATE, 4);

        assertThat(averages).containsOnlyKeys(FuelType.MOTOR);
        assertThat(averages.get(FuelType.MOTOR)).isEqualByComparingTo("120.000");
    }

    private DaySummary day(BigDecimal motor, BigDecimal diesel) {
        return new DaySummary(DATE, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                motor, diesel, null, null, BigDecimal.ZERO, BigDecimal.ZERO, 0, List.of());
    }
}
