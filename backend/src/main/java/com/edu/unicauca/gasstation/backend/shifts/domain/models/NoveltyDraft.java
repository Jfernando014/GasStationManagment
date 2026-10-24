package com.edu.unicauca.gasstation.backend.shifts.domain.models;

import com.edu.unicauca.gasstation.backend.shifts.NoveltyType;
import java.util.UUID;

/**
 * Novelty asked for by the administrator, before it is validated and saved.
 *
 * @param type                REPLACEMENT, ABSENCE, SHIFT_CHANGE or SWAP
 * @param workerId            absent worker (REPLACEMENT, ABSENCE), worker whose code changes (SHIFT_CHANGE) or first
 *                            worker of the swap
 * @param counterpartWorkerId replacement (REPLACEMENT) or second worker of the swap (SWAP); null otherwise
 * @param shiftCode           code to work: required in SHIFT_CHANGE, optional in REPLACEMENT (the code of the absent
 *                            worker when null), ignored in ABSENCE and SWAP
 * @param exceptionalCoverage accepts a code of a role different from the worker's role (VAL-D5)
 * @param reason              3 to 255 characters without leading or trailing spaces (VAL-D6)
 */
public record NoveltyDraft(NoveltyType type, UUID workerId, UUID counterpartWorkerId, String shiftCode,
                           boolean exceptionalCoverage, String reason) {
}
