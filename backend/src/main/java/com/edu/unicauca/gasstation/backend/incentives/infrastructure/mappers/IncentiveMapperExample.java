package com.edu.unicauca.gasstation.backend.incentives.infrastructure.mappers;

import com.edu.unicauca.gasstation.backend.incentives.domain.models.IncentiveModelExample;
import org.mapstruct.Mapper;

/**
 * Example MapStruct mapper for incentives module.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = org.mapstruct.ReportingPolicy.IGNORE)
public interface IncentiveMapperExample {

    IncentiveModelExample toModel(Object entity);
}
