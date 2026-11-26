package com.edu.unicauca.gasstation.backend.inventory.infrastructure.mappers;

import com.edu.unicauca.gasstation.backend.inventory.api.dto.VariationAlertSettingRequest;
import com.edu.unicauca.gasstation.backend.inventory.api.dto.VariationAlertSettingResponse;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.VariationAlertSetting;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface VariationAlertSettingMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    VariationAlertSetting toDomain(VariationAlertSettingRequest request);

    VariationAlertSettingResponse toResponse(VariationAlertSetting setting);

    List<VariationAlertSettingResponse> toResponses(List<VariationAlertSetting> settings);
}
