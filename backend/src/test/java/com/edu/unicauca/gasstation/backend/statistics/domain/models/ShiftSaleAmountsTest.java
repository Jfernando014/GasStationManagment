package com.edu.unicauca.gasstation.backend.statistics.domain.models;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ShiftSaleAmountsTest {

    @Test
    void preservesEveryRecordedValueAndScaleWithoutMixingCopAndGallons() {
        List<BigDecimal> supplied = Stream.of(
                "26920.123456789012345678900", "1.000", "0.000000000000000000001", "42.120000",
                "8400000.123456789012345678900", "9000000.0000", "8999999.990000")
                .map(BigDecimal::new).toList();
        var sales = from(supplied.toArray(BigDecimal[]::new));

        List<BigDecimal> stored = List.of(sales.motorGallons(), sales.dieselGallons(),
                sales.extraGallons(), sales.maxproGallons(), sales.basketDominusAmountCop(),
                sales.dominusTirillaCop(), sales.totalDeliveredCop());
        for (int i = 0; i < supplied.size(); i++) {
            assertThat(stored.get(i)).isEqualTo(supplied.get(i));
            assertThat(stored.get(i).scale()).isEqualTo(supplied.get(i).scale());
        }
        assertThat(sales.dominusTirillaCop()).isNotEqualByComparingTo(sales.totalDeliveredCop());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "0.00", "0.000", "0.00000000000000000000"})
    void acceptsExplicitZeroAndPreservesItsScale(String value) {
        BigDecimal zero = new BigDecimal(value);
        var sales = new ShiftSaleAmounts(zero, zero, zero, zero, zero, zero, zero);

        assertThat(List.of(sales.motorGallons(), sales.dieselGallons(), sales.extraGallons(),
                sales.maxproGallons(), sales.basketDominusAmountCop(), sales.dominusTirillaCop(),
                sales.totalDeliveredCop())).allSatisfy(amount -> {
                    assertThat(amount).isEqualTo(zero);
                    assertThat(amount.scale()).isEqualTo(zero.scale());
                });
    }

    @Test
    void keepsReportedSignedValuesWithoutApplyingAnAdjustmentPolicy() {
        var gallons = new BigDecimal("-1.123456789");
        var cop = new BigDecimal("-100.00500");
        var sales = new ShiftSaleAmounts(gallons, gallons, gallons, gallons, cop, cop, cop);

        assertThat(sales.motorGallons()).isEqualTo(gallons);
        assertThat(sales.totalDeliveredCop()).isEqualTo(cop);
    }

    @ParameterizedTest
    @MethodSource("missingValues")
    void rejectsMissingObservationsInsteadOfInventingZero(int index, String field) {
        BigDecimal[] values = {BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO};
        values[index] = null;

        assertThatThrownBy(() -> from(values)).isInstanceOf(NullPointerException.class).hasMessage(field);
    }

    static Stream<Arguments> missingValues() {
        String[] fields = {"motorGallons", "dieselGallons", "extraGallons", "maxproGallons",
                "basketDominusAmountCop", "dominusTirillaCop", "totalDeliveredCop"};
        return IntStream.range(0, fields.length).mapToObj(i -> Arguments.of(i, fields[i]));
    }

    private static ShiftSaleAmounts from(BigDecimal[] values) {
        return new ShiftSaleAmounts(values[0], values[1], values[2], values[3], values[4], values[5], values[6]);
    }
}
