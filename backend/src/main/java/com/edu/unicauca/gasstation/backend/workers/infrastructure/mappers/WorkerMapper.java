package com.edu.unicauca.gasstation.backend.workers.infrastructure.mappers;

import com.edu.unicauca.gasstation.backend.workers.api.dtos.CreateWorkerRequest;
import com.edu.unicauca.gasstation.backend.workers.api.dtos.UpdateWorkerRequest;
import com.edu.unicauca.gasstation.backend.workers.api.dtos.WorkerResponse;
import com.edu.unicauca.gasstation.backend.workers.domain.models.Worker;
import com.edu.unicauca.gasstation.backend.workers.domain.models.WorkerDetail;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Converts between the REST DTOs and the domain models of the workers module.
 *
 * <p>Requests become a new {@link Worker} through its public constructor, so the domain rules
 * (trimmed values, starts active) apply. Responses combine the worker and its role ({@link WorkerDetail}).
 */
@Mapper(componentModel = "spring")
public interface WorkerMapper {

    Worker toDomain(CreateWorkerRequest request);

    /** Carries the new values to the update use case; only name, document and role are read. */
    Worker toDomain(UpdateWorkerRequest request);

    @Mapping(target = ".", source = "worker")
    @Mapping(target = "roleName", source = "role.name")
    @Mapping(target = "dispenser", source = "role.dispenser")
    WorkerResponse toResponse(WorkerDetail detail);
}
