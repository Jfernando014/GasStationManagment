package com.edu.unicauca.gasstation.backend.appearance.infrastructure.persistence.repositories;

import com.edu.unicauca.gasstation.backend.appearance.domain.models.ColorCategory;
import com.edu.unicauca.gasstation.backend.appearance.infrastructure.persistence.entities.UiColorSettingEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data JPA repository for {@link UiColorSettingEntity}. */
public interface JpaUiColorSettingRepository extends JpaRepository<UiColorSettingEntity, UUID> {

    Optional<UiColorSettingEntity> findByCategoryAndEntityKey(ColorCategory category, String entityKey);

    void deleteByCategoryAndEntityKey(ColorCategory category, String entityKey);

    void deleteByCategory(ColorCategory category);
}
