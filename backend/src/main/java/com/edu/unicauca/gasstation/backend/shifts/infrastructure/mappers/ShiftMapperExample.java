package com.edu.unicauca.gasstation.backend.shifts.infrastructure.mappers;

import com.edu.unicauca.gasstation.backend.shifts.domain.models.ShiftModelExample;
import org.mapstruct.Mapper;

/**
 * Example MapStruct mapper for shifts module.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = org.mapstruct.ReportingPolicy.IGNORE)
public interface ShiftMapperExample {

    ShiftModelExample toModel(Object entity);
}
