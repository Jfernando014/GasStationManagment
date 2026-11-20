package com.edu.unicauca.gasstation.backend.inventory.domain.services;

import com.edu.unicauca.gasstation.backend.inventory.domain.models.AutonomyThreshold;
import com.edu.unicauca.gasstation.backend.inventory.domain.repositories.AutonomyThresholdRepository;
import com.edu.unicauca.gasstation.backend.inventory.exception.AutonomyThresholdAlreadyExistsException;
import com.edu.unicauca.gasstation.backend.inventory.exception.AutonomyThresholdNotFoundException;
import com.edu.unicauca.gasstation.backend.inventory.exception.InvalidAutonomyThresholdException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AutonomyThresholdService {

    private final AutonomyThresholdRepository repository;
    private final Clock clock;

    @Transactional
    public AutonomyThreshold create(AutonomyThreshold threshold) {
        validate(threshold);
        if (repository.existsByValidFrom(threshold.getValidFrom())) {
            throw new AutonomyThresholdAlreadyExistsException(threshold.getValidFrom());
        }
        threshold.setId(null);
        threshold.setCreatedAt(LocalDateTime.now(clock));
        return repository.save(threshold);
    }

    @Transactional
    public AutonomyThreshold update(Long id, AutonomyThreshold changes) {
        validate(changes);
        AutonomyThreshold existing = findById(id);
        if (repository.existsByValidFromAndIdNot(changes.getValidFrom(), id)) {
            throw new AutonomyThresholdAlreadyExistsException(changes.getValidFrom());
        }
        existing.setValidFrom(changes.getValidFrom());
        existing.setWarningDays(changes.getWarningDays());
        existing.setCriticalDays(changes.getCriticalDays());
        return repository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        findById(id);
        repository.deleteById(id);
    }

    private AutonomyThreshold findById(Long id) {
        return repository.findById(id).orElseThrow(() -> new AutonomyThresholdNotFoundException(id));
    }

    public Optional<AutonomyThreshold> findEffectiveOn(LocalDate date) {
        return repository.findEffectiveOn(date);
    }

    public List<AutonomyThreshold> findAll() {
        return repository.findAll();
    }

    private void validate(AutonomyThreshold threshold) {
        if (threshold.getValidFrom() == null) {
            throw new InvalidAutonomyThresholdException("The validity date is required");
        }
        BigDecimal warning = threshold.getWarningDays();
        BigDecimal critical = threshold.getCriticalDays();
        if (warning == null || critical == null) {
            throw new InvalidAutonomyThresholdException("The warning and critical thresholds are required");
        }
        if (critical.signum() <= 0) {
            throw new InvalidAutonomyThresholdException("The critical threshold must be greater than zero");
        }
        if (critical.compareTo(warning) >= 0) {
            throw new InvalidAutonomyThresholdException("The critical threshold must be lower than the warning threshold");
        }
    }
}