package com.edu.unicauca.gasstation.backend.sales.domain.repositories;

import com.edu.unicauca.gasstation.backend.sales.domain.models.VendorShiftClose;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface VendorShiftCloseRepository {

    VendorShiftClose save(VendorShiftClose close);

    List<VendorShiftClose> saveAll(List<VendorShiftClose> closes);

    List<VendorShiftClose> findByJournalDate(LocalDate journalDate);

    /** Closes of the journal dates between both (inclusive). The JPA persistence adapter reads them at once. */
    default List<VendorShiftClose> findByJournalDateBetween(LocalDate from, LocalDate to) {
        return from.datesUntil(to.plusDays(1)).flatMap(date -> findByJournalDate(date).stream()).toList();
    }

    Optional<VendorShiftClose> findByJournalDateAndVendorDocument(LocalDate journalDate, String vendorDocument);

    boolean existsByJournalDate(LocalDate journalDate);

    default void deleteByJournalDate(LocalDate journalDate) {
        // Optional for read-only test doubles; implemented by the JPA persistence adapter.
    }
}
