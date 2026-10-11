package com.edu.unicauca.gasstation.backend.sales.domain.models;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SalesTargetPeriodTest {

    @Test
    void annualPeriodHasNoMonth() {
        SalesTargetPeriod period = SalesTargetPeriod.annual(2026);

        assertThat(period.year()).isEqualTo(2026);
        assertThat(period.month()).isNull();
        assertThat(period.isAnnual()).isTrue();
        assertThat(period.isMonthly()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12})
    void acceptsEveryCalendarMonth(int month) {
        SalesTargetPeriod period = SalesTargetPeriod.monthly(2026, month);

        assertThat(period.year()).isEqualTo(2026);
        assertThat(period.month()).isEqualTo(month);
        assertThat(period.isAnnual()).isFalse();
        assertThat(period.isMonthly()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(ints = {Integer.MIN_VALUE, -1, 0, 13, Integer.MAX_VALUE})
    void rejectsInvalidMonthsThroughFactoryAndConstructor(int month) {
        assertThatThrownBy(() -> SalesTargetPeriod.monthly(2026, month))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SalesTargetPeriod(2026, month))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void annualAndMonthlyPeriodsRemainDistinct() {
        SalesTargetPeriod annual = SalesTargetPeriod.annual(2026);
        SalesTargetPeriod monthly = SalesTargetPeriod.monthly(2026, 1);

        assertThat(annual).isNotEqualTo(monthly);
        assertThat(annual.month()).isNull();
        assertThat(monthly).isEqualTo(new SalesTargetPeriod(2026, 1));
    }
}
