package com.edu.unicauca.gasstation.backend.sales.domain.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.edu.unicauca.gasstation.backend.sales.domain.models.CashDifferenceStatus;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class CashBalanceCalculatorTest {

    private static BigDecimal cop(String value) {
        return new BigDecimal(value);
    }

    @Test
    void thePlanillaBalanceIsTheCashMovementsBalancePlusTheAccumulatedStationCash() {
        BigDecimal planilla = CashBalanceCalculator.planillaBalance(cop("12114000"), cop("12516000"));

        assertThat(planilla).isEqualByComparingTo("24630000");
    }

    @Test
    void augustFirstGivesMinus531AndThePlanillaHasMoreCashThanDominus() {
        BigDecimal planilla = CashBalanceCalculator.planillaBalance(cop("12114000"), cop("12516000"));

        BigDecimal difference = CashBalanceCalculator.difference(cop("24629469"), planilla);

        assertThat(difference).isEqualByComparingTo("-531");
        assertThat(CashBalanceCalculator.statusOf(difference)).isEqualTo(CashDifferenceStatus.PLANILLA_MAYOR);
    }

    @Test
    void aPlanillaWithLessCashThanDominusGivesAPositiveDifference() {
        BigDecimal difference = CashBalanceCalculator.difference(cop("24630500"), cop("24630000"));

        assertThat(difference).isEqualByComparingTo("500");
        assertThat(CashBalanceCalculator.statusOf(difference)).isEqualTo(CashDifferenceStatus.PLANILLA_MENOR);
    }

    @Test
    void equalBalancesMatch() {
        BigDecimal difference = CashBalanceCalculator.difference(cop("500000.00"), cop("500000"));

        assertThat(difference).isEqualByComparingTo("0");
        assertThat(CashBalanceCalculator.statusOf(difference)).isEqualTo(CashDifferenceStatus.CUADRA);
    }

    @Test
    void resultsKeepTwoDecimalsAndRoundHalfUp() {
        assertThat(CashBalanceCalculator.difference(cop("100"), cop("100.005")).toPlainString()).isEqualTo("-0.01");
        assertThat(CashBalanceCalculator.difference(cop("100.005"), cop("100")).toPlainString()).isEqualTo("0.01");
        assertThat(CashBalanceCalculator.planillaBalance(cop("1"), cop("2")).scale()).isEqualTo(2);
    }

    @Test
    void doesNotAcceptMissingFigures() {
        assertThatThrownBy(() -> CashBalanceCalculator.planillaBalance(null, BigDecimal.ONE))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> CashBalanceCalculator.planillaBalance(BigDecimal.ONE, null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> CashBalanceCalculator.difference(null, BigDecimal.ONE))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> CashBalanceCalculator.difference(BigDecimal.ONE, null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> CashBalanceCalculator.statusOf(null)).isInstanceOf(NullPointerException.class);
    }
}
