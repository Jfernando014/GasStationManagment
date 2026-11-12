package com.edu.unicauca.gasstation.backend.workers.domain.services;

import com.edu.unicauca.gasstation.backend.shifts.catalog.RoleInfo;
import com.edu.unicauca.gasstation.backend.shifts.catalog.ShiftExternalService;
import com.edu.unicauca.gasstation.backend.workers.domain.models.AffectedDate;
import com.edu.unicauca.gasstation.backend.workers.domain.models.Fortnight;
import com.edu.unicauca.gasstation.backend.workers.domain.models.HoursSync;
import com.edu.unicauca.gasstation.backend.workers.domain.models.OvertimeAdjustment;
import com.edu.unicauca.gasstation.backend.workers.domain.models.OvertimeParameterHistory;
import com.edu.unicauca.gasstation.backend.workers.domain.models.OvertimeSheet;
import com.edu.unicauca.gasstation.backend.workers.domain.models.OvertimeSummary;
import com.edu.unicauca.gasstation.backend.workers.domain.models.OvertimeWorker;
import com.edu.unicauca.gasstation.backend.workers.domain.models.Worker;
import com.edu.unicauca.gasstation.backend.workers.domain.models.WorkerDayHours;
import com.edu.unicauca.gasstation.backend.workers.domain.ports.HolidayCalendarPort;
import com.edu.unicauca.gasstation.backend.workers.domain.repositories.HoursSyncRepository;
import com.edu.unicauca.gasstation.backend.workers.domain.repositories.OvertimeAdjustmentRepository;
import com.edu.unicauca.gasstation.backend.workers.domain.repositories.OvertimeParameterRepository;
import com.edu.unicauca.gasstation.backend.workers.domain.repositories.WorkerDayHoursRepository;
import com.edu.unicauca.gasstation.backend.workers.domain.repositories.WorkerRepository;
import com.edu.unicauca.gasstation.backend.workers.exception.WorkerNotFoundException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Builds the fortnight overtime sheets from what is kept. Nothing is stored: each sheet is worked out when asked,
 * with one read of the hours, one of the states of the days, one of the parameter versions, one of the adjustments
 * and one of the holidays, whatever the number of workers.
 */
@Service
@RequiredArgsConstructor
public class OvertimeSheetService {

    private final WorkerDayHoursRepository hoursRepository;
    private final HoursSyncRepository syncRepository;
    private final OvertimeParameterRepository parameterRepository;
    private final OvertimeAdjustmentRepository adjustmentRepository;
    private final HolidayCalendarPort holidayCalendar;
    private final WorkerRepository workerRepository;
    private final ShiftExternalService shiftExternalService;

    /** Sheet of one worker; fails if the worker does not exist or the fortnight is not valid. */
    public OvertimeSheet getWorkerSheet(UUID workerId, YearMonth month, int fortnightNumber) {
        Fortnight fortnight = Fortnight.of(month, fortnightNumber);
        Worker worker = workerRepository.findById(workerId).orElseThrow(() -> new WorkerNotFoundException(workerId));
        Context context = context(fortnight);
        Map<LocalDate, WorkerDayHours> records = hoursRepository
                .findByWorkerBetween(workerId, fortnight.from(), fortnight.to()).stream()
                .collect(Collectors.toMap(WorkerDayHours::workDate, Function.identity()));
        List<OvertimeAdjustment> adjustments = adjustmentRepository.findByWorkerAndFortnight(workerId,
                fortnight.from());
        Map<UUID, RoleInfo> roles = shiftExternalService.getRolesByIds(Set.of(worker.getRoleId()));
        return OvertimeCalculator.sheet(worker(worker, roles), fortnight, records, context.holidays(),
                context.parameters(), adjustments, context.affectedDates());
    }

    /** Sheets of every worker who has hours or adjustments in the fortnight, ordered by name. */
    public OvertimeSummary getSummary(YearMonth month, int fortnightNumber) {
        Fortnight fortnight = Fortnight.of(month, fortnightNumber);
        Context context = context(fortnight);
        Map<UUID, Map<LocalDate, WorkerDayHours>> recordsByWorker = hoursRepository
                .findBetween(fortnight.from(), fortnight.to()).stream()
                .collect(Collectors.groupingBy(WorkerDayHours::workerId,
                        Collectors.toMap(WorkerDayHours::workDate, Function.identity())));
        Map<UUID, List<OvertimeAdjustment>> adjustmentsByWorker = adjustmentRepository
                .findByFortnight(fortnight.from()).stream()
                .collect(Collectors.groupingBy(OvertimeAdjustment::workerId));
        Set<UUID> workerIds = new HashSet<>(recordsByWorker.keySet());
        workerIds.addAll(adjustmentsByWorker.keySet());
        List<Worker> workers = workerIds.isEmpty() ? List.of() : workerRepository.findAllByIds(workerIds);
        Map<UUID, RoleInfo> roles = workers.isEmpty() ? Map.of() : shiftExternalService.getRolesByIds(
                workers.stream().map(Worker::getRoleId).collect(Collectors.toSet()));
        List<OvertimeSheet> sheets = workers.stream()
                .sorted(Comparator.comparing(Worker::getFullName).thenComparing(Worker::getId))
                .map(worker -> OvertimeCalculator.sheet(worker(worker, roles), fortnight,
                        recordsByWorker.getOrDefault(worker.getId(), Map.of()), context.holidays(),
                        context.parameters(), adjustmentsByWorker.getOrDefault(worker.getId(), List.of()),
                        context.affectedDates()))
                .toList();
        return new OvertimeSummary(fortnight, sheets, context.affectedDates());
    }

    private Context context(Fortnight fortnight) {
        // The night of the last day ends on the next morning, which may be a Sunday or a holiday
        Set<LocalDate> holidays = holidayCalendar.holidaysBetween(fortnight.from(), fortnight.to().plusDays(1));
        OvertimeParameterHistory parameters = OvertimeParameterHistory.of(parameterRepository.findAll());
        Map<LocalDate, HoursSync> syncs = syncRepository.findBetween(fortnight.from(), fortnight.to()).stream()
                .collect(Collectors.toMap(HoursSync::workDate, Function.identity()));
        return new Context(holidays, parameters, OvertimeCalculator.affectedDates(fortnight, syncs));
    }

    private static OvertimeWorker worker(Worker worker, Map<UUID, RoleInfo> roles) {
        RoleInfo role = roles.get(worker.getRoleId());
        return new OvertimeWorker(worker.getId(), worker.getFullName(), role == null ? null : role.name());
    }

    private record Context(Set<LocalDate> holidays, OvertimeParameterHistory parameters,
                           List<AffectedDate> affectedDates) {
    }
}
