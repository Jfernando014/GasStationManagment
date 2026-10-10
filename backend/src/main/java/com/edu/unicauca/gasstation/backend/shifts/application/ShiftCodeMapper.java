package com.edu.unicauca.gasstation.backend.shifts.application;

import com.edu.unicauca.gasstation.backend.shifts.api.dtos.RoleResponse;
import com.edu.unicauca.gasstation.backend.shifts.api.dtos.ShiftCodeResponse;
import com.edu.unicauca.gasstation.backend.shifts.api.dtos.ShiftSchedule;
import com.edu.unicauca.gasstation.backend.shifts.domain.Role;
import com.edu.unicauca.gasstation.backend.shifts.domain.ShiftCode;
import java.util.ArrayList;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
interface ShiftCodeMapper {

    @Mapping(target = "schedule", expression = "java(toSchedule(shiftCode))")
    ShiftCodeResponse toResponse(ShiftCode shiftCode);

    RoleResponse toRoleResponse(Role role);

    default ShiftSchedule toSchedule(ShiftCode shiftCode) {
        List<ShiftSchedule.Segment> segments = new ArrayList<>();
        if (shiftCode.getStartHour1() != null) {
            segments.add(new ShiftSchedule.Segment(shiftCode.getStartHour1(), shiftCode.getEndHour1()));
        }
        if (shiftCode.getStartHour2() != null) {
            segments.add(new ShiftSchedule.Segment(shiftCode.getStartHour2(), shiftCode.getEndHour2()));
        }
        return new ShiftSchedule(segments, shiftCode.totalHours());
    }
}
