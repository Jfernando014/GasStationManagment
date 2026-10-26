package com.edu.unicauca.gasstation.backend.shifts.domain.services;

import com.edu.unicauca.gasstation.backend.shifts.daystaff.DayStaff.NoveltyType;
import com.edu.unicauca.gasstation.backend.shifts.daystaff.DayStaff.Step1Status;
import com.edu.unicauca.gasstation.backend.shifts.domain.models.DayLineup;
import com.edu.unicauca.gasstation.backend.shifts.domain.models.DayStaffStep;
import com.edu.unicauca.gasstation.backend.shifts.domain.models.NoveltyDraft;
import com.edu.unicauca.gasstation.backend.shifts.domain.models.ShiftAssignment;
import com.edu.unicauca.gasstation.backend.shifts.domain.models.ShiftCode;
import com.edu.unicauca.gasstation.backend.shifts.domain.models.ShiftNovelty;
import com.edu.unicauca.gasstation.backend.shifts.domain.repositories.ShiftAssignmentRepository;
import com.edu.unicauca.gasstation.backend.shifts.domain.repositories.ShiftCodeRepository;
import com.edu.unicauca.gasstation.backend.shifts.domain.repositories.ShiftNoveltyRepository;
import com.edu.unicauca.gasstation.backend.shifts.exception.ShiftError;
import com.edu.unicauca.gasstation.backend.shifts.exception.ShiftException;
import com.edu.unicauca.gasstation.backend.shifts.spi.DayJournalPort;
import com.edu.unicauca.gasstation.backend.shifts.spi.ScheduleWorkerDirectory;
import com.edu.unicauca.gasstation.backend.shifts.spi.ScheduleWorkerInfo;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
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
 * Use cases of step 1 of the journal, "Personal del día": read the staff of the day, register and cancel
 * novelties, and confirm the staff.
 *
 * <p>The grid ({@code shift_assignment}) is never modified: novelties are stored apart, so the original and the final
 * code are both kept. Past dates are accepted only when the journal of that date exists and is still editable
 * ({@link DayJournalPort}); the grid rule that rejects past days does not apply here. Coverage and pairing
 * problems only warn and never block.
 */
@Service
@RequiredArgsConstructor
public class ShiftDayStaffService {

    private final ShiftAssignmentRepository shiftAssignmentRepository;
    private final ShiftNoveltyRepository shiftNoveltyRepository;
    private final ShiftCodeRepository shiftCodeRepository;
    private final ScheduleWorkerDirectory scheduleWorkerDirectory;
    private final DayJournalPort dayJournalPort;

    /**
     * Step 1 of a date: the grid with its active novelties, the workers on DESCANSO apart and the warnings. Works
     * for any date; a closed or missing journal comes back with {@code editable = false}.
     */
    @Transactional(readOnly = true)
    public DayStaffStep getStep(LocalDate date) {
        return step(Objects.requireNonNull(date, "date"));
    }

    /**
     * Registers a novelty of the day and returns step 1 with it applied.
     *
     * @throws ShiftException {@code JOURNAL_NOT_FOUND} if there is no journal for the date
     * @throws ShiftException {@code JOURNAL_NOT_EDITABLE} if the journal is closed
     * @throws ShiftException {@code INVALID_NOVELTY_REASON} if the reason does not have 3 to 255 characters
     * @throws ShiftException {@code SAME_WORKER} if the replacement or the other worker is the same worker
     * @throws ShiftException {@code WORKER_NOT_FOUND} if a worker does not exist
     * @throws ShiftException {@code WORKER_INACTIVE} if the worker who would start working is inactive
     * @throws ShiftException {@code SHIFT_HOURS_OVERLAP} if the replacement works a shift whose hours cross
     * @throws ShiftException {@code WORKER_ALREADY_WORKING} if the replacement already works another shift
     * @throws ShiftException {@code SHIFT_CODE_NOT_FOUND} if the code does not exist or is not active
     * @throws ShiftException {@code EXCEPTIONAL_COVERAGE_REQUIRED} if a code of another role is not marked as exceptional
     * @throws ShiftException {@code NOVELTY_CONFLICT} if a worker already takes part in another novelty that day
     * @throws ShiftException {@code INVALID_NOVELTY} if the novelty does not fit the staff of the day
     */
    @Transactional
    public DayStaffStep registerNovelty(LocalDate date, NoveltyDraft draft) {
        requireEditable(Objects.requireNonNull(date, "date"));
        register(date, Objects.requireNonNull(draft, "draft"));
        return step(date);
    }

    /**
     * Cancels a novelty: the day goes back to the grid code. Cancelling an already cancelled novelty changes
     * nothing.
     *
     * @throws ShiftException {@code JOURNAL_NOT_FOUND} if there is no journal for the date
     * @throws ShiftException {@code JOURNAL_NOT_EDITABLE} if the journal is closed
     * @throws ShiftException {@code NOVELTY_NOT_FOUND} if the novelty does not exist or belongs to another date
     */
    @Transactional
    public DayStaffStep cancelNovelty(LocalDate date, UUID noveltyId) {
        requireEditable(Objects.requireNonNull(date, "date"));
        ShiftNovelty novelty = shiftNoveltyRepository.findById(noveltyId)
                .filter(found -> found.getWorkDate().equals(date))
                .orElseThrow(() -> new ShiftException(ShiftError.NOVELTY_NOT_FOUND, noveltyId));
        if (novelty.isActive()) {
            novelty.cancel(Instant.now());
            shiftNoveltyRepository.save(novelty);
        }
        return step(date);
    }

    /**
     * Confirms the staff of the day in a single transaction: (1) the journal must be editable, (2) the
     * novelties still pending in the request are saved, (3) at least one worker must be left working and
     * (4) step 1 of the journal is marked as completed. If any step fails, nothing is saved. Warnings do
     * not block.
     *
     * @param pending novelties to save together with the confirmation; empty when they were registered before
     * @throws ShiftException {@code NO_STAFF_TO_CONFIRM} if nobody would work that day
     */
    @Transactional
    public DayStaffStep confirm(LocalDate date, List<NoveltyDraft> pending) {
        requireEditable(Objects.requireNonNull(date, "date"));
        pending.forEach(draft -> register(date, draft));
        DayContext day = load(date, Set.of());
        boolean someoneWorks = day.lineup().working().stream()
                .anyMatch(line -> day.activeWorkers().containsKey(line.workerId()));
        if (!someoneWorks) {
            throw new ShiftException(ShiftError.NO_STAFF_TO_CONFIRM, date);
        }
        dayJournalPort.markStep1Completed(date);
        return step(date);
    }

    private void requireEditable(LocalDate date) {
        if (dayJournalPort.step1Status(date) == Step1Status.NO_JOURNAL) {
            throw new ShiftException(ShiftError.JOURNAL_NOT_FOUND, date);
        }
        if (!dayJournalPort.isEditable(date)) {
            throw new ShiftException(ShiftError.JOURNAL_NOT_EDITABLE, date);
        }
    }

    private void register(LocalDate date, NoveltyDraft draft) {
        if (draft.type() == null || draft.type() == NoveltyType.NONE) {
            throw new ShiftException(ShiftError.INVALID_NOVELTY, "El tipo de novedad no es válido");
        }
        String reason = requireValidReason(draft.reason());
        if (draft.workerId().equals(draft.counterpartWorkerId())) {
            throw new ShiftException(ShiftError.SAME_WORKER);
        }
        Set<UUID> involved = new HashSet<>();
        involved.add(draft.workerId());
        if (draft.counterpartWorkerId() != null) {
            involved.add(draft.counterpartWorkerId());
        }
        DayContext day = load(date, involved);
        involved.forEach(worker -> requireWithoutNovelty(day, worker));
        ShiftNovelty novelty = switch (draft.type()) {
            case REPLACEMENT -> replacement(date, draft, reason, day);
            case ABSENCE -> absence(date, draft, reason, day);
            case SHIFT_CHANGE -> shiftChange(date, draft, reason, day);
            case SWAP -> swap(date, draft, reason, day);
            default -> throw new ShiftException(ShiftError.INVALID_NOVELTY, "El tipo de novedad no es válido");
        };
        shiftNoveltyRepository.save(novelty);
    }

    /** Replacement: the absent worker leaves; the replacement works the code of the absent worker or the one given. */
    private ShiftNovelty replacement(LocalDate date, NoveltyDraft draft, String reason, DayContext day) {
        UUID replacementId = requireCounterpart(draft, "Indique el vendedor que reemplaza");
        DayLineup.Line absent = requireWorking(day, draft.workerId(), date);
        ScheduleWorkerInfo replacement = requireActive(day, replacementId);
        ShiftCode code = draft.shiftCode() == null ? absent.workedCode() : requireWorkingCode(draft.shiftCode());
        day.lineup().line(replacementId).filter(DayLineup.Line::working).ifPresent(current -> {
            if (current.workedCode().overlaps(code)) {
                throw new ShiftException(ShiftError.SHIFT_HOURS_OVERLAP, replacement.fullName(), current.workedCode().getCode(), code.getCode());
            }
            throw new ShiftException(ShiftError.WORKER_ALREADY_WORKING, replacement.fullName(), current.workedCode().getCode());
        });
        boolean exceptional = requireRoleOrExceptional(replacement, code, draft.exceptionalCoverage());
        return ShiftNovelty.register(date, NoveltyType.REPLACEMENT, absent.workerId(), replacementId,
                absent.gridCode(), code, exceptional, reason, Instant.now());
    }

    /** Absence: the worker does not come and nobody covers; the shift is left uncovered and only warned. */
    private ShiftNovelty absence(LocalDate date, NoveltyDraft draft, String reason, DayContext day) {
        DayLineup.Line absent = requireWorking(day, draft.workerId(), date);
        return ShiftNovelty.register(date, NoveltyType.ABSENCE, absent.workerId(), null, absent.gridCode(), null,
                false, reason, Instant.now());
    }

    /**
     * Shift change: a worker works another code. A worker without a working code that day (DESCANSO or no row) is added to
     * the staff this way, which also covers a day without grid.
     */
    private ShiftNovelty shiftChange(LocalDate date, NoveltyDraft draft, String reason, DayContext day) {
        if (draft.shiftCode() == null || draft.shiftCode().isBlank()) {
            throw new ShiftException(ShiftError.INVALID_NOVELTY, "Indique el código del cambio de turno");
        }
        ScheduleWorkerInfo worker = requireActive(day, draft.workerId());
        ShiftCode code = requireWorkingCode(draft.shiftCode());
        DayLineup.Line line = day.lineup().line(worker.id()).orElse(null);
        if (line != null && line.working() && line.workedCode().getId().equals(code.getId())) {
            throw new ShiftException(ShiftError.INVALID_NOVELTY, worker.fullName() + " ya trabaja " + code.getCode() + " ese día");
        }
        ShiftCode original = line == null || line.gridCode() == null ? restCode() : line.gridCode();
        boolean exceptional = requireRoleOrExceptional(worker, code, draft.exceptionalCoverage());
        return ShiftNovelty.register(date, NoveltyType.SHIFT_CHANGE, worker.id(), null, original, code, exceptional,
                reason, Instant.now());
    }

    /** Swap: two working workers swap their codes of the day. */
    private ShiftNovelty swap(LocalDate date, NoveltyDraft draft, String reason, DayContext day) {
        UUID otherId = requireCounterpart(draft, "Indique el otro vendedor del intercambio");
        DayLineup.Line first = requireWorking(day, draft.workerId(), date);
        DayLineup.Line second = requireWorking(day, otherId, date);
        if (first.workedCode().getId().equals(second.workedCode().getId())) {
            throw new ShiftException(ShiftError.INVALID_NOVELTY, "Los dos vendedores tienen el mismo código; no hay nada que intercambiar");
        }
        boolean firstExceptional = requireRoleOrExceptional(
                requireActive(day, first.workerId()), second.workedCode(), draft.exceptionalCoverage());
        boolean secondExceptional = requireRoleOrExceptional(
                requireActive(day, otherId), first.workedCode(), draft.exceptionalCoverage());
        return ShiftNovelty.register(date, NoveltyType.SWAP, first.workerId(), otherId, first.gridCode(),
                second.gridCode(), firstExceptional || secondExceptional, reason, Instant.now());
    }

    /** The reason is measured without leading and trailing spaces. */
    private static String requireValidReason(String reason) {
        String stripped = reason == null ? "" : reason.strip();
        if (stripped.length() < 3 || stripped.length() > 255) {
            throw new ShiftException(ShiftError.INVALID_NOVELTY_REASON);
        }
        return stripped;
    }

    private static UUID requireCounterpart(NoveltyDraft draft, String message) {
        if (draft.counterpartWorkerId() == null) {
            throw new ShiftException(ShiftError.INVALID_NOVELTY, message);
        }
        return draft.counterpartWorkerId();
    }

    private static ScheduleWorkerInfo requireWorker(DayContext day, UUID workerId) {
        ScheduleWorkerInfo worker = day.workers().get(workerId);
        if (worker == null) {
            throw new ShiftException(ShiftError.WORKER_NOT_FOUND, workerId);
        }
        return worker;
    }

    private static ScheduleWorkerInfo requireActive(DayContext day, UUID workerId) {
        ScheduleWorkerInfo worker = requireWorker(day, workerId);
        if (!worker.active()) {
            throw new ShiftException(ShiftError.WORKER_INACTIVE, workerId);
        }
        return worker;
    }

    /** The worker has a working code that day (not DESCANSO, not absent). */
    private static DayLineup.Line requireWorking(DayContext day, UUID workerId, LocalDate date) {
        ScheduleWorkerInfo worker = requireWorker(day, workerId);
        return day.lineup().line(workerId)
                .filter(DayLineup.Line::working)
                .orElseThrow(() -> new ShiftException(ShiftError.INVALID_NOVELTY, worker.fullName() + " no trabaja el " + date));
    }

    /** A worker takes part in at most one active novelty per day. */
    private static void requireWithoutNovelty(DayContext day, UUID workerId) {
        if (day.lineup().hasNovelty(workerId)) {
            throw new ShiftException(ShiftError.NOVELTY_CONFLICT, requireWorker(day, workerId).fullName());
        }
    }

    /** The code exists, is active and is a working code (not DESCANSO). */
    private ShiftCode requireWorkingCode(String code) {
        ShiftCode shiftCode = shiftCodeRepository.findByCode(code)
                .filter(ShiftCode::isActive)
                .orElseThrow(() -> new ShiftException(ShiftError.SHIFT_CODE_NOT_FOUND, code));
        if (shiftCode.getRole() == null) {
            throw new ShiftException(ShiftError.INVALID_NOVELTY, "El código " + code + " no es un turno de trabajo");
        }
        return shiftCode;
    }

    private ShiftCode restCode() {
        return shiftCodeRepository.findByCode(DayLineup.REST_CODE)
                .orElseThrow(() -> new ShiftException(ShiftError.SHIFT_CODE_NOT_FOUND, DayLineup.REST_CODE));
    }

    /**
     * A code of a role different from the worker's role needs the explicit mark.
     *
     * @return true when the code belongs to another role (the coverage is exceptional)
     */
    private static boolean requireRoleOrExceptional(ScheduleWorkerInfo worker, ShiftCode code, boolean exceptional) {
        boolean otherRole = !code.getRole().getId().equals(worker.roleId());
        if (otherRole && !exceptional) {
            throw new ShiftException(ShiftError.EXCEPTIONAL_COVERAGE_REQUIRED, worker.fullName(), code.getCode(), code.getRole().getName());
        }
        return otherRole;
    }

    private DayStaffStep step(LocalDate date) {
        DayContext day = load(date, Set.of());
        Map<UUID, ScheduleWorkerInfo> active = day.activeWorkers();
        boolean noSchedule = day.grid().stream().noneMatch(row -> active.containsKey(row.getWorkerId()));
        List<DayLineup.Line> working = day.lineup().working().stream()
                .filter(line -> active.containsKey(line.workerId()))
                .toList();
        List<DayStaffStep.RestingWorker> resting = day.lineup().lines().stream()
                .filter(line -> !line.working() && active.containsKey(line.workerId()))
                .map(line -> new DayStaffStep.RestingWorker(line.workerId(), active.get(line.workerId()).fullName()))
                .sorted(Comparator.comparing(DayStaffStep.RestingWorker::fullName))
                .toList();
        return new DayStaffStep(date, dayJournalPort.step1Status(date), dayJournalPort.isEditable(date),
                DayStaffAssembler.members(day.lineup(), active), resting,
                DayStaffAssembler.absentees(day.lineup(), active), day.novelties(),
                StaffingAdvisor.warnings(noSchedule, working, active));
    }

    /**
     * Grid, active novelties and workers of one day: two queries plus one batch read of the workers.
     *
     * @param extraWorkers workers of a novelty being registered, so they are read in the same batch
     */
    private DayContext load(LocalDate date, Set<UUID> extraWorkers) {
        List<ShiftAssignment> grid = shiftAssignmentRepository.findByWorkDateBetween(date, date);
        List<ShiftNovelty> novelties = shiftNoveltyRepository.findByWorkDateBetween(date, date);
        Set<UUID> ids = new HashSet<>(extraWorkers);
        grid.forEach(row -> ids.add(row.getWorkerId()));
        novelties.forEach(novelty -> {
            ids.add(novelty.getWorkerId());
            if (novelty.getCounterpartWorkerId() != null) {
                ids.add(novelty.getCounterpartWorkerId());
            }
        });
        return new DayContext(grid, novelties, DayLineup.of(grid, novelties), scheduleWorkerDirectory.getWorkersByIds(ids));
    }

    /** Everything a use case needs about one day. {@code workers} holds active and inactive workers. */
    private record DayContext(List<ShiftAssignment> grid, List<ShiftNovelty> novelties, DayLineup lineup,
                              Map<UUID, ScheduleWorkerInfo> workers) {

        Map<UUID, ScheduleWorkerInfo> activeWorkers() {
            return workers.values().stream()
                    .filter(ScheduleWorkerInfo::active)
                    .collect(Collectors.toMap(ScheduleWorkerInfo::id, Function.identity()));
        }
    }
}
