package com.edu.unicauca.gasstation.backend.appearance.domain.repositories;

import com.edu.unicauca.gasstation.backend.appearance.domain.models.ColorCategory;
import com.edu.unicauca.gasstation.backend.appearance.domain.models.ColorSetting;
import java.util.List;
import java.util.Optional;

/** Persistence port for editable UI color overrides. */
public interface ColorSettingRepository {

    List<ColorSetting> findAll();

    Optional<ColorSetting> findByCategoryAndEntityKey(ColorCategory category, String entityKey);

    ColorSetting save(ColorSetting setting);

    void deleteByCategoryAndEntityKey(ColorCategory category, String entityKey);

    void deleteByCategory(ColorCategory category);

    void deleteAll();
}
