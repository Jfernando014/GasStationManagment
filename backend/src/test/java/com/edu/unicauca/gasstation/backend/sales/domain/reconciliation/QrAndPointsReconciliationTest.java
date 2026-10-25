package com.edu.unicauca.gasstation.backend.sales.domain.reconciliation;

import static com.edu.unicauca.gasstation.backend.sales.domain.reconciliation.ReconciliationTestData.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class QrAndPointsReconciliationTest {

    private static RedebanTransaction row(int line, LocalTime time, String amount) {
        return new RedebanTransaction(DAY, time, new BigDecimal(amount), null, "R" + line, line);
    }

    /** Real shifts of 05/08: Carlos DIA6 (6-12), Mónica 12-7 (12-19) and Wiltom NOCHE (18-6). */
    private static List<VendorShift> shifts() {
        return List.of(
                new VendorShift(CARLOS, VendorShift.hoursOf(List.of(new int[]{6, 12}))),
                new VendorShift(MONICA, VendorShift.hoursOf(List.of(new int[]{12, 19}))),
                new VendorShift(WILTOM, VendorShift.hoursOf(List.of(new int[]{18, 6}))));
    }

    /** Synthetic Redeban rows that add up to the 876.008 of the planillas of 05/08, one block per vendor. */
    private static List<RedebanTransaction> august5Rows() {
        return List.of(
                row(2, LocalTime.of(7, 30), "377902"),                  // Carlos
                row(3, LocalTime.of(14, 20), "162606"),                 // Mónica
                row(4, LocalTime.of(20, 10), "200000"),                 // Wiltom, evening
                // 02:30 of the 6th is still operative day 05/08: the night of day X belongs to day X
                new RedebanTransaction(DAY.plusDays(1), LocalTime.of(2, 30), new BigDecimal("135500"), null, "R5", 5));
    }

    @Test
    void operativeDayRunsFromSixToSix() {
        assertEquals(DAY, new RedebanTransaction(DAY.plusDays(1), LocalTime.of(2, 30), BigDecimal.ONE, null, null, 2)
                .operativeDate());
        assertEquals(DAY.minusDays(1), new RedebanTransaction(DAY, LocalTime.of(5, 59), BigDecimal.ONE, null, null, 2)
                .operativeDate());
        assertEquals(DAY, new RedebanTransaction(DAY, LocalTime.of(6, 0), BigDecimal.ONE, null, null, 2)
                .operativeDate());
        assertEquals(DAY, new RedebanTransaction(DAY, LocalTime.of(23, 59), BigDecimal.ONE, null, null, 2)
                .operativeDate());
        assertEquals(DAY, new RedebanTransaction(DAY, null, BigDecimal.ONE, null, null, 2).operativeDate());
        assertNull(new RedebanTransaction(null, LocalTime.of(2, 0), BigDecimal.ONE, null, null, 2).operativeDate());
    }

    @Test
    void shiftHoursCoverOvernightAndDaySegments() {
        assertEquals(12, VendorShift.hoursOf(List.of(new int[]{18, 6})).size());
        assertTrue(VendorShift.hoursOf(List.of(new int[]{18, 6})).containsAll(Set.of(18, 23, 0, 5)));
        assertFalse(VendorShift.hoursOf(List.of(new int[]{18, 6})).contains(6));
        assertEquals(12, VendorShift.hoursOf(List.of(new int[]{6, 18})).size());
        assertTrue(VendorShift.hoursOf(List.of(new int[]{14, 24})).contains(23));
        assertEquals(7, VendorShift.hoursOf(List.of(new int[]{6, 9}, new int[]{17, 21})).size());
    }

    @Test
    void assignsEachRowToTheVendorOnShiftAtThatHour() {
        List<AssignedTransaction> assigned = QrShiftAssigner.assign(august5Rows(), shifts());

        assertEquals(CARLOS, assigned.get(0).workerId());
        assertEquals(MONICA, assigned.get(1).workerId());
        assertEquals(WILTOM, assigned.get(2).workerId());
        assertEquals(WILTOM, assigned.get(3).workerId());   // 02:30 belongs to the night vendor
        assertTrue(assigned.stream().allMatch(t -> t.status() == AssignmentStatus.ASSIGNED));
        assertEquals(DAY, assigned.get(3).transaction().operativeDate());
    }

    @Test
    void neverGuessesWhenTwoVendorsOverlapOrThereIsNoTime() {
        // A titular on DIA (6-18) and a support on 12-7 both cover 14:00
        List<VendorShift> overlapping = List.of(
                new VendorShift(CARLOS, VendorShift.hoursOf(List.of(new int[]{6, 18}))),
                new VendorShift(MONICA, VendorShift.hoursOf(List.of(new int[]{12, 19}))));
        List<AssignedTransaction> assigned = QrShiftAssigner.assign(List.of(
                row(2, LocalTime.of(9, 0), "1000"),
                row(3, LocalTime.of(14, 0), "2000"),
                new RedebanTransaction(DAY, null, new BigDecimal("3000"), null, "R4", 4)), overlapping);

        assertEquals(AssignmentStatus.ASSIGNED, assigned.get(0).status());
        assertEquals(AssignmentStatus.AMBIGUOUS, assigned.get(1).status());
        assertNull(assigned.get(1).workerId());
        assertEquals(AssignmentStatus.NO_TIME, assigned.get(2).status());
    }

    @Test
    void hourWithNobodyOnShiftIsReported() {
        List<AssignedTransaction> assigned = QrShiftAssigner.assign(
                List.of(row(2, LocalTime.of(5, 59), "1000")),
                List.of(new VendorShift(CARLOS, VendorShift.hoursOf(List.of(new int[]{6, 18})))));

        assertEquals(AssignmentStatus.NO_SHIFT, assigned.get(0).status());
    }

    @Test
    void august5QrTotalIs876008AndEveryVendorMatches() {
        QrReconciliationResult result = QrReconciliationCalculator.calculate(DAY, august5(), NAMES,
                QrShiftAssigner.assign(august5Rows(), shifts()));

        assertTrue(result.redebanLoaded());
        assertEquals(money("876008"), result.redebanTotal());
        assertEquals(money("876008"), result.planillaTotal());
        assertEquals(money("876008"), result.qrTotal());
        assertTrue(result.matchesDay());
        assertEquals(money("0"), result.difference());
        assertTrue(result.perVendorReliable());
        assertEquals(money("0"), result.unassignedTotal());
        assertTrue(result.vendors().stream().allMatch(VendorQrCheck::matches));
        assertEquals(money("377902"), result.vendors().get(2).redeban());   // Carlos
        assertEquals(money("162606"), result.vendors().get(0).redeban());   // Mónica
        assertEquals(money("335500"), result.vendors().get(3).redeban());   // Wiltom: 200.000 + 135.500
    }

    @Test
    void perVendorComparisonIsMarkedUnreliableWhileRowsRemainUnassigned() {
        List<RedebanTransaction> rows = List.of(
                row(2, LocalTime.of(20, 10), "335500"),
                new RedebanTransaction(DAY, null, new BigDecimal("540508"), null, "R3", 3));

        QrReconciliationResult result = QrReconciliationCalculator.calculate(DAY, august5(), NAMES,
                QrShiftAssigner.assign(rows, shifts()));

        // The day still matches (876.008) but Carlos and Mónica cannot be checked one by one
        assertTrue(result.matchesDay());
        assertFalse(result.perVendorReliable());
        assertEquals(money("540508"), result.unassignedTotal());
        assertEquals(1L, result.byStatus().get(AssignmentStatus.NO_TIME));
        assertTrue(result.vendors().get(3).matches());                       // Wiltom
        assertFalse(result.vendors().get(2).matches());                      // Carlos
        assertEquals(money("-377902"), result.vendors().get(2).difference());
    }

    @Test
    void withoutARedebanFileTheQrIsTheSumOfThePlanillas() {
        QrReconciliationResult result = QrReconciliationCalculator.calculate(DAY, august5(), NAMES, List.of());

        assertFalse(result.redebanLoaded());
        assertEquals(money("876008"), result.qrTotal());
        assertFalse(result.matchesDay());
    }

    @Test
    void qrDifferenceBetweenRedebanAndPlanillasIsExact() {
        List<AssignedTransaction> short1000 = QrShiftAssigner.assign(
                List.of(row(2, LocalTime.of(20, 0), "875008")), shifts());

        QrReconciliationResult result = QrReconciliationCalculator.calculate(DAY, august5(), NAMES, short1000);

        assertFalse(result.matchesDay());
        assertEquals(money("-1000"), result.difference());
    }

    @Test
    void daysWithoutRedemptionsCompleteWithZeroWithoutTypingAnything() {
        List<VendorDeclaration> none = List.of(vendor(MONICA, "12-7", "0", "0", "0", "0", "0"));

        PointsResult result = PointsReconciliation.compare(DAY, none, NAMES, null, null);

        assertEquals(PointsStatus.COMPLETED_ZERO, result.status());
        assertEquals(money("0"), result.pointsTotal());
    }

    @Test
    void august5RedemptionsMatchThePuntosColombiaTotalOf41380() {
        PointsResult result = PointsReconciliation.compare(DAY, august5(), NAMES, money("41380"), "MANUAL");

        assertEquals(money("41380"), result.declared());
        assertEquals(PointsStatus.MATCH, result.status());
        assertEquals(money("0"), result.difference());
    }

    @Test
    void redemptionsWithoutALoadedTotalStayPending() {
        PointsResult result = PointsReconciliation.compare(DAY, august5(), NAMES, null, null);

        assertEquals(PointsStatus.PENDING_TOTAL, result.status());
        assertNull(result.difference());
    }

    @Test
    void differenceIsShownExactly() {
        PointsResult result = PointsReconciliation.compare(DAY, august5(), NAMES, money("40000"), "MANUAL");

        assertEquals(PointsStatus.DIFFERENCE, result.status());
        assertEquals(money("-1380"), result.difference());
    }

    @Test
    void pointsReportedWhileEveryPlanillaHasZeroIsADifference() {
        PointsResult result = PointsReconciliation.compare(DAY, List.of(vendor(MONICA, "12-7", "0", "0", "0", "0", "0")),
                NAMES, money("500"), "MANUAL");

        assertEquals(PointsStatus.DIFFERENCE, result.status());
        assertEquals(money("500"), result.difference());
    }

    @Test
    void unassignedRowsAreListedWithTheirReason() {
        List<VendorShift> overlapping = List.of(
                new VendorShift(CARLOS, VendorShift.hoursOf(List.of(new int[]{6, 18}))),
                new VendorShift(MONICA, VendorShift.hoursOf(List.of(new int[]{12, 19}))));
        List<AssignedTransaction> assigned = QrShiftAssigner.assign(List.of(
                row(2, LocalTime.of(9, 0), "1000"),
                row(3, LocalTime.of(14, 0), "2000"),
                new RedebanTransaction(DAY, null, new BigDecimal("3000"), "QR", "R4", 4)), overlapping);

        QrReconciliationResult result = QrReconciliationCalculator.calculate(DAY, august5(), NAMES, assigned);

        assertEquals(2, result.unassigned().size());
        assertEquals(3, result.unassigned().get(0).row());
        assertEquals(AssignmentStatus.AMBIGUOUS, result.unassigned().get(0).status());
        assertEquals(money("2000"), result.unassigned().get(0).amount());
        assertEquals(AssignmentStatus.NO_TIME, result.unassigned().get(1).status());
        assertEquals("R4", result.unassigned().get(1).reference());
    }
}
