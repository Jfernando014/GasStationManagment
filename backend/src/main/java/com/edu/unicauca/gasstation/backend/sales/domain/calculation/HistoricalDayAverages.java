package com.edu.unicauca.gasstation.backend.sales.domain.calculation;

import com.edu.unicauca.gasstation.backend.sales.domain.repositories.SalesHistoryReadPort;
import com.edu.unicauca.gasstation.backend.sales.domain.models.SalesTargetCategory;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

/** ESCENARIOS E2:E8 excludes zero station-day totals. No fabricated fallback for missing weekdays. */
public final class HistoricalDayAverages {
    private final SalesHistoryReadPort history;
    public HistoricalDayAverages(SalesHistoryReadPort history) { this.history=Objects.requireNonNull(history); }
    public Map<DayOfWeek, Optional<ExactFraction>> read(LocalDate from, LocalDate to, SalesTargetCategory category) {
        Objects.requireNonNull(from);Objects.requireNonNull(to);Objects.requireNonNull(category);
        if (to.isBefore(from)) throw new IllegalArgumentException("Invalid history range");
        var totals=new EnumMap<DayOfWeek,BigDecimal>(DayOfWeek.class);
        var counts=new EnumMap<DayOfWeek,Integer>(DayOfWeek.class);
        var seen=new HashSet<LocalDate>();
        for(var sale:history.findDailySales(from,to,category)) {
            Objects.requireNonNull(sale);Objects.requireNonNull(sale.date());Objects.requireNonNull(sale.value());
            if(sale.date().isBefore(from)||sale.date().isAfter(to)||!seen.add(sale.date()))
                throw new IllegalArgumentException("History source returned duplicate or out-of-range daily totals");
            // Adjustments/refunds have unconfirmed semantics and must not silently become baseline weights.
            if(sale.value().signum()<0) throw new IllegalArgumentException("Negative daily totals require an approved adjustment policy");
            if(sale.value().signum()==0) continue;
            totals.merge(sale.date().getDayOfWeek(),sale.value(),BigDecimal::add);
            counts.merge(sale.date().getDayOfWeek(),1,Integer::sum);
        }
        var result=new EnumMap<DayOfWeek,Optional<ExactFraction>>(DayOfWeek.class);
        for(var day:DayOfWeek.values()) result.put(day,counts.containsKey(day)
                ? Optional.of(ExactFraction.of(totals.get(day)).divide(ExactFraction.of(BigDecimal.valueOf(counts.get(day))))) : Optional.empty());
        return Collections.unmodifiableMap(result);
    }
}
