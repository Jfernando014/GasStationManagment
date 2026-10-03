package com.edu.unicauca.gasstation.backend.inventory.infrastructure.mappers;

import com.edu.unicauca.gasstation.backend.inventory.domain.models.InventoryModelExample;
import org.mapstruct.Mapper;

/**
 * Example MapStruct mapper for inventory module.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = org.mapstruct.ReportingPolicy.IGNORE)
public interface InventoryMapperExample {

    InventoryModelExample toModel(Object entity);
}
