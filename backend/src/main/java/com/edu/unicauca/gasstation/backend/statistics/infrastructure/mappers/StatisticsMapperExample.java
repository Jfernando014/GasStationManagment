package com.edu.unicauca.gasstation.backend.statistics.infrastructure.mappers;

import com.edu.unicauca.gasstation.backend.statistics.domain.models.StatisticsModelExample;
import org.mapstruct.Mapper;

/**
 * Example MapStruct mapper for statistics module.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = org.mapstruct.ReportingPolicy.IGNORE)
public interface StatisticsMapperExample {

    StatisticsModelExample toModel(Object entity);
}
