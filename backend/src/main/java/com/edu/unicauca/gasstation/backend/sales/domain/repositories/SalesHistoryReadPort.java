package com.edu.unicauca.gasstation.backend.sales.domain.repositories;

import com.edu.unicauca.gasstation.backend.sales.domain.models.SalesTargetCategory;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Reserved for the owning sales history source. No implementation or fabricated production data. */
public interface SalesHistoryReadPort {
    List<DailySale> findDailySales(LocalDate fromInclusive, LocalDate toInclusive, SalesTargetCategory category);
    record DailySale(LocalDate date, BigDecimal value) { }
}
