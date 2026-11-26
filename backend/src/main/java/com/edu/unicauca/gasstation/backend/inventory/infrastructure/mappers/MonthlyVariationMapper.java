package com.edu.unicauca.gasstation.backend.inventory.infrastructure.mappers;

import com.edu.unicauca.gasstation.backend.inventory.api.dto.MonthlyVariationResponse;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.MonthlyVariation;
import org.mapstruct.Mapper;

import java.time.YearMonth;

@Mapper(componentModel = "spring", uses = VariationAlertSettingMapper.class)
public interface MonthlyVariationMapper {

    MonthlyVariationResponse toResponse(MonthlyVariation variation);

    default String toText(YearMonth month) {
        return month == null ? null : month.toString();
    }
}
