package com.edu.unicauca.gasstation.backend.shifts.domain.services;

import com.edu.unicauca.gasstation.backend.shifts.domain.models.DailyCoverage;
import com.edu.unicauca.gasstation.backend.shifts.domain.models.Role;
import com.edu.unicauca.gasstation.backend.shifts.domain.models.ScheduleChange;
import com.edu.unicauca.gasstation.backend.shifts.domain.models.ScheduleView;
import com.edu.unicauca.gasstation.backend.shifts.domain.models.ShiftAssignment;
import com.edu.unicauca.gasstation.backend.shifts.domain.models.ShiftCode;
import com.edu.unicauca.gasstation.backend.shifts.domain.models.ShiftPeriod;
import com.edu.unicauca.gasstation.backend.shifts.domain.models.WorkerShifts;
import com.edu.unicauca.gasstation.backend.shifts.domain.repositories.ShiftAssignmentRepository;
import com.edu.unicauca.gasstation.backend.shifts.domain.repositories.ShiftCodeRepository;
import com.edu.unicauca.gasstation.backend.shifts.exception.ShiftError;
import com.edu.unicauca.gasstation.backend.shifts.exception.ShiftException;
import com.edu.unicauca.gasstation.backend.shifts.spi.ScheduleWorkerDirectory;
import com.edu.unicauca.gasstation.backend.shifts.spi.ScheduleWorkerInfo;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use cases of the monthly shift schedule: save or edit one day, assign a code to a range, and read the schedule of
 * a range with its daily coverage. Every change made here is manual; the automatic rotation lives in
 * {@link ShiftRotationService}. Returns domain results; the controller converts them
 * to DTOs.
 *
 * <p>Workers are read through {@link ScheduleWorkerDirectory}, implemented by the {@code workers} module, so this
 * module never depends on {@code workers}. Coverage is always calculated over every active worker, never stored.
 */
@Service
@RequiredArgsConstructor
public class ShiftScheduleService {

    /** Time zone of the station. "Today" is always calculated with it; the server may run in UTC. */
    private static final ZoneId STATION_ZONE = ZoneId.of("America/Bogota");

    /** Longest range assigned at once: two months, so a typo in a date cannot rewrite a whole year. */
    private static final int MAX_RANGE_DAYS = 62;

    private static final String REST = "DESCANSO";

    private final ShiftAssignmentRepository shiftAssignmentRepository;
    private final ShiftCodeRepository shiftCodeRepository;
    private final ScheduleWorkerDirectory scheduleWorkerDirectory;

    /**
     * Saves the shift code of a worker on one day, replacing the one the day had (code and note).
     *
     * @throws ShiftException {@code PAST_DATE} if the date is before today
     * @throws ShiftException {@code WORKER_NOT_FOUND} if the worker does not exist
     * @throws ShiftException {@code WORKER_INACTIVE} if the worker is inactive
     * @throws ShiftException {@code SHIFT_CODE_NOT_FOUND} if the code does not exist or is not active
     * @throws ShiftException {@code ROLE_MISMATCH} if the code belongs to a role different from the worker's
     *                                         (DESCANSO has no role and is valid for everyone)
     */
    @Transactional
    public ScheduleChange assign(UUID workerId, LocalDate date, String shiftCode, String note) {
        requireNotPast(date);
        ScheduleWorkerInfo worker = requireAvailableWorker(workerId);
        ShiftCode code = shiftCodeRepository.findByCode(shiftCode)
                .filter(ShiftCode::isActive)
                .orElseThrow(() -> new ShiftException(ShiftError.SHIFT_CODE_NOT_FOUND, shiftCode));
        Role codeRole = code.getRole();
        if (codeRole != null && !codeRole.getId().equals(worker.roleId())) {
            throw new ShiftException(ShiftError.ROLE_MISMATCH, code.getCode(), codeRole.getName());
        }

        ShiftAssignment assignment = shiftAssignmentRepository.findByWorkerIdAndWorkDate(workerId, date)
                .orElse(null);
        if (assignment == null) {
            assignment = new ShiftAssignment(workerId, date, code, note);
        } else {
            assignment.change(code, note);
        }
        shiftAssignmentRepository.save(assignment);
        return new ScheduleChange(1, uncoveredDates(date, date));
    }

    /**
     * Assigns the same code to a worker on every day of a range, replacing what each day had. Every row is manual, so
     * the automatic rotation keeps it. With {@code leaveSundaysAsRest} the Sundays of the range get DESCANSO instead.
     *
     * @throws ShiftException {@code INVALID_DATE_RANGE} if {@code to} is before {@code from}
     * @throws ShiftException {@code INVALID_RANGE} if the range is longer than 62 days
     * @throws ShiftException {@code PAST_DATE} if {@code from} is before today
     * @throws ShiftException {@code WORKER_NOT_FOUND} if the worker does not exist
     * @throws ShiftException {@code WORKER_INACTIVE} if the worker is inactive
     * @throws ShiftException {@code SHIFT_CODE_NOT_FOUND} if the code does not exist or is not active
     * @throws ShiftException {@code ROLE_MISMATCH} if the code belongs to a role different from the worker's
     */
    @Transactional
    public ScheduleChange assignRange(UUID workerId, LocalDate from, LocalDate to, String shiftCode, String note,
                                      boolean leaveSundaysAsRest) {
        requireValidRange(from, to);
        if (ChronoUnit.DAYS.between(from, to) + 1 > MAX_RANGE_DAYS) {
            throw new ShiftException(ShiftError.INVALID_RANGE, "El rango no puede superar " + MAX_RANGE_DAYS + " días");
        }
        requireNotPast(from);
        ScheduleWorkerInfo worker = requireAvailableWorker(workerId);
        ShiftCode code = requireCodeForWorker(shiftCode, worker);
        ShiftCode rest = leaveSundaysAsRest ? requireCodeForWorker(REST, worker) : null;

        Map<LocalDate, ShiftAssignment> existing = shiftAssignmentRepository.findByWorkDateBetween(from, to).stream()
                .filter(assignment -> assignment.getWorkerId().equals(workerId))
                .collect(Collectors.toMap(ShiftAssignment::getWorkDate, Function.identity()));
        List<ShiftAssignment> rows = new ArrayList<>();
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            ShiftCode dayCode = rest != null && date.getDayOfWeek() == DayOfWeek.SUNDAY ? rest : code;
            ShiftAssignment assignment = existing.get(date);
            if (assignment == null) {
                assignment = new ShiftAssignment(workerId, date, dayCode, note);
            } else {
                assignment.change(dayCode, note);
            }
            rows.add(assignment);
        }
        shiftAssignmentRepository.saveAll(rows);
        return new ScheduleChange(rows.size(), uncoveredDates(from, to));
    }

    /** The code exists, is active and belongs to the worker's role; DESCANSO has no role and is valid for everyone. */
    private ShiftCode requireCodeForWorker(String shiftCode, ScheduleWorkerInfo worker) {
        ShiftCode code = shiftCodeRepository.findByCode(shiftCode)
                .filter(ShiftCode::isActive)
                .orElseThrow(() -> new ShiftException(ShiftError.SHIFT_CODE_NOT_FOUND, shiftCode));
        Role codeRole = code.getRole();
        if (codeRole != null && !codeRole.getId().equals(worker.roleId())) {
            throw new ShiftException(ShiftError.ROLE_MISMATCH, code.getCode(), codeRole.getName());
        }
        return code;
    }

    /**
     * Schedule of a range with its daily coverage. Returns what is stored, past days included.
     *
     * <p>Only active workers with at least one shift in the range are shown; rows of inactive workers are kept but
     * hidden. The filters only affect the rows shown; coverage is calculated over every active worker.
     *
     * @param workerId only this worker; null for all
     * @param period   only codes of this period; DESCANSO has no period and is not shown when filtering; null for all
     * @throws ShiftException {@code INVALID_DATE_RANGE} if {@code to} is before {@code from}
     */
    @Transactional(readOnly = true)
    public ScheduleView getSchedule(LocalDate from, LocalDate to, UUID workerId, ShiftPeriod period) {
        requireValidRange(from, to);
        ActiveAssignments active = activeAssignments(from, to);

        Predicate<ShiftAssignment> visible = assignment ->
                (workerId == null || workerId.equals(assignment.getWorkerId()))
                        && (period == null || period == assignment.getShiftCode().getPeriod());
        Map<UUID, List<ShiftAssignment>> byWorker = active.assignments().stream()
                .filter(visible)
                .sorted(Comparator.comparing(ShiftAssignment::getWorkDate))
                .collect(Collectors.groupingBy(ShiftAssignment::getWorkerId, LinkedHashMap::new, Collectors.toList()));

        List<WorkerShifts> workers = byWorker.entrySet().stream()
                .map(entry -> new WorkerShifts(entry.getKey(),
                        active.workers().get(entry.getKey()).fullName(), entry.getValue()))
                .sorted(Comparator.comparing(WorkerShifts::fullName))
                .toList();
        return new ScheduleView(from, to, workers, ScheduleCoverageCalculator.coverage(from, to, active.assignments()));
    }

    private void requireNotPast(LocalDate date) {
        if (date.isBefore(LocalDate.now(STATION_ZONE))) {
            throw new ShiftException(ShiftError.PAST_DATE, date);
        }
    }

    private static void requireValidRange(LocalDate start, LocalDate end) {
        if (end.isBefore(start)) {
            throw new ShiftException(ShiftError.INVALID_DATE_RANGE, start, end);
        }
    }

    private ScheduleWorkerInfo requireAvailableWorker(UUID workerId) {
        ScheduleWorkerInfo worker = scheduleWorkerDirectory.getWorkersByIds(Set.of(workerId)).get(workerId);
        if (worker == null) {
            throw new ShiftException(ShiftError.WORKER_NOT_FOUND, workerId);
        }
        if (!worker.active()) {
            throw new ShiftException(ShiftError.WORKER_INACTIVE, workerId);
        }
        return worker;
    }

    /** Days of the range without coverage, after the changes of the current transaction. */
    private List<LocalDate> uncoveredDates(LocalDate from, LocalDate to) {
        return ScheduleCoverageCalculator.coverage(from, to, activeAssignments(from, to).assignments()).stream()
                .filter(day -> !day.covered())
                .map(DailyCoverage::date)
                .toList();
    }

    /** Assignments of the range that belong to active workers, and those workers keyed by id. */
    private ActiveAssignments activeAssignments(LocalDate from, LocalDate to) {
        List<ShiftAssignment> assignments = shiftAssignmentRepository.findByWorkDateBetween(from, to);
        Set<UUID> workerIds = assignments.stream().map(ShiftAssignment::getWorkerId).collect(Collectors.toSet());
        Map<UUID, ScheduleWorkerInfo> activeWorkers = scheduleWorkerDirectory.getWorkersByIds(workerIds).values()
                .stream()
                .filter(ScheduleWorkerInfo::active)
                .collect(Collectors.toMap(ScheduleWorkerInfo::id, Function.identity()));
        List<ShiftAssignment> activeOnly = assignments.stream()
                .filter(assignment -> activeWorkers.containsKey(assignment.getWorkerId()))
                .toList();
        return new ActiveAssignments(activeOnly, activeWorkers);
    }

    /** Assignments of active workers in a range, with those workers keyed by id. */
    private record ActiveAssignments(List<ShiftAssignment> assignments, Map<UUID, ScheduleWorkerInfo> workers) {
    }
}
