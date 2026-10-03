package com.edu.unicauca.gasstation.backend.sales.infrastructure.mappers;

import com.edu.unicauca.gasstation.backend.sales.domain.models.SaleModelExample;
import org.mapstruct.Mapper;

/**
 * Example MapStruct mapper for sales module.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = org.mapstruct.ReportingPolicy.IGNORE)
public interface SaleMapperExample {

    SaleModelExample toModel(Object entity);
}
