package com.edu.unicauca.gasstation.backend.inventory.domain.services;

import com.edu.unicauca.gasstation.backend.inventory.FuelType;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.AlertStatus;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.AutonomyLevel;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.FuelStockSummary;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.InventoryAlert;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.InventoryHome;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.Tank;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.TankMeasurement;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.TankReading;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.TankSnapshot;
import com.edu.unicauca.gasstation.backend.inventory.domain.repositories.TankMeasurementRepository;
import com.edu.unicauca.gasstation.backend.inventory.domain.repositories.TankRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InventoryHomeService {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private static final Comparator<InventoryAlert> MOST_CRITICAL_FIRST =
            Comparator.comparingInt((InventoryAlert alert) -> alert.getLevel() == AutonomyLevel.CRITICAL ? 0 : 1)
                    .thenComparing(InventoryAlert::getAutonomyDays);

    private final TankRepository tankRepository;
    private final TankMeasurementRepository measurementRepository;
    private final InventoryAlertService alertService;

    public InventoryHome get() {
        List<InventoryAlert> open = alertService.findAll(null);
        Map<FuelType, AutonomyLevel> alertLevels = open.stream().collect(Collectors.toMap(
                InventoryAlert::getFuelType, InventoryAlert::getLevel,
                (first, second) -> first == AutonomyLevel.CRITICAL ? first : second));

        List<TankSnapshot> tanks = tankRepository.findAllOrderByCode().stream()
                .sorted(Comparator.comparing(Tank::getCode))
                .map(tank -> snapshot(tank, alertLevels.get(tank.getFuelType())))
                .toList();

        List<InventoryAlert> pending = open.stream()
                .filter(alert -> alert.getStatus() != AlertStatus.ATTENDED)
                .toList();

        return InventoryHome.builder()
                .fuels(summarize(tanks, alertLevels))
                .tanks(tanks)
                .mostCriticalAlert(pending.stream().min(MOST_CRITICAL_FIRST).orElse(null))
                .pendingAlerts(pending.size())
                .build();
    }

    private TankSnapshot snapshot(Tank tank, AutonomyLevel alertLevel) {
        Optional<TankMeasurement> latest = measurementRepository.findLatestContainingTank(tank.getId());
        Optional<TankReading> reading = latest.flatMap(measurement -> measurement.readingOf(tank.getId()));
        BigDecimal gallons = reading.map(TankReading::getVolumeGallons).orElse(null);
        BigDecimal capacity = tank.getMaxCapacityGallons();
        return TankSnapshot.builder()
                .tankId(tank.getId())
                .tankCode(tank.getCode())
                .fuelType(tank.getFuelType())
                .heightCm(reading.map(TankReading::getHeightCm).orElse(null))
                .gallons(gallons)
                .capacityGallons(capacity)
                .percentage(percentage(gallons, capacity))
                .measuredOn(latest.map(TankMeasurement::getJournalDate).orElse(null))
                .measuredAt(latest.map(TankMeasurement::getMeasuredAt).orElse(null))
                .alertLevel(alertLevel)
                .build();
    }

    private List<FuelStockSummary> summarize(List<TankSnapshot> tanks, Map<FuelType, AutonomyLevel> alertLevels) {
        Map<FuelType, List<TankSnapshot>> byFuel = tanks.stream().collect(Collectors.groupingBy(
                TankSnapshot::getFuelType, () -> new EnumMap<>(FuelType.class), Collectors.toList()));
        return byFuel.entrySet().stream().map(entry -> {
            List<TankSnapshot> group = entry.getValue();
            BigDecimal current = sum(group.stream().map(TankSnapshot::getGallons));
            BigDecimal capacity = sum(group.stream().map(TankSnapshot::getCapacityGallons));
            return FuelStockSummary.builder()
                    .fuelType(entry.getKey())
                    .currentGallons(current)
                    .capacityGallons(capacity)
                    .percentage(percentage(current, capacity))
                    .complete(group.stream().allMatch(tank -> tank.getGallons() != null))
                    .alertLevel(alertLevels.get(entry.getKey()))
                    .build();
        }).toList();
    }

    private BigDecimal sum(java.util.stream.Stream<BigDecimal> values) {
        return values.filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal percentage(BigDecimal current, BigDecimal capacity) {
        if (current == null || capacity == null || capacity.signum() <= 0) {
            return null;
        }
        return current.multiply(HUNDRED).divide(capacity, 1, RoundingMode.HALF_UP);
    }
}