package com.edu.unicauca.gasstation.backend.statistics.application;

import com.edu.unicauca.gasstation.backend.shifts.holidays.HolidayQuery;
import com.edu.unicauca.gasstation.backend.statistics.domain.calculation.WorkerPerformance.PerformanceWeekday;
import com.edu.unicauca.gasstation.backend.statistics.domain.calculation.WorkerPerformance.Report;
import com.edu.unicauca.gasstation.backend.statistics.domain.models.ShiftSaleHistory.EffectiveRole;
import com.edu.unicauca.gasstation.backend.statistics.domain.repositories.ShiftSaleHistoryRepository;
import com.edu.unicauca.gasstation.backend.statistics.exception.SellerPerformanceSourceException;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static com.edu.unicauca.gasstation.backend.statistics.SellerPerformanceTestData.A;
import static com.edu.unicauca.gasstation.backend.statistics.SellerPerformanceTestData.B;
import static com.edu.unicauca.gasstation.backend.statistics.SellerPerformanceTestData.sale;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class WorkerPerformanceServiceTest {
    private final ShiftSaleHistoryRepository history = mock(ShiftSaleHistoryRepository.class);
    private final HolidayQuery holidayQuery = mock(HolidayQuery.class);
    private final WorkerPerformanceService service = new WorkerPerformanceService(history, holidayQuery);

    @Test
    void cachesHolidayLookupOncePerDistinctYearMonth() {
        LocalDate aug3 = LocalDate.of(2026, 8, 3);
        LocalDate aug7 = LocalDate.of(2026, 8, 7);
        LocalDate aug10 = LocalDate.of(2026, 8, 10);
        LocalDate sep1 = LocalDate.of(2026, 9, 1);

        when(history.findAllProcessed()).thenReturn(List.of(
                sale(aug3, A, "DIA", EffectiveRole.TITULAR, "100", "0"),
                sale(aug7, A, "DIA", EffectiveRole.TITULAR, "140", "0"),
                sale(aug10, B, "DIA", EffectiveRole.TITULAR, "80", "0"),
                sale(sep1, A, "NOCHE", EffectiveRole.APOYO, "60", "0")));
        when(holidayQuery.getHolidays(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31)))
                .thenReturn(Set.of(aug7));
        when(holidayQuery.getHolidays(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30)))
                .thenReturn(Set.of());

        Report report = service.report(A);

        assertThat(report.closedShifts().total()).isEqualTo(3);
        assertThat(report.byWeekday().get(0).weekday()).isEqualTo(PerformanceWeekday.MONDAY);
        assertThat(report.byWeekday().get(0).asHolder().performancePercent()).isEqualByComparingTo("111.11");
        assertThat(report.byWeekday().get(7).weekday()).isEqualTo(PerformanceWeekday.HOLIDAY);
        assertThat(report.byWeekday().get(7).asHolder().shifts()).isEqualTo(1);
        verify(history, times(1)).findAllProcessed();
        verify(holidayQuery, times(1)).getHolidays(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31));
        verify(holidayQuery, times(1)).getHolidays(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30));
        verifyNoMoreInteractions(history, holidayQuery);
    }

    @Test
    void skipsHolidayQueriesWhenWorkerHasNoCloses() {
        when(history.findAllProcessed()).thenReturn(List.of(sale(B, "100", "0")));

        Report report = service.report(A);

        assertThat(report.closedShifts().total()).isZero();
        verifyNoInteractions(holidayQuery);
    }

    @Test
    void invalidWorkerIdDoesNotQueryRepositories() {
        assertThatThrownBy(() -> service.report(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> service.report(new UUID(0, 0))).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(history, holidayQuery);
    }

    @Test
    void storageFailureWrapsInSellerPerformanceSourceException() {
        when(history.findAllProcessed()).thenThrow(new IllegalStateException("db down"));
        assertThatThrownBy(() -> service.report(A)).isInstanceOf(SellerPerformanceSourceException.class);
    }

    @Test
    void holidayFailureWrapsInSellerPerformanceSourceException() {
        when(history.findAllProcessed()).thenReturn(List.of(sale(A, "100", "0")));
        when(holidayQuery.getHolidays(any(), any())).thenThrow(new IllegalStateException("holiday service failed"));
        assertThatThrownBy(() -> service.report(A)).isInstanceOf(SellerPerformanceSourceException.class);
    }
}
