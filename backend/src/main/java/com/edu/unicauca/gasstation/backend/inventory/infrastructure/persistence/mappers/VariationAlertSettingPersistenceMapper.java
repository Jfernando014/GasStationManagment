package com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.mappers;

import com.edu.unicauca.gasstation.backend.inventory.domain.models.VariationAlertSetting;
import com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.entities.VariationAlertSettingEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface VariationAlertSettingPersistenceMapper {

    VariationAlertSetting toDomain(VariationAlertSettingEntity entity);

    VariationAlertSettingEntity toEntity(VariationAlertSetting setting);
}
