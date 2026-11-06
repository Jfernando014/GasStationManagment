package com.edu.unicauca.gasstation.backend.inventory.domain.services;

import com.edu.unicauca.gasstation.backend.inventory.domain.models.AlertStatus;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.AutonomyLevel;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.FuelStockSummary;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.InventoryAlert;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.InventoryHome;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.Tank;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.TankMeasurement;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.TankSnapshot;
import com.edu.unicauca.gasstation.backend.inventory.domain.repositories.TankMeasurementRepository;
import com.edu.unicauca.gasstation.backend.inventory.domain.repositories.TankRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.edu.unicauca.gasstation.backend.inventory.AlertTestData.alert;
import static com.edu.unicauca.gasstation.backend.inventory.FuelTestData.tank;
import static com.edu.unicauca.gasstation.backend.inventory.FuelType.DIESEL;
import static com.edu.unicauca.gasstation.backend.inventory.FuelType.MOTOR;
import static com.edu.unicauca.gasstation.backend.inventory.MeasurementTestData.measurement;
import static com.edu.unicauca.gasstation.backend.inventory.MeasurementTestData.reading;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryHomeServiceTest {

    private static final LocalDate DATE = LocalDate.of(2026, 8, 5);

    @Mock
    private TankRepository tankRepository;
    @Mock
    private TankMeasurementRepository measurementRepository;
    @Mock
    private InventoryAlertService alertService;

    private InventoryHomeService service;

    @BeforeEach
    void setUp() {
        service = new InventoryHomeService(tankRepository, measurementRepository, alertService);
        // T3 is listed first on purpose: the service must sort by code
        when(tankRepository.findAllOrderByCode()).thenReturn(List.of(
                withCapacity(tank(3L, "T3", DIESEL, "0.5"), "2000"),
                withCapacity(tank(1L, "T1", MOTOR, "0.5"), "1000"),
                withCapacity(tank(2L, "T2", MOTOR, "0.5"), "1000")));
        // T1 was measured today, T2 only yesterday, T3 never
        TankMeasurement today = measurement(2L, DATE, reading(1L, "100", "500"));
        today.setMeasuredAt(LocalDateTime.of(2026, 8, 5, 9, 30));
        TankMeasurement yesterday = measurement(1L, DATE.minusDays(1), reading(2L, "90", "300"));
        yesterday.setMeasuredAt(LocalDateTime.of(2026, 8, 4, 9, 0));
        when(measurementRepository.findLatestContainingTank(1L)).thenReturn(Optional.of(today));
        when(measurementRepository.findLatestContainingTank(2L)).thenReturn(Optional.of(yesterday));
        when(measurementRepository.findLatestContainingTank(3L)).thenReturn(Optional.empty());
        when(alertService.findAll(null)).thenReturn(List.of());
    }

    @Test
    void showsEachTankWithItsOwnLastMeasurementSortedByCode() {
        InventoryHome home = service.get();

        assertThat(home.getTanks()).extracting(TankSnapshot::getTankCode).containsExactly("T1", "T2", "T3");
        TankSnapshot t1 = home.getTanks().get(0);
        assertThat(t1.getHeightCm()).isEqualByComparingTo("100");
        assertThat(t1.getGallons()).isEqualByComparingTo("500");
        assertThat(t1.getCapacityGallons()).isEqualByComparingTo("1000");
        assertThat(t1.getPercentage()).isEqualByComparingTo("50.0");
        assertThat(t1.getMeasuredOn()).isEqualTo(DATE);
        assertThat(t1.getMeasuredAt()).isEqualTo(LocalDateTime.of(2026, 8, 5, 9, 30));
        // T2 keeps the reading of the previous journal
        TankSnapshot t2 = home.getTanks().get(1);
        assertThat(t2.getGallons()).isEqualByComparingTo("300");
        assertThat(t2.getMeasuredOn()).isEqualTo(DATE.minusDays(1));
    }

    @Test
    void aTankThatWasNeverMeasuredHasNoReading() {
        TankSnapshot t3 = service.get().getTanks().get(2);

        assertThat(t3.getGallons()).isNull();
        assertThat(t3.getHeightCm()).isNull();
        assertThat(t3.getPercentage()).isNull();
        assertThat(t3.getMeasuredOn()).isNull();
        assertThat(t3.getCapacityGallons()).isEqualByComparingTo("2000");
    }

    @Test
    void addsUpTheTanksOfEachFuel() {
        List<FuelStockSummary> fuels = service.get().getFuels();

        assertThat(fuels).extracting(FuelStockSummary::getFuelType).containsExactly(MOTOR, DIESEL);
        FuelStockSummary motor = fuels.get(0);
        assertThat(motor.getCurrentGallons()).isEqualByComparingTo("800");
        assertThat(motor.getCapacityGallons()).isEqualByComparingTo("2000");
        assertThat(motor.getPercentage()).isEqualByComparingTo("40.0");
        assertThat(motor.isComplete()).isTrue();
        FuelStockSummary diesel = fuels.get(1);
        assertThat(diesel.getCurrentGallons()).isEqualByComparingTo("0");
        assertThat(diesel.isComplete()).isFalse();
    }

    @Test
    void marksTheTanksOfAFuelWithAnOpenAlertEvenIfItWasAttended() {
        when(alertService.findAll(null)).thenReturn(List.of(
                alert(1L, MOTOR, AutonomyLevel.WARNING, AlertStatus.ATTENDED, "4.00", DATE)));

        InventoryHome home = service.get();

        assertThat(home.getTanks()).extracting(TankSnapshot::getAlertLevel)
                .containsExactly(AutonomyLevel.WARNING, AutonomyLevel.WARNING, null);
        assertThat(home.getFuels().get(0).getAlertLevel()).isEqualTo(AutonomyLevel.WARNING);
        assertThat(home.getFuels().get(1).getAlertLevel()).isNull();
    }

    @Test
    void theBannerShowsTheMostCriticalPendingAlert() {
        InventoryAlert warning = alert(1L, MOTOR, AutonomyLevel.WARNING, AlertStatus.ACTIVE, "4.00", DATE);
        InventoryAlert critical = alert(2L, DIESEL, AutonomyLevel.CRITICAL, AlertStatus.READ, "1.50", DATE);
        InventoryAlert criticalWorse = alert(3L, DIESEL, AutonomyLevel.CRITICAL, AlertStatus.ACTIVE, "0.80", DATE);
        InventoryAlert attended = alert(4L, MOTOR, AutonomyLevel.CRITICAL, AlertStatus.ATTENDED, "0.20", DATE);
        when(alertService.findAll(null)).thenReturn(List.of(warning, critical, criticalWorse, attended));

        InventoryHome home = service.get();

        // The attended one is lower than all of them but it no longer asks for attention
        assertThat(home.getMostCriticalAlert().getId()).isEqualTo(3L);
        assertThat(home.getPendingAlerts()).isEqualTo(3);
    }

    @Test
    void withoutAlertsThereIsNoBannerAndTheCountIsZero() {
        InventoryHome home = service.get();

        assertThat(home.getMostCriticalAlert()).isNull();
        assertThat(home.getPendingAlerts()).isZero();
    }

    private Tank withCapacity(Tank tank, String capacity) {
        tank.setMaxCapacityGallons(new BigDecimal(capacity));
        return tank;
    }
}