package com.edu.unicauca.gasstation.backend.statistics;

import com.edu.unicauca.gasstation.backend.statistics.domain.calculation.SellerPerformance.*;
import com.edu.unicauca.gasstation.backend.statistics.domain.models.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Explicitly confirmed test dates and parameter provenance, never production defaults. */
public final class SellerPerformanceTestData {
    public static final LocalDate DATE = LocalDate.of(2026, 8, 3);
    public static final UUID A = UUID.fromString("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa");
    public static final UUID B = UUID.fromString("bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb");
    public static final Window SINGLE_DAY = new Window(DATE, DATE);
    private static final Instant NOW = Instant.parse("2026-08-04T12:00:00Z");

    private SellerPerformanceTestData() { }

    public static ClosedJournalSnapshot journal(LocalDate date) {
        return new ClosedJournalSnapshot(date.toEpochDay(), date, date.toEpochDay(),
                date.plusDays(1).atTime(6, 0, 0, 999999999));
    }

    public static ShiftSaleHistory sale(LocalDate date, UUID worker, String code,
                                       ShiftSaleHistory.EffectiveRole role, String gallons, String basket) {
        return new ShiftSaleHistory(journal(date), new ShiftSaleIdentity(date, worker.toString(), code),
                worker, "Recorded " + worker, code, role,
                new ShiftSaleAmounts(new BigDecimal(gallons), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                        new BigDecimal(basket), new BigDecimal("999999.12345"), new BigDecimal("888888.54321")));
    }

    public static ShiftSaleHistory sale(UUID worker, String gallons, String basket) {
        return sale(DATE, worker, "DIA", ShiftSaleHistory.EffectiveRole.TITULAR, gallons, basket);
    }

    public static JournalSalesProcessing processed(LocalDate date, int count) {
        return new JournalSalesProcessing(journal(date), JournalSalesProcessing.Status.PROCESSED,
                1, count, null, NOW, NOW);
    }

    public static JournalSalesProcessing pending(LocalDate date) {
        return new JournalSalesProcessing(journal(date), JournalSalesProcessing.Status.PENDING,
                1, null, "upstream unresolved", null, NOW);
    }

    public static ShiftSaleHistoryRange history(Window window, ShiftSaleHistory... sales) {
        return new ShiftSaleHistoryRange(window.from(), window.to(),
                window.from().datesUntil(window.to().plusDays(1)).map(date -> {
                    var rows = List.of(sales).stream().filter(sale -> sale.journal().journalDate().equals(date)).toList();
                    return new ShiftSaleHistoryRange.Day(date, processed(date, rows.size()), rows);
                }).toList());
    }

    public static IndividualTarget target(UUID worker, Window window, Measure measure, String amount) {
        return new IndividualTarget(worker, window, measure, new BigDecimal(amount), "explicit assigned target fixture");
    }

    public static List<ShiftClassification> classified(ShiftSaleHistoryRange history, ShiftPeriod period) {
        return history.sales().stream().map(sale -> new ShiftClassification(sale.identity(), period, DayType.REGULAR,
                "explicit historical classification fixture")).toList();
    }
}
