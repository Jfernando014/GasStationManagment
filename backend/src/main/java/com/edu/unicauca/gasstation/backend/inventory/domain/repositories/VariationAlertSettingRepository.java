package com.edu.unicauca.gasstation.backend.inventory.domain.repositories;

import com.edu.unicauca.gasstation.backend.inventory.domain.models.VariationAlertSetting;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface VariationAlertSettingRepository {

    VariationAlertSetting save(VariationAlertSetting setting);

    Optional<VariationAlertSetting> findById(Long id);

    /** The setting with the latest validity date that is not after the given date. */
    Optional<VariationAlertSetting> findEffectiveOn(LocalDate date);

    /** The setting with the earliest validity date. */
    Optional<VariationAlertSetting> findOldest();

    boolean existsByValidFrom(LocalDate validFrom);

    boolean existsByValidFromAndIdNot(LocalDate validFrom, Long id);

    void deleteById(Long id);

    /** All settings, most recent validity first. */
    List<VariationAlertSetting> findAll();
}
