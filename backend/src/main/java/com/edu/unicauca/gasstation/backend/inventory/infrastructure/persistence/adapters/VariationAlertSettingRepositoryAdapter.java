package com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.adapters;

import com.edu.unicauca.gasstation.backend.inventory.domain.models.VariationAlertSetting;
import com.edu.unicauca.gasstation.backend.inventory.domain.repositories.VariationAlertSettingRepository;
import com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.mappers.VariationAlertSettingPersistenceMapper;
import com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.repositories.JpaVariationAlertSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class VariationAlertSettingRepositoryAdapter implements VariationAlertSettingRepository {

    private final JpaVariationAlertSettingRepository repository;
    private final VariationAlertSettingPersistenceMapper mapper;

    @Override
    public VariationAlertSetting save(VariationAlertSetting setting) {
        return mapper.toDomain(repository.save(mapper.toEntity(setting)));
    }

    @Override
    public Optional<VariationAlertSetting> findById(Long id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<VariationAlertSetting> findEffectiveOn(LocalDate date) {
        return repository.findFirstByValidFromLessThanEqualOrderByValidFromDesc(date).map(mapper::toDomain);
    }

    @Override
    public Optional<VariationAlertSetting> findOldest() {
        return repository.findFirstByOrderByValidFromAsc().map(mapper::toDomain);
    }

    @Override
    public boolean existsByValidFrom(LocalDate validFrom) {
        return repository.existsByValidFrom(validFrom);
    }

    @Override
    public boolean existsByValidFromAndIdNot(LocalDate validFrom, Long id) {
        return repository.existsByValidFromAndIdNot(validFrom, id);
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    @Override
    public List<VariationAlertSetting> findAll() {
        return repository.findAllByOrderByValidFromDesc().stream().map(mapper::toDomain).toList();
    }
}
