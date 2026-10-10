package com.edu.unicauca.gasstation.backend.shifts.domain.services;

import com.edu.unicauca.gasstation.backend.shifts.ScheduleWorkerDirectory;
import com.edu.unicauca.gasstation.backend.shifts.ScheduleWorkerInfo;
import com.edu.unicauca.gasstation.backend.shifts.domain.models.DailyCoverage;
import com.edu.unicauca.gasstation.backend.shifts.domain.models.Role;
import com.edu.unicauca.gasstation.backend.shifts.domain.models.ScheduleChange;
import com.edu.unicauca.gasstation.backend.shifts.domain.models.ScheduleView;
import com.edu.unicauca.gasstation.backend.shifts.domain.models.ShiftAssignment;
import com.edu.unicauca.gasstation.backend.shifts.domain.models.ShiftCode;
import com.edu.unicauca.gasstation.backend.shifts.domain.models.ShiftPeriod;
import com.edu.unicauca.gasstation.backend.shifts.domain.models.WorkerShifts;
import com.edu.unicauca.gasstation.backend.shifts.exception.InvalidDateRangeException;
import com.edu.unicauca.gasstation.backend.shifts.exception.InvalidRotationLengthException;
import com.edu.unicauca.gasstation.backend.shifts.exception.PastDateException;
import com.edu.unicauca.gasstation.backend.shifts.exception.RotationNotAllowedException;
import com.edu.unicauca.gasstation.backend.shifts.exception.ScheduleWorkerNotFoundException;
import com.edu.unicauca.gasstation.backend.shifts.exception.ShiftCodeNotFoundException;
import com.edu.unicauca.gasstation.backend.shifts.exception.ShiftRoleMismatchException;
import com.edu.unicauca.gasstation.backend.shifts.exception.WorkerNotAvailableException;
import com.edu.unicauca.gasstation.backend.shifts.domain.repositories.RoleRepository;
import com.edu.unicauca.gasstation.backend.shifts.domain.repositories.ShiftAssignmentRepository;
import com.edu.unicauca.gasstation.backend.shifts.domain.repositories.ShiftCodeRepository;
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
 * Use cases of the monthly shift schedule (S1-27): generate the 2x2 rotation of a titular, save or edit one day,
 * and read the schedule of a range with its daily coverage. Returns domain results; the controller converts them
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

    private static final String TITULAR = "TITULAR";
    private static final String APOYO = "APOYO";

    /** 2x2 rotation of a titular, repeated from the start date: 2 days, 2 nights, 2 days off. */
    private static final List<String> ROTATION_PATTERN = List.of("DIA", "DIA", "NOCHE", "NOCHE", "DESCANSO", "DESCANSO");

    /**
     * Longest rotation, counted from its start date. Bounds a single request: a typo in the year would otherwise
     * create thousands of rows in one transaction.
     */
    private static final int MAX_ROTATION_MONTHS = 3;

    private final ShiftAssignmentRepository shiftAssignmentRepository;
    private final ShiftCodeRepository shiftCodeRepository;
    private final RoleRepository roleRepository;
    private final ScheduleWorkerDirectory scheduleWorkerDirectory;

    /**
     * Saves the shift code of a worker on one day, replacing the one the day had (code and note).
     *
     * @throws PastDateException               if the date is before today
     * @throws ScheduleWorkerNotFoundException if the worker does not exist
     * @throws WorkerNotAvailableException     if the worker is inactive
     * @throws ShiftCodeNotFoundException      if the code does not exist or is not active
     * @throws ShiftRoleMismatchException      if the code belongs to a role different from the worker's
     *                                         (DESCANSO has no role and is valid for everyone)
     */
    @Transactional
    public ScheduleChange assign(UUID workerId, LocalDate date, String shiftCode, String note) {
        requireNotPast(date);
        ScheduleWorkerInfo worker = requireAvailableWorker(workerId);
        ShiftCode code = shiftCodeRepository.findByCode(shiftCode)
                .filter(ShiftCode::isActive)
                .orElseThrow(() -> new ShiftCodeNotFoundException(shiftCode));
        Role codeRole = code.getRole();
        if (codeRole != null && !codeRole.getId().equals(worker.roleId())) {
            throw new ShiftRoleMismatchException(code.getCode(), codeRole.getName());
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
     * Schedule of a range with its daily coverage. Returns what is stored, past days included.
     *
     * <p>Only active workers with at least one shift in the range are shown; rows of inactive workers are kept but
     * hidden. The filters only affect the rows shown; coverage is calculated over every active worker.
     *
     * @param workerId only this worker; null for all
     * @param period   only codes of this period; DESCANSO has no period and is not shown when filtering; null for all
     * @throws InvalidDateRangeException if {@code to} is before {@code from}
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
        return new ScheduleView(from, to, workers, coverage(from, to, active.assignments()));
    }

    /**
     * Generates the 2x2 rotation (DIA, DIA, NOCHE, NOCHE, DESCANSO, DESCANSO) of a titular from {@code startDate},
     * replacing everything the worker had in the range. The exact day code (DIA6, 9-6) is changed afterwards by
     * editing that day. Rows of the rotation have no note.
     *
     * <p>The range covers at least one full cycle (shorter changes are made day by day) and at most a quarter from
     * {@code startDate}.
     *
     * @throws PastDateException               if {@code startDate} is before today
     * @throws InvalidDateRangeException       if {@code endDate} is before {@code startDate}
     * @throws InvalidRotationLengthException  if the range is shorter than one cycle or longer than a quarter
     * @throws ScheduleWorkerNotFoundException if the worker does not exist
     * @throws WorkerNotAvailableException     if the worker is inactive
     * @throws RotationNotAllowedException     if the worker is not TITULAR
     */
    @Transactional
    public ScheduleChange generateRotation(UUID workerId, LocalDate startDate, LocalDate endDate) {
        requireNotPast(startDate);
        requireValidRange(startDate, endDate);
        requireRotationLength(startDate, endDate);
        ScheduleWorkerInfo worker = requireAvailableWorker(workerId);
        boolean titular = roleRepository.findByName(TITULAR)
                .map(role -> role.getId().equals(worker.roleId()))
                .orElse(false);
        if (!titular) {
            throw new RotationNotAllowedException();
        }

        // Immediate JPQL DELETE before the INSERTs, so the unique (worker, date) constraint is not broken
        shiftAssignmentRepository.deleteByWorkerInRange(workerId, startDate, endDate);

        Map<String, ShiftCode> codes = Set.copyOf(ROTATION_PATTERN).stream()
                .collect(Collectors.toMap(Function.identity(), code -> shiftCodeRepository.findByCode(code)
                        .orElseThrow(() -> new ShiftCodeNotFoundException(code))));
        List<ShiftAssignment> rotation = new ArrayList<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            int position = (int) (ChronoUnit.DAYS.between(startDate, date) % ROTATION_PATTERN.size());
            rotation.add(new ShiftAssignment(workerId, date, codes.get(ROTATION_PATTERN.get(position)), null));
        }
        shiftAssignmentRepository.saveAll(rotation);
        return new ScheduleChange(rotation.size(), uncoveredDates(startDate, endDate));
    }

    private void requireNotPast(LocalDate date) {
        if (date.isBefore(LocalDate.now(STATION_ZONE))) {
            throw new PastDateException(date);
        }
    }

    private static void requireValidRange(LocalDate start, LocalDate end) {
        if (end.isBefore(start)) {
            throw new InvalidDateRangeException(start, end);
        }
    }

    /** At least one full cycle of the pattern and at most a quarter, both counted from the start date. */
    private static void requireRotationLength(LocalDate startDate, LocalDate endDate) {
        LocalDate minEndDate = startDate.plusDays(ROTATION_PATTERN.size() - 1L);
        LocalDate maxEndDate = startDate.plusMonths(MAX_ROTATION_MONTHS).minusDays(1);
        if (endDate.isBefore(minEndDate) || endDate.isAfter(maxEndDate)) {
            throw new InvalidRotationLengthException(startDate, minEndDate, maxEndDate);
        }
    }

    private ScheduleWorkerInfo requireAvailableWorker(UUID workerId) {
        ScheduleWorkerInfo worker = scheduleWorkerDirectory.getWorkersByIds(Set.of(workerId)).get(workerId);
        if (worker == null) {
            throw new ScheduleWorkerNotFoundException(workerId);
        }
        if (!worker.active()) {
            throw new WorkerNotAvailableException(workerId);
        }
        return worker;
    }

    /** Days of the range without coverage, after the changes of the current transaction. */
    private List<LocalDate> uncoveredDates(LocalDate from, LocalDate to) {
        return coverage(from, to, activeAssignments(from, to).assignments()).stream()
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

    /**
     * Coverage of every day of the range, including days without any row (those are not covered). A day is covered
     * when there is a titular with a DAY code, a titular with a NIGHT code and, if any code of the day requires
     * support, at least one APOYO shift (any code; the exact paired code is not required).
     */
    private static List<DailyCoverage> coverage(LocalDate from, LocalDate to, List<ShiftAssignment> assignments) {
        Map<LocalDate, List<ShiftCode>> codesByDate = assignments.stream()
                .collect(Collectors.groupingBy(ShiftAssignment::getWorkDate,
                        Collectors.mapping(ShiftAssignment::getShiftCode, Collectors.toList())));
        List<DailyCoverage> coverage = new ArrayList<>();
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            List<ShiftCode> codes = codesByDate.getOrDefault(date, List.of());
            boolean dayCovered = codes.stream().anyMatch(code -> hasRole(code, TITULAR) && code.getPeriod() == ShiftPeriod.DAY);
            boolean nightCovered = codes.stream().anyMatch(code -> hasRole(code, TITULAR) && code.getPeriod() == ShiftPeriod.NIGHT);
            boolean supportNeeded = codes.stream().anyMatch(ShiftCode::isRequiresSupport);
            boolean supportCovered = !supportNeeded || codes.stream().anyMatch(code -> hasRole(code, APOYO));
            coverage.add(new DailyCoverage(date, dayCovered, nightCovered, supportCovered));
        }
        return coverage;
    }

    private static boolean hasRole(ShiftCode code, String roleName) {
        return code.getRole() != null && roleName.equals(code.getRole().getName());
    }

    /** Assignments of active workers in a range, with those workers keyed by id. */
    private record ActiveAssignments(List<ShiftAssignment> assignments, Map<UUID, ScheduleWorkerInfo> workers) {
    }
}
