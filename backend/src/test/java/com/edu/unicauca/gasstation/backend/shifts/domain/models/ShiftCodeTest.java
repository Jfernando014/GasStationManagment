package com.edu.unicauca.gasstation.backend.shifts.domain.models;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.edu.unicauca.gasstation.backend.shifts.ShiftTestData;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ShiftCodeTest {

    private static final String CODE = "TEST";

    @ParameterizedTest(name = "{0} = {1} hours")
    @CsvSource({
            "DIA, 12",
            "NOCHE, 12",
            "6-9y5-9, 7",
            "DESCANSO, 0"
    })
    void totalHoursAddsEverySegmentAndCountsPastMidnight(String code, int expectedHours) {
        assertThat(ShiftTestData.shiftCode(code).totalHours()).isEqualTo(expectedHours);
    }

    @Test
    void segmentsAreReturnedInOrderAndCannotBeModified() {
        List<ShiftSegment> segments = ShiftTestData.shiftCode("6-9y5-9").segments();

        assertThat(segments).containsExactly(new ShiftSegment(6, 9), new ShiftSegment(17, 21));
        assertThat(ShiftTestData.shiftCode("DESCANSO").segments()).isEmpty();
        assertThatThrownBy(() -> segments.add(new ShiftSegment(1, 2)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void segmentTwoMayStartExactlyWhenSegmentOneEnds() {
        assertThatCode(() -> shift(ShiftPeriod.DAY, 6, 9, 9, 12)).doesNotThrowAnyException();
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(nullValues = "null", value = {
            "segment 1 without end,                   DAY, 6,  null, null, null",
            "crosses midnight with DAY period,        DAY, 18, 6,    null, null",
            "segment 2 starts before segment 1 ends,  DAY, 6,  12,   10,   14"
    })
    void rejectsInvalidSchedule(String description, ShiftPeriod period,
                                Integer start1, Integer end1, Integer start2, Integer end2) {
        assertThatThrownBy(() -> shift(period, start1, end1, start2, end2))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("shift code " + CODE);
    }

    private static ShiftCode shift(ShiftPeriod period, Integer start1, Integer end1, Integer start2, Integer end2) {
        return ShiftCode.builder()
                .code(CODE)
                .name("Test")
                .role(ShiftTestData.titularRole())
                .period(period)
                .startHour1(toShort(start1))
                .endHour1(toShort(end1))
                .startHour2(toShort(start2))
                .endHour2(toShort(end2))
                .active(true)
                .build();
    }

    private static Short toShort(Integer value) {
        return value == null ? null : value.shortValue();
    }
}
