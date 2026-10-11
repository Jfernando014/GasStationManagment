package com.edu.unicauca.gasstation.backend.sales.domain.models;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SalesTargetTest {

    @Test
    void acceptsMonthlyFuelExampleInGallons() {
        SalesTargetIdentity identity = identity(SalesTargetPeriod.monthly(2026, 8), SalesTargetCategory.FUEL_AGGREGATE);
        SalesTarget target = new SalesTarget(identity, new BigDecimal("26920"), SalesTargetUnit.GALLONS);

        assertThat(target.identity()).isEqualTo(identity);
        assertThat(target.value()).isEqualTo(new BigDecimal("26920"));
        assertThat(target.unit()).isEqualTo(SalesTargetUnit.GALLONS);
    }

    @Test
    void acceptsMonthlyBasketExampleInCop() {
        SalesTarget target = new SalesTarget(
                identity(SalesTargetPeriod.monthly(2026, 8), SalesTargetCategory.BASKET),
                new BigDecimal("8400000"), SalesTargetUnit.COP);

        assertThat(target.value()).isEqualTo(new BigDecimal("8400000"));
        assertThat(target.unit()).isEqualTo(SalesTargetUnit.COP);
    }

    @Test
    void annualAndMonthlyTargetsKeepTheirExplicitValues() {
        SalesTarget annual = new SalesTarget(
                identity(SalesTargetPeriod.annual(2026), SalesTargetCategory.FUEL_AGGREGATE),
                new BigDecimal("100000"), SalesTargetUnit.GALLONS);
        SalesTarget monthly = new SalesTarget(
                identity(SalesTargetPeriod.monthly(2026, 8), SalesTargetCategory.FUEL_AGGREGATE),
                new BigDecimal("26920"), SalesTargetUnit.GALLONS);

        assertThat(annual.identity().period().isAnnual()).isTrue();
        assertThat(annual.value()).isEqualTo(new BigDecimal("100000"));
        assertThat(monthly.identity().period().isMonthly()).isTrue();
        assertThat(monthly.value()).isEqualTo(new BigDecimal("26920"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"-1", "-0.0000000001", "-8400000"})
    void rejectsNegativeValuesForBothCategories(String value) {
        assertThatThrownBy(() -> new SalesTarget(
                identity(SalesTargetPeriod.annual(2026), SalesTargetCategory.FUEL_AGGREGATE),
                new BigDecimal(value), SalesTargetUnit.GALLONS))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("value must not be negative");
        assertThatThrownBy(() -> new SalesTarget(
                identity(SalesTargetPeriod.annual(2026), SalesTargetCategory.BASKET),
                new BigDecimal(value), SalesTargetUnit.COP))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("value must not be negative");
    }

    @Test
    void acceptsZeroWithoutIntroducingAPositiveOnlyRule() {
        for (SalesTargetCategory category : SalesTargetCategory.values()) {
            SalesTargetUnit unit = category == SalesTargetCategory.FUEL_AGGREGATE
                    ? SalesTargetUnit.GALLONS : SalesTargetUnit.COP;
            SalesTarget target = new SalesTarget(
                    identity(SalesTargetPeriod.annual(2026), category), new BigDecimal("0.000"), unit);
            assertThat(target.value()).isEqualTo(new BigDecimal("0.000"));
        }
    }

    @Test
    void rejectsNullIdentity() {
        assertThatThrownBy(() -> new SalesTarget(null, BigDecimal.ONE, SalesTargetUnit.GALLONS))
                .isInstanceOf(NullPointerException.class).hasMessage("identity");
    }

    @Test
    void rejectsNullValue() {
        assertThatThrownBy(() -> new SalesTarget(
                identity(SalesTargetPeriod.annual(2026), SalesTargetCategory.FUEL_AGGREGATE),
                null, SalesTargetUnit.GALLONS))
                .isInstanceOf(NullPointerException.class).hasMessage("value");
    }

    @Test
    void rejectsNullUnit() {
        assertThatThrownBy(() -> new SalesTarget(
                identity(SalesTargetPeriod.annual(2026), SalesTargetCategory.FUEL_AGGREGATE), BigDecimal.ONE, null))
                .isInstanceOf(NullPointerException.class).hasMessage("unit");
    }

    @Test
    void rejectsCopForFuel() {
        assertThatThrownBy(() -> new SalesTarget(
                identity(SalesTargetPeriod.annual(2026), SalesTargetCategory.FUEL_AGGREGATE),
                BigDecimal.ONE, SalesTargetUnit.COP))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsGallonsForBasket() {
        assertThatThrownBy(() -> new SalesTarget(
                identity(SalesTargetPeriod.annual(2026), SalesTargetCategory.BASKET),
                BigDecimal.ONE, SalesTargetUnit.GALLONS))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"26920.123456789012345678900", "8400000.000", "0.000000000000000000001"})
    void preservesDecimalPrecisionAndScaleForBothUnits(String value) {
        BigDecimal amount = new BigDecimal(value);
        SalesTarget fuel = new SalesTarget(
                identity(SalesTargetPeriod.monthly(2026, 8), SalesTargetCategory.FUEL_AGGREGATE),
                amount, SalesTargetUnit.GALLONS);
        SalesTarget basket = new SalesTarget(
                identity(SalesTargetPeriod.monthly(2026, 8), SalesTargetCategory.BASKET),
                amount, SalesTargetUnit.COP);

        assertThat(fuel.value()).isEqualTo(amount);
        assertThat(basket.value()).isEqualTo(amount);
        assertThat(fuel.value().scale()).isEqualTo(amount.scale());
        assertThat(basket.value().scale()).isEqualTo(amount.scale());
    }

    private static SalesTargetIdentity identity(SalesTargetPeriod period, SalesTargetCategory category) {
        return new SalesTargetIdentity(period, category);
    }
}
