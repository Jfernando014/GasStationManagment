package com.edu.unicauca.gasstation.backend.sales.application;

import com.edu.unicauca.gasstation.backend.sales.IndividualTargetQuery;
import com.edu.unicauca.gasstation.backend.sales.domain.calculation.DistributionCalculation.Assignment;
import com.edu.unicauca.gasstation.backend.sales.domain.calculation.DistributionCalculation.Coverage;
import com.edu.unicauca.gasstation.backend.sales.domain.calculation.DistributionCalculation.Input;
import com.edu.unicauca.gasstation.backend.sales.domain.calculation.SalesTargetDistributionEngine;
import com.edu.unicauca.gasstation.backend.sales.domain.models.SalesTargetCategory;
import com.edu.unicauca.gasstation.backend.sales.domain.models.SalesTargetIdentity;
import com.edu.unicauca.gasstation.backend.sales.domain.models.SalesTargetPeriod;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.DistributionProfileRepository;
import com.edu.unicauca.gasstation.backend.sales.domain.services.SalesTargetService;
import com.edu.unicauca.gasstation.backend.shifts.catalog.ShiftCodeInfo;
import com.edu.unicauca.gasstation.backend.shifts.daystaff.DayStaffQuery;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Divides the monthly station target among the sellers with the official share of the latest registered profile.
 * It reuses the distribution engine as it is and only supplies its inputs: the stored monthly target, the profile and
 * the effective day staff of every day of the month. Anything missing gives an empty list, never a zero target.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IndividualTargetQueryService implements IndividualTargetQuery {

    private static final int OUTPUT_SCALE = 3;

    private final SalesTargetService targets;
    private final DistributionProfileRepository profiles;
    private final DayStaffQuery staff;

    @Override
    public List<IndividualTargetInfo> findIndividualTargets(LocalDate from, LocalDate to, Measure measure) {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        Objects.requireNonNull(measure, "measure");
        YearMonth month = YearMonth.from(from);
        if (!from.equals(month.atDay(1)) || !to.equals(month.atEndOfMonth())) {
            return List.of();
        }
        SalesTargetCategory category = switch (measure) {
            case FUEL_GALLONS -> SalesTargetCategory.FUEL_AGGREGATE;
            case BASKET_COP -> SalesTargetCategory.BASKET;
        };
        var target = targets.findByIdentity(new SalesTargetIdentity(
                SalesTargetPeriod.monthly(month.getYear(), month.getMonthValue()), category));
        var profile = profiles.findLatest();
        if (target.isEmpty() || profile.isEmpty()) {
            return List.of();
        }
        // The day staff already carries the segments of the code actually worked, so the hours come from it directly
        List<Coverage> covered = staff.getDayStaff(from, to).stream()
                .flatMap(day -> day.members().stream().map(member -> new Coverage(
                        new Assignment(day.date(), member.workerId(), member.workedCode(), member.support()),
                        hoursOf(member.segments()))))
                .toList();
        if (covered.isEmpty()) {
            return List.of();
        }
        String source = "SALES_TARGET:" + target.get().id() + " " + category
                + "; DISTRIBUTION_PROFILE:" + profile.get().id() + " " + profile.get().profile().name()
                + "; OFFICIAL_SHARE; ASSIGNMENTS:EFFECTIVE_DAY_STAFF";
        try {
            var result = new SalesTargetDistributionEngine().calculate(new Input(
                    target.get().target(), profile.get().profile(), covered, OUTPUT_SCALE));
            return result.byWorker().entrySet().stream()
                    .map(entry -> new IndividualTargetInfo(entry.getKey(), entry.getValue().official(), source))
                    .toList();
        } catch (IllegalArgumentException cannotBeResolved) {
            // Invalid segments, a repeated worker on a date or a month without covered hours: unavailable
            return List.of();
        }
    }

    private static Set<Integer> hoursOf(List<ShiftCodeInfo.Segment> segments) {
        Set<Integer> hours = new TreeSet<>();
        for (var segment : segments) {
            int start = segment.startHour();
            int end = segment.endHour();
            if (start < 0 || start > 23 || end < 0 || end > 24 || start == end) {
                throw new IllegalArgumentException("Invalid shift segment");
            }
            // An end of 24 is midnight; a lower end than the start crosses it
            for (int hour = start; hour != end % 24; hour = (hour + 1) % 24) {
                hours.add(hour);
            }
        }
        return hours;
    }
}
