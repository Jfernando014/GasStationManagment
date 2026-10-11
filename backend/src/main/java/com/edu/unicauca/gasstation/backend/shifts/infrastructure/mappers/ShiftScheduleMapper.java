package com.edu.unicauca.gasstation.backend.shifts.infrastructure.mappers;

import com.edu.unicauca.gasstation.backend.shifts.api.dtos.DayCoverage;
import com.edu.unicauca.gasstation.backend.shifts.api.dtos.ScheduleDay;
import com.edu.unicauca.gasstation.backend.shifts.api.dtos.ScheduleResponse;
import com.edu.unicauca.gasstation.backend.shifts.api.dtos.ScheduleUpdateResponse;
import com.edu.unicauca.gasstation.backend.shifts.api.dtos.WorkerSchedule;
import com.edu.unicauca.gasstation.backend.shifts.domain.models.DailyCoverage;
import com.edu.unicauca.gasstation.backend.shifts.domain.models.ScheduleChange;
import com.edu.unicauca.gasstation.backend.shifts.domain.models.ScheduleView;
import com.edu.unicauca.gasstation.backend.shifts.domain.models.ShiftAssignment;
import com.edu.unicauca.gasstation.backend.shifts.domain.models.WorkerShifts;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Converts the results of the shift schedule use cases into the DTOs returned by the REST API.
 * Each assignment becomes a day with its shift code as text.
 */
@Mapper(componentModel = "spring")
public interface ShiftScheduleMapper {

    ScheduleUpdateResponse toResponse(ScheduleChange change);

    ScheduleResponse toResponse(ScheduleView schedule);

    @Mapping(target = "days", source = "assignments")
    WorkerSchedule toWorkerSchedule(WorkerShifts workerShifts);

    @Mapping(target = "date", source = "workDate")
    @Mapping(target = "shiftCode", source = "shiftCode.code")
    ScheduleDay toScheduleDay(ShiftAssignment assignment);

    @Mapping(target = "covered", expression = "java(coverage.covered())")
    DayCoverage toDayCoverage(DailyCoverage coverage);
}
