package com.edu.unicauca.gasstation.backend.inventory.domain.services;

import com.edu.unicauca.gasstation.backend.inventory.FuelType;
import com.edu.unicauca.gasstation.backend.inventory.SalesHistoryPort;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.*;
import com.edu.unicauca.gasstation.backend.inventory.domain.repositories.TankMeasurementRepository;
import com.edu.unicauca.gasstation.backend.inventory.domain.repositories.TankRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class FuelAutonomyService {

    private static final int DAYS_SCALE = 2;

    private final TankRepository tankRepository;
    private final TankMeasurementRepository measurementRepository;
    private final SalesHistoryPort salesHistoryPort;
    private final AutonomyThresholdService thresholdService;
    private final int historyWeeks;

    public FuelAutonomyService(TankRepository tankRepository,
                               TankMeasurementRepository measurementRepository,
                               SalesHistoryPort salesHistoryPort,
                               AutonomyThresholdService thresholdService,
                               @Value("${inventory.autonomy.history-weeks:4}") int historyWeeks) {
        this.tankRepository = tankRepository;
        this.measurementRepository = measurementRepository;
        this.salesHistoryPort = salesHistoryPort;
        this.thresholdService = thresholdService;
        this.historyWeeks = historyWeeks;
    }

    /**
     * Days of autonomy per fuel: measured stock divided by the average sales of the same day of
     * the week (not a flat average). The level uses the thresholds in force on the journal date.
     */
    public List<FuelAutonomy> calculate(LocalDate journalDate) {
        Map<FuelType, List<Tank>> tanksByFuel = tankRepository.findAllOrderByCode().stream()
                .collect(Collectors.groupingBy(Tank::getFuelType,
                        () -> new EnumMap<>(FuelType.class), Collectors.toList()));
        TankMeasurement measurement = measurementRepository.findByJournalDate(journalDate).orElse(null);
        Map<FuelType, BigDecimal> expectedSales =
                salesHistoryPort.averageGallonsForSameWeekday(journalDate, historyWeeks);
        AutonomyThreshold threshold = thresholdService.findEffectiveOn(journalDate).orElse(null);

        return tanksByFuel.entrySet().stream()
                .map(entry -> calculateFuel(entry.getKey(), entry.getValue(), measurement,
                        expectedSales.get(entry.getKey()), threshold))
                .toList();
    }

    private FuelAutonomy calculateFuel(FuelType fuelType, List<Tank> tanks, TankMeasurement measurement,
                                       BigDecimal expectedSales, AutonomyThreshold threshold) {
        List<TankStock> stocks = new ArrayList<>();
        BigDecimal stock = BigDecimal.ZERO;
        boolean complete = measurement != null;
        for (Tank tank : tanks) {
            Optional<TankReading> reading = measurement == null
                    ? Optional.empty() : measurement.readingOf(tank.getId());
            if (reading.isEmpty()) {
                complete = false;
                continue;
            }
            TankReading found = reading.get();
            stocks.add(new TankStock(tank.getId(), tank.getCode(),
                    found.getHeightCm(), found.getVolumeGallons()));
            stock = stock.add(found.getVolumeGallons());
        }

        FuelAutonomy.FuelAutonomyBuilder builder = FuelAutonomy.builder()
                .fuelType(fuelType)
                .tanks(stocks)
                .warningDays(threshold == null ? null : threshold.getWarningDays())
                .criticalDays(threshold == null ? null : threshold.getCriticalDays());

        if (!complete) {
            return builder.status(AutonomyStatus.INCOMPLETE_MEASUREMENT)
                    .level(AutonomyLevel.NOT_EVALUATED).build();
        }
        builder.stockGallons(stock).expectedDailySalesGallons(expectedSales);
        if (expectedSales == null || expectedSales.signum() <= 0) {
            return builder.status(AutonomyStatus.NO_SALES_HISTORY)
                    .level(AutonomyLevel.NOT_EVALUATED).build();
        }

        BigDecimal days = stock.divide(expectedSales, DAYS_SCALE, RoundingMode.HALF_UP);
        return builder.status(AutonomyStatus.CALCULATED)
                .autonomyDays(days)
                .level(levelOf(days, threshold))
                .build();
    }

    private AutonomyLevel levelOf(BigDecimal days, AutonomyThreshold threshold) {
        if (threshold == null) {
            return AutonomyLevel.NOT_EVALUATED;
        }
        if (days.compareTo(threshold.getCriticalDays()) <= 0) {
            return AutonomyLevel.CRITICAL;
        }
        if (days.compareTo(threshold.getWarningDays()) <= 0) {
            return AutonomyLevel.WARNING;
        }
        return AutonomyLevel.OK;
    }
}