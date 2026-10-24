package com.edu.unicauca.gasstation.backend.inventory.domain.models;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static com.edu.unicauca.gasstation.backend.inventory.MeasurementTestData.measurement;
import static com.edu.unicauca.gasstation.backend.inventory.MeasurementTestData.reading;
import static org.assertj.core.api.Assertions.assertThat;

class TankMeasurementTest {

    private final TankMeasurement measurement = measurement(1L, LocalDate.of(2026, 8, 5),
            reading(1L, "55", "539"), reading(2L, "10", "30"));

    @Test
    void readingOfReturnsTheReadingOfTheTank() {
        assertThat(measurement.readingOf(2L)).isPresent();
        assertThat(measurement.readingOf(2L).get().getVolumeGallons()).isEqualByComparingTo("30");
    }

    @Test
    void readingOfIsEmptyWhenTheTankWasNotMeasured() {
        assertThat(measurement.readingOf(9L)).isEmpty();
    }
}