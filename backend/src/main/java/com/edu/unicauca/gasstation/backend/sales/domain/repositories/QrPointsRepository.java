package com.edu.unicauca.gasstation.backend.sales.domain.repositories;

import com.edu.unicauca.gasstation.backend.sales.domain.reconciliation.AssignedTransaction;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface QrPointsRepository {

    /** Replaces every Redeban row stored for the date. */
    void replaceRedeban(LocalDate date, List<AssignedTransaction> transactions);

    List<AssignedTransaction> findRedeban(LocalDate date);

    /** Sum of Redeban rows per day in the inclusive range; days without transactions are absent. */
    Map<LocalDate, BigDecimal> findRedebanTotals(LocalDate from, LocalDate to);

    Optional<PointsDay> findPoints(LocalDate date);

    /** Puntos Colombia totals per day in the inclusive range; days without a stored total are absent. */
    Map<LocalDate, BigDecimal> findPointsTotals(LocalDate from, LocalDate to);

    void savePoints(LocalDate date, BigDecimal total, String source);

    record PointsDay(LocalDate date, BigDecimal total, String source) {
    }
}
