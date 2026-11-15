package com.edu.unicauca.gasstation.backend.sales.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.edu.unicauca.gasstation.backend.sales.domain.models.SalesParameterKey;
import com.edu.unicauca.gasstation.backend.sales.domain.models.SalesParameterValue;
import com.edu.unicauca.gasstation.backend.sales.domain.reconciliation.DayFranchiseTotals;
import com.edu.unicauca.gasstation.backend.sales.domain.reconciliation.MonthlyReconciliation;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.CardReconciliationRepository;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.QrPointsRepository;
import com.edu.unicauca.gasstation.backend.sales.exception.SalesParameterNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MonthlyReconciliationServiceTest {

    private static final LocalDate DAY5 = LocalDate.of(2026, 8, 5);

    private final CardReconciliationRepository cards = mock(CardReconciliationRepository.class);
    private final QrPointsRepository qrPoints = mock(QrPointsRepository.class);
    private final SalesParameterService parameters = mock(SalesParameterService.class);
    private final MonthlyReconciliationService service = new MonthlyReconciliationService(cards, qrPoints, parameters);

    private static BigDecimal money(String value) {
        return new BigDecimal(value).setScale(2);
    }

    private static SalesParameterValue rate(String value) {
        return new SalesParameterValue(SalesParameterKey.CREDIBANCO_COMMISSION_RATE, new BigDecimal(value),
                LocalDate.of(2026, 1, 1));
    }

    @Test
    void readsTheWholeMonthAndAddsItUpWithTheRateOfItsLastDay() {
        LocalDate first = LocalDate.of(2026, 8, 1);
        LocalDate last = LocalDate.of(2026, 8, 31);
        when(parameters.effective(SalesParameterKey.CREDIBANCO_COMMISSION_RATE, last)).thenReturn(rate("0.026500"));
        when(cards.findDailyTotals(first, last)).thenReturn(Map.of(DAY5, new DayFranchiseTotals(money("0"),
                money("3618384"), money("2072928"), money("0"), money("0"), money("0"), money("876008"))));
        when(qrPoints.findRedebanTotals(first, last)).thenReturn(Map.of());
        when(qrPoints.findPointsTotals(first, last)).thenReturn(Map.of(DAY5, money("41380")));

        MonthlyReconciliation result = service.get(YearMonth.of(2026, 8));

        assertEquals("2026-08", result.month());
        assertEquals(31, result.days().size());
        assertEquals(money("5691312"), result.totals().totalEds());
        assertEquals(money("876008"), result.totals().qr());
        assertEquals(money("41380"), result.totals().points());
        assertEquals(money("150819.77"), result.commission().amount());
        verify(cards).findDailyTotals(first, last);
        verify(qrPoints).findRedebanTotals(first, last);
        verify(qrPoints).findPointsTotals(first, last);
    }

    @Test
    void theRateIsLookedUpOnTheLastDayOfTheMonthWhateverItsLength() {
        LocalDate lastOfFebruary = LocalDate.of(2026, 2, 28);
        when(parameters.effective(SalesParameterKey.CREDIBANCO_COMMISSION_RATE, lastOfFebruary))
                .thenReturn(rate("0.030000"));
        when(cards.findDailyTotals(any(), any())).thenReturn(Map.of());
        when(qrPoints.findRedebanTotals(any(), any())).thenReturn(Map.of());
        when(qrPoints.findPointsTotals(any(), any())).thenReturn(Map.of());

        MonthlyReconciliation result = service.get(YearMonth.of(2026, 2));

        assertEquals(28, result.days().size());
        assertEquals(new BigDecimal("0.030000"), result.commission().rate());
        assertEquals(lastOfFebruary, result.commission().rateDate());
    }

    @Test
    void withoutAnEffectiveRateItFailsBeforeReadingAnyData() {
        when(parameters.effective(any(), any())).thenThrow(new SalesParameterNotFoundException(
                "No hay un valor de 'credibanco-commission-rate' vigente el 2025-12-31"));

        assertThrows(SalesParameterNotFoundException.class, () -> service.get(YearMonth.of(2025, 12)));

        verifyNoInteractions(cards, qrPoints);
    }
}
