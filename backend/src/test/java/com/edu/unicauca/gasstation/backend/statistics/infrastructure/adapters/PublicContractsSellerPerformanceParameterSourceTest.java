package com.edu.unicauca.gasstation.backend.statistics.infrastructure.adapters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.edu.unicauca.gasstation.backend.sales.IndividualTargetQuery;
import com.edu.unicauca.gasstation.backend.sales.IndividualTargetQuery.IndividualTargetInfo;
import com.edu.unicauca.gasstation.backend.shifts.holidays.HolidayQuery;
import com.edu.unicauca.gasstation.backend.statistics.domain.calculation.SellerPerformance.DayType;
import com.edu.unicauca.gasstation.backend.statistics.domain.calculation.SellerPerformance.Measure;
import com.edu.unicauca.gasstation.backend.statistics.domain.calculation.SellerPerformance.ShiftClassification;
import com.edu.unicauca.gasstation.backend.statistics.domain.calculation.SellerPerformance.ShiftPeriod;
import com.edu.unicauca.gasstation.backend.statistics.domain.calculation.SellerPerformance.Window;
import com.edu.unicauca.gasstation.backend.statistics.domain.models.ClosedJournalSnapshot;
import com.edu.unicauca.gasstation.backend.statistics.domain.models.ShiftSaleAmounts;
import com.edu.unicauca.gasstation.backend.statistics.domain.models.ShiftSaleHistory;
import com.edu.unicauca.gasstation.backend.statistics.domain.models.ShiftSaleHistory.EffectiveRole;
import com.edu.unicauca.gasstation.backend.statistics.domain.models.ShiftSaleHistoryRange;
import com.edu.unicauca.gasstation.backend.statistics.domain.models.ShiftSaleIdentity;
import com.edu.unicauca.gasstation.backend.statistics.domain.repositories.ShiftSaleHistoryRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PublicContractsSellerPerformanceParameterSourceTest {

    private static final LocalDate HOLIDAY = LocalDate.of(2026, 8, 7);
    private static final LocalDate REGULAR_DAY = LocalDate.of(2026, 8, 8);
    private static final Window WINDOW = new Window(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31));

    private final ShiftSaleHistoryRepository history = mock(ShiftSaleHistoryRepository.class);
    private final HolidayQuery holidays = mock(HolidayQuery.class);
    private final IndividualTargetQuery targets = mock(IndividualTargetQuery.class);
    private PublicContractsSellerPerformanceParameterSource source;

    @BeforeEach
    void setUp() {
        source = new PublicContractsSellerPerformanceParameterSource(history, holidays, targets);
        when(holidays.isHoliday(HOLIDAY)).thenReturn(true);
    }

    @Test
    void classifiesEachCloseByItsWorkedCodeAndTheHolidayCalendarAndLeavesUnknownCodesOut() {
        var holidayDay = sale(HOLIDAY, "A", "DIA", EffectiveRole.TITULAR);
        var night = sale(REGULAR_DAY, "B", "NOCHE", EffectiveRole.TITULAR);
        var partial = sale(REGULAR_DAY, "C", "12-7", EffectiveRole.APOYO);
        var unknown = sale(REGULAR_DAY, "D", "NUEVO", EffectiveRole.TITULAR);
        var range = mock(ShiftSaleHistoryRange.class);
        when(range.sales()).thenReturn(List.of(holidayDay, night, partial, unknown));
        when(history.readRange(WINDOW.from(), WINDOW.to())).thenReturn(range);

        var classifications = source.read(WINDOW, Measure.FUEL_GALLONS).classifications();

        assertThat(classifications).extracting(ShiftClassification::identity)
                .containsExactly(holidayDay.identity(), night.identity(), partial.identity());
        assertThat(classifications).extracting(ShiftClassification::period)
                .containsExactly(ShiftPeriod.DAY, ShiftPeriod.NIGHT, ShiftPeriod.DAY);
        assertThat(classifications).extracting(ShiftClassification::dayType)
                .containsExactly(DayType.HOLIDAY, DayType.REGULAR, DayType.REGULAR);
        assertThat(classifications).allSatisfy(item -> assertThat(item.source()).isNotBlank());
    }

    @Test
    void takesTheIndividualTargetsOfTheRequestedPeriodAndMeasure() {
        var worker = UUID.randomUUID();
        var range = mock(ShiftSaleHistoryRange.class);
        when(range.sales()).thenReturn(List.of());
        when(history.readRange(WINDOW.from(), WINDOW.to())).thenReturn(range);
        when(targets.findIndividualTargets(WINDOW.from(), WINDOW.to(), IndividualTargetQuery.Measure.BASKET_COP))
                .thenReturn(List.of(new IndividualTargetInfo(worker, new BigDecimal("2313717.583"), "target source")));

        var snapshot = source.read(WINDOW, Measure.BASKET_COP);

        assertThat(snapshot.targets()).singleElement().satisfies(target -> {
            assertThat(target.workerId()).isEqualTo(worker);
            assertThat(target.window()).isEqualTo(WINDOW);
            assertThat(target.measure()).isEqualTo(Measure.BASKET_COP);
            assertThat(target.amount()).isEqualByComparingTo("2313717.583");
            assertThat(target.source()).isEqualTo("target source");
        });
        verify(targets).findIndividualTargets(WINDOW.from(), WINDOW.to(), IndividualTargetQuery.Measure.BASKET_COP);
    }

    @Test
    void withoutTargetsTheListIsEmptyAndNotAZeroTarget() {
        var range = mock(ShiftSaleHistoryRange.class);
        when(range.sales()).thenReturn(List.of());
        when(history.readRange(any(), any())).thenReturn(range);
        when(targets.findIndividualTargets(any(), any(), any())).thenReturn(List.of());

        assertThat(source.read(WINDOW, Measure.FUEL_GALLONS).targets()).isEmpty();
    }

    private static ShiftSaleHistory sale(LocalDate date, String document, String code, EffectiveRole role) {
        var journal = new ClosedJournalSnapshot(1L, date, 1L, LocalDateTime.of(date.plusDays(1), java.time.LocalTime.of(6, 0)));
        var amounts = new ShiftSaleAmounts(BigDecimal.TEN, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE,
                BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN);
        return new ShiftSaleHistory(journal, new ShiftSaleIdentity(date, document, code), UUID.randomUUID(), "Vendedor",
                code, role, amounts);
    }
}
