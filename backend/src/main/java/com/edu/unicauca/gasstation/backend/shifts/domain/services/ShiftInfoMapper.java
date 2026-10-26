package com.edu.unicauca.gasstation.backend.shifts.domain.services;

import com.edu.unicauca.gasstation.backend.shifts.catalog.RoleInfo;
import com.edu.unicauca.gasstation.backend.shifts.catalog.ShiftCodeInfo;
import com.edu.unicauca.gasstation.backend.shifts.domain.models.Role;
import com.edu.unicauca.gasstation.backend.shifts.domain.models.ShiftCode;
import java.util.List;

/**
 * Converts domain models into the public records of this module. Shared by {@link ShiftCatalogService} and
 * {@link DayStaffQueryService}, so both expose roles and segments the same way.
 */
final class ShiftInfoMapper {

    private ShiftInfoMapper() {
    }

    static RoleInfo toRoleInfo(Role role) {
        return new RoleInfo(role.getId(), role.getName(), role.getDispenser());
    }

    /** Segments of the code in order; a segment whose end is lower than its start ends the next day. */
    static List<ShiftCodeInfo.Segment> toSegments(ShiftCode shiftCode) {
        return shiftCode.segments().stream()
                .map(segment -> new ShiftCodeInfo.Segment(segment.startHour(), segment.endHour()))
                .toList();
    }
}
