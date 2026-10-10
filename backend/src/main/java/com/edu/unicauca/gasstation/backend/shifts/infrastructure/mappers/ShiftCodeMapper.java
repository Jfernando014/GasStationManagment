package com.edu.unicauca.gasstation.backend.shifts.infrastructure.mappers;

import com.edu.unicauca.gasstation.backend.shifts.api.dtos.RoleResponse;
import com.edu.unicauca.gasstation.backend.shifts.api.dtos.ShiftCodeResponse;
import com.edu.unicauca.gasstation.backend.shifts.api.dtos.ShiftSchedule;
import com.edu.unicauca.gasstation.backend.shifts.domain.models.Role;
import com.edu.unicauca.gasstation.backend.shifts.domain.models.ShiftCode;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Converts domain models of the shift catalog into the DTOs returned by the REST API.
 * The period enum is sent as text; the segments of the shift code are grouped into a {@link ShiftSchedule}.
 */
@Mapper(componentModel = "spring")
public interface ShiftCodeMapper {

    @Mapping(target = "schedule", expression = "java(toSchedule(shiftCode))")
    ShiftCodeResponse toResponse(ShiftCode shiftCode);

    RoleResponse toRoleResponse(Role role);

    default ShiftSchedule toSchedule(ShiftCode shiftCode) {
        List<ShiftSchedule.Segment> segments = shiftCode.segments().stream()
                .map(segment -> new ShiftSchedule.Segment(segment.startHour(), segment.endHour()))
                .toList();
        return new ShiftSchedule(segments, shiftCode.totalHours());
    }
}
