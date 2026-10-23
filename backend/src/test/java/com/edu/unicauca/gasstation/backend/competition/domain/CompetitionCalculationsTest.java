package com.edu.unicauca.gasstation.backend.competition.domain;

import com.edu.unicauca.gasstation.backend.competition.domain.models.*;
import com.edu.unicauca.gasstation.backend.inventory.FuelType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.*;

class CompetitionCalculationsTest {
    @Test void computesMeanMinimumAndSignedPercentagesWithDecimals() {
        var summary = PriceCalculations.summarize(List.of(observation("100.10"), observation("110.20"), observation("130.30")));
        assertThat(summary.competitorCount()).isEqualTo(3);
        assertThat(summary.average()).isEqualByComparingTo("113.533333");
        assertThat(summary.minimum()).isEqualByComparingTo("100.10");
        var difference = PriceCalculations.difference(new BigDecimal("110.11"), summary.minimum());
        assertThat(difference.absoluteCopPerGallon()).isEqualByComparingTo("10.010000");
        assertThat(difference.percentage()).isEqualByComparingTo("10.000000");
        assertThat(PriceCalculations.difference(new BigDecimal("90.09"), summary.minimum()).percentage()).isEqualByComparingTo("-10");
    }
    @Test void explicitlyHandlesNoMarketNoOwnPriceAndZeroReference() {
        var summary = PriceCalculations.summarize(List.of());
        assertThat(summary.competitorCount()).isZero();
        assertThat(summary.average()).isNull(); assertThat(summary.minimum()).isNull();
        assertThat(PriceCalculations.difference(null, BigDecimal.ONE).absoluteCopPerGallon()).isNull();
        assertThat(PriceCalculations.difference(BigDecimal.ONE, null).percentage()).isNull();
        var zero = PriceCalculations.difference(BigDecimal.ONE, BigDecimal.ZERO);
        assertThat(zero.absoluteCopPerGallon()).isEqualByComparingTo("1"); assertThat(zero.percentage()).isNull();
    }
    @Test void financialMarginUsesSellingPriceAndAllowsLossesAndZeroExplicitCost() {
        var margin = PriceCalculations.margin(new BigDecimal("125"), new BigDecimal("100"));
        assertThat(margin.grossCopPerGallon()).isEqualByComparingTo("25");
        assertThat(margin.grossMarginPercentage()).isEqualByComparingTo("20");
        assertThat(PriceCalculations.margin(new BigDecimal("100"), new BigDecimal("125")).grossMarginPercentage()).isEqualByComparingTo("-25");
        assertThat(PriceCalculations.margin(BigDecimal.ONE, BigDecimal.ZERO).grossMarginPercentage()).isEqualByComparingTo("100");
    }
    @Test void noAcquisitionCostMeansNoEstimatedMargin() {
        var margin = PriceCalculations.margin(BigDecimal.ONE, null);
        assertThat(margin.grossCopPerGallon()).isNull(); assertThat(margin.grossMarginPercentage()).isNull();
    }
    @ParameterizedTest @ValueSource(strings = {"0", "-1", "0.001", "10000000000", "9999999999.999"})
    void rejectsInvalidPricesWithoutRounding(String price) {
        assertThatThrownBy(() -> PriceCalculations.money(new BigDecimal(price), false)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void rejectsInvalidCostAndAcceptsTheNumericBoundary() {
        assertThatThrownBy(() -> PriceCalculations.margin(BigDecimal.ONE, new BigDecimal("-0.01"))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PriceCalculations.money(null, false)).isInstanceOf(IllegalArgumentException.class);
        PriceCalculations.money(new BigDecimal("9999999999.99"), false);
    }
    private static PriceObservation observation(String price) {
        return new PriceObservation(1L, 1L, FuelType.MOTOR, new BigDecimal(price), Instant.parse("2020-01-01T00:00:00Z"), null);
    }
}
