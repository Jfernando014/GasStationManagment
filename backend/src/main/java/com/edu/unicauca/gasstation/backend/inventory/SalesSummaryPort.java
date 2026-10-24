package com.edu.unicauca.gasstation.backend.inventory;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Contract owned by Inventory and implemented by the Sales module.
 * It provides what was sold on a journal (step 2) in Inventory's own terms.
 */
public interface SalesSummaryPort {

    /**
     * @return the sales of the journal date, or empty if step 2 has no data for it
     */
    Optional<SalesSummary> findByJournalDate(LocalDate journalDate);
}