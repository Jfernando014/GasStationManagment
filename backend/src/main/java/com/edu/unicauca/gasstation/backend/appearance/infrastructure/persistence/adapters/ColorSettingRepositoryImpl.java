package com.edu.unicauca.gasstation.backend.appearance.infrastructure.persistence.adapters;

import com.edu.unicauca.gasstation.backend.appearance.domain.models.ColorCategory;
import com.edu.unicauca.gasstation.backend.appearance.domain.models.ColorHex;
import com.edu.unicauca.gasstation.backend.appearance.domain.models.ColorSetting;
import com.edu.unicauca.gasstation.backend.appearance.domain.repositories.ColorSettingRepository;
import com.edu.unicauca.gasstation.backend.appearance.infrastructure.persistence.entities.UiColorSettingEntity;
import com.edu.unicauca.gasstation.backend.appearance.infrastructure.persistence.repositories.JpaUiColorSettingRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/** JPA adapter implementing {@link ColorSettingRepository}. */
@Repository
@RequiredArgsConstructor
public class ColorSettingRepositoryImpl implements ColorSettingRepository {

    private final JpaUiColorSettingRepository jpaRepository;

    @Override
    public List<ColorSetting> findAll() {
        return jpaRepository.findAll().stream()
                .map(ColorSettingRepositoryImpl::toDomain)
                .toList();
    }

    @Override
    public Optional<ColorSetting> findByCategoryAndEntityKey(ColorCategory category, String entityKey) {
        return jpaRepository.findByCategoryAndEntityKey(category, entityKey)
                .map(ColorSettingRepositoryImpl::toDomain);
    }

    @Override
    public ColorSetting save(ColorSetting setting) {
        UiColorSettingEntity entity = toEntity(setting);
        UiColorSettingEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public void deleteByCategoryAndEntityKey(ColorCategory category, String entityKey) {
        jpaRepository.deleteByCategoryAndEntityKey(category, entityKey);
        jpaRepository.flush();
    }

    @Override
    public void deleteByCategory(ColorCategory category) {
        jpaRepository.deleteByCategory(category);
        jpaRepository.flush();
    }

    @Override
    public void deleteAll() {
        jpaRepository.deleteAll();
        jpaRepository.flush();
    }

    private static ColorSetting toDomain(UiColorSettingEntity entity) {
        return new ColorSetting(
                entity.getId(),
                entity.getCategory(),
                entity.getEntityKey(),
                ColorHex.restore(entity.getColorHex()),
                entity.getUpdatedAt());
    }

    private static UiColorSettingEntity toEntity(ColorSetting setting) {
        UiColorSettingEntity entity = new UiColorSettingEntity();
        entity.setId(setting.id());
        entity.setCategory(setting.category());
        entity.setEntityKey(setting.entityKey());
        entity.setColorHex(setting.color().value());
        entity.setUpdatedAt(setting.updatedAt());
        return entity;
    }
}
