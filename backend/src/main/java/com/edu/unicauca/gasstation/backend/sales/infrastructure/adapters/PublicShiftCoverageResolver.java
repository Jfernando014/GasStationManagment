package com.edu.unicauca.gasstation.backend.sales.infrastructure.adapters;

import com.edu.unicauca.gasstation.backend.sales.domain.calculation.DistributionCalculation.*;
import com.edu.unicauca.gasstation.backend.shifts.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Resolves explicitly supplied schedule rows through public contracts only. Does not claim to query assignments. */
@Component
@RequiredArgsConstructor
public class PublicShiftCoverageResolver {
    private final ShiftExternalService shifts;
    private final ScheduleWorkerDirectory workers;

    public List<Coverage> resolve(List<Assignment> assignments) {
        var ids = assignments.stream().map(Assignment::workerId).distinct().toList();
        var directory = workers.getWorkersByIds(ids);
        var catalog = shifts.getActiveCatalog().stream().collect(Collectors.toMap(ShiftCodeInfo::code, Function.identity()));
        var result = new ArrayList<Coverage>();
        for (var assignment : assignments) {
            var worker = directory.get(assignment.workerId());
            if (worker == null || !worker.active()) throw new IllegalArgumentException("Worker absent or inactive: " + assignment.workerId());
            var code = catalog.get(assignment.shiftCode());
            if (code == null) throw new IllegalArgumentException("Unknown or inactive shift code: " + assignment.shiftCode());
            if (code.role() != null && !code.role().id().equals(worker.roleId()))
                throw new IllegalArgumentException("Shift role does not match worker role");
            var hours = new TreeSet<Integer>();
            for (var segment : code.segments()) {
                int start = segment.startHour(), end = segment.endHour();
                if (start < 0 || start > 23 || end < 0 || end > 23 || start == end)
                    throw new IllegalArgumentException("Invalid public shift segment");
                for (int hour = start; hour != end; hour = (hour + 1) % 24) hours.add(hour);
            }
            // Excel uses the same worksheet date for 00..05 and 18..23 of NOCHE.
            // This is explicitly a workbook-date calculation, not historical night-date attribution.
            result.add(new Coverage(assignment, hours));
        }
        return List.copyOf(result);
    }
}
