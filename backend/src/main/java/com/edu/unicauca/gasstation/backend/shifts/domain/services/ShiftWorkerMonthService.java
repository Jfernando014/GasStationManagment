package com.edu.unicauca.gasstation.backend.shifts.domain.services;

import com.edu.unicauca.gasstation.backend.shifts.daystaff.DayStaff.NoveltyType;
import com.edu.unicauca.gasstation.backend.shifts.domain.models.DayLineup;
import com.edu.unicauca.gasstation.backend.shifts.domain.models.ShiftAssignment;
import com.edu.unicauca.gasstation.backend.shifts.domain.models.ShiftCode;
import com.edu.unicauca.gasstation.backend.shifts.domain.models.ShiftNovelty;
import com.edu.unicauca.gasstation.backend.shifts.domain.models.ShiftSegment;
import com.edu.unicauca.gasstation.backend.shifts.domain.models.WorkerMonth;
import com.edu.unicauca.gasstation.backend.shifts.domain.models.WorkerMonth.CodeCount;
import com.edu.unicauca.gasstation.backend.shifts.domain.models.WorkerMonth.Kind;
import com.edu.unicauca.gasstation.backend.shifts.domain.repositories.ShiftAssignmentRepository;
import com.edu.unicauca.gasstation.backend.shifts.domain.repositories.ShiftNoveltyRepository;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read-only use case behind the profile of a worker: the schedule of one month with the novelties of the day staff
 * seen from that worker. It does not check that the worker exists ({@code shifts} does not depend on
 * {@code workers}): a worker without schedule gets every day empty and the totals in zero.
 *
 * <p>Two reads whatever the month: the assignments and the active novelties of its range. Cancelled novelties are
 * never returned by the repository, so they do not count.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ShiftWorkerMonthService {

    private final ShiftAssignmentRepository shiftAssignmentRepository;
    private final ShiftNoveltyRepository shiftNoveltyRepository;

    /**
     * @param workerId worker to describe
     * @param month    month to describe
     */
    public WorkerMonth getWorkerMonth(UUID workerId, YearMonth month) {
        LocalDate from = month.atDay(1);
        LocalDate to = month.atEndOfMonth();

        Map<LocalDate, ShiftAssignment> grid = shiftAssignmentRepository.findByWorkDateBetween(from, to).stream()
                .filter(assignment -> assignment.getWorkerId().equals(workerId))
                .collect(Collectors.toMap(ShiftAssignment::getWorkDate, Function.identity()));
        List<ShiftNovelty> novelties = shiftNoveltyRepository.findByWorkDateBetween(from, to).stream()
                .filter(novelty -> novelty.involves(workerId))
                .toList();
        // A worker takes part in one novelty per day; if two reach here the first registered wins, as in DayLineup.
        Map<LocalDate, ShiftNovelty> noveltyByDate = novelties.stream()
                .collect(Collectors.toMap(ShiftNovelty::getWorkDate, Function.identity(), (first, later) -> first));

        List<WorkerMonth.Day> days = new ArrayList<>();
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            days.add(day(workerId, date, grid.get(date), noveltyByDate.get(date)));
        }
        return new WorkerMonth(workerId, month, List.copyOf(days), summary(workerId, grid, novelties));
    }

    private static WorkerMonth.Day day(UUID workerId, LocalDate date, ShiftAssignment row, ShiftNovelty novelty) {
        String gridCode = row == null ? null : row.getShiftCode().getCode();
        int hours = row == null ? 0 : row.getShiftCode().totalHours();
        String note = row == null ? null : row.getNote();
        if (novelty == null) {
            return new WorkerMonth.Day(date, gridCode, gridCode, hours, note, List.of());
        }
        WorkerMonth.Novelty seen = seenFrom(workerId, novelty, gridCode);
        return new WorkerMonth.Day(date, gridCode, seen.finalCode(), hours, note, List.of(seen));
    }

    /** The same novelty reads differently for the affected worker and for the counterpart. */
    private static WorkerMonth.Novelty seenFrom(UUID workerId, ShiftNovelty novelty, String gridCode) {
        boolean affected = novelty.getWorkerId().equals(workerId);
        String ownGrid = gridCode == null ? DayLineup.REST_CODE : gridCode;
        String original = novelty.getOriginalShiftCode().getCode();
        String finalCode = novelty.getFinalShiftCode() == null ? null : novelty.getFinalShiftCode().getCode();
        Kind kind;
        String from;
        String to;
        switch (novelty.getType()) {
            case ABSENCE -> {
                kind = Kind.ABSENT;
                from = original;
                to = null;
            }
            case REPLACEMENT -> {
                kind = affected ? Kind.ABSENT : Kind.COVERING;
                from = affected ? original : ownGrid;
                to = affected ? null : finalCode;
            }
            case SHIFT_CHANGE -> {
                kind = Kind.SHIFT_CHANGE;
                from = original;
                to = finalCode;
            }
            case SWAP -> {
                kind = Kind.SWAP;
                from = ownGrid;
                to = affected ? finalCode : original;
            }
            default -> throw new IllegalStateException("Unexpected novelty type " + novelty.getType());
        }
        return new WorkerMonth.Novelty(kind, from, to, novelty.getReason(), novelty.isExceptionalCoverage());
    }

    private static WorkerMonth.Summary summary(UUID workerId, Map<LocalDate, ShiftAssignment> grid,
                                               List<ShiftNovelty> novelties) {
        int hours = 0;
        int shifts = 0;
        int rest = 0;
        Map<String, Integer> counts = new LinkedHashMap<>();
        Map<String, ShiftCode> codes = new LinkedHashMap<>();
        for (ShiftAssignment row : grid.values()) {
            ShiftCode code = row.getShiftCode();
            if (DayLineup.REST_CODE.equals(code.getCode())) {
                rest++;
                continue;
            }
            shifts++;
            hours += code.totalHours();
            counts.merge(code.getCode(), 1, Integer::sum);
            codes.putIfAbsent(code.getCode(), code);
        }
        List<CodeCount> byCode = counts.entrySet().stream()
                .sorted(Comparator
                        .comparingInt((Map.Entry<String, Integer> entry) -> firstStartHour(codes.get(entry.getKey())))
                        .thenComparing(Map.Entry::getKey))
                .map(entry -> new CodeCount(entry.getKey(), entry.getValue()))
                .toList();

        int replacements = (int) novelties.stream()
                .filter(novelty -> novelty.getType() == NoveltyType.REPLACEMENT
                        && workerId.equals(novelty.getCounterpartWorkerId()))
                .count();
        int absences = (int) novelties.stream()
                .filter(novelty -> (novelty.getType() == NoveltyType.ABSENCE
                        || novelty.getType() == NoveltyType.REPLACEMENT) && novelty.getWorkerId().equals(workerId))
                .count();
        return new WorkerMonth.Summary(hours, shifts, rest, byCode, replacements, absences);
    }

    /** Working codes in the order of the day: by the hour the first segment starts, then by name. */
    private static int firstStartHour(ShiftCode code) {
        return code.segments().stream().mapToInt(ShiftSegment::startHour).findFirst().orElse(Integer.MAX_VALUE);
    }
}
