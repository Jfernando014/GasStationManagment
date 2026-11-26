package com.edu.unicauca.gasstation.backend.sales.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.edu.unicauca.gasstation.backend.inventory.FuelType;
import com.edu.unicauca.gasstation.backend.sales.SellerCloseQuery;
import com.edu.unicauca.gasstation.backend.sales.SellerCloseQuery.DaySummary;
import com.edu.unicauca.gasstation.backend.sales.domain.models.SalesParameterKey;
import com.edu.unicauca.gasstation.backend.sales.domain.models.SalesParameterValue;
import com.edu.unicauca.gasstation.backend.sales.domain.profit.CardPercentageMode;
import com.edu.unicauca.gasstation.backend.sales.domain.profit.FuelProfit;
import com.edu.unicauca.gasstation.backend.sales.domain.profit.FuelProfitCardSetting;
import com.edu.unicauca.gasstation.backend.sales.domain.profit.FuelProfitReport;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.FuelMonthlyTargetReader;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.FuelPriceReadPort;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.FuelProfitCardSettingRepository;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.FuelPurchasePriceReadPort;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.SalesParameterRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FuelProfitServiceTest {

    @Mock
    private SellerCloseQuery sellerCloseQuery;
    @Mock
    private FuelPriceReadPort salePrices;
    @Mock
    private FuelPurchasePriceReadPort purchasePrices;
    @Mock
    private SalesParameterService parameters;
    @Mock
    private SalesParameterRepository parameterValues;
    @Mock
    private FuelMonthlyTargetReader targets;
    @Mock
    private FuelProfitCardSettingRepository cardSettings;

    @InjectMocks
    private FuelProfitService service;

    @Test
    void buildsMonthlyReportUsingMonthEndCommissionRateAndEffectiveDayValues() {
        YearMonth month = YearMonth.of(2026, 8);
        LocalDate day5 = LocalDate.of(2026, 8, 5);
        LocalDate today = LocalDate.of(2026, 8, 10);

        when(parameters.effective(SalesParameterKey.CREDIBANCO_COMMISSION_RATE, LocalDate.of(2026, 8, 31)))
                .thenReturn(new SalesParameterValue(
                        SalesParameterKey.CREDIBANCO_COMMISSION_RATE, new BigDecimal("0.0265"), LocalDate.of(2026, 1, 1)));
        when(cardSettings.findByMonth(month)).thenReturn(Optional.empty());
        when(targets.findGallonTargets(month)).thenReturn(Map.of(FuelType.MOTOR, new BigDecimal("1000")));

        when(sellerCloseQuery.findDaySummary(any(LocalDate.class))).thenReturn(Optional.empty());
        when(sellerCloseQuery.findDaySummary(day5)).thenReturn(Optional.of(new DaySummary(
                day5,
                BigDecimal.ZERO, BigDecimal.ZERO,
                new BigDecimal("200000"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                new BigDecimal("1000000"), BigDecimal.ZERO,
                new BigDecimal("100"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                new BigDecimal("100"), BigDecimal.ZERO, 0, List.of())));

        when(salePrices.findEffectivePrice(any(), any())).thenReturn(Optional.of(new BigDecimal("16000")));
        when(purchasePrices.findEffectivePrice(any(), any())).thenReturn(Optional.of(new BigDecimal("14000")));
        when(parameterValues.findEffective(eq(SalesParameterKey.FREIGHT_PER_GALLON), any()))
                .thenReturn(Optional.of(new SalesParameterValue(
                        SalesParameterKey.FREIGHT_PER_GALLON, new BigDecimal("200"), LocalDate.of(2026, 1, 1))));
        when(parameterValues.findEffective(eq(SalesParameterKey.FINANCIAL_COST_PER_GALLON), any()))
                .thenReturn(Optional.of(new SalesParameterValue(
                        SalesParameterKey.FINANCIAL_COST_PER_GALLON, new BigDecimal("100"), LocalDate.of(2026, 1, 1))));

        FuelProfitReport report = service.report(month, today);

        assertEquals(today, report.referenceDate());
        assertEquals(1, report.daysWithData());
        assertEquals(CardPercentageMode.REAL, report.card().mode());
        assertEquals(0, new BigDecimal("20").compareTo(report.card().appliedPercentage()));

        FuelProfit motor = report.fuels().stream()
                .filter(f -> f.fuelType() == FuelType.MOTOR)
                .findFirst()
                .orElseThrow();
        assertTrue(motor.complete());
        assertEquals(new BigDecimal("1600000.00"), motor.sales());
        assertEquals(new BigDecimal("170000.00"), motor.realProfit());
        assertEquals(new BigDecimal("1700000.00"), motor.targetProfit());
    }

    @Test
    void returnsDefaultRealCardSettingWhenMissingAndSavesManualSetting() {
        YearMonth month = YearMonth.of(2026, 9);
        when(cardSettings.findByMonth(month)).thenReturn(Optional.empty());

        FuelProfitCardSetting fallback = service.cardSetting(month);
        assertEquals(CardPercentageMode.REAL, fallback.mode());
        assertEquals("2026-09", fallback.month());

        FuelProfitCardSetting manual = new FuelProfitCardSetting(
                "2026-09", CardPercentageMode.MANUAL, new BigDecimal("30"));
        FuelProfitCardSetting saved = service.saveCardSetting(manual);

        assertEquals(manual, saved);
        verify(cardSettings).save(manual);
    }
}
