package com.edu.unicauca.gasstation.backend.sales.api.dto;

import com.edu.unicauca.gasstation.backend.sales.domain.calculation.DistributionCalculation.*;
import com.edu.unicauca.gasstation.backend.sales.domain.models.SalesTarget;
import java.time.LocalDate;
import java.util.*;

/** HTTP projection keeps profile/target IDs and exposes hour buckets as structured JSON entries. */
public record SalesDistributionResponse(long targetId, long profileId, SalesTarget target, int outputScale,
        String decimalPolicy, String parameterSource, String assignmentSource, Totals monthlyTotal,
        List<Allocation> allocations, Map<LocalDate, Totals> byDate, List<HourSummary> byHour,
        Map<UUID, Totals> byWorker, List<HourKey> uncoveredHours) {
    public record HourSummary(LocalDate date, int hour, Totals totals) { }
    public static SalesDistributionResponse from(long targetId, long profileId, Result result) {
        return new SalesDistributionResponse(targetId,profileId,result.target(),result.outputScale(),result.decimalPolicy(),
                result.parameterSource(),result.assignmentSource(),result.monthlyTotal(),result.allocations(),result.byDate(),
                result.byHour().entrySet().stream().map(e->new HourSummary(e.getKey().date(),e.getKey().hour(),e.getValue())).toList(),
                result.byWorker(),result.uncoveredHours());
    }
}
