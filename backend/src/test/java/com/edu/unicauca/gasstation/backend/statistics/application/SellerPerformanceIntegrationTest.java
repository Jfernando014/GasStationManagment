package com.edu.unicauca.gasstation.backend.statistics.application;

import com.edu.unicauca.gasstation.backend.statistics.domain.calculation.SellerPerformance.*;
import com.edu.unicauca.gasstation.backend.statistics.domain.models.ShiftSaleHistoryRange;
import com.edu.unicauca.gasstation.backend.statistics.domain.ports.SellerPerformanceParameterSource;
import com.edu.unicauca.gasstation.backend.statistics.domain.ports.SellerPerformanceParameterSource.Snapshot;
import com.edu.unicauca.gasstation.backend.statistics.domain.repositories.ShiftSaleHistoryRepository;
import com.edu.unicauca.gasstation.backend.statistics.exception.SellerPerformanceSourceException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import static com.edu.unicauca.gasstation.backend.statistics.SellerPerformanceTestData.*;
import static com.edu.unicauca.gasstation.backend.statistics.domain.calculation.SellerPerformance.Availability.*;
import static com.edu.unicauca.gasstation.backend.statistics.domain.calculation.SellerPerformance.Measure.*;
import static com.edu.unicauca.gasstation.backend.statistics.domain.calculation.SellerPerformance.ShiftPeriod.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class SellerPerformanceIntegrationTest {
    private final ShiftSaleHistoryRepository history = mock(ShiftSaleHistoryRepository.class);
    private final SellerPerformanceParameterSource parameters = mock(SellerPerformanceParameterSource.class);
    private final SellerPerformanceService service = new SellerPerformanceService(history, Optional.of(parameters));

    @Test void oneHistoryAndParameterReadFeedAllExistingEnginesWithoutWritingOrPoolingUnits() {
        var observations = history(SINGLE_DAY, sale(A, "100", "12345"), sale(B, "300", "67890"));
        when(history.readRange(DATE, DATE)).thenReturn(observations);
        when(parameters.read(SINGLE_DAY, FUEL_GALLONS)).thenReturn(new Snapshot(
                List.of(target(A, SINGLE_DAY, FUEL_GALLONS, "100"), target(B, SINGLE_DAY, FUEL_GALLONS, "200")),
                classified(observations, DAY), List.of(new ShiftMultiplier(SINGLE_DAY, FUEL_GALLONS, DAY,
                        new BigDecimal("1.25"), "approved multiplier test fixture"))));
        var report = service.report(SINGLE_DAY, FUEL_GALLONS, 2);
        assertThat(report.source()).isEqualTo("CONFIRMED_SHIFT_SALES_HISTORY");
        assertThat(report.unit()).isEqualTo("GALLONS");
        assertThat(report.roundingMode()).isEqualTo("HALF_EVEN");
        assertThat(report.compliance().sellers()).extracting(Compliance::workerId).containsExactly(B, A);
        assertThat(report.compliance().sellers().getFirst().percent()).isEqualByComparingTo("150");
        assertThat(report.comparison().sellers().getFirst().stationAverage()).isEqualByComparingTo("200");
        assertThat(report.comparison().sellers().getFirst().relativePercent()).isEqualByComparingTo("50");
        assertThat(report.normalization().sellers().getFirst().normalizedSales()).isEqualByComparingTo("125");
        verify(history).readRange(DATE, DATE);
        verify(parameters).read(SINGLE_DAY, FUEL_GALLONS);
        verifyNoMoreInteractions(history, parameters);
    }

    @Test void noRegisteredProducerLeavesObservedSalesVisibleAndEveryUnsupportedMetricUnavailable() {
        when(history.readRange(DATE, DATE)).thenReturn(history(SINGLE_DAY, sale(A, "5", "70")));
        var report = new SellerPerformanceService(history).report(SINGLE_DAY, BASKET_COP, 2);
        assertThat(report.unit()).isEqualTo("COP");
        assertThat(report.compliance().availability()).isEqualTo(UNAVAILABLE);
        assertThat(report.compliance().sellers().getFirst().sales()).isEqualByComparingTo("70");
        assertThat(report.compliance().sellers().getFirst().percent()).isNull();
        assertThat(report.compliance().sellers().getFirst().reason()).isEqualTo(Reason.TARGET_NOT_PROVIDED);
        assertThat(report.comparison().reason()).isEqualTo(Reason.SHIFT_CLASSIFICATION_NOT_PROVIDED);
        assertThat(report.normalization().reason()).isEqualTo(Reason.SHIFT_CLASSIFICATION_NOT_PROVIDED);
        verifyNoInteractions(parameters);
    }

    @Test void aMissingMultiplierDoesNotBlockVerifiedComplianceOrComparison() {
        var observations = history(SINGLE_DAY, sale(A, "5", "70"));
        when(history.readRange(DATE, DATE)).thenReturn(observations);
        when(parameters.read(SINGLE_DAY, BASKET_COP)).thenReturn(new Snapshot(
                List.of(target(A, SINGLE_DAY, BASKET_COP, "100")), classified(observations, NIGHT), List.of()));
        var report = service.report(SINGLE_DAY, BASKET_COP, 2);
        assertThat(report.compliance().availability()).isEqualTo(AVAILABLE);
        assertThat(report.comparison().availability()).isEqualTo(AVAILABLE);
        assertThat(report.normalization().sellers().getFirst().reason()).isEqualTo(Reason.MULTIPLIER_NOT_PROVIDED);
        assertThat(report.normalization().sellers().getFirst().normalizedSales()).isNull();
        assertThat(report.normalization().sellers().getFirst().scenario().period()).isEqualTo(NIGHT);
    }

    @Test void absentPendingAndConfirmedEmptyHistoryDoNotConsultParameterSources() {
        for (var range : List.of(
                new ShiftSaleHistoryRange(DATE, DATE, List.of(new ShiftSaleHistoryRange.Day(DATE, null, List.of()))),
                new ShiftSaleHistoryRange(DATE, DATE, List.of(new ShiftSaleHistoryRange.Day(DATE, pending(DATE), List.of()))),
                history(SINGLE_DAY))) {
            when(history.readRange(DATE, DATE)).thenReturn(range);
            var report = service.report(SINGLE_DAY, FUEL_GALLONS, 2);
            assertThat(report.compliance().availability()).isEqualTo(range.complete() ? NO_DATA : UNAVAILABLE);
            assertThat(report.comparison().availability()).isEqualTo(range.complete() ? NO_DATA : UNAVAILABLE);
            assertThat(report.normalization().availability()).isEqualTo(range.complete() ? NO_DATA : UNAVAILABLE);
        }
        verifyNoInteractions(parameters);
    }

    @Test void adjustmentsWithoutAnApprovedPolicyRemainUnavailableAndDoNotReadParameters() {
        when(history.readRange(DATE, DATE)).thenReturn(history(SINGLE_DAY, sale(A, "-1", "70")));
        var report = service.report(SINGLE_DAY, FUEL_GALLONS, 2);
        assertThat(report.compliance().reason()).isEqualTo(Reason.NEGATIVE_OBSERVATIONS_REQUIRE_POLICY);
        assertThat(report.comparison().reason()).isEqualTo(Reason.NEGATIVE_OBSERVATIONS_REQUIRE_POLICY);
        assertThat(report.normalization().reason()).isEqualTo(Reason.NEGATIVE_OBSERVATIONS_REQUIRE_POLICY);
        verifyNoInteractions(parameters);
    }

    @Test void nullMalformedOrFailedSourcesNeverBecomeNoDataOrClientErrors() {
        var observations = history(SINGLE_DAY, sale(A, "5", "70"));
        when(history.readRange(DATE, DATE)).thenReturn(observations);
        assertThatThrownBy(() -> service.report(SINGLE_DAY, FUEL_GALLONS, 2))
                .isInstanceOf(SellerPerformanceSourceException.class).hasCauseInstanceOf(NullPointerException.class);
        when(parameters.read(SINGLE_DAY, FUEL_GALLONS)).thenReturn(new Snapshot(
                List.of(target(A, SINGLE_DAY, FUEL_GALLONS, "10"), target(A, SINGLE_DAY, FUEL_GALLONS, "10")),
                List.of(), List.of()));
        assertThatThrownBy(() -> service.report(SINGLE_DAY, FUEL_GALLONS, 2))
                .isInstanceOf(SellerPerformanceSourceException.class).hasCauseInstanceOf(IllegalArgumentException.class);
        var failure = new IllegalStateException("upstream unavailable");
        when(parameters.read(SINGLE_DAY, FUEL_GALLONS)).thenThrow(failure);
        assertThatThrownBy(() -> service.report(SINGLE_DAY, FUEL_GALLONS, 2))
                .isInstanceOf(SellerPerformanceSourceException.class).hasCause(failure);
        when(history.readRange(DATE, DATE)).thenReturn(history(new Window(DATE, DATE.plusDays(1))));
        assertThatThrownBy(() -> service.report(SINGLE_DAY, FUEL_GALLONS, 2))
                .isInstanceOf(SellerPerformanceSourceException.class).hasCauseInstanceOf(IllegalStateException.class);
    }

    @Test void invalidRequestsAreRejectedBeforeEitherSourceIsRead() {
        assertThatThrownBy(() -> service.report(SINGLE_DAY, FUEL_GALLONS, 13)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.report(null, FUEL_GALLONS, 2)).isInstanceOf(NullPointerException.class);
        verifyNoInteractions(history, parameters);
    }
}
