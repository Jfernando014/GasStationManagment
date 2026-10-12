package com.edu.unicauca.gasstation.backend.sales.domain.calculation;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.*;
import static com.edu.unicauca.gasstation.backend.sales.domain.calculation.DistributionCalculation.*;

/** CALC METAS F11, K11, AB11 and B4:C7: expected hourly mass -> normalized individual share -> target. */
public final class SalesTargetDistributionEngine {
    public Result calculate(Input input) {
        var period = input.target().identity().period();
        if (period.isAnnual()) throw new IllegalArgumentException("A configured monthly target is required; annual targets are not divided");
        var month = YearMonth.of(period.year(), period.month());
        var profile = input.profile();
        var coverage = new ArrayList<>(input.coverage());
        coverage.sort(Comparator.comparing((Coverage c) -> c.assignment().date()).thenComparing(c -> c.assignment().workerId()));
        var seen = new HashSet<String>();
        for (var c : coverage) {
            if (!YearMonth.from(c.assignment().date()).equals(month)) throw new IllegalArgumentException("Assignment outside target month");
            if (!seen.add(c.assignment().date() + "/" + c.assignment().workerId())) throw new IllegalArgumentException("Duplicate worker/date assignment");
        }
        var keys = new ArrayList<Allocation>();
        var officialMass = new ArrayList<ExactFraction>();
        var referenceMass = new ArrayList<ExactFraction>();
        var uncovered = new ArrayList<HourKey>();
        for (int day = 1; day <= month.lengthOfMonth(); day++) {
            var date = month.atDay(day);
            var history = ExactFraction.of(profile.historicalDailyGallons().get(date.getDayOfWeek()));
            for (int hour = 0; hour < 24; hour++) {
                var mass = history.multiply(ExactFraction.of(profile.hourlyWeights().get(hour)));
                if (mass.compareTo(ExactFraction.ZERO) == 0) continue;
                final int h = hour;
                var active = coverage.stream().filter(c -> c.assignment().date().equals(date) && c.hours().contains(h)).toList();
                if (active.isEmpty()) { uncovered.add(new HourKey(date, hour)); continue; }
                var official = active.stream().map(c -> ExactFraction.of(profile.supportShiftCodes().contains(c.assignment().shiftCode())
                        ? profile.supportShare() : profile.titularShare())).toList();
                var reference = active.stream().map(c -> ExactFraction.of(profile.supportShiftCodes().contains(c.assignment().shiftCode())
                        ? profile.referenceSupportShare() : profile.referenceTitularShare())).toList();
                var sumOfficial = official.stream().reduce(ExactFraction.ZERO, ExactFraction::add);
                var sumReference = reference.stream().reduce(ExactFraction.ZERO, ExactFraction::add);
                if (sumOfficial.compareTo(ExactFraction.ZERO) == 0 || sumReference.compareTo(ExactFraction.ZERO) == 0)
                    throw new IllegalArgumentException("Active workers have zero total share at " + date + "/" + hour);
                for (int i = 0; i < active.size(); i++) {
                    keys.add(new Allocation(date, hour, active.get(i).assignment().workerId(), null, null));
                    officialMass.add(mass.multiply(official.get(i)).divide(sumOfficial));
                    referenceMass.add(mass.multiply(reference.get(i)).divide(sumReference));
                }
            }
        }
        if (keys.isEmpty()) throw new IllegalArgumentException("No covered hours with positive historical weight; calculation unavailable");
        var official = ConservingAllocation.allocate(input.target().value(), officialMass, input.outputScale());
        var reference = ConservingAllocation.allocate(input.target().value(), referenceMass, input.outputScale());
        var allocations = new ArrayList<Allocation>();
        var byDate = new TreeMap<java.time.LocalDate, Totals>(); var byHour = new TreeMap<HourKey, Totals>(); var byWorker = new TreeMap<UUID, Totals>();
        for (int i = 0; i < keys.size(); i++) {
            var key = keys.get(i); var totals = new Totals(official.get(i), reference.get(i));
            allocations.add(new Allocation(key.date(), key.hour(), key.workerId(), totals.official(), totals.reference()));
            byDate.merge(key.date(), totals, Totals::add); byHour.merge(new HourKey(key.date(), key.hour()), totals, Totals::add);
            byWorker.merge(key.workerId(), totals, Totals::add);
        }
        return new Result(input.target(), input.outputScale(), "EXACT_RATIONAL_LARGEST_REMAINDER_DATE_HOUR_UUID", profile.source(),
                new Totals(input.target().value(), input.target().value()), allocations, byDate, byHour, byWorker, uncovered,
                "EXPLICIT_CALLER_SUPPLIED_WORKSHEET_DATE_ASSIGNMENTS");
    }
}
