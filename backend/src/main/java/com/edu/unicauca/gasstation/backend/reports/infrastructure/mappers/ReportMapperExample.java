package com.edu.unicauca.gasstation.backend.reports.infrastructure.mappers;

import com.edu.unicauca.gasstation.backend.reports.domain.models.ReportModelExample;
import org.mapstruct.Mapper;

/**
 * Example MapStruct mapper for reports module.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = org.mapstruct.ReportingPolicy.IGNORE)
public interface ReportMapperExample {

    ReportModelExample toModel(Object entity);
}
