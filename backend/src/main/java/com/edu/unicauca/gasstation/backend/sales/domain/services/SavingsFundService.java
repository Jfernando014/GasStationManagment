package com.edu.unicauca.gasstation.backend.sales.domain.services;

import com.edu.unicauca.gasstation.backend.sales.domain.models.SavingsFundDaySummary;
import com.edu.unicauca.gasstation.backend.sales.domain.models.SavingsFundMovement;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface SavingsFundService {

    SavingsFundMovement registerContribution(LocalDate date, BigDecimal amount, String description);

    SavingsFundMovement registerPayment(LocalDate date, BigDecimal amount, String description);

    BigDecimal getBalanceAt(LocalDate date);

    /** Previous balance, contribution and payments of one day, to rebuild the fund of that day. */
    SavingsFundDaySummary summarize(LocalDate date);

    List<SavingsFundMovement> listMovements();
}
