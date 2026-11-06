package com.edu.unicauca.gasstation.backend.inventory.infrastructure.mappers;

import com.edu.unicauca.gasstation.backend.inventory.api.dto.DailyControlResponse;
import com.edu.unicauca.gasstation.backend.inventory.api.dto.FuelControlResponse;
import com.edu.unicauca.gasstation.backend.inventory.api.dto.ProductControlResponse;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.DailyControl;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.FuelControl;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.ProductControl;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {FuelVariationMapper.class, FuelAutonomyMapper.class})
public interface DailyControlMapper {

    DailyControlResponse toResponse(DailyControl control);

    FuelControlResponse toFuelResponse(FuelControl control);

    ProductControlResponse toProductResponse(ProductControl control);
}