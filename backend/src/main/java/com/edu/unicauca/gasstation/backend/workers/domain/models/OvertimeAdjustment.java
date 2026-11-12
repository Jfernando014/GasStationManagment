package com.edu.unicauca.gasstation.backend.workers.domain.models;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Hours that an administrator adds to or subtracts from one category of the sheet of a worker in a fortnight.
 * Nothing carries over to the next fortnight.
 *
 * @param fortnightStart first day of the fortnight it belongs to
 */
public record OvertimeAdjustment(Long id, UUID workerId, LocalDate fortnightStart, OvertimeCategory category,
                                 AdjustmentDirection direction, int hours, String reason, Instant createdAt,
                                 String createdBy) {

    /** Hours with their sign: positive when it adds, negative when it subtracts. */
    public int signedHours() {
        return direction.sign() * hours;
    }
}
