package com.edu.unicauca.gasstation.backend.sales.domain.reconciliation;

import static com.edu.unicauca.gasstation.backend.sales.domain.reconciliation.ReconciliationTestData.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CredibancoReconciliationCalculatorTest {

    private static final BigDecimal RATE = new BigDecimal("0.0265");

    private static CredibancoDayResult dayOf(List<VendorDeclaration> declarations) {
        return CredibancoReconciliationCalculator.calculate(DAY, declarations, NAMES, money("876008"),
                DayFranchiseTotals.ZERO, august1to4(), previousMonth(), RATE);
    }

    @Test
    void august5TotalEdsIsMastercardPlusVisaWithoutQr() {
        CredibancoDayResult result = dayOf(august5());

        assertTrue(result.allMatch());
        assertEquals(money("3618384"), result.franchises().mastercard());
        assertEquals(money("2072928"), result.franchises().visa());
        assertEquals(money("5691312"), result.totalEds());
        assertEquals(money("876008"), result.franchises().qr());
    }

    @Test
    void everyVendorMatchesHisPlanillaAndTheRestingVendorMatchesWithZero() {
        CredibancoDayResult result = dayOf(august5());

        assertEquals(4, result.vendors().size());
        assertTrue(result.vendors().stream().allMatch(VendorCredibancoCheck::matches));
        assertEquals("Carlos Andrés Paredes", result.vendors().get(2).workerName());
        assertEquals(money("2019760"), result.vendors().get(2).vouchersTotal());
    }

    @Test
    void qrIsAddedToTheAccountingTotalOnlyWhenEveryoneMatches() {
        CredibancoDayResult result = dayOf(august5());

        assertTrue(result.qrAdded());
        assertEquals(money("3618384"), result.accounting().mastercard());
        assertEquals(money("2072928"), result.accounting().visa());
        assertEquals(money("876008"), result.accounting().qr());
        // Workbook accounting TOTAL of day 5
        assertEquals(money("6567320"), result.accounting().total());
    }

    @Test
    void aDiscrepancyShowsTheExactDifferenceAndKeepsQrOutOfTheTotal() {
        List<VendorDeclaration> declarations = new java.util.ArrayList<>(august5());
        declarations.set(2, vendor(CARLOS, "DIA6", "1000000", "1019760", "2000000", "377902", "41380"));

        CredibancoDayResult result = dayOf(declarations);

        assertFalse(result.allMatch());
        assertFalse(result.qrAdded());
        VendorCredibancoCheck carlos = result.vendors().get(2);
        assertFalse(carlos.matches());
        assertEquals(money("19760"), carlos.difference());
        assertEquals(money("5691312"), result.accounting().total());
        assertEquals(money("0"), result.accounting().qr());
        // The rest of the day is still calculated: the discrepancy does not block anything
        assertEquals(money("5691312"), result.totalEds());
    }

    @Test
    void aDayWithoutVendorsDoesNotMatch() {
        assertFalse(dayOf(List.of()).allMatch());
    }

    @Test
    void monthToDateAddsDaysOneToFiveAndCommissionIsTwoPointSixFivePercentOfTotalEds() {
        CredibancoDayResult result = dayOf(august5());

        assertEquals(money("13948244"), result.monthToDate().mastercard());
        assertEquals(money("7548843"), result.monthToDate().visa());
        assertEquals(money("21497087"), result.monthToDate().totalEds());
        assertEquals(money("21497087"), result.commissionBase());
        // 21.497.087 x 2,65 % = 569.672,8055
        assertEquals(money("569672.81"), result.commission());
        assertEquals(money("104995536"), result.previousMonth().totalEds());
    }

    @Test
    void commissionOfTheWholeMonthMatchesTheWorkbook() {
        // Workbook J38 = 98.868.793 and N40 = 2.620.023,015
        Map<LocalDate, DayFranchiseTotals> month = Map.of(LocalDate.of(2026, 8, 27), new DayFranchiseTotals(
                money("0"), money("66683965"), money("32184828"), money("0"), money("0"), money("0"),
                money("19476459")));

        CredibancoDayResult result = CredibancoReconciliationCalculator.calculate(LocalDate.of(2026, 8, 28), List.of(),
                NAMES, money("0"), DayFranchiseTotals.ZERO, month, PeriodTotals.NONE, RATE);

        assertEquals(money("98868793"), result.commissionBase());
        assertEquals(money("2620023.01"), result.commission());
    }

    @Test
    void qrNeverGeneratesCommission() {
        CredibancoDayResult withQr = dayOf(august5());
        CredibancoDayResult withoutQr = CredibancoReconciliationCalculator.calculate(DAY, august5(), NAMES,
                money("0"), DayFranchiseTotals.ZERO, august1to4(), previousMonth(), RATE);

        assertEquals(withQr.commission(), withoutQr.commission());
    }

    @Test
    void daysOfAnotherMonthAreNotAccumulated() {
        Map<LocalDate, DayFranchiseTotals> other = Map.of(LocalDate.of(2026, 7, 31), new DayFranchiseTotals(
                money("0"), money("1000000"), money("1000000"), money("0"), money("0"), money("0"), money("0")));

        CredibancoDayResult result = CredibancoReconciliationCalculator.calculate(DAY, august5(), NAMES,
                money("876008"), DayFranchiseTotals.ZERO, other, PeriodTotals.NONE, RATE);

        assertEquals(money("5691312"), result.monthToDate().totalEds());
    }

    @Test
    void dawnVouchersMoveToThePreviousDayInTheAccountingReport() {
        VendorDeclaration withDawn = new VendorDeclaration(CARLOS, "NOCHE", money("1000"), money("2000"),
                money("300"), money("100"), money("200"), money("30"), money("3630"), null, null);
        DayFranchiseTotals previous = new DayFranchiseTotals(money("0"), money("0"), money("0"), money("5"),
                money("50"), money("500"), money("0"));

        CredibancoDayResult result = CredibancoReconciliationCalculator.calculate(DAY, List.of(withDawn), NAMES,
                money("0"), previous, Map.of(), PeriodTotals.NONE, RATE);

        assertTrue(result.allMatch());
        assertEquals(money("1000"), result.franchises().mastercard());
        // franchise + dawn of the previous day - dawn of the day
        assertEquals(money("950"), result.accounting().mastercard());   // 1000 + 50 - 100
        assertEquals(money("2300"), result.accounting().visa());        // 2000 + 500 - 200
        assertEquals(money("275"), result.accounting().amex());         // 300 + 5 - 30
        // TOTAL EDS ignores dawn columns, exactly as the workbook's J = SUM(D:E)
        assertEquals(money("3000"), result.totalEds());
    }

    @Test
    void negativeAmountIsRejectedNamingTheField() {
        ReconciliationValidationException error = assertThrows(ReconciliationValidationException.class,
                () -> vendor(CARLOS, "DIA", "1000", "-1", "0", "0", "0"));

        assertEquals("visa", error.field());
    }

    @Test
    void moreThanTwoDecimalsIsRejectedNamingTheField() {
        ReconciliationValidationException error = assertThrows(ReconciliationValidationException.class,
                () -> new VendorDeclaration(CARLOS, "DIA", new BigDecimal("1000.123"), null, null, null, null, null,
                        null, null, null));

        assertEquals("mastercard", error.field());
    }

    @Test
    void invalidCommissionRateIsRejected() {
        assertThrows(ReconciliationValidationException.class, () -> CredibancoReconciliationCalculator.calculate(DAY,
                august5(), NAMES, money("0"), DayFranchiseTotals.ZERO, Map.of(), PeriodTotals.NONE,
                new BigDecimal("2.65")));
    }
}
