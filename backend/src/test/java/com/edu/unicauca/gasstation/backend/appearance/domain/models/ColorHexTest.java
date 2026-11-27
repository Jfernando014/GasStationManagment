package com.edu.unicauca.gasstation.backend.appearance.domain.models;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.edu.unicauca.gasstation.backend.appearance.exception.InsufficientColorContrastException;
import com.edu.unicauca.gasstation.backend.appearance.exception.InvalidColorHexException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ColorHexTest {

    @Test
    void normalizesTrimmedLowercaseHex() {
        ColorHex hex = ColorHex.of("  #0284c7 ");

        assertThat(hex.value()).isEqualTo("#0284C7");
    }

    @Test
    void acceptsAllCatalogDefaultsAndCuratedPresets() {
        ColorCatalog.fuelDefaults().forEach(entry ->
                assertThat(ColorHex.of(entry.defaultHex()).value()).isEqualTo(entry.defaultHex()));
        ColorCatalog.shiftDefaults().forEach(entry ->
                assertThat(ColorHex.of(entry.defaultHex()).value()).isEqualTo(entry.defaultHex()));
        ColorCatalog.curatedPalette().forEach(preset ->
                assertThat(ColorHex.of(preset.hex()).value()).isEqualTo(preset.hex()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "0284C7", "#FFF", "#12345G", "#1234567", "red"})
    void rejectsMalformedHexValues(String invalid) {
        assertThatThrownBy(() -> ColorHex.of(invalid))
                .isInstanceOf(InvalidColorHexException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"#FFFFFF", "#F8FAFC", "#F1F5F9", "#FEF9C3", "#E2E8F0"})
    void rejectsLowContrastNearWhiteColors(String paleColor) {
        assertThatThrownBy(() -> ColorHex.of(paleColor))
                .isInstanceOf(InsufficientColorContrastException.class);
    }
}
