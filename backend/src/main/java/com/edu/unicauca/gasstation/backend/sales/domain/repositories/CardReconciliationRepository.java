package com.edu.unicauca.gasstation.backend.sales.domain.repositories;

import com.edu.unicauca.gasstation.backend.sales.domain.reconciliation.DayFranchiseTotals;
import com.edu.unicauca.gasstation.backend.sales.domain.reconciliation.PeriodTotals;
import com.edu.unicauca.gasstation.backend.sales.domain.reconciliation.VendorDeclaration;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface CardReconciliationRepository {

    /** Inserts or replaces the vendor's row of the day. */
    void upsert(LocalDate date, VendorDeclaration declaration);

    /** @return true when a row existed */
    boolean delete(LocalDate date, UUID workerId);

    List<VendorDeclaration> findByDate(LocalDate date);

    /** Daily totals between both dates (inclusive); the QR of each day is the sum of the planillas. */
    Map<LocalDate, DayFranchiseTotals> findDailyTotals(LocalDate from, LocalDate to);

    Optional<PeriodTotals> findPreviousMonthBaseline(YearMonth month);

    void saveBaseline(YearMonth month, PeriodTotals totals);
}
