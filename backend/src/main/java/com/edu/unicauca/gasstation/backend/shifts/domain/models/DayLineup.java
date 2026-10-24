package com.edu.unicauca.gasstation.backend.shifts.domain.models;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Who works one day: the grid of the day with its active novelties applied, in the order they were registered.
 * Pure calculation, never stored; shared by the step 1 use cases and by {@code DayStaffQuery}.
 *
 * <p>A worker takes part in at most one novelty per day (RN-D4). The registration rejects a second one; if one
 * reaches this class anyway, the first one by registration order is kept and the later one is ignored.
 */
public final class DayLineup {

    /** Code of the day off in the catalog; used as the grid code of a worker without a row. */
    public static final String REST_CODE = "DESCANSO";

    private final Map<UUID, Line> lines = new LinkedHashMap<>();
    private final List<Absence> absences = new ArrayList<>();
    private final List<ShiftNovelty> applied = new ArrayList<>();

    private DayLineup() {
    }

    /**
     * @param grid      assignments of the day (DESCANSO included)
     * @param novelties active novelties of the day, in registration order; an empty list gives the grid as it is
     */
    public static DayLineup of(List<ShiftAssignment> grid, List<ShiftNovelty> novelties) {
        DayLineup lineup = new DayLineup();
        grid.forEach(row -> lineup.lines.put(row.getWorkerId(),
                new Line(row.getWorkerId(), row.getShiftCode(), row.getShiftCode(), null, null)));
        Set<UUID> involved = new HashSet<>();
        for (ShiftNovelty novelty : novelties) {
            UUID counterpart = novelty.getCounterpartWorkerId();
            if (involved.contains(novelty.getWorkerId()) || (counterpart != null && involved.contains(counterpart))) {
                continue;
            }
            lineup.apply(novelty);
            involved.add(novelty.getWorkerId());
            if (counterpart != null) {
                involved.add(counterpart);
            }
        }
        return lineup;
    }

    private void apply(ShiftNovelty novelty) {
        UUID worker = novelty.getWorkerId();
        UUID counterpart = novelty.getCounterpartWorkerId();
        switch (novelty.getType()) {
            case REPLACEMENT -> {
                absent(novelty);
                lines.put(counterpart, new Line(counterpart, gridCodeOf(counterpart), novelty.getFinalShiftCode(),
                        novelty, worker));
            }
            case ABSENCE -> absent(novelty);
            case SHIFT_CHANGE -> lines.put(worker,
                    new Line(worker, gridCodeOf(worker), novelty.getFinalShiftCode(), novelty, null));
            case SWAP -> {
                lines.put(worker, new Line(worker, gridCodeOf(worker), novelty.getFinalShiftCode(), novelty, counterpart));
                lines.put(counterpart, new Line(counterpart, gridCodeOf(counterpart), novelty.getOriginalShiftCode(),
                        novelty, worker));
            }
            default -> throw new IllegalStateException("Unexpected novelty type " + novelty.getType());
        }
        applied.add(novelty);
    }

    private void absent(ShiftNovelty novelty) {
        lines.remove(novelty.getWorkerId());
        absences.add(new Absence(novelty.getWorkerId(), novelty.getOriginalShiftCode(), novelty));
    }

    private ShiftCode gridCodeOf(UUID worker) {
        Line line = lines.get(worker);
        return line == null ? null : line.gridCode();
    }

    /** Every worker of the day that is not absent: working ones and the ones on DESCANSO. */
    public List<Line> lines() {
        return List.copyOf(lines.values());
    }

    /** Workers with a working code (not DESCANSO). */
    public List<Line> working() {
        return lines.values().stream().filter(Line::working).toList();
    }

    public List<Absence> absences() {
        return List.copyOf(absences);
    }

    /** Novelties that changed the day; a novelty ignored because of RN-D4 is not here. */
    public List<ShiftNovelty> applied() {
        return List.copyOf(applied);
    }

    public Optional<Line> line(UUID worker) {
        return Optional.ofNullable(lines.get(worker));
    }

    /** True when the worker already takes part in an applied novelty of the day. */
    public boolean hasNovelty(UUID worker) {
        return applied.stream().anyMatch(novelty -> novelty.involves(worker));
    }

    /**
     * One worker of the day.
     *
     * @param workerId      worker identifier
     * @param gridCode      code in the grid; null when the worker has no row that day
     * @param workedCode    code actually worked; DESCANSO when resting
     * @param novelty       novelty that changed the worker's day; null when none
     * @param otherWorkerId the replaced worker (REPLACEMENT) or the other worker (SWAP); null otherwise
     */
    public record Line(UUID workerId, ShiftCode gridCode, ShiftCode workedCode, ShiftNovelty novelty,
                       UUID otherWorkerId) {

        /** True when the worked code has a role, that is, it is not DESCANSO. */
        public boolean working() {
            return workedCode != null && workedCode.getRole() != null;
        }

        /** Grid code as text; DESCANSO when the worker had no row. */
        public String gridCodeName() {
            return gridCode == null ? REST_CODE : gridCode.getCode();
        }
    }

    /**
     * A worker of the grid who does not work because of a novelty (ABSENCE or REPLACEMENT).
     *
     * @param workerId identifier of the absent worker
     * @param gridCode code the worker had in the grid
     * @param novelty  novelty that took the worker out of the day
     */
    public record Absence(UUID workerId, ShiftCode gridCode, ShiftNovelty novelty) {
    }
}
