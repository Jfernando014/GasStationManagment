package com.edu.unicauca.gasstation.backend.security.infrastructure.mappers;

import com.edu.unicauca.gasstation.backend.security.domain.models.SecurityModelExample;
import org.mapstruct.Mapper;

/**
 * Example MapStruct mapper for security module.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = org.mapstruct.ReportingPolicy.IGNORE)
public interface SecurityMapperExample {

    SecurityModelExample toModel(Object entity);
}
