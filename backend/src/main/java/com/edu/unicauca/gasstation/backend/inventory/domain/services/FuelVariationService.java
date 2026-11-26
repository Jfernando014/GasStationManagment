package com.edu.unicauca.gasstation.backend.inventory.domain.services;

import com.edu.unicauca.gasstation.backend.inventory.FuelType;
import com.edu.unicauca.gasstation.backend.inventory.SalesSummary;
import com.edu.unicauca.gasstation.backend.inventory.SalesSummaryPort;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.FuelReception;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.FuelVariation;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.GaugeTable;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.Tank;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.TankMeasurement;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.TankReading;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.VariationStatus;
import com.edu.unicauca.gasstation.backend.inventory.domain.repositories.FuelReceptionRepository;
import com.edu.unicauca.gasstation.backend.inventory.domain.repositories.TankMeasurementRepository;
import com.edu.unicauca.gasstation.backend.inventory.domain.repositories.TankRepository;
import com.edu.unicauca.gasstation.backend.inventory.exception.HeightExceedsGaugeException;
import com.edu.unicauca.gasstation.backend.inventory.exception.TankWithoutGaugeTableException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
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
        Map<FuelType, List<Tank>> tanksByFuel = tanksByFuel();

        TankMeasurement current = measurementRepository.findByJournalDate(journalDate).orElse(null);
        TankMeasurement previous = measurementRepository.findByJournalDate(journalDate.minusDays(1)).orElse(null);
        Map<FuelType, BigDecimal> sold = salesSummaryPort.findByJournalDate(journalDate)
                .map(SalesSummary::gallonsByFuel)
                .orElse(Map.of());
        List<FuelReception> receptions = receptionRepository.search(null, journalDate, journalDate);

        return calculateDay(tanksByFuel, current, previous, sold, receptions, gaugeTableService::calculateVolume);
    }

    /**
     * The same variation as {@link #calculate} for every date between both (inclusive), reading the measurements,
     * receptions, sales and gauge tables of the whole range once.
     */
    public Map<LocalDate, List<FuelVariation>> calculateRange(LocalDate from, LocalDate to) {
        Map<FuelType, List<Tank>> tanksByFuel = tanksByFuel();

        // The day before the range holds the initial stock of its first day
        Map<LocalDate, TankMeasurement> measurements = measurementRepository
                .findByJournalDateBetween(from.minusDays(1), to).stream()
                .collect(Collectors.toMap(TankMeasurement::getJournalDate, Function.identity()));
        Map<LocalDate, Map<FuelType, BigDecimal>> sales = salesSummaryPort.findByJournalDateRange(from, to).stream()
                .collect(Collectors.toMap(SalesSummary::journalDate, SalesSummary::gallonsByFuel));
        Map<LocalDate, List<FuelReception>> receptions = receptionRepository.search(null, from, to).stream()
                .collect(Collectors.groupingBy(FuelReception::getReceptionDate));
        BiFunction<Long, BigDecimal, BigDecimal> volumes = volumesReadingEachTableOnce(tanksByFuel);

        Map<LocalDate, List<FuelVariation>> variations = new LinkedHashMap<>();
        from.datesUntil(to.plusDays(1)).forEach(date -> variations.put(date, calculateDay(tanksByFuel,
                measurements.get(date), measurements.get(date.minusDays(1)), sales.getOrDefault(date, Map.of()),
                receptions.getOrDefault(date, List.of()), volumes)));
        return variations;
    }

    private Map<FuelType, List<Tank>> tanksByFuel() {
        return tankRepository.findAllOrderByCode().stream()
                .collect(Collectors.groupingBy(Tank::getFuelType,
                        () -> new EnumMap<>(FuelType.class), Collectors.toList()));
    }

    private List<FuelVariation> calculateDay(Map<FuelType, List<Tank>> tanksByFuel, TankMeasurement current,
                                             TankMeasurement previous, Map<FuelType, BigDecimal> sold,
                                             List<FuelReception> receptions,
                                             BiFunction<Long, BigDecimal, BigDecimal> volumes) {
        Map<Long, BigDecimal> receivedByTank = receptions.stream()
                .collect(Collectors.groupingBy(FuelReception::getTankId,
                        Collectors.reducing(BigDecimal.ZERO, FuelReception::getGallons, BigDecimal::add)));

        return tanksByFuel.entrySet().stream()
                .map(entry -> calculateFuel(entry.getKey(), entry.getValue(), current, previous,
                        sold.get(entry.getKey()), receivedByTank, volumes))
                .toList();
    }

    private FuelVariation calculateFuel(FuelType fuelType, List<Tank> tanks, TankMeasurement current,
                                        TankMeasurement previous, BigDecimal sold,
                                        Map<Long, BigDecimal> receivedByTank,
                                        BiFunction<Long, BigDecimal, BigDecimal> volumes) {
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
        BigDecimal tolerance = toleranceGallons(tanks, current, volumes);
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
    private BigDecimal toleranceGallons(List<Tank> tanks, TankMeasurement current,
                                        BiFunction<Long, BigDecimal, BigDecimal> volumes) {
        BigDecimal total = BigDecimal.ZERO;
        for (Tank tank : tanks) {
            BigDecimal toleranceCm = tank.getToleranceCm();
            if (toleranceCm == null || toleranceCm.signum() == 0) {
                continue;
            }
            BigDecimal height = current.readingOf(tank.getId()).orElseThrow().getHeightCm();
            BigDecimal low = height.subtract(toleranceCm).max(BigDecimal.ZERO);
            BigDecimal span = volumeAbove(tank.getId(), height.add(toleranceCm), height, volumes)
                    .subtract(volumes.apply(tank.getId(), low));
            total = total.add(span.multiply(HALF));
        }
        return total.setScale(TOLERANCE_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Volume at the upper bound, or at the measured height if the bound is beyond the gauge table.
     */
    private BigDecimal volumeAbove(Long tankId, BigDecimal upper, BigDecimal height,
                                   BiFunction<Long, BigDecimal, BigDecimal> volumes) {
        try {
            return volumes.apply(tankId, upper);
        } catch (HeightExceedsGaugeException e) {
            return volumes.apply(tankId, height);
        }
    }

    /** Volume of a tank at a height, keeping each gauge table in memory after reading it. */
    private BiFunction<Long, BigDecimal, BigDecimal> volumesReadingEachTableOnce(
            Map<FuelType, List<Tank>> tanksByFuel) {
        Map<Long, Tank> tanksById = tanksByFuel.values().stream()
                .flatMap(List::stream)
                .collect(Collectors.toMap(Tank::getId, Function.identity()));
        Map<Long, GaugeTable> tables = new HashMap<>();
        return (tankId, height) -> {
            Long tableId = tanksById.get(tankId).getGaugeTableId();
            if (tableId == null) {
                throw new TankWithoutGaugeTableException(tankId);
            }
            return gaugeTableService.calculateVolume(
                    tables.computeIfAbsent(tableId, gaugeTableService::findById), height);
        };
    }
}