package com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.adapters;

import com.edu.unicauca.gasstation.backend.inventory.domain.models.AutonomyThreshold;
import com.edu.unicauca.gasstation.backend.inventory.domain.repositories.AutonomyThresholdRepository;
import com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.mappers.AutonomyThresholdPersistenceMapper;
import com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.repositories.JpaAutonomyThresholdRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AutonomyThresholdRepositoryAdapter implements AutonomyThresholdRepository {

    private final JpaAutonomyThresholdRepository repository;
    private final AutonomyThresholdPersistenceMapper mapper;

    @Override
    public AutonomyThreshold save(AutonomyThreshold threshold) {
        return mapper.toDomain(repository.save(mapper.toEntity(threshold)));
    }

    @Override
    public Optional<AutonomyThreshold> findEffectiveOn(LocalDate date) {
        return repository.findFirstByValidFromLessThanEqualOrderByValidFromDesc(date).map(mapper::toDomain);
    }

    @Override
    public boolean existsByValidFrom(LocalDate validFrom) {
        return repository.existsByValidFrom(validFrom);
    }

    @Override
    public List<AutonomyThreshold> findAll() {
        return repository.findAllByOrderByValidFromDesc().stream().map(mapper::toDomain).toList();
    }
}