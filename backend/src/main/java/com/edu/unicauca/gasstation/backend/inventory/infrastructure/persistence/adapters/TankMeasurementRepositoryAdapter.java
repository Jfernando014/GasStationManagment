package com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.adapters;

import com.edu.unicauca.gasstation.backend.inventory.domain.models.TankMeasurement;
import com.edu.unicauca.gasstation.backend.inventory.domain.repositories.TankMeasurementRepository;
import com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.entities.TankMeasurementEntity;
import com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.entities.TankReadingEntity;
import com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.mappers.TankMeasurementPersistenceMapper;
import com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.repositories.JpaTankMeasurementRepository;
import com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.repositories.JpaTankReadingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class TankMeasurementRepositoryAdapter implements TankMeasurementRepository {

    private final JpaTankMeasurementRepository measurementRepository;
    private final JpaTankReadingRepository readingRepository;
    private final TankMeasurementPersistenceMapper mapper;

    @Override
    @Transactional
    public TankMeasurement save(TankMeasurement measurement) {
        TankMeasurementEntity saved = measurementRepository.saveAndFlush(mapper.toEntity(measurement));
        if (measurement.getId() != null) {
            // Reemplazo: se borran las lecturas anteriores antes de insertar las nuevas
            readingRepository.deleteByMeasurementId(saved.getId());
        }
        List<TankReadingEntity> readings = measurement.getReadings().stream()
                .map(r -> mapper.toReadingEntity(r, saved.getId()))
                .toList();
        List<TankReadingEntity> savedReadings = readingRepository.saveAll(readings);
        readingRepository.flush();
        return mapper.toDomain(saved, savedReadings);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TankMeasurement> findByJournalDate(LocalDate journalDate) {
        return measurementRepository.findByJournalDate(journalDate).map(this::withReadings);
    }

    @Override
    public boolean existsByJournalDate(LocalDate journalDate) {
        return measurementRepository.existsByJournalDate(journalDate);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TankMeasurement> findLatestBefore(LocalDate journalDate) {
        return measurementRepository.findFirstByJournalDateLessThanOrderByJournalDateDesc(journalDate)
                .map(this::withReadings);
    }

    private TankMeasurement withReadings(TankMeasurementEntity entity) {
        return mapper.toDomain(entity, readingRepository.findByMeasurementIdOrderByTankId(entity.getId()));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TankMeasurement> findLatestContainingTank(Long tankId) {
        return measurementRepository.findContainingTank(tankId, PageRequest.of(0, 1)).stream()
                .findFirst()
                .map(this::withReadings);
    }
}