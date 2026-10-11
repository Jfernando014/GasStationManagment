package com.edu.unicauca.gasstation.backend.sales.domain.models;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SalesTargetIdentityTest {

    @Test
    void identitiesDistinguishCategoryYearAndMonth() {
        SalesTargetIdentity fuel = new SalesTargetIdentity(
                SalesTargetPeriod.annual(2026), SalesTargetCategory.FUEL_AGGREGATE);

        assertThat(fuel).isEqualTo(new SalesTargetIdentity(
                SalesTargetPeriod.annual(2026), SalesTargetCategory.FUEL_AGGREGATE));
        assertThat(fuel).isNotEqualTo(new SalesTargetIdentity(
                SalesTargetPeriod.annual(2026), SalesTargetCategory.BASKET));
        assertThat(fuel).isNotEqualTo(new SalesTargetIdentity(
                SalesTargetPeriod.annual(2027), SalesTargetCategory.FUEL_AGGREGATE));
        assertThat(fuel).isNotEqualTo(new SalesTargetIdentity(
                SalesTargetPeriod.monthly(2026, 1), SalesTargetCategory.FUEL_AGGREGATE));
        assertThat(new SalesTargetIdentity(SalesTargetPeriod.monthly(2026, 1), SalesTargetCategory.BASKET))
                .isNotEqualTo(new SalesTargetIdentity(
                        SalesTargetPeriod.monthly(2026, 2), SalesTargetCategory.BASKET));
    }

    @Test
    void requiresPeriodAndCategoryToDescribeAnIdentity() {
        assertThatThrownBy(() -> new SalesTargetIdentity(null, SalesTargetCategory.BASKET))
                .isInstanceOf(NullPointerException.class).hasMessage("period");
        assertThatThrownBy(() -> new SalesTargetIdentity(SalesTargetPeriod.annual(2026), null))
                .isInstanceOf(NullPointerException.class).hasMessage("category");
    }

    @Test
    void aggregateCategoriesAreSeparateFromExcelFuelProducts() {
        assertThat(SalesTargetCategory.values())
                .containsExactly(SalesTargetCategory.FUEL_AGGREGATE, SalesTargetCategory.BASKET);
        assertThat(FuelProduct.values())
                .containsExactly(FuelProduct.MOTOR, FuelProduct.DIESEL, FuelProduct.MAXPRO, FuelProduct.EXTRA);
    }
}
