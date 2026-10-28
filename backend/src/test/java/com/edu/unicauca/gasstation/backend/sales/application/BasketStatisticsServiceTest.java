package com.edu.unicauca.gasstation.backend.sales.application;

import com.edu.unicauca.gasstation.backend.inventory.ProductCategory;
import com.edu.unicauca.gasstation.backend.sales.*;
import com.edu.unicauca.gasstation.backend.sales.BasketSalesReadPort.Sale;
import com.edu.unicauca.gasstation.backend.sales.WorkedShiftsReadPort.WorkedShift;
import com.edu.unicauca.gasstation.backend.sales.api.dto.StatisticsChartResponse.*;
import com.edu.unicauca.gasstation.backend.sales.domain.calculation.StatisticsPeriod;
import com.edu.unicauca.gasstation.backend.sales.exception.StatisticsSourceException;
import com.edu.unicauca.gasstation.backend.shifts.catalog.*;
import com.edu.unicauca.gasstation.backend.shifts.spi.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static com.edu.unicauca.gasstation.backend.inventory.ProductCategory.*;

class BasketStatisticsServiceTest {
    final LocalDate from = LocalDate.of(2026, 8, 1), to = from.plusDays(30);
    final UUID a = new UUID(0, 1), b = new UUID(0, 2);
    final BasketSalesReadPort sales = mock(BasketSalesReadPort.class);
    final WorkedShiftsReadPort shifts = mock(WorkedShiftsReadPort.class);
    final ScheduleWorkerDirectory workers = mock(ScheduleWorkerDirectory.class);
    BasketStatisticsService service;

    @BeforeEach void setup() {
        var beans = new StaticListableBeanFactory();
        beans.addBean("sales", sales); beans.addBean("attendance", shifts);
        service = new BasketStatisticsService(beans.getBeanProvider(BasketSalesReadPort.class), beans.getBeanProvider(WorkedShiftsReadPort.class), workers);
        when(workers.getWorkersByIds(any())).thenReturn(Map.of(a, new ScheduleWorkerInfo(a, "Ana", null, false)));
        when(shifts.read(from, to)).thenReturn(new WorkedShiftsReadPort.Snapshot(false, List.of()));
    }
    Sale sale(String id, LocalDate date, UUID worker, ProductCategory category, String amount, String quantity) {
        return new Sale(id, date, worker, 1, category, new BigDecimal(amount), new BigDecimal(quantity));
    }
    void sales(Sale... rows) { when(sales.read(from, to)).thenReturn(new BasketSalesReadPort.Snapshot(true, List.of(rows))); }
    void attendance(WorkedShift... rows) { when(shifts.read(from, to)).thenReturn(new WorkedShiftsReadPort.Snapshot(true, List.of(rows))); }
    com.edu.unicauca.gasstation.backend.sales.api.dto.BasketStatisticsResponse read(UUID worker, ProductCategory category) {
        return service.read(from, to, worker, category, StatisticsPeriod.DAY, 2);
    }

    @Test void groupsSellersCategoriesTotalsAndParticipationWithoutExcludingInactiveWorkers() {
        sales(sale("1", from, a, LUBRICANTE, "100.125", "2"), sale("2", to, a, LUBRICANTE, "49.875", "1"),
                sale("3", from, a, ADITIVO, "50", "1"), sale("4", from, b, OTRO, "200", "4"));
        var result = read(null, null);
        assertThat(result.charts().source()).isEqualTo(Source.REAL_HISTORICAL_SALES);
        assertThat(result.charts().availability()).isEqualTo(Availability.AVAILABLE);
        assertThat(result.summary().amount()).isEqualByComparingTo("400");
        assertThat(result.summary().quantity()).isEqualByComparingTo("8");
        assertThat(result.groups()).hasSize(3);
        assertThat(result.groups().getFirst().workerName()).isEqualTo("Ana");
        assertThat(result.groups().getFirst().amount()).isEqualByComparingTo("150");
        assertThat(result.groups().getFirst().participationPercent()).isEqualByComparingTo("37.50");
        assertThat(result.groups().getLast().workerName()).isNull();
        assertThat(result.summary().averageAmountPerWorkedShift()).isNull();
        assertThat(result.groups()).allSatisfy(g -> assertThat(g.workedShifts()).isNull());
    }
    @Test void filtersWorkerAndCategoryAndRetainsInclusiveBoundaryDates() {
        sales(sale("1", from, a, REFRIGERANTE, "10", "1"), sale("2", to, a, REFRIGERANTE, "20", "2"),
                sale("3", from, b, REFRIGERANTE, "100", "1"), sale("4", from, a, OTRO, "200", "1"));
        var result = read(a, REFRIGERANTE);
        assertThat(result.summary().amount()).isEqualByComparingTo("30");
        assertThat(result.groups()).singleElement().satisfies(g -> assertThat(g.participationPercent()).isEqualByComparingTo("100"));
        assertThat(result.charts().series().getFirst().points()).extracting(Point::periodStart).containsExactly(from, to);
        verify(sales).read(from, to);
    }
    @Test void averagesCountAllVerifiedShiftsIncludingThoseWithoutSales() {
        sales(sale("1", from, a, LUBRICANTE, "100", "1"), sale("2", from, a, ADITIVO, "20", "1"));
        attendance(new WorkedShift("a1", a, from), new WorkedShift("a2", a, to), new WorkedShift("b1", b, from));
        var all = read(null, null);
        assertThat(all.summary().workedShifts()).isEqualTo(3);
        assertThat(all.summary().averageAmountPerWorkedShift()).isEqualByComparingTo("40");
        assertThat(all.groups().getFirst().workedShifts()).isEqualTo(2);
        assertThat(all.groups().getFirst().averageAmountPerWorkedShift()).isEqualByComparingTo("50");
        assertThat(read(a, null).summary().averageAmountPerWorkedShift()).isEqualByComparingTo("60");
    }
    @Test void incompleteOrAbsentAttendanceCannotCertifyAnAverage() {
        sales(sale("1", from, a, OTRO, "10", "1"));
        attendance(new WorkedShift("wrongDay", a, to));
        assertThat(read(a, null).workedShiftsReason()).isEqualTo("SALES_WITHOUT_VERIFIED_WORKED_SHIFT");
        assertThat(read(a, null).summary().workedShifts()).isNull();
        attendance();
        assertThat(read(a, null).summary().averageAmountPerWorkedShift()).isNull();
    }
    @Test void recordedZeroIsAvailableButShareOverZeroIsUndefined() {
        sales(sale("zero", from, a, OTRO, "0", "0"));
        attendance(new WorkedShift("a1", a, from));
        var result = read(null, null);
        assertThat(result.charts().availability()).isEqualTo(Availability.AVAILABLE);
        assertThat(result.summary().amount()).isZero();
        assertThat(result.summary().averageAmountPerWorkedShift()).isZero();
        assertThat(result.groups().getFirst().participationPercent()).isNull();
        assertThat(result.charts().series().getFirst().points().getFirst().value()).isZero();
    }
    @Test void noRowsAndUnavailableSourceHaveDifferentStatusesAndNoArtificialZero() {
        sales();
        var empty = read(null, null);
        assertThat(empty.charts().availability()).isEqualTo(Availability.NO_DATA);
        assertThat(empty.summary()).isNull(); assertThat(empty.charts().series()).isEmpty();
        when(sales.read(from, to)).thenReturn(new BasketSalesReadPort.Snapshot(false, List.of()));
        assertThat(read(null, null).charts().availability()).isEqualTo(Availability.UNAVAILABLE);
        verifyNoInteractions(workers, shifts);
    }
    @Test void missingPortsDoNotRequireProductionFallbackBeans() {
        var beans = new StaticListableBeanFactory();
        var missing = new BasketStatisticsService(beans.getBeanProvider(BasketSalesReadPort.class), beans.getBeanProvider(WorkedShiftsReadPort.class), workers);
        var result = missing.read(from, to, null, null, StatisticsPeriod.MONTH, 2);
        assertThat(result.charts().reason()).isEqualTo("SALES_SOURCE_NOT_CONNECTED");
        assertThat(result.summary()).isNull();
    }
    @Test void missingAttendancePortKeepsRealTotalsAndLeavesAveragesUnavailable() {
        sales(sale("1", from, a, OTRO, "10", "1"));
        var beans = new StaticListableBeanFactory();
        beans.addBean("sales", sales);
        var missing = new BasketStatisticsService(beans.getBeanProvider(BasketSalesReadPort.class), beans.getBeanProvider(WorkedShiftsReadPort.class), workers);
        var result = missing.read(from, to, null, null, StatisticsPeriod.DAY, 2);
        assertThat(result.charts().availability()).isEqualTo(Availability.AVAILABLE);
        assertThat(result.summary().amount()).isEqualByComparingTo("10");
        assertThat(result.summary().averageAmountPerWorkedShift()).isNull();
        assertThat(result.workedShiftsReason()).isEqualTo("WORKED_SHIFTS_SOURCE_NOT_CONNECTED");
        assertThat(result.charts().series()).noneMatch(s -> s.metric() == Metric.AVERAGE_PER_WORKED_SHIFT);
    }
    @Test void sharesAndAveragesUseHalfEvenWhileMonetaryTotalsStayExact() {
        sales(sale("1", from, a, OTRO, "1.005", "1"), sale("2", from, b, OTRO, "2.010", "1"));
        attendance(new WorkedShift("a1", a, from), new WorkedShift("b1", b, from));
        var result = read(null, null);
        assertThat(result.summary().amount()).isEqualByComparingTo("3.015");
        assertThat(result.groups().getFirst().amount()).isEqualByComparingTo("1.005");
        assertThat(result.groups().getFirst().participationPercent()).isEqualByComparingTo("33.33");
        assertThat(result.groups().getFirst().averageAmountPerWorkedShift()).isEqualByComparingTo("1.00");
        assertThat(result.charts().series()).filteredOn(s -> s.metric() == Metric.PARTICIPATION).singleElement()
                .satisfies(s -> assertThat(s.unit()).isEqualTo(Unit.PERCENT));
    }
    @Test void invalidRecordConstructionInsideAnAdapterIsASourceFailure() {
        when(sales.read(from, to)).thenThrow(new IllegalArgumentException("negative recorded sale"));
        assertThatThrownBy(() -> read(null, null)).isInstanceOf(StatisticsSourceException.class).hasCauseInstanceOf(IllegalArgumentException.class);
        doReturn(new BasketSalesReadPort.Snapshot(true, List.of(sale("1", from, a, OTRO, "1", "1")))).when(sales).read(from, to);
        when(shifts.read(from, to)).thenThrow(new NullPointerException("missing worker"));
        assertThatThrownBy(() -> read(null, null)).isInstanceOf(StatisticsSourceException.class).hasCauseInstanceOf(NullPointerException.class);
    }
    @Test void rejectsCorruptSourceRangesAndDuplicateLinesBeforeFiltering() {
        var row = sale("1", from, a, OTRO, "1", "1");
        sales(row, row);
        assertThatThrownBy(() -> read(b, null)).isInstanceOf(StatisticsSourceException.class);
        sales(sale("outside", from.minusDays(1), b, OTRO, "1", "1"));
        assertThatThrownBy(() -> read(a, null)).isInstanceOf(StatisticsSourceException.class);
    }
    @Test void rejectsCorruptAttendanceAndNullSourceSnapshots() {
        sales(sale("1", from, a, OTRO, "1", "1"));
        var shift = new WorkedShift("1", a, from);
        attendance(shift, shift);
        assertThatThrownBy(() -> read(null, null)).isInstanceOf(StatisticsSourceException.class);
        attendance(new WorkedShift("2", a, to.plusDays(1)));
        assertThatThrownBy(() -> read(null, null)).isInstanceOf(StatisticsSourceException.class);
        when(shifts.read(from, to)).thenReturn(null);
        assertThatThrownBy(() -> read(null, null)).isInstanceOf(StatisticsSourceException.class);
        when(sales.read(from, to)).thenReturn(null);
        assertThatThrownBy(() -> read(null, null)).isInstanceOf(StatisticsSourceException.class);
    }
    @ParameterizedTest @ValueSource(ints = {-1, 13}) void invalidPrecision(int scale) {
        assertThatThrownBy(() -> service.read(from, to, null, null, StatisticsPeriod.DAY, scale)).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(sales);
    }
    @Test void invalidDatesAndIdentifiersAreRejectedBeforeSourceAccess() {
        assertThatThrownBy(() -> service.read(to, from, null, null, StatisticsPeriod.DAY, 2)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.read(from, from.plusDays(3660), null, null, StatisticsPeriod.DAY, 2)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> read(new UUID(0, 0), null)).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(sales);
    }
    @Test void portRecordsRejectInvalidIdentitiesNegativeValuesAndFakeUnavailableData() {
        var row = sale("1", from, a, OTRO, "1", "1");
        assertThatThrownBy(() -> new BasketSalesReadPort.Snapshot(false, List.of(row))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> sale("", from, a, OTRO, "1", "1")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> sale("1", from, new UUID(0, 0), OTRO, "1", "1")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> sale("1", from, a, OTRO, "-1", "1")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> sale("1", from, a, OTRO, "1", "-1")).isInstanceOf(IllegalArgumentException.class);
    }
}
