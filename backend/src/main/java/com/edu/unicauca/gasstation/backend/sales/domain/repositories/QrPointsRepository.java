package com.edu.unicauca.gasstation.backend.sales.domain.repositories;

import com.edu.unicauca.gasstation.backend.sales.domain.reconciliation.AssignedTransaction;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface QrPointsRepository {

    /** Replaces every Redeban row stored for the date. */
    void replaceRedeban(LocalDate date, List<AssignedTransaction> transactions);

    List<AssignedTransaction> findRedeban(LocalDate date);

    Optional<PointsDay> findPoints(LocalDate date);

    void savePoints(LocalDate date, BigDecimal total, String source);

    record PointsDay(LocalDate date, BigDecimal total, String source) {
    }
}
