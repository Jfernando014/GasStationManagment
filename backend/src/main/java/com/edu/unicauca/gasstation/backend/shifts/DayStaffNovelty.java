package com.edu.unicauca.gasstation.backend.shifts;

import java.util.UUID;

/**
 * Novelty applied to a member of the day.
 *
 * @param type               NONE when there is no novelty
 * @param replacedWorkerId   only REPLACEMENT and SWAP; else null
 * @param replacedWorkerName only REPLACEMENT and SWAP; else null
 * @param reason             null when type is NONE
 */
public record DayStaffNovelty(
        NoveltyType type,
        UUID replacedWorkerId,
        String replacedWorkerName,
        String reason) {

    /** A member without novelty. */
    public static DayStaffNovelty none() {
        return new DayStaffNovelty(NoveltyType.NONE, null, null, null);
    }
}
