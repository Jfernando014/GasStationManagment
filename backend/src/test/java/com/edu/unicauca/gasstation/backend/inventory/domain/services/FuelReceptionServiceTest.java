package com.edu.unicauca.gasstation.backend.inventory.domain.services;

import com.edu.unicauca.gasstation.backend.inventory.domain.models.FuelReception;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.Tank;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.TankMeasurement;
import com.edu.unicauca.gasstation.backend.inventory.domain.repositories.FuelReceptionRepository;
import com.edu.unicauca.gasstation.backend.inventory.domain.repositories.TankMeasurementRepository;
import com.edu.unicauca.gasstation.backend.inventory.domain.repositories.TankRepository;
import com.edu.unicauca.gasstation.backend.inventory.exception.FuelReceptionNotFoundException;
import com.edu.unicauca.gasstation.backend.inventory.exception.InvalidFuelReceptionException;
import com.edu.unicauca.gasstation.backend.inventory.exception.ReceptionExceedsCapacityException;
import com.edu.unicauca.gasstation.backend.inventory.exception.TankNotFoundException;
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

import static com.edu.unicauca.gasstation.backend.inventory.MeasurementTestData.measurement;
import static com.edu.unicauca.gasstation.backend.inventory.MeasurementTestData.reading;
import static com.edu.unicauca.gasstation.backend.inventory.MeasurementTestData.reception;
import static com.edu.unicauca.gasstation.backend.inventory.MeasurementTestData.tankWithGauge;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FuelReceptionServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 8, 5);
    private static final LocalDate MEASURED = LocalDate.of(2026, 8, 1);
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-08-05T15:00:00Z"), ZoneId.of("America/Bogota"));
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 5, 10, 0);

    @Mock
    private FuelReceptionRepository receptionRepository;

    @Mock
    private TankRepository tankRepository;

    @Mock
    private TankMeasurementRepository measurementRepository;

    private FuelReceptionService service;

    @BeforeEach
    void setUp() {
        service = new FuelReceptionService(receptionRepository, tankRepository, measurementRepository, CLOCK);
    }

    // ---------- create ----------

    @Test
    void createStoresTheReceptionWithTimestampsAndATrimmedSupplier() {
        givenTank(1L, "10000");
        givenNoMeasurementFor(1L);
        when(receptionRepository.save(any(FuelReception.class))).thenAnswer(inv -> inv.getArgument(0));
        FuelReception request = reception(99L, 1L, TODAY, "3970");
        request.setSupplier("  Terpel  ");

        FuelReception created = service.create(request);

        assertThat(created.getId()).isNull();
        assertThat(created.getSupplier()).isEqualTo("Terpel");
        assertThat(created.getCreatedAt()).isEqualTo(NOW);
        assertThat(created.getUpdatedAt()).isEqualTo(NOW);
    }

    @Test
    void createRejectsAFutureDate() {
        givenTank(1L, "10000");

        assertThatThrownBy(() -> service.create(reception(null, 1L, TODAY.plusDays(1), "100")))
                .isInstanceOf(InvalidFuelReceptionException.class);
        verify(receptionRepository, never()).save(any());
    }

    @Test
    void createRejectsAnUnknownTank() {
        when(tankRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(reception(null, 99L, TODAY, "100")))
                .isInstanceOf(TankNotFoundException.class);
        verify(receptionRepository, never()).save(any());
    }

    // ---------- capacidad sin medición ----------

    @Test
    void withoutMeasurementTheReceptionMayFillTheTankExactly() {
        givenTank(1L, "10000");
        givenNoMeasurementFor(1L);
        when(receptionRepository.save(any(FuelReception.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(service.create(reception(null, 1L, TODAY, "10000")).getGallons()).isEqualByComparingTo("10000");
    }

    @Test
    void withoutMeasurementTheReceptionCannotExceedTheCapacity() {
        givenTank(1L, "10000");
        givenNoMeasurementFor(1L);

        assertThatThrownBy(() -> service.create(reception(null, 1L, TODAY, "10000.01")))
                .isInstanceOf(ReceptionExceedsCapacityException.class);
        verify(receptionRepository, never()).save(any());
    }

    // ---------- capacidad con medición ----------

    @Test
    void addsTheLastMeasuredGallonsAndTheLaterReceptionsToTheCheck() {
        givenTank(1L, "10000");
        givenLatestMeasurement(1L, "6000");
        when(receptionRepository.sumGallonsAfter(1L, MEASURED, null)).thenReturn(new BigDecimal("3000"));
        when(receptionRepository.save(any(FuelReception.class))).thenAnswer(inv -> inv.getArgument(0));

        // 6000 medidos + 3000 recibidos después + 1000 = 10000: justo cabe
        assertThat(service.create(reception(null, 1L, TODAY, "1000")).getGallons()).isEqualByComparingTo("1000");
    }

    @Test
    void rejectsAReceptionThatExceedsTheMeasuredStockPlusLaterReceptions() {
        givenTank(1L, "10000");
        givenLatestMeasurement(1L, "6000");
        when(receptionRepository.sumGallonsAfter(1L, MEASURED, null)).thenReturn(new BigDecimal("3000"));

        assertThatThrownBy(() -> service.create(reception(null, 1L, TODAY, "1000.01")))
                .isInstanceOf(ReceptionExceedsCapacityException.class)
                .hasMessage(new ReceptionExceedsCapacityException().getMessage());
        verify(receptionRepository, never()).save(any());
    }

    @Test
    void aReceptionOnTheDayOfTheMeasurementIsAlreadyIncludedInIt() {
        givenTank(1L, "10000");
        givenLatestMeasurement(1L, "9500");
        when(receptionRepository.save(any(FuelReception.class))).thenAnswer(inv -> inv.getArgument(0));

        // no suma a lo medido: solo se comprueba que quepa por sí sola
        assertThat(service.create(reception(null, 1L, MEASURED, "1000")).getGallons()).isEqualByComparingTo("1000");
        verify(receptionRepository, never()).sumGallonsAfter(any(), any(), any());
    }

    @Test
    void aReceptionBeforeTheMeasurementIsAlreadyIncludedInIt() {
        givenTank(1L, "10000");
        givenLatestMeasurement(1L, "9500");
        when(receptionRepository.save(any(FuelReception.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(service.create(reception(null, 1L, MEASURED.minusDays(1), "1000")).getId()).isNull();
        verify(receptionRepository, never()).sumGallonsAfter(any(), any(), any());
    }

    @Test
    void aReceptionBeforeTheMeasurementStillHasToFitInTheTank() {
        givenTank(1L, "10000");
        givenLatestMeasurement(1L, "9500");

        assertThatThrownBy(() -> service.create(reception(null, 1L, MEASURED.minusDays(1), "10001")))
                .isInstanceOf(ReceptionExceedsCapacityException.class);
    }

    // ---------- update ----------

    @Test
    void updateKeepsTheCreationTimeAndExcludesTheReceptionItselfFromTheSum() {
        FuelReception existing = reception(7L, 1L, TODAY, "500");
        givenTank(1L, "10000");
        givenLatestMeasurement(1L, "6000");
        when(receptionRepository.findById(7L)).thenReturn(Optional.of(existing));
        // si no se pasara el id 7 a la suma, este stub no coincidiría y el test fallaría
        when(receptionRepository.sumGallonsAfter(1L, MEASURED, 7L)).thenReturn(new BigDecimal("3000"));
        when(receptionRepository.save(existing)).thenReturn(existing);
        FuelReception changes = reception(null, 1L, TODAY, "1000");
        changes.setSupplier(" Petrobras ");

        FuelReception updated = service.update(7L, changes);

        assertThat(updated.getId()).isEqualTo(7L);
        assertThat(updated.getGallons()).isEqualByComparingTo("1000");
        assertThat(updated.getSupplier()).isEqualTo("Petrobras");
        assertThat(updated.getCreatedAt()).isEqualTo(TODAY.atTime(8, 0));
        assertThat(updated.getUpdatedAt()).isEqualTo(NOW);
    }

    @Test
    void updateRevalidatesTheCapacity() {
        FuelReception existing = reception(7L, 1L, TODAY, "500");
        givenTank(1L, "10000");
        givenLatestMeasurement(1L, "6000");
        when(receptionRepository.findById(7L)).thenReturn(Optional.of(existing));
        when(receptionRepository.sumGallonsAfter(1L, MEASURED, 7L)).thenReturn(new BigDecimal("3000"));

        assertThatThrownBy(() -> service.update(7L, reception(null, 1L, TODAY, "1000.01")))
                .isInstanceOf(ReceptionExceedsCapacityException.class);
        verify(receptionRepository, never()).save(any());
    }

    @Test
    void updateCanMoveTheReceptionToAnotherTank() {
        FuelReception existing = reception(7L, 1L, TODAY, "500");
        givenTank(2L, "5000");
        givenNoMeasurementFor(2L);
        when(receptionRepository.findById(7L)).thenReturn(Optional.of(existing));
        when(receptionRepository.save(existing)).thenReturn(existing);

        assertThat(service.update(7L, reception(null, 2L, TODAY, "500")).getTankId()).isEqualTo(2L);
    }

    @Test
    void updateThrowsWhenTheReceptionDoesNotExist() {
        when(receptionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(99L, reception(null, 1L, TODAY, "100")))
                .isInstanceOf(FuelReceptionNotFoundException.class);
        verify(receptionRepository, never()).save(any());
    }

    // ---------- consultas ----------

    @Test
    void findByIdReturnsTheReception() {
        FuelReception existing = reception(7L, 1L, TODAY, "500");
        when(receptionRepository.findById(7L)).thenReturn(Optional.of(existing));

        assertThat(service.findById(7L)).isSameAs(existing);
    }

    @Test
    void findByIdThrowsWhenTheReceptionDoesNotExist() {
        when(receptionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(99L))
                .isInstanceOf(FuelReceptionNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void searchDelegatesTheFiltersToTheRepository() {
        FuelReception existing = reception(7L, 1L, TODAY, "500");
        LocalDate from = TODAY.minusDays(10);
        when(receptionRepository.search(1L, from, TODAY)).thenReturn(List.of(existing));

        assertThat(service.search(1L, from, TODAY)).containsExactly(existing);
    }

    @Test
    void searchAcceptsMissingFiltersAndASingleDayRange() {
        when(receptionRepository.search(null, null, null)).thenReturn(List.of());
        when(receptionRepository.search(null, TODAY, TODAY)).thenReturn(List.of());

        assertThat(service.search(null, null, null)).isEmpty();
        assertThat(service.search(null, TODAY, TODAY)).isEmpty();
    }

    @Test
    void searchRejectsAStartDateAfterTheEndDate() {
        assertThatThrownBy(() -> service.search(1L, TODAY, TODAY.minusDays(1)))
                .isInstanceOf(InvalidFuelReceptionException.class);
        verifyNoInteractions(receptionRepository);
    }

    // ---------- helpers ----------

    private void givenTank(long tankId, String capacityGallons) {
        Tank tank = tankWithGauge(tankId, null);
        tank.setMaxCapacityGallons(new BigDecimal(capacityGallons));
        when(tankRepository.findById(tankId)).thenReturn(Optional.of(tank));
    }

    private void givenNoMeasurementFor(long tankId) {
        when(measurementRepository.findLatestContainingTank(tankId)).thenReturn(Optional.empty());
    }

    private void givenLatestMeasurement(long tankId, String gallons) {
        TankMeasurement latest = measurement(1L, MEASURED, reading(tankId, "100", gallons));
        when(measurementRepository.findLatestContainingTank(tankId)).thenReturn(Optional.of(latest));
    }
}