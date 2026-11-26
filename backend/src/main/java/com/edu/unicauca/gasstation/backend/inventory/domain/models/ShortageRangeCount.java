package com.edu.unicauca.gasstation.backend.inventory.domain.models;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** Days of the month with a shortage in the range, against the allowed ones. */
@Getter
@AllArgsConstructor
public class ShortageRangeCount {
    private final GallonRange range;
    private final int count;
    private final int limit;
    /** Null when the month has no calculated day. */
    private final AlertLevel status;
}
