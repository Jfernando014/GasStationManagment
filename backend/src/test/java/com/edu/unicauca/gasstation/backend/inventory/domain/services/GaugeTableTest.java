package com.edu.unicauca.gasstation.backend.inventory.domain.services;

import com.edu.unicauca.gasstation.backend.inventory.GaugeTestData;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.GaugePoint;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.GaugeTable;
import com.edu.unicauca.gasstation.backend.inventory.exception.HeightExceedsGaugeException;
import com.edu.unicauca.gasstation.backend.inventory.exception.InvalidGaugeTableException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static com.edu.unicauca.gasstation.backend.inventory.GaugeTestData.assertInvalidGauge;
import static com.edu.unicauca.gasstation.backend.inventory.GaugeTestData.curve;
import static com.edu.unicauca.gasstation.backend.inventory.GaugeTestData.point;
import static com.edu.unicauca.gasstation.backend.inventory.GaugeTestData.table;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GaugeTableTest {

    // ---------- validate ----------

    @Test
    void validateAcceptsAValidCurve() {
        assertThatCode(() -> curve(null, "Curva").validate()).doesNotThrowAnyException();
    }

    @Test
    void validateAcceptsEqualVolumeWhenHeightIncreases() {
        GaugeTable table = table(null, "Curva", point("0", "1"), point("1", "1"));

        assertThatCode(table::validate).doesNotThrowAnyException();
    }

    @Test
    void validateAcceptsOneDecimalInTheHeight() {
        GaugeTable table = table(null, "Curva", point("55.5", "1"), point("56", "3"));

        assertThatCode(table::validate).doesNotThrowAnyException();
    }

    @Test
    void validateAcceptsTrailingZerosAndTheLargestHeight() {
        GaugeTable table = table(null, "Curva", point("100.0", "1.00"), point("9999.9", "3"));

        assertThatCode(table::validate).doesNotThrowAnyException();
    }

    @Test
    void validateRejectsFewerThanTwoPoints() {
        assertInvalidGauge(() -> table(null, "Curva", point("1", "1")).validate(), "points");
    }

    @Test
    void validateRejectsAnEmptyTable() {
        assertInvalidGauge(() -> table(null, "Curva").validate(), "points");
    }

    @Test
    void validateRejectsARepeatedHeight() {
        assertInvalidGauge(() -> table(null, "Curva", point("1", "1"), point("1", "2")).validate(), "points[1]");
    }

    @Test
    void validateRejectsAHeightLowerThanThePreviousOne() {
        GaugeTable table = table(null, "Curva", point("1", "1"), point("5", "5"), point("3", "6"));

        assertInvalidGauge(table::validate, "points[2]");
    }

    @Test
    void validateRejectsAVolumeLowerThanThePreviousOne() {
        assertInvalidGauge(() -> table(null, "Curva", point("1", "10"), point("2", "9")).validate(), "points[1]");
    }

    @Test
    void validateRejectsNegativeValues() {
        assertInvalidGauge(() -> table(null, "Curva", point("-1", "1"), point("2", "3")).validate(), "points[0]");
        assertInvalidGauge(() -> table(null, "Curva", point("1", "-1"), point("2", "3")).validate(), "points[0]");
    }

    @Test
    void validateRejectsMissingValues() {
        GaugeTable table = table(null, "Curva", new GaugePoint(null, new BigDecimal("1")), point("2", "3"));

        assertInvalidGauge(table::validate, "points[0]");
    }

    @Test
    void validateRejectsMoreThanOneDecimalInTheHeight() {
        assertInvalidGauge(() -> table(null, "Curva", point("10.25", "1"), point("11", "3")).validate(), "points[0]");
    }

    @Test
    void validateRejectsMoreThanTwoDecimalsInTheVolume() {
        assertInvalidGauge(() -> table(null, "Curva", point("1", "1"), point("2", "1.005")).validate(), "points[1]");
    }

    @Test
    void validateRejectsAHeightWithTooManyIntegerDigits() {
        assertInvalidGauge(() -> table(null, "Curva", point("1", "1"), point("10000", "3")).validate(), "points[1]");
    }

    @Test
    void invalidGaugeTableExceptionUsesTheDetailAsMessage() {
        GaugeTable table = table(null, "Curva", point("1", "1"), point("1", "2"));

        assertThatThrownBy(table::validate)
                .isInstanceOfSatisfying(InvalidGaugeTableException.class, e -> {
                    assertThat(e.getDetail()).isNotBlank();
                    assertThat(e.getMessage()).isEqualTo(e.getDetail());
                });
    }

    @Test
    void invalidGaugeTableExceptionFallsBackToTheGenericMessageWithoutDetail() {
        assertThat(new InvalidGaugeTableException("points", null).getMessage())
                .isEqualTo("Please correct the height and volume values");
        assertThat(new InvalidGaugeTableException("points", "  ").getMessage())
                .isEqualTo("Please correct the height and volume values");
    }

    // ---------- volumeAt ----------

    @Test
    void volumeAtReturnsTheCertifiedVolumeOnAnExactPoint() {
        assertThat(curve(null, "Curva").volumeAt(new BigDecimal("55"))).isEqualByComparingTo("559");
    }

    @Test
    void volumeAtInterpolatesBetweenTwoPoints() {
        // 55 cm -> 559 gal y 56 cm -> 574 gal: a mitad de camino, 566,5
        assertThat(curve(null, "Curva").volumeAt(new BigDecimal("55.5"))).isEqualByComparingTo("566.5");
    }

    @Test
    void volumeAtInterpolatesBetweenFarApartPoints() {
        // 1 cm -> 1 gal y 5 cm -> 16 gal: a 3 cm, 8,5
        assertThat(curve(null, "Curva").volumeAt(new BigDecimal("3"))).isEqualByComparingTo("8.5");
    }

    @Test
    void volumeAtRoundsToTwoDecimals() {
        GaugeTable table = table(null, "Curva", point("0", "0"), point("3", "10"));

        assertThat(table.volumeAt(new BigDecimal("1"))).isEqualByComparingTo("3.33");
    }

    @Test
    void volumeAtTheFirstPointReturnsItsVolume() {
        assertThat(curve(null, "Curva").volumeAt(new BigDecimal("1"))).isEqualByComparingTo("1");
    }

    @Test
    void volumeAtBelowTheFirstPointIsZero() {
        assertThat(curve(null, "Curva").volumeAt(new BigDecimal("0.5"))).isEqualByComparingTo("0");
        assertThat(curve(null, "Curva").volumeAt(BigDecimal.ZERO)).isEqualByComparingTo("0");
    }

    @Test
    void volumeAtTheLastPointReturnsItsVolume() {
        assertThat(curve(null, "Curva").volumeAt(new BigDecimal("238"))).isEqualByComparingTo("3090");
    }

    @Test
    void volumeAboveTheLastPointThrowsWithTheMaximumHeight() {
        assertThatThrownBy(() -> curve(null, "Curva").volumeAt(new BigDecimal("238.1")))
                .isInstanceOfSatisfying(HeightExceedsGaugeException.class,
                        e -> assertThat(e.getMaxHeightCm()).isEqualByComparingTo("238"));
    }

    @Test
    void volumeAtRejectsANegativeHeight() {
        assertThatThrownBy(() -> curve(null, "Curva").volumeAt(new BigDecimal("-1")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void volumeAtRejectsANullHeight() {
        assertThatThrownBy(() -> curve(null, "Curva").volumeAt(null)).isInstanceOf(NullPointerException.class);
    }

    // ---------- derived values ----------

    @Test
    void exposesTheLimitsOfTheCurve() {
        GaugeTable table = curve(null, "Curva");

        assertThat(table.minHeightCm()).isEqualByComparingTo("1");
        assertThat(table.maxHeightCm()).isEqualByComparingTo("238");
        assertThat(table.maxVolumeGallons()).isEqualByComparingTo("3090");
    }
}