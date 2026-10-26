package com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence.adapters;

import com.edu.unicauca.gasstation.backend.sales.domain.repositories.DayStaffReadPort;
import com.edu.unicauca.gasstation.backend.shifts.daystaff.DayStaff;
import com.edu.unicauca.gasstation.backend.shifts.daystaff.DayStaffQuery;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DayStaffReadPortAdapter implements DayStaffReadPort {

    private final Optional<DayStaffQuery> dayStaffQuery;

    @Override
    public List<AssignedVendor> findStaffForDate(LocalDate date) {
        if (dayStaffQuery.isEmpty() || date == null) {
            return Collections.emptyList();
        }
        try {
            DayStaff dayStaff = dayStaffQuery.get().getDayStaff(date);
            if (dayStaff == null || dayStaff.members() == null) {
                return Collections.emptyList();
            }
            return dayStaff.members().stream()
                    .map(this::mapMember)
                    .toList();
        } catch (Exception e) {
            log.warn("Unable to fetch day staff for date {}: {}", date, e.getMessage());
            return Collections.emptyList();
        }
    }

    private AssignedVendor mapMember(DayStaff.Member member) {
        String roleName = member.dayRole() != null ? member.dayRole().name() : "ISLERO";
        String doc = member.workerId() != null ? member.workerId().toString() : member.fullName();
        return new AssignedVendor(
                doc,
                member.fullName(),
                roleName,
                member.workedCode(),
                member.dispenser(),
                member.workedCode()
        );
    }
}
