package com.edu.unicauca.gasstation.backend.sales.api;

import com.edu.unicauca.gasstation.backend.sales.api.dto.CalculateDistributionRequest;
import com.edu.unicauca.gasstation.backend.sales.api.dto.CalculateDistributionRequest.AssignmentRequest;
import com.edu.unicauca.gasstation.backend.sales.domain.calculation.DistributionCalculation.Assignment;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class SalesTargetDistributionRequestTest {
    private static final LocalDate DATE = LocalDate.of(2026, 8, 1);
    private static final UUID WORKER = new UUID(0, 1);
    private CalculateDistributionRequest request(AssignmentRequest... rows) {
        return new CalculateDistributionRequest(1L, 2, List.of(rows));
    }

    @Test void omittedEndDatePreservesSingleDayContract() {
        assertThat(request(new AssignmentRequest(DATE, WORKER, "DIA")).toDomainAssignments())
                .containsExactly(new Assignment(DATE, WORKER, "DIA"));
    }

    @Test void inclusiveRangeIsEquivalentToExplicitDailyRows() {
        var compact = request(new AssignmentRequest(DATE, WORKER, "DIA", DATE.plusDays(2)));
        var explicit = request(new AssignmentRequest(DATE, WORKER, "DIA"),
                new AssignmentRequest(DATE.plusDays(1), WORKER, "DIA"),
                new AssignmentRequest(DATE.plusDays(2), WORKER, "DIA"));
        assertThat(compact.toDomainAssignments()).isEqualTo(explicit.toDomainAssignments());
        assertThatThrownBy(() -> compact.toDomainAssignments().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test void leapMonthAndEqualEndpointsExpandWithoutInventedRestDays() {
        var leap = LocalDate.of(2028, 2, 1);
        assertThat(request(new AssignmentRequest(leap, WORKER, "DIA", leap.withDayOfMonth(29)))
                .toDomainAssignments()).hasSize(29).last().isEqualTo(new Assignment(leap.withDayOfMonth(29), WORKER, "DIA"));
        assertThat(request(new AssignmentRequest(DATE, WORKER, "DIA", DATE)).toDomainAssignments()).hasSize(1);
    }

    @Test void reversedAndCrossMonthRangesAreRejectedBeforeExpansion() {
        for (var end : List.of(DATE.minusDays(1), DATE.plusMonths(1), LocalDate.MAX))
            assertThatThrownBy(() -> request(new AssignmentRequest(DATE, WORKER, "DIA", end)).toDomainAssignments())
                    .isInstanceOf(IllegalArgumentException.class);
    }

    @Test void overlappingRangesAndSingleRowsAreRejectedForTheSameWorkerOnly() {
        var range = new AssignmentRequest(DATE, WORKER, "DIA", DATE.plusDays(2));
        assertThatThrownBy(() -> request(range, new AssignmentRequest(DATE.plusDays(1), WORKER, "NOCHE"))
                .toDomainAssignments()).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> request(range, new AssignmentRequest(DATE.plusDays(2), WORKER, "DIA", DATE.plusDays(3)))
                .toDomainAssignments()).isInstanceOf(IllegalArgumentException.class);
        assertThat(request(range, new AssignmentRequest(DATE, new UUID(0, 2), "DIA"))
                .toDomainAssignments()).hasSize(4);
    }
}
