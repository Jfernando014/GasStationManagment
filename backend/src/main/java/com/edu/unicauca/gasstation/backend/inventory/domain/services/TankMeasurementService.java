package com.edu.unicauca.gasstation.backend.inventory.domain.services;

import com.edu.unicauca.gasstation.backend.inventory.domain.models.GaugeTable;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.Tank;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.TankMeasurement;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.TankReading;
import com.edu.unicauca.gasstation.backend.inventory.domain.repositories.TankMeasurementRepository;
import com.edu.unicauca.gasstation.backend.inventory.domain.repositories.TankRepository;
import com.edu.unicauca.gasstation.backend.inventory.exception.InvalidTankMeasurementException;
import com.edu.unicauca.gasstation.backend.inventory.exception.TankNotFoundException;
import com.edu.unicauca.gasstation.backend.inventory.exception.TankWithoutGaugeTableException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TankMeasurementService {

    private final TankMeasurementRepository measurementRepository;
    private final TankRepository tankRepository;
    private final GaugeTableService gaugeTableService;
    private final Clock clock;

    /**
     * Guarda las lecturas de la jornada. Cada lectura trae tankId y heightCm; los galones se calculan aquí.
     * Si ya existe una medición para esa fecha, las lecturas enviadas reemplazan las del mismo tanque
     * y las de los demás tanques se conservan. Todo se calcula antes de guardar: si una lectura falla, no se guarda nada.
     */
    @Transactional
    public TankMeasurement save(LocalDate journalDate, List<TankReading> requested) {
        requireValid(requested);

        Map<Long, GaugeTable> tablesById = new HashMap<>();
        Map<Long, TankReading> calculated = new LinkedHashMap<>();
        for (TankReading request : requested) {
            Tank tank = tankRepository.findById(request.getTankId())
                    .orElseThrow(() -> new TankNotFoundException(request.getTankId()));
            if (tank.getGaugeTableId() == null) {
                throw new TankWithoutGaugeTableException(tank.getId());
            }
            // Los tanques del mismo tipo comparten tabla: se carga una sola vez por operación
            GaugeTable table = tablesById.computeIfAbsent(tank.getGaugeTableId(), gaugeTableService::findById);
            calculated.put(tank.getId(), new TankReading(
                    tank.getId(), request.getHeightCm(), gaugeTableService.calculateVolume(table, request.getHeightCm())));
        }

        TankMeasurement measurement = measurementRepository.findByJournalDate(journalDate)
                .orElseGet(() -> newMeasurement(journalDate));
        Map<Long, TankReading> merged = new LinkedHashMap<>();
        measurement.getReadings().forEach(r -> merged.put(r.getTankId(), r));
        merged.putAll(calculated);
        measurement.setReadings(List.copyOf(merged.values()));
        measurement.setMeasuredAt(LocalDateTime.now(clock));

        return measurementRepository.save(measurement);
    }

    @Transactional(readOnly = true)
    public Optional<TankMeasurement> findByJournalDate(LocalDate journalDate) {
        return measurementRepository.findByJournalDate(journalDate);
    }

    /** Última medición anterior a la fecha: es el "inicial" de la jornada. */
    @Transactional(readOnly = true)
    public Optional<TankMeasurement> findLatestBefore(LocalDate journalDate) {
        return measurementRepository.findLatestBefore(journalDate);
    }

    private TankMeasurement newMeasurement(LocalDate journalDate) {
        TankMeasurement measurement = new TankMeasurement();
        measurement.setJournalDate(journalDate);
        return measurement;
    }

    private void requireValid(List<TankReading> requested) {
        if (requested == null || requested.isEmpty()) {
            throw new InvalidTankMeasurementException("The measurement must include at least one tank reading");
        }
        Set<Long> seen = new HashSet<>();
        for (TankReading reading : requested) {
            if (!seen.add(reading.getTankId())) {
                throw new InvalidTankMeasurementException("The measurement includes the same tank more than once");
            }
        }
    }
}