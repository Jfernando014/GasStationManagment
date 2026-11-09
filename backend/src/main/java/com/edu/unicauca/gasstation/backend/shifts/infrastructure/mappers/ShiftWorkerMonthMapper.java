package com.edu.unicauca.gasstation.backend.shifts.infrastructure.mappers;

import com.edu.unicauca.gasstation.backend.shifts.api.dtos.ScheduleDtos.WorkerMonthResponse;
import com.edu.unicauca.gasstation.backend.shifts.domain.models.WorkerMonth;
import org.mapstruct.Mapper;

/**
 * Converts the month of a worker into the DTO of the REST API. The records have the same shape; the kind of each
 * novelty travels as text.
 */
@Mapper(componentModel = "spring")
public interface ShiftWorkerMonthMapper {

    WorkerMonthResponse toResponse(WorkerMonth month);
}
