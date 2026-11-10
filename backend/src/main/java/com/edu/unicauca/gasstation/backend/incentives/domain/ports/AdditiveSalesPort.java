package com.edu.unicauca.gasstation.backend.incentives.domain.ports;

import com.edu.unicauca.gasstation.backend.incentives.domain.models.AdditiveSaleUnits;
import java.time.LocalDate;
import java.util.List;

/** Additive units sold per seller, read from the closes that sales holds. */
public interface AdditiveSalesPort {

    /**
     * One row per seller and additive SKU with units above zero, summed when sales reports the same pair more than
     * once. The list is empty when nobody sold additives or sales holds no seller close for the date.
     */
    List<AdditiveSaleUnits> findByDate(LocalDate journalDate);
}
