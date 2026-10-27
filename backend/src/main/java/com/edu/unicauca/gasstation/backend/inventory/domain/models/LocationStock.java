package com.edu.unicauca.gasstation.backend.inventory.domain.models;

import lombok.Builder;
import lombok.Getter;

/** Units of a product in one location on a journal date. */
@Getter
@Builder
public class LocationStock {

    private final int opening;
    private final int initial;
    private final int entries;
    /** Net units transferred: negative in the office, positive in the counter. */
    private final int transfers;
    private final int adjustments;
    private final int sold;
    private final int closing;

    public boolean hasActivity() {
        return opening != 0 || initial != 0 || entries != 0 || transfers != 0
                || adjustments != 0 || sold != 0 || closing != 0;
    }
}