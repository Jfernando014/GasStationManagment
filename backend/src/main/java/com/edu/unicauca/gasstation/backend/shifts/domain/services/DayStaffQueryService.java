package com.edu.unicauca.gasstation.backend.shifts.domain.services;

import com.edu.unicauca.gasstation.backend.shifts.DayJournalPort;
import com.edu.unicauca.gasstation.backend.shifts.DayStaff;
import com.edu.unicauca.gasstation.backend.shifts.DayStaffAbsentee;
import com.edu.unicauca.gasstation.backend.shifts.DayStaffMember;
import com.edu.unicauca.gasstation.backend.shifts.DayStaffQuery;
import com.edu.unicauca.gasstation.backend.shifts.DayStaffWarning;
import com.edu.unicauca.gasstation.backend.shifts.ScheduleWorkerDirectory;
import com.edu.unicauca.gasstation.backend.shifts.ScheduleWorkerInfo;
import com.edu.unicauca.gasstation.backend.shifts.Step1Status;
import com.edu.unicauca.gasstation.backend.shifts.domain.models.DailyCoverage;
import com.edu.unicauca.gasstation.backend.shifts.domain.models.DayLineup;
import com.edu.unicauca.gasstation.backend.shifts.domain.models.ShiftAssignment;
import com.edu.unicauca.gasstation.backend.shifts.domain.models.ShiftNovelty;
import com.edu.unicauca.gasstation.backend.shifts.domain.repositories.ShiftAssignmentRepository;
import com.edu.unicauca.gasstation.backend.shifts.domain.repositories.ShiftNoveltyRepository;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
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
 * Implementation of {@link DayStaffQuery}: who works each day according to the grid and, once
 * step 1 of the journal is CONFIRMED, its active novelties. Before the confirmation it returns the grid as it is,
 * with the real state of step 1 and {@code noveltiesApplied = false}.
 *
 * <p>Reads, whatever the size of the range: the assignments and the active novelties of the range (one SQL query
 * each) and the workers involved (one batch read; no SQL when there are no ids). The state of step 1 is asked to
 * {@link DayJournalPort} once per day. Workers that are inactive or missing from the directory are left out, as in
 * the shift schedule.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DayStaffQueryService implements DayStaffQuery {

    private final ShiftAssignmentRepository shiftAssignmentRepository;
    private final ShiftNoveltyRepository shiftNoveltyRepository;
    private final ScheduleWorkerDirectory scheduleWorkerDirectory;
    private final DayJournalPort dayJournalPort;

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
        List<ShiftNovelty> novelties = shiftNoveltyRepository.findByWorkDateBetween(from, to);
        Map<UUID, ScheduleWorkerInfo> activeWorkers = activeWorkers(assignments, novelties);
        Map<LocalDate, List<ShiftAssignment>> gridByDate = assignments.stream()
                .collect(Collectors.groupingBy(ShiftAssignment::getWorkDate));
        Map<LocalDate, List<ShiftNovelty>> noveltiesByDate = novelties.stream()
                .collect(Collectors.groupingBy(ShiftNovelty::getWorkDate));

        List<DayStaff> days = new ArrayList<>();
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            days.add(dayStaff(date, gridByDate.getOrDefault(date, List.of()),
                    noveltiesByDate.getOrDefault(date, List.of()), activeWorkers));
        }
        return List.copyOf(days);
    }

    private DayStaff dayStaff(LocalDate date, List<ShiftAssignment> grid, List<ShiftNovelty> novelties,
                              Map<UUID, ScheduleWorkerInfo> workers) {
        Step1Status status = dayJournalPort.step1Status(date);
        boolean confirmed = status == Step1Status.CONFIRMED;
        // Novelties apply only once step 1 is confirmed; DESCANSO never makes a member
        DayLineup lineup = DayLineup.of(grid, confirmed ? novelties : List.of());
        List<DayStaffMember> members = DayStaffAssembler.members(lineup, workers);
        List<DayStaffAbsentee> absentees = confirmed ? DayStaffAssembler.absentees(lineup, workers) : List.of();
        boolean noSchedule = grid.stream().noneMatch(row -> workers.containsKey(row.getWorkerId()));
        return new DayStaff(date, members, absentees, status, confirmed,
                warnings(date, noSchedule && members.isEmpty(), lineup, workers));
    }

    /** NO_SCHEDULE alone when the day has nobody; otherwise the coverage warnings of the codes actually worked. */
    private static List<DayStaffWarning> warnings(LocalDate date, boolean noSchedule, DayLineup lineup,
                                                  Map<UUID, ScheduleWorkerInfo> workers) {
        if (noSchedule) {
            return List.of(DayStaffWarning.NO_SCHEDULE);
        }
        List<ShiftAssignment> worked = lineup.lines().stream()
                .filter(line -> workers.containsKey(line.workerId()))
                .map(line -> new ShiftAssignment(line.workerId(), date, line.workedCode(), null))
                .toList();
        DailyCoverage coverage = ScheduleCoverageCalculator.coverage(date, date, worked).get(0);
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

    /** One batch read of every worker of the grid and of the novelties; only the active ones are kept. */
    private Map<UUID, ScheduleWorkerInfo> activeWorkers(List<ShiftAssignment> assignments, List<ShiftNovelty> novelties) {
        Set<UUID> ids = new HashSet<>();
        assignments.forEach(assignment -> ids.add(assignment.getWorkerId()));
        novelties.forEach(novelty -> {
            ids.add(novelty.getWorkerId());
            if (novelty.getCounterpartWorkerId() != null) {
                ids.add(novelty.getCounterpartWorkerId());
            }
        });
        return scheduleWorkerDirectory.getWorkersByIds(ids).values().stream()
                .filter(ScheduleWorkerInfo::active)
                .collect(Collectors.toMap(ScheduleWorkerInfo::id, Function.identity()));
    }
}
