package com.edu.unicauca.gasstation.backend.inventory.domain.models;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** Days of the month with a surplus in the range. */
@Getter
@AllArgsConstructor
public class SurplusRangeCount {
    private final GallonRange range;
    private final int count;
}
