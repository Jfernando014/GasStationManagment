package com.edu.unicauca.gasstation.backend.inventory.domain.services;

import com.edu.unicauca.gasstation.backend.inventory.FuelType;
import com.edu.unicauca.gasstation.backend.inventory.SalesHistoryPort;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.AutonomyLevel;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.AutonomyStatus;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.AutonomyThreshold;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.FuelAutonomy;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.Tank;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.TankMeasurement;
import com.edu.unicauca.gasstation.backend.inventory.domain.repositories.TankMeasurementRepository;
import com.edu.unicauca.gasstation.backend.inventory.domain.repositories.TankRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.edu.unicauca.gasstation.backend.inventory.FuelTestData.tank;
import static com.edu.unicauca.gasstation.backend.inventory.MeasurementTestData.measurement;
import static com.edu.unicauca.gasstation.backend.inventory.MeasurementTestData.reading;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FuelAutonomyServiceTest {

    private static final LocalDate DATE = LocalDate.of(2026, 8, 5);
    private static final int WEEKS = 4;

    @Mock
    private TankRepository tankRepository;
    @Mock
    private TankMeasurementRepository measurementRepository;
    @Mock
    private SalesHistoryPort salesHistoryPort;
    @Mock
    private AutonomyThresholdService thresholdService;

    private FuelAutonomyService service;

    @BeforeEach
    void setUp() {
        service = new FuelAutonomyService(tankRepository, measurementRepository, salesHistoryPort,
                thresholdService, WEEKS);
    }

    // ---------- days and level ----------

    @ParameterizedTest
    @CsvSource({"500,6.00,OK", "600,5.00,WARNING", "1000,3.00,WARNING", "1500,2.00,CRITICAL", "3000,1.00,CRITICAL"})
    void levelFollowsTheThresholds(String expectedSales, String days, AutonomyLevel level) {
        given(motorTanks(), motorMeasurement(), Map.of(FuelType.MOTOR, new BigDecimal(expectedSales)),
                threshold("5.0", "2.0"));

        FuelAutonomy autonomy = onlyFuel();

        assertThat(autonomy.getStatus()).isEqualTo(AutonomyStatus.CALCULATED);
        assertThat(autonomy.getStockGallons()).isEqualByComparingTo("3000.00");
        assertThat(autonomy.getAutonomyDays()).isEqualByComparingTo(days);
        assertThat(autonomy.getLevel()).isEqualTo(level);
        assertThat(autonomy.getWarningDays()).isEqualByComparingTo("5.0");
        assertThat(autonomy.getCriticalDays()).isEqualByComparingTo("2.0");
    }

    @Test
    void daysAreRoundedToTwoDecimals() {
        given(motorTanks(), motorMeasurement(), Map.of(FuelType.MOTOR, new BigDecimal("700")), threshold("5.0", "2.0"));

        assertThat(onlyFuel().getAutonomyDays()).isEqualByComparingTo("4.29");
    }

    @Test
    void usesTheConfiguredNumberOfWeeksOfHistory() {
        given(motorTanks(), motorMeasurement(), Map.of(FuelType.MOTOR, new BigDecimal("600")), threshold("5.0", "2.0"));

        service.calculate(DATE);

        verify(salesHistoryPort).averageGallonsForSameWeekday(DATE, WEEKS);
    }

    @Test
    void levelIsNotEvaluatedWhenNoThresholdApplies() {
        given(motorTanks(), motorMeasurement(), Map.of(FuelType.MOTOR, new BigDecimal("600")), null);

        FuelAutonomy autonomy = onlyFuel();

        assertThat(autonomy.getAutonomyDays()).isEqualByComparingTo("5.00");
        assertThat(autonomy.getLevel()).isEqualTo(AutonomyLevel.NOT_EVALUATED);
        assertThat(autonomy.getWarningDays()).isNull();
        assertThat(autonomy.getCriticalDays()).isNull();
    }

    @Test
    void theAlertListsTheMeasuredTanksWithTheirLevel() {
        given(motorTanks(), motorMeasurement(), Map.of(FuelType.MOTOR, new BigDecimal("600")), threshold("5.0", "2.0"));

        FuelAutonomy autonomy = onlyFuel();

        assertThat(autonomy.getTanks()).hasSize(2);
        assertThat(autonomy.getTanks().get(0).getTankId()).isEqualTo(1L);
        assertThat(autonomy.getTanks().get(0).getTankCode()).isEqualTo("T1C1");
        assertThat(autonomy.getTanks().get(0).getHeightCm()).isEqualByComparingTo("100");
        assertThat(autonomy.getTanks().get(0).getGallons()).isEqualByComparingTo("1800.00");
        assertThat(autonomy.getTanks().get(1).getTankCode()).isEqualTo("T1C2");
    }

    @Test
    void eachFuelIsEvaluatedOnItsOwn() {
        List<Tank> tanks = List.of(tank(1L, "T1C1", FuelType.MOTOR, "0"), tank(4L, "T3", FuelType.DIESEL, "0"));
        given(tanks, measurement(1L, DATE, reading(1L, "100", "1800.00"), reading(4L, "80", "900.00")),
                Map.of(FuelType.MOTOR, new BigDecimal("600")), threshold("5.0", "2.0"));

        List<FuelAutonomy> result = service.calculate(DATE);

        assertThat(result).extracting(FuelAutonomy::getFuelType).containsExactly(FuelType.MOTOR, FuelType.DIESEL);
        assertThat(result.get(0).getStatus()).isEqualTo(AutonomyStatus.CALCULATED);
        assertThat(result.get(0).getAutonomyDays()).isEqualByComparingTo("3.00");
        assertThat(result.get(1).getStatus()).isEqualTo(AutonomyStatus.NO_SALES_HISTORY);
    }

    // ---------- statuses ----------

    @Test
    void incompleteWhenATankWasNotMeasured() {
        given(motorTanks(), measurement(1L, DATE, reading(1L, "100", "1800.00")),
                Map.of(FuelType.MOTOR, new BigDecimal("600")), threshold("5.0", "2.0"));

        FuelAutonomy autonomy = onlyFuel();

        assertThat(autonomy.getStatus()).isEqualTo(AutonomyStatus.INCOMPLETE_MEASUREMENT);
        assertThat(autonomy.getStockGallons()).isNull();
        assertThat(autonomy.getAutonomyDays()).isNull();
        assertThat(autonomy.getLevel()).isEqualTo(AutonomyLevel.NOT_EVALUATED);
        assertThat(autonomy.getTanks()).hasSize(1);
    }

    @Test
    void incompleteWhenThereIsNoMeasurement() {
        given(motorTanks(), null, Map.of(FuelType.MOTOR, new BigDecimal("600")), threshold("5.0", "2.0"));

        FuelAutonomy autonomy = onlyFuel();

        assertThat(autonomy.getStatus()).isEqualTo(AutonomyStatus.INCOMPLETE_MEASUREMENT);
        assertThat(autonomy.getTanks()).isEmpty();
    }

    @Test
    void noSalesHistoryWhenTheFuelHasNoAverage() {
        given(motorTanks(), motorMeasurement(), Map.of(), threshold("5.0", "2.0"));

        FuelAutonomy autonomy = onlyFuel();

        assertThat(autonomy.getStatus()).isEqualTo(AutonomyStatus.NO_SALES_HISTORY);
        assertThat(autonomy.getStockGallons()).isEqualByComparingTo("3000.00");
        assertThat(autonomy.getAutonomyDays()).isNull();
        assertThat(autonomy.getLevel()).isEqualTo(AutonomyLevel.NOT_EVALUATED);
    }

    @Test
    void noSalesHistoryWhenTheAverageIsZero() {
        given(motorTanks(), motorMeasurement(), Map.of(FuelType.MOTOR, BigDecimal.ZERO), threshold("5.0", "2.0"));

        assertThat(onlyFuel().getStatus()).isEqualTo(AutonomyStatus.NO_SALES_HISTORY);
    }

    // ---------- helpers ----------

    private FuelAutonomy onlyFuel() {
        List<FuelAutonomy> result = service.calculate(DATE);
        assertThat(result).hasSize(1);
        return result.get(0);
    }

    private List<Tank> motorTanks() {
        return List.of(tank(1L, "T1C1", FuelType.MOTOR, "0"), tank(2L, "T1C2", FuelType.MOTOR, "0"));
    }

    /** 1800 + 1200 = 3000 net gallons. */
    private TankMeasurement motorMeasurement() {
        return measurement(1L, DATE, reading(1L, "100", "1800.00"), reading(2L, "90", "1200.00"));
    }

    private AutonomyThreshold threshold(String warning, String critical) {
        AutonomyThreshold threshold = new AutonomyThreshold();
        threshold.setValidFrom(LocalDate.of(2026, 8, 1));
        threshold.setWarningDays(new BigDecimal(warning));
        threshold.setCriticalDays(new BigDecimal(critical));
        return threshold;
    }

    private void given(List<Tank> tanks, TankMeasurement measurement, Map<FuelType, BigDecimal> history,
                       AutonomyThreshold threshold) {
        when(tankRepository.findAllOrderByCode()).thenReturn(tanks);
        when(measurementRepository.findByJournalDate(DATE)).thenReturn(Optional.ofNullable(measurement));
        when(salesHistoryPort.averageGallonsForSameWeekday(DATE, WEEKS)).thenReturn(history);
        when(thresholdService.findEffectiveOn(DATE)).thenReturn(Optional.ofNullable(threshold));
    }
}