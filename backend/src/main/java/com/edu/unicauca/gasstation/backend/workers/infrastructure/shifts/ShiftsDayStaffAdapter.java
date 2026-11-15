package com.edu.unicauca.gasstation.backend.workers.infrastructure.shifts;

import com.edu.unicauca.gasstation.backend.shifts.catalog.ShiftCodeInfo;
import com.edu.unicauca.gasstation.backend.shifts.daystaff.DayStaff;
import com.edu.unicauca.gasstation.backend.shifts.daystaff.DayStaffQuery;
import com.edu.unicauca.gasstation.backend.workers.domain.models.DayHoursReading;
import com.edu.unicauca.gasstation.backend.workers.domain.models.DayHoursSegment;
import com.edu.unicauca.gasstation.backend.workers.domain.models.WorkerDayHours;
import com.edu.unicauca.gasstation.backend.workers.domain.ports.DayStaffPort;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Reads the staff of an operational day from the query that shifts publishes. Hours are kept as shifts gives them:
 * the sum of the segments of the code worked.
 */
@Component
@RequiredArgsConstructor
public class ShiftsDayStaffAdapter implements DayStaffPort {

    private final DayStaffQuery dayStaffQuery;

    @Override
    public DayHoursReading read(LocalDate workDate) {
        DayStaff day = dayStaffQuery.getDayStaff(workDate);
        List<WorkerDayHours> records = new ArrayList<>();
        day.members().forEach(member -> records.add(toRecord(day, member)));
        day.absentees().forEach(absentee -> records.add(toRecord(day, absentee)));
        return new DayHoursReading(day.noveltiesApplied(), records);
    }

    private static WorkerDayHours toRecord(DayStaff day, DayStaff.Member member) {
        List<DayHoursSegment> segments = member.segments().stream().map(ShiftsDayStaffAdapter::toSegment).toList();
        DayStaff.Novelty novelty = member.novelty();
        return new WorkerDayHours(day.date(), member.workerId(), member.fullName(), member.workedCode(),
                member.gridCode(), member.dayRole().name(), member.totalHours(), novelty.type().name(),
                novelty.replacedWorkerId(), novelty.replacedWorkerName(), novelty.reason(), null,
                day.noveltiesApplied(), segments);
    }

    // Someone who was in the grid but did not work: 0 hours and the novelty that took the worker out
    private static WorkerDayHours toRecord(DayStaff day, DayStaff.Absentee absentee) {
        return new WorkerDayHours(day.date(), absentee.workerId(), absentee.fullName(), null, absentee.gridCode(),
                null, 0, absentee.type().name(), null, null, absentee.reason(), null, day.noveltiesApplied(),
                List.of());
    }

    private static DayHoursSegment toSegment(ShiftCodeInfo.Segment segment) {
        return DayHoursSegment.of(segment.startHour(), segment.endHour());
    }
}
