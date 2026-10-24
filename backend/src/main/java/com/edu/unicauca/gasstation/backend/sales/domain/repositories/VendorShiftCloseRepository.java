package com.edu.unicauca.gasstation.backend.sales.domain.repositories;

import com.edu.unicauca.gasstation.backend.sales.domain.models.VendorShiftClose;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface VendorShiftCloseRepository {

    VendorShiftClose save(VendorShiftClose close);

    List<VendorShiftClose> saveAll(List<VendorShiftClose> closes);

    List<VendorShiftClose> findByJournalDate(LocalDate journalDate);

    Optional<VendorShiftClose> findByJournalDateAndVendorDocument(LocalDate journalDate, String vendorDocument);

    boolean existsByJournalDate(LocalDate journalDate);
}
