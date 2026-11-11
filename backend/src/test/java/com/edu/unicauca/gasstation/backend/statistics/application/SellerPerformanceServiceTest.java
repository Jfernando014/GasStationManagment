package com.edu.unicauca.gasstation.backend.statistics.application;

import com.edu.unicauca.gasstation.backend.statistics.domain.calculation.SellerPerformance.*;
import com.edu.unicauca.gasstation.backend.statistics.domain.repositories.ShiftSaleHistoryRepository;
import java.util.List;
import org.junit.jupiter.api.Test;

import static com.edu.unicauca.gasstation.backend.statistics.SellerPerformanceTestData.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class SellerPerformanceServiceTest {
    private final ShiftSaleHistoryRepository history = mock(ShiftSaleHistoryRepository.class);
    private final SellerPerformanceService service = new SellerPerformanceService(history);

    @Test
    void readsHistoryOnceAndDoesNotWriteSnapshots() {
        when(history.readRange(DATE, DATE)).thenReturn(history(SINGLE_DAY, sale(A, "10", "20")));
        var result = service.compliance(SINGLE_DAY, Measure.FUEL_GALLONS, List.of(), 2);
        assertThat(result.sellers().getFirst().reason()).isEqualTo(Reason.TARGET_NOT_PROVIDED);
        verify(history).readRange(DATE, DATE);
        verifyNoMoreInteractions(history);
    }

    @Test
    void invalidPrecisionOrMissingParametersDoNotQueryHistory() {
        assertThatThrownBy(() -> service.compliance(SINGLE_DAY, Measure.FUEL_GALLONS, List.of(), -1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.compare(SINGLE_DAY, Measure.FUEL_GALLONS, null, 2))
                .isInstanceOf(NullPointerException.class);
        verifyNoInteractions(history);
    }

    @Test
    void databaseFailuresPropagateInsteadOfBecomingAnEmptySuccessfulPeriod() {
        var failure = new IllegalStateException("database unavailable");
        when(history.readRange(DATE, DATE)).thenThrow(failure);
        assertThatThrownBy(() -> service.compliance(SINGLE_DAY, Measure.FUEL_GALLONS, List.of(), 2)).isSameAs(failure);
    }

    @Test
    void aWrongRangeOrNullResponseIsAnIntegrationFailure() {
        when(history.readRange(DATE, DATE)).thenReturn(history(new Window(DATE, DATE.plusDays(1))));
        assertThatThrownBy(() -> service.compliance(SINGLE_DAY, Measure.FUEL_GALLONS, List.of(), 2))
                .isInstanceOf(IllegalStateException.class);
        when(history.readRange(DATE, DATE)).thenReturn(null);
        assertThatThrownBy(() -> service.compliance(SINGLE_DAY, Measure.FUEL_GALLONS, List.of(), 2))
                .isInstanceOf(NullPointerException.class);
    }
}
