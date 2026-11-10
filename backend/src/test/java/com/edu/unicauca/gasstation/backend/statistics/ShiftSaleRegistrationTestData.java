package com.edu.unicauca.gasstation.backend.statistics;

import com.edu.unicauca.gasstation.backend.sales.SellerCloseQuery.VendorCloseSummary;
import com.edu.unicauca.gasstation.backend.shifts.catalog.RoleInfo;
import com.edu.unicauca.gasstation.backend.shifts.catalog.ShiftCodeInfo;
import com.edu.unicauca.gasstation.backend.shifts.daystaff.DayStaff;
import com.edu.unicauca.gasstation.backend.statistics.domain.models.ClosedJournalSnapshot;
import com.edu.unicauca.gasstation.backend.statistics.domain.models.JournalSalesProcessing;
import com.edu.unicauca.gasstation.backend.statistics.domain.models.ShiftSaleAmounts;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Public source contracts only; deliberately distinct monetary observations and gallon values. */
public final class ShiftSaleRegistrationTestData {
    public static final LocalDate DATE = LocalDate.of(2026, 11, 10);
    public static final UUID WORKER_A = UUID.fromString("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa");
    public static final UUID WORKER_B = UUID.fromString("bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb");
    public static final ClosedJournalSnapshot JOURNAL =
            new ClosedJournalSnapshot(15L, DATE, 105L, DATE.plusDays(1).atTime(6, 15, 30, 999999999));
    public static final ShiftSaleAmounts AMOUNTS = new ShiftSaleAmounts(
            new BigDecimal("26920.123456789012345678900"), new BigDecimal("1.000"),
            new BigDecimal("-0.000000000000000000001"), new BigDecimal("42.120000"),
            new BigDecimal("8400000.123456789012345678900"), new BigDecimal("9000000.0000"),
            new BigDecimal("8999999.990000"));
    private static final Instant NOW = Instant.parse("2026-11-11T11:15:30Z");

    private ShiftSaleRegistrationTestData() {
    }

    public static VendorCloseSummary close(UUID worker, String code) {
        return close(worker.toString(), code);
    }

    public static VendorCloseSummary close(String document, String code) {
        return close(DATE, document, code, "Recorded seller", 1, AMOUNTS);
    }

    public static VendorCloseSummary close(LocalDate date, String document, String code, String name,
            Integer pump, ShiftSaleAmounts amounts) {
        return new VendorCloseSummary(date, document, name, code, pump,
                BigDecimal.ONE, BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                amounts.totalDeliveredCop(), amounts.motorGallons(), amounts.dieselGallons(), amounts.extraGallons(),
                amounts.maxproGallons(), amounts.basketDominusAmountCop(), amounts.dominusTirillaCop(),
                new BigDecimal("-13.00"), "FALTA", List.of());
    }

    public static DayStaff.Member member(UUID worker, String code, String role) {
        return member(worker, code, code, role, DayStaff.Novelty.none());
    }

    public static DayStaff.Member member(UUID worker, String workedCode, String gridCode, String role,
            DayStaff.Novelty novelty) {
        int dispenser = "APOYO".equals(role) ? 3 : 1;
        return DayStaff.Member.of(worker, "Different staff name", workedCode, gridCode,
                List.of(new ShiftCodeInfo.Segment(6, 18)), 12,
                new RoleInfo(UUID.fromString("00000000-0000-0000-0000-000000000001"), role, dispenser), novelty);
    }

    public static DayStaff day(DayStaff.Member... members) {
        return new DayStaff(DATE, List.of(members), List.of(), DayStaff.Step1Status.CONFIRMED, true, List.of());
    }

    public static JournalSalesProcessing processed(int count) {
        return new JournalSalesProcessing(JOURNAL, JournalSalesProcessing.Status.PROCESSED,
                1, count, null, NOW, NOW);
    }

    public static JournalSalesProcessing pending() {
        return new JournalSalesProcessing(JOURNAL, JournalSalesProcessing.Status.PENDING,
                1, null, "retry needed", null, NOW);
    }
}
