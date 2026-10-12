package com.edu.unicauca.gasstation.backend.shifts.domain.models;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class DailyCoverageTest {

    private static final LocalDate DATE = LocalDate.of(2026, 11, 2);

    @Test
    void isCoveredWhenDayNightAndSupportAreCovered() {
        assertThat(new DailyCoverage(DATE, true, true, true).covered()).isTrue();
    }

    @Test
    void isNotCoveredWhenAnyConditionFails() {
        assertThat(new DailyCoverage(DATE, false, true, true).covered()).isFalse();
        assertThat(new DailyCoverage(DATE, true, false, true).covered()).isFalse();
        assertThat(new DailyCoverage(DATE, true, true, false).covered()).isFalse();
    }
}
