package com.edu.unicauca.gasstation.backend.workers.domain.models;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Fixed copy of what the day staff said about one worker on an operational day. The hours are those of the code
 * worked, as the shifts module reports them. A worker who was in the grid but did not work (absence, or replaced by
 * someone else) has no worked code, no role, no segments and 0 hours, and keeps the novelty that explains it.
 *
 * @param noveltyType      NONE, REPLACEMENT, ABSENCE, SHIFT_CHANGE or SWAP
 * @param replacedWorkerId only when the worker covered someone else or swapped; else null
 * @param journalId        journal that closed the day; null when it is not known
 * @param noveltiesApplied false when the day staff was read before the novelties of the day could be applied
 */
public record WorkerDayHours(LocalDate workDate, UUID workerId, String workerName, String workedCode,
                             String gridCode, String dayRole, int totalHours, String noveltyType,
                             UUID replacedWorkerId, String replacedWorkerName, String reason, Long journalId,
                             boolean noveltiesApplied, List<DayHoursSegment> segments) {

    public WorkerDayHours {
        segments = List.copyOf(segments);
    }

    /** True when the worker was in the grid but did not work the day. */
    public boolean absent() {
        return workedCode == null;
    }

    public WorkerDayHours withJournalId(Long journal) {
        return new WorkerDayHours(workDate, workerId, workerName, workedCode, gridCode, dayRole, totalHours,
                noveltyType, replacedWorkerId, replacedWorkerName, reason, journal, noveltiesApplied, segments);
    }
}
