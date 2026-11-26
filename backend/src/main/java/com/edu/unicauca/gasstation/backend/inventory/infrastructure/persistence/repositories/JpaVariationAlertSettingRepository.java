package com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.repositories;

import com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.entities.VariationAlertSettingEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface JpaVariationAlertSettingRepository extends JpaRepository<VariationAlertSettingEntity, Long> {

    Optional<VariationAlertSettingEntity> findFirstByValidFromLessThanEqualOrderByValidFromDesc(LocalDate date);

    Optional<VariationAlertSettingEntity> findFirstByOrderByValidFromAsc();

    boolean existsByValidFrom(LocalDate validFrom);

    boolean existsByValidFromAndIdNot(LocalDate validFrom, Long id);

    List<VariationAlertSettingEntity> findAllByOrderByValidFromDesc();
}
