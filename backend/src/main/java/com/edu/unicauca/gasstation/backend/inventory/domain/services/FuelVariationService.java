package com.edu.unicauca.gasstation.backend.inventory.domain.services;

import com.edu.unicauca.gasstation.backend.inventory.FuelType;
import com.edu.unicauca.gasstation.backend.inventory.SalesSummary;
import com.edu.unicauca.gasstation.backend.inventory.SalesSummaryPort;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.FuelReception;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.FuelVariation;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.Tank;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.TankMeasurement;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.TankReading;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.VariationStatus;
import com.edu.unicauca.gasstation.backend.inventory.domain.repositories.FuelReceptionRepository;
import com.edu.unicauca.gasstation.backend.inventory.domain.repositories.TankMeasurementRepository;
import com.edu.unicauca.gasstation.backend.inventory.domain.repositories.TankRepository;
import com.edu.unicauca.gasstation.backend.inventory.exception.HeightExceedsGaugeException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FuelVariationService {

    private static final BigDecimal HALF = new BigDecimal("0.5");
    private static final int TOLERANCE_SCALE = 3;

    private final TankRepository tankRepository;
    private final TankMeasurementRepository measurementRepository;
    private final FuelReceptionRepository receptionRepository;
    private final SalesSummaryPort salesSummaryPort;
    private final GaugeTableService gaugeTableService;

    /**
     * Variation per fuel for a journal date. The initial stock is the measurement of the
     * immediately preceding date; no earlier measurement is used, because the entries and
     * sales of the days in between would be missing.
     */
    public List<FuelVariation> calculate(LocalDate journalDate) {
        Map<FuelType, List<Tank>> tanksByFuel = tankRepository.findAllOrderByCode().stream()
                .collect(Collectors.groupingBy(Tank::getFuelType,
                        () -> new EnumMap<>(FuelType.class), Collectors.toList()));

        TankMeasurement current = measurementRepository.findByJournalDate(journalDate).orElse(null);
        TankMeasurement previous = measurementRepository.findByJournalDate(journalDate.minusDays(1)).orElse(null);
        Map<FuelType, BigDecimal> sold = salesSummaryPort.findByJournalDate(journalDate)
                .map(SalesSummary::gallonsByFuel)
                .orElse(Map.of());
        Map<Long, BigDecimal> receivedByTank = receptionRepository.search(null, journalDate, journalDate).stream()
                .collect(Collectors.groupingBy(FuelReception::getTankId,
                        Collectors.reducing(BigDecimal.ZERO, FuelReception::getGallons, BigDecimal::add)));

        return tanksByFuel.entrySet().stream()
                .map(entry -> calculateFuel(entry.getKey(), entry.getValue(), current, previous,
                        sold.get(entry.getKey()), receivedByTank))
                .toList();
    }

    private FuelVariation calculateFuel(FuelType fuelType, List<Tank> tanks, TankMeasurement current,
                                        TankMeasurement previous, BigDecimal sold,
                                        Map<Long, BigDecimal> receivedByTank) {
        BigDecimal entries = tanks.stream()
                .map(tank -> receivedByTank.getOrDefault(tank.getId(), BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal measured = totalOf(tanks, current);
        BigDecimal initial = totalOf(tanks, previous);

        FuelVariation.FuelVariationBuilder builder = FuelVariation.builder()
                .fuelType(fuelType)
                .tankIds(tanks.stream().map(Tank::getId).toList())
                .initialGallons(initial)
                .entriesGallons(entries)
                .soldGallons(sold)
                .measuredGallons(measured);

        VariationStatus status = statusOf(measured, previous, initial, sold);
        builder.status(status);
        if (status != VariationStatus.OK) {
            return builder.build();
        }

        BigDecimal theoretical = initial.add(entries).subtract(sold);
        BigDecimal variance = measured.subtract(theoretical);
        BigDecimal tolerance = toleranceGallons(tanks, current);
        return builder
                .theoreticalGallons(theoretical)
                .varianceGallons(variance)
                .toleranceGallons(tolerance)
                .outOfTolerance(variance.abs().compareTo(tolerance) > 0)
                .build();
    }

    private VariationStatus statusOf(BigDecimal measured, TankMeasurement previous,
                                     BigDecimal initial, BigDecimal sold) {
        if (measured == null) {
            return VariationStatus.INCOMPLETE_MEASUREMENT;
        }
        if (previous == null) {
            return VariationStatus.NO_PREVIOUS_MEASUREMENT;
        }
        if (initial == null) {
            return VariationStatus.INCOMPLETE_MEASUREMENT;
        }
        if (sold == null) {
            return VariationStatus.NO_SALES_DATA;
        }
        return VariationStatus.OK;
    }

    /**
     * Net gallons of all the tanks of a fuel, or null if any of them was not measured.
     */
    private BigDecimal totalOf(List<Tank> tanks, TankMeasurement measurement) {
        if (measurement == null) {
            return null;
        }
        BigDecimal total = BigDecimal.ZERO;
        for (Tank tank : tanks) {
            Optional<TankReading> reading = measurement.readingOf(tank.getId());
            if (reading.isEmpty()) {
                return null;
            }
            total = total.add(reading.get().getVolumeGallons());
        }
        return total;
    }

    /**
     * Each tank's tolerance in centimetres is converted with the gauge table around its measured
     * height: half of V(h + tol) - V(h - tol), since the curve is not linear. The tolerances are
     * added up, which is the worst case (every tank wrong in the same direction).
     */
    private BigDecimal toleranceGallons(List<Tank> tanks, TankMeasurement current) {
        BigDecimal total = BigDecimal.ZERO;
        for (Tank tank : tanks) {
            BigDecimal toleranceCm = tank.getToleranceCm();
            if (toleranceCm == null || toleranceCm.signum() == 0) {
                continue;
            }
            BigDecimal height = current.readingOf(tank.getId()).orElseThrow().getHeightCm();
            BigDecimal low = height.subtract(toleranceCm).max(BigDecimal.ZERO);
            BigDecimal span = volumeAbove(tank.getId(), height.add(toleranceCm), height)
                    .subtract(gaugeTableService.calculateVolume(tank.getId(), low));
            total = total.add(span.multiply(HALF));
        }
        return total.setScale(TOLERANCE_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Volume at the upper bound, or at the measured height if the bound is beyond the gauge table.
     */
    private BigDecimal volumeAbove(Long tankId, BigDecimal upper, BigDecimal height) {
        try {
            return gaugeTableService.calculateVolume(tankId, upper);
        } catch (HeightExceedsGaugeException e) {
            return gaugeTableService.calculateVolume(tankId, height);
        }
    }
}