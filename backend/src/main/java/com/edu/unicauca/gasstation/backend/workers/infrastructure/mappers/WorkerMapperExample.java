package com.edu.unicauca.gasstation.backend.workers.infrastructure.mappers;

import com.edu.unicauca.gasstation.backend.workers.domain.models.WorkerModelExample;
import org.mapstruct.Mapper;

/**
 * Example MapStruct mapper for workers module.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = org.mapstruct.ReportingPolicy.IGNORE)
public interface WorkerMapperExample {

    WorkerModelExample toModel(Object entity);
}
