package com.edu.unicauca.gasstation.backend.sales.domain.services;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SalesTargetStoragePolicyTest {

    @ParameterizedTest
    @ValueSource(strings = {"0", "0.000000000000000", "26920", "8400000.000", "1.2300000000000",
            "0.000000000001", "999999999999999999.999999999999", "-999999999999999999.999999999999",
            "1E+17", "1E-12", "12300000000000E-13"})
    void acceptsExactlyRepresentableValuesWithoutChangingScale(String text) {
        BigDecimal value = new BigDecimal(text);
        int originalScale = value.scale();
        String originalRepresentation = value.toString();

        assertThatCode(() -> SalesTargetStoragePolicy.validate(value)).doesNotThrowAnyException();
        assertThat(value.scale()).isEqualTo(originalScale);
        assertThat(value.toString()).isEqualTo(originalRepresentation);
    }

    @ParameterizedTest
    @ValueSource(strings = {"1000000000000000000", "-1000000000000000000", "1E18", "-1E19",
            "1000000000000000000.000000000000"})
    void rejectsValuesAtOrBeyondTheAbsoluteLimit(String text) {
        assertThatThrownBy(() -> SalesTargetStoragePolicy.validate(new BigDecimal(text)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("absolute value must be less than 10^18");
    }

    @ParameterizedTest
    @ValueSource(strings = {"1.0000000000001", "0.0000000000001", "-0.0000000000001", "1E-13",
            "999999999999999999.9999999999999"})
    void rejectsAnyValueRequiringRounding(String text) {
        assertThatThrownBy(() -> SalesTargetStoragePolicy.validate(new BigDecimal(text)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("value must be exactly representable with 12 decimal places");
    }

    @Test
    void rejectsNull() {
        assertThatThrownBy(() -> SalesTargetStoragePolicy.validate(null))
                .isInstanceOf(NullPointerException.class).hasMessage("value");
    }
}
