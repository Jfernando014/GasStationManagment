package com.edu.unicauca.gasstation.backend.incentives.domain.services;

import static com.edu.unicauca.gasstation.backend.incentives.IncentiveTestData.GASOLINE_ID;
import static com.edu.unicauca.gasstation.backend.incentives.IncentiveTestData.GASOLINE_NAME;
import static com.edu.unicauca.gasstation.backend.incentives.IncentiveTestData.GASOLINE_SKU;
import static com.edu.unicauca.gasstation.backend.incentives.IncentiveTestData.rule;
import static com.edu.unicauca.gasstation.backend.incentives.IncentiveTestData.sale;
import static org.assertj.core.api.Assertions.assertThat;

import com.edu.unicauca.gasstation.backend.incentives.domain.models.AdditiveSaleUnits;
import com.edu.unicauca.gasstation.backend.incentives.domain.models.RuleTimeline;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class StandardUnitValueCalculatorTest {

    private final StandardUnitValueCalculator calculator = new StandardUnitValueCalculator();

    private final RuleTimeline rules = RuleTimeline.of(List.of(
            rule(GASOLINE_ID, 800, LocalDate.of(2025, 11, 1)),
            rule(GASOLINE_ID, 1_000, LocalDate.of(2026, 2, 1))));

    private static AdditiveSaleUnits saleOn(LocalDate date) {
        return sale(date, "1001", "ANA MILENA VILLAMIL", GASOLINE_SKU, GASOLINE_NAME, 5);
    }

    @Test
    void everyUnitEarnsTheValueOfTheRuleInForceOnTheDayItWasSold() {
        assertThat(calculator.valuePerUnit(rules, saleOn(LocalDate.of(2026, 1, 31))))
                .hasValueSatisfying(value -> assertThat(value).isEqualByComparingTo("800"));
        assertThat(calculator.valuePerUnit(rules, saleOn(LocalDate.of(2026, 2, 1))))
                .hasValueSatisfying(value -> assertThat(value).isEqualByComparingTo("1000"));
    }

    @Test
    void theValueDoesNotDependOnTheNumberOfUnits() {
        AdditiveSaleUnits many = sale(LocalDate.of(2026, 2, 10), "1001", "ANA MILENA VILLAMIL", GASOLINE_SKU,
                GASOLINE_NAME, 500);

        assertThat(calculator.valuePerUnit(rules, many))
                .hasValueSatisfying(value -> assertThat(value).isEqualByComparingTo("1000"));
    }

    @Test
    void withoutARuleInForceThereIsNoValue() {
        assertThat(calculator.valuePerUnit(rules, saleOn(LocalDate.of(2025, 10, 31)))).isEmpty();
        assertThat(calculator.valuePerUnit(RuleTimeline.of(List.of()), saleOn(LocalDate.of(2026, 2, 1)))).isEmpty();
    }

    @Test
    void aRuleOfZeroPesosIsAValueNotAMissingRule() {
        RuleTimeline stopped = RuleTimeline.of(List.of(
                rule(GASOLINE_ID, 800, LocalDate.of(2025, 11, 1)),
                rule(GASOLINE_ID, 0, LocalDate.of(2026, 2, 1))));

        assertThat(calculator.valuePerUnit(stopped, saleOn(LocalDate.of(2026, 3, 1))))
                .hasValueSatisfying(value -> assertThat(value).isEqualByComparingTo(BigDecimal.ZERO));
    }
}
