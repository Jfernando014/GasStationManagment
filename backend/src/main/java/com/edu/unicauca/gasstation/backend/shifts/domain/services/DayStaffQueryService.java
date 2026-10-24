package com.edu.unicauca.gasstation.backend.shifts.domain.services;

import static com.edu.unicauca.gasstation.backend.shifts.domain.services.ScheduleCoverageCalculator.TITULAR;

import com.edu.unicauca.gasstation.backend.shifts.DayStaff;
import com.edu.unicauca.gasstation.backend.shifts.DayStaffMember;
import com.edu.unicauca.gasstation.backend.shifts.DayStaffNovelty;
import com.edu.unicauca.gasstation.backend.shifts.DayStaffQuery;
import com.edu.unicauca.gasstation.backend.shifts.DayStaffWarning;
import com.edu.unicauca.gasstation.backend.shifts.ScheduleWorkerDirectory;
import com.edu.unicauca.gasstation.backend.shifts.ScheduleWorkerInfo;
import com.edu.unicauca.gasstation.backend.shifts.Step1Status;
import com.edu.unicauca.gasstation.backend.shifts.domain.models.DailyCoverage;
import com.edu.unicauca.gasstation.backend.shifts.domain.models.ShiftAssignment;
import com.edu.unicauca.gasstation.backend.shifts.domain.models.ShiftCode;
import com.edu.unicauca.gasstation.backend.shifts.domain.repositories.ShiftAssignmentRepository;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of {@link DayStaffQuery} with the day schedule (S2-20, phase 1): who works each day according to
 * {@code shift_assignment}. Novelties are not applied yet, so every member comes with novelty NONE, there are no
 * absentees and step 1 is UNKNOWN.
 *
 * <p>At most two SQL queries per call, whatever the size of the range: the assignments of the range (with their
 * shift code and role loaded) and the workers of those assignments. Both ports are called exactly once, but when the
 * range has no assignments the worker directory receives no ids and does not touch the database, so a test that
 * counts SQL statements sees one query in that case. Workers that are inactive or missing from the directory are
 * left out, as in the shift schedule.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DayStaffQueryService implements DayStaffQuery {

    /** RN-Q2: titulares before apoyos, then start hour of the first segment, then full name, then worker id. */
    private static final Comparator<DayStaffMember> MEMBER_ORDER = Comparator
            .comparing((DayStaffMember member) -> !TITULAR.equals(member.dayRole().name()))
            .thenComparingInt(DayStaffQueryService::firstStartHour)
            .thenComparing(DayStaffMember::fullName)
            .thenComparing(DayStaffMember::workerId);

    private final ShiftAssignmentRepository shiftAssignmentRepository;
    private final ScheduleWorkerDirectory scheduleWorkerDirectory;

    @Override
    public DayStaff getDayStaff(LocalDate date) {
        Objects.requireNonNull(date, "date");
        return getDayStaff(date, date).get(0);
    }

    @Override
    public List<DayStaff> getDayStaff(LocalDate from, LocalDate to) {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        if (to.isBefore(from)) {
            throw new IllegalArgumentException("La fecha final no puede ser anterior a la fecha inicial");
        }
        if (ChronoUnit.DAYS.between(from, to) + 1 > MAX_RANGE_DAYS) {
            throw new IllegalArgumentException("El rango no puede superar " + MAX_RANGE_DAYS + " días");
        }

        List<ShiftAssignment> assignments = shiftAssignmentRepository.findByWorkDateBetween(from, to);
        Set<UUID> workerIds = assignments.stream().map(ShiftAssignment::getWorkerId).collect(Collectors.toSet());
        Map<UUID, ScheduleWorkerInfo> activeWorkers = scheduleWorkerDirectory.getWorkersByIds(workerIds).values()
                .stream()
                .filter(ScheduleWorkerInfo::active)
                .collect(Collectors.toMap(ScheduleWorkerInfo::id, Function.identity()));
        List<ShiftAssignment> active = assignments.stream()
                .filter(assignment -> activeWorkers.containsKey(assignment.getWorkerId()))
                .toList();

        Map<LocalDate, List<ShiftAssignment>> byDate = active.stream()
                .collect(Collectors.groupingBy(ShiftAssignment::getWorkDate));
        Map<LocalDate, DailyCoverage> coverageByDate = ScheduleCoverageCalculator.coverage(from, to, active).stream()
                .collect(Collectors.toMap(DailyCoverage::date, Function.identity()));

        List<DayStaff> days = new ArrayList<>();
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            List<ShiftAssignment> dayAssignments = byDate.getOrDefault(date, List.of());
            days.add(dayStaff(date, dayAssignments, activeWorkers, coverageByDate.get(date)));
        }
        return List.copyOf(days);
    }

    private static DayStaff dayStaff(LocalDate date, List<ShiftAssignment> assignments,
                                     Map<UUID, ScheduleWorkerInfo> workers, DailyCoverage coverage) {
        // DESCANSO has no role: it counts as "there is a schedule" but never as a member (RN-Q1)
        List<DayStaffMember> members = assignments.stream()
                .filter(assignment -> assignment.getShiftCode().getRole() != null)
                .map(assignment -> member(assignment, workers.get(assignment.getWorkerId())))
                .sorted(MEMBER_ORDER)
                .toList();
        return new DayStaff(date, members, List.of(), Step1Status.UNKNOWN, false,
                warnings(assignments.isEmpty(), coverage));
    }

    private static DayStaffMember member(ShiftAssignment assignment, ScheduleWorkerInfo worker) {
        ShiftCode code = assignment.getShiftCode();
        // The role of the day comes from the worked code, not from the worker; dispenser and support derive from it
        return DayStaffMember.of(
                worker.id(),
                worker.fullName(),
                code.getCode(),
                code.getCode(),
                ShiftInfoMapper.toSegments(code),
                code.totalHours(),
                ShiftInfoMapper.toRoleInfo(code.getRole()),
                DayStaffNovelty.none());
    }

    /** NO_SCHEDULE alone when the day has no active row; otherwise the coverage warnings of the day. */
    private static List<DayStaffWarning> warnings(boolean noSchedule, DailyCoverage coverage) {
        if (noSchedule) {
            return List.of(DayStaffWarning.NO_SCHEDULE);
        }
        List<DayStaffWarning> warnings = new ArrayList<>();
        if (!coverage.dayCovered()) {
            warnings.add(DayStaffWarning.DAY_TITULAR_MISSING);
        }
        if (!coverage.nightCovered()) {
            warnings.add(DayStaffWarning.NIGHT_TITULAR_MISSING);
        }
        if (!coverage.supportCovered()) {
            warnings.add(DayStaffWarning.SUPPORT_MISSING);
        }
        return warnings;
    }

    private static int firstStartHour(DayStaffMember member) {
        return member.segments().isEmpty() ? Integer.MAX_VALUE : member.segments().get(0).startHour();
    }
}
