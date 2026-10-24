package com.edu.unicauca.gasstation.backend.inventory.domain.services;

import com.edu.unicauca.gasstation.backend.inventory.domain.models.TankMeasurement;
import com.edu.unicauca.gasstation.backend.inventory.domain.repositories.GaugeTableRepository;
import com.edu.unicauca.gasstation.backend.inventory.domain.repositories.TankMeasurementRepository;
import com.edu.unicauca.gasstation.backend.inventory.domain.repositories.TankRepository;
import com.edu.unicauca.gasstation.backend.inventory.exception.GaugeTableNotFoundException;
import com.edu.unicauca.gasstation.backend.inventory.exception.HeightExceedsGaugeException;
import com.edu.unicauca.gasstation.backend.inventory.exception.InvalidTankMeasurementException;
import com.edu.unicauca.gasstation.backend.inventory.exception.TankNotFoundException;
import com.edu.unicauca.gasstation.backend.inventory.exception.TankWithoutGaugeTableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static com.edu.unicauca.gasstation.backend.inventory.GaugeTestData.curve;
import static com.edu.unicauca.gasstation.backend.inventory.MeasurementTestData.measurement;
import static com.edu.unicauca.gasstation.backend.inventory.MeasurementTestData.reading;
import static com.edu.unicauca.gasstation.backend.inventory.MeasurementTestData.tankWithGauge;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TankMeasurementServiceTest {

    private static final LocalDate DATE = LocalDate.of(2026, 8, 5);
    // 15:00 UTC son las 10:00 en Bogotá
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-08-05T15:00:00Z"), ZoneId.of("America/Bogota"));
    private static final long GAUGE_TABLE_ID = 10L;

    @Mock
    private TankMeasurementRepository measurementRepository;

    @Mock
    private TankRepository tankRepository;

    @Mock
    private GaugeTableRepository gaugeTableRepository;

    private TankMeasurementService service;

    @BeforeEach
    void setUp() {
        // GaugeTableService real: así se comprueba el cálculo completo (curva menos 20 gal)
        GaugeTableService gaugeTableService = new GaugeTableService(
                gaugeTableRepository, tankRepository, new GaugeCsvParser(), new BigDecimal("20"));
        service = new TankMeasurementService(measurementRepository, tankRepository, gaugeTableService, CLOCK);
    }

    // ---------- cálculo ----------

    @Test
    void savesTheReadingWithTheGallonsCalculatedFromTheGaugeTable() {
        givenTankWithCurve(1L);
        givenCurve();
        givenNoMeasurementYet();
        givenSaveReturnsItsArgument();

        TankMeasurement saved = service.save(DATE, List.of(reading(1L, "55", null)));

        assertThat(saved.getId()).isNull();
        assertThat(saved.getJournalDate()).isEqualTo(DATE);
        assertThat(saved.getMeasuredAt()).isEqualTo(LocalDateTime.of(2026, 8, 5, 10, 0));
        assertThat(saved.getReadings()).hasSize(1);
        // curva 559 gal - 20 de volumen muerto
        assertThat(saved.getReadings().get(0).getVolumeGallons()).isEqualByComparingTo("539");
        assertThat(saved.getReadings().get(0).getHeightCm()).isEqualByComparingTo("55");
    }

    @Test
    void interpolatesBetweenTwoPointsOfTheCurve() {
        givenTankWithCurve(1L);
        givenCurve();
        givenNoMeasurementYet();
        givenSaveReturnsItsArgument();

        TankMeasurement saved = service.save(DATE, List.of(reading(1L, "55.5", null)));

        assertThat(saved.getReadings().get(0).getVolumeGallons()).isEqualByComparingTo("546.5");
    }

    @Test
    void neverStoresNegativeGallons() {
        givenTankWithCurve(1L);
        givenCurve();
        givenNoMeasurementYet();
        givenSaveReturnsItsArgument();

        // a 5 cm la curva da 16 gal: 16 - 20 no puede quedar negativo
        TankMeasurement saved = service.save(DATE, List.of(reading(1L, "5", null)));

        assertThat(saved.getReadings().get(0).getVolumeGallons()).isEqualByComparingTo("0");
    }

    @Test
    void loadsASharedGaugeTableOnlyOnce() {
        givenTankWithCurve(1L);
        givenTankWithCurve(2L);
        givenCurve();
        givenNoMeasurementYet();
        givenSaveReturnsItsArgument();

        TankMeasurement saved = service.save(DATE, List.of(reading(1L, "55", null), reading(2L, "56", null)));

        assertThat(saved.getReadings()).hasSize(2);
        assertThat(saved.readingOf(2L).get().getVolumeGallons()).isEqualByComparingTo("554");
        verify(gaugeTableRepository, times(1)).findById(GAUGE_TABLE_ID);
    }

    // ---------- reemplazo ----------

    @Test
    void keepsTheIdAndTheOtherTanksWhenTheJournalAlreadyHasAMeasurement() {
        TankMeasurement existing = measurement(5L, DATE, reading(1L, "10", "100"), reading(2L, "20", "200"));
        existing.setMeasuredAt(DATE.atTime(8, 0));
        givenTankWithCurve(1L);
        givenCurve();
        when(measurementRepository.findByJournalDate(DATE)).thenReturn(Optional.of(existing));
        givenSaveReturnsItsArgument();

        TankMeasurement saved = service.save(DATE, List.of(reading(1L, "55", null)));

        assertThat(saved.getId()).isEqualTo(5L);
        assertThat(saved.getReadings()).hasSize(2);
        assertThat(saved.readingOf(1L).get().getVolumeGallons()).isEqualByComparingTo("539");
        assertThat(saved.readingOf(2L).get().getVolumeGallons()).isEqualByComparingTo("200");
        assertThat(saved.getMeasuredAt()).isEqualTo(LocalDateTime.of(2026, 8, 5, 10, 0));
    }

    // ---------- errores ----------

    @Test
    void rejectsAnEmptyMeasurement() {
        assertThatThrownBy(() -> service.save(DATE, List.of())).isInstanceOf(InvalidTankMeasurementException.class);
        assertThatThrownBy(() -> service.save(DATE, null)).isInstanceOf(InvalidTankMeasurementException.class);
        verifyNoInteractions(measurementRepository, tankRepository);
    }

    @Test
    void rejectsTheSameTankTwice() {
        assertThatThrownBy(() -> service.save(DATE, List.of(reading(1L, "10", null), reading(1L, "20", null))))
                .isInstanceOf(InvalidTankMeasurementException.class);
        verifyNoInteractions(measurementRepository, tankRepository);
    }

    @Test
    void rejectsAnUnknownTank() {
        when(tankRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.save(DATE, List.of(reading(99L, "10", null))))
                .isInstanceOf(TankNotFoundException.class);
        verify(measurementRepository, never()).save(any());
    }

    @Test
    void rejectsATankWithoutGaugeTable() {
        when(tankRepository.findById(1L)).thenReturn(Optional.of(tankWithGauge(1L, null)));

        assertThatThrownBy(() -> service.save(DATE, List.of(reading(1L, "10", null))))
                .isInstanceOf(TankWithoutGaugeTableException.class);
        verify(measurementRepository, never()).save(any());
    }

    @Test
    void rejectsATankWhoseGaugeTableDoesNotExist() {
        givenTankWithCurve(1L);
        when(gaugeTableRepository.findById(GAUGE_TABLE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.save(DATE, List.of(reading(1L, "10", null))))
                .isInstanceOf(GaugeTableNotFoundException.class);
        verify(measurementRepository, never()).save(any());
    }

    @Test
    void savesNothingWhenOneReadingExceedsTheGaugeTable() {
        givenTankWithCurve(1L);
        givenTankWithCurve(2L);
        givenCurve();

        // la primera lectura es válida, la segunda pasa de 238 cm: no se guarda ninguna
        assertThatThrownBy(() -> service.save(DATE, List.of(reading(1L, "55", null), reading(2L, "300", null))))
                .isInstanceOf(HeightExceedsGaugeException.class);
        verify(measurementRepository, never()).save(any());
    }

    // ---------- consultas ----------

    @Test
    void findByJournalDateDelegatesToTheRepository() {
        TankMeasurement existing = measurement(1L, DATE, reading(1L, "55", "539"));
        when(measurementRepository.findByJournalDate(DATE)).thenReturn(Optional.of(existing));

        assertThat(service.findByJournalDate(DATE)).containsSame(existing);
    }

    @Test
    void findLatestBeforeDelegatesToTheRepository() {
        TankMeasurement previous = measurement(1L, DATE.minusDays(1), reading(1L, "55", "539"));
        when(measurementRepository.findLatestBefore(DATE)).thenReturn(Optional.of(previous));

        assertThat(service.findLatestBefore(DATE)).containsSame(previous);
    }

    // ---------- helpers ----------

    private void givenTankWithCurve(long tankId) {
        when(tankRepository.findById(tankId)).thenReturn(Optional.of(tankWithGauge(tankId, GAUGE_TABLE_ID)));
    }

    private void givenCurve() {
        when(gaugeTableRepository.findById(GAUGE_TABLE_ID))
                .thenReturn(Optional.of(curve(GAUGE_TABLE_ID, "Tanques 1 y 2")));
    }

    private void givenNoMeasurementYet() {
        when(measurementRepository.findByJournalDate(DATE)).thenReturn(Optional.empty());
    }

    private void givenSaveReturnsItsArgument() {
        when(measurementRepository.save(any(TankMeasurement.class))).thenAnswer(inv -> inv.getArgument(0));
    }
}