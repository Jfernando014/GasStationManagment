package com.edu.unicauca.gasstation.backend.incentives.domain.repositories;

import com.edu.unicauca.gasstation.backend.incentives.domain.models.AdditiveSaleUnits;
import java.time.LocalDate;
import java.util.List;

/** Storage of the additive units sold per seller on each journal date. */
public interface AdditiveSaleRepository {

    /** Deletes the rows of the date and stores these, in the caller's transaction. */
    void replaceDay(LocalDate journalDate, List<AdditiveSaleUnits> sales);

    /** Rows of the date ordered by seller name and SKU. */
    List<AdditiveSaleUnits> findByDate(LocalDate journalDate);
}
