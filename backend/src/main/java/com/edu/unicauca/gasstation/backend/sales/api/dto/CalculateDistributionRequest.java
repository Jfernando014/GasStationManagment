package com.edu.unicauca.gasstation.backend.sales.api.dto;

import com.edu.unicauca.gasstation.backend.sales.domain.calculation.DistributionCalculation.Assignment;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

/** Explicit schedule input; never interpreted as assignments fetched from shifts. */
public record CalculateDistributionRequest(@NotNull Long profileId, @NotNull @Min(0) @Max(12) Integer outputScale,
        @NotNull @Size(min=1) List<@NotNull @Valid AssignmentRequest> assignments) {
    /** Expands only caller-declared ranges; never infers a rotation or fetches a schedule. */
    public List<Assignment> toDomainAssignments() {
        var expanded = new ArrayList<Assignment>();
        var datesByWorker = new HashMap<UUID, Set<LocalDate>>();
        for (var row : assignments) {
            for (var assignment : row.expand()) {
                if (!datesByWorker.computeIfAbsent(assignment.workerId(), ignored -> new HashSet<>()).add(assignment.date()))
                    throw new IllegalArgumentException("Duplicate worker/date assignment");
                expanded.add(assignment);
            }
        }
        return List.copyOf(expanded);
    }

    /** Optional endDate is inclusive and repeats this exact shift on every calendar date. */
    public record AssignmentRequest(@NotNull LocalDate date, @NotNull UUID workerId, @NotBlank String shiftCode,
                                    LocalDate endDate) {
        public AssignmentRequest(LocalDate date, UUID workerId, String shiftCode) {
            this(date, workerId, shiftCode, null);
        }
        public List<Assignment> expand() {
            var end = endDate == null ? date : endDate;
            if (end.isBefore(date)) throw new IllegalArgumentException("Assignment endDate precedes date");
            // The calculation accepts one monthly target. Bound expansion before allocating any dates.
            if (!YearMonth.from(date).equals(YearMonth.from(end)))
                throw new IllegalArgumentException("Assignment range must stay within one month");
            var result = new ArrayList<Assignment>();
            for (int day = date.getDayOfMonth(); day <= end.getDayOfMonth(); day++)
                result.add(new Assignment(date.withDayOfMonth(day), workerId, shiftCode));
            return List.copyOf(result);
        }
    }
}
