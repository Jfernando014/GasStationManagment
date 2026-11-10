package com.edu.unicauca.gasstation.backend.sales.domain.services;

import com.edu.unicauca.gasstation.backend.sales.domain.models.SavingsFundDaySummary;
import com.edu.unicauca.gasstation.backend.sales.domain.models.SavingsFundMovement;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SavingsFundService {

    SavingsFundMovement registerContribution(LocalDate date, BigDecimal amount, String description);

    /**
     * Registers the contribution of the day unless the day already has one, for example one typed by hand. Unlike
     * {@link #registerContribution} it never fails for a duplicate, so it can be repeated safely.
     *
     * @return the new movement, or empty when the day already had its contribution
     */
    Optional<SavingsFundMovement> registerContributionIfAbsent(LocalDate date, BigDecimal amount, String description);

    SavingsFundMovement registerPayment(LocalDate date, BigDecimal amount, String description);

    BigDecimal getBalanceAt(LocalDate date);

    /** Previous balance, contribution and payments of one day, to rebuild the fund of that day. */
    SavingsFundDaySummary summarize(LocalDate date);

    List<SavingsFundMovement> listMovements();
}
