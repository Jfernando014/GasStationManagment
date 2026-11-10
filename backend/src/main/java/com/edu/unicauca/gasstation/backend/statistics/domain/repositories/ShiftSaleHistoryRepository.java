package com.edu.unicauca.gasstation.backend.statistics.domain.repositories;

import com.edu.unicauca.gasstation.backend.statistics.domain.models.ClosedJournalSnapshot;
import com.edu.unicauca.gasstation.backend.statistics.domain.models.JournalSalesProcessing;
import com.edu.unicauca.gasstation.backend.statistics.domain.models.ShiftSaleHistory;
import com.edu.unicauca.gasstation.backend.statistics.domain.models.ShiftSaleIdentity;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Atomic storage of historical snapshots. No source queries, events or mutable external entities. */
public interface ShiftSaleHistoryRepository {

    /**
     * Commits all closes and the day's PROCESSED marker in an independent transaction, even for an empty list.
     * Concurrent calls for the same date are serialized. A processed date is returned unchanged.
     * On failure, all sale writes roll back, the date is marked PENDING in a separate transaction, and the
     * original exception is rethrown. If storage is unavailable, recording PENDING can fail too.
     */
    JournalSalesProcessing saveJournal(ClosedJournalSnapshot journal, List<ShiftSaleHistory> sales);

    /** Records a failed upstream or storage attempt independently; never downgrades a processed date. */
    JournalSalesProcessing markPending(ClosedJournalSnapshot journal, String error);

    Optional<JournalSalesProcessing> findProcessing(LocalDate journalDate);

    List<JournalSalesProcessing> findPending();

    List<ShiftSaleHistory> findByDate(LocalDate journalDate);

    Optional<ShiftSaleHistory> findByIdentity(ShiftSaleIdentity identity);
}
