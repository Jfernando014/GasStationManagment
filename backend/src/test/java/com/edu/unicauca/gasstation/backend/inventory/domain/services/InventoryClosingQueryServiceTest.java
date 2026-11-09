package com.edu.unicauca.gasstation.backend.inventory.domain.services;

import com.edu.unicauca.gasstation.backend.inventory.FuelType;
import com.edu.unicauca.gasstation.backend.inventory.InventoryClosingSnapshot;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.AlertStatus;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.AutonomyLevel;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.FuelVariation;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.VariationStatus;
import com.edu.unicauca.gasstation.backend.inventory.domain.repositories.InventoryAlertRepository;
import com.edu.unicauca.gasstation.backend.inventory.domain.repositories.TankMeasurementRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static com.edu.unicauca.gasstation.backend.inventory.AlertTestData.alert;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryClosingQueryServiceTest {

    private static final LocalDate DATE = LocalDate.of(2026, 8, 5);

    @Mock
    private TankMeasurementRepository measurementRepository;
    @Mock
    private FuelVariationService variationService;
    @Mock
    private InventoryAlertRepository alertRepository;

    @InjectMocks
    private InventoryClosingQueryService service;

    @Test
    void publishesTheMeasurementFlagTheVariationsAndTheNewAlerts() {
        when(measurementRepository.existsByJournalDate(DATE)).thenReturn(true);
        when(variationService.calculate(DATE)).thenReturn(List.of(variation()));
        when(alertRepository.findByOpenedOn(DATE)).thenReturn(List.of(
                alert(7L, FuelType.MOTOR, AutonomyLevel.CRITICAL, AlertStatus.ACTIVE, "1.25", DATE)));

        InventoryClosingSnapshot snapshot = service.findClosingSnapshot(DATE);

        assertThat(snapshot.hasMeasurement()).isTrue();
        assertThat(snapshot.variations()).hasSize(1);
        assertThat(snapshot.variations().get(0).fuelType()).isEqualTo("MOTOR");
        assertThat(snapshot.variations().get(0).status()).isEqualTo("OK");
        assertThat(snapshot.variations().get(0).varianceGallons()).isEqualByComparingTo("-39.863");
        assertThat(snapshot.variations().get(0).outOfTolerance()).isTrue();
        assertThat(snapshot.alerts()).hasSize(1);
        assertThat(snapshot.alerts().get(0).id()).isEqualTo(7L);
        assertThat(snapshot.alerts().get(0).alertType()).isEqualTo("AUTONOMY");
        assertThat(snapshot.alerts().get(0).fuelType()).isEqualTo("MOTOR");
        assertThat(snapshot.alerts().get(0).level()).isEqualTo("CRITICAL");
        assertThat(snapshot.alerts().get(0).status()).isEqualTo("ACTIVE");
        assertThat(snapshot.alerts().get(0).autonomyDays()).isEqualByComparingTo("1.25");
        assertThat(snapshot.alerts().get(0).openedOn()).isEqualTo(DATE);
    }

    @Test
    void returnsAnEmptySnapshotWhenThereIsNoJournalDate() {
        InventoryClosingSnapshot snapshot = service.findClosingSnapshot(null);

        assertThat(snapshot.hasMeasurement()).isFalse();
        assertThat(snapshot.variations()).isEmpty();
        assertThat(snapshot.alerts()).isEmpty();
    }

    private FuelVariation variation() {
        return FuelVariation.builder()
                .fuelType(FuelType.MOTOR)
                .status(VariationStatus.OK)
                .initialGallons(new BigDecimal("1558.05"))
                .entriesGallons(new BigDecimal("3970"))
                .soldGallons(new BigDecimal("784.587"))
                .theoreticalGallons(new BigDecimal("4743.463"))
                .measuredGallons(new BigDecimal("4703.60"))
                .varianceGallons(new BigDecimal("-39.863"))
                .toleranceGallons(new BigDecimal("30"))
                .outOfTolerance(true)
                .build();
    }
}
