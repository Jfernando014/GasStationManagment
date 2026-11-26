package com.edu.unicauca.gasstation.backend.inventory.domain.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.edu.unicauca.gasstation.backend.inventory.FuelType;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.AlertLevel;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.DailyVariationEntry;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.FuelMonthlyVariation;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.FuelVariation;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.GallonRange;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.ShortageRangeCount;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.SurplusRangeCount;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.VariationAlertSetting;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.VariationStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class MonthlyVariationCalculatorTest {

    @Test
    void classifiesGallonRangeBoundariesContiguously() {
        assertEquals(Optional.empty(), GallonRange.of(new BigDecimal("0.99")));
        assertEquals(Optional.of(GallonRange.FROM_1_TO_49), GallonRange.of(new BigDecimal("1")));
        assertEquals(Optional.of(GallonRange.FROM_1_TO_49), GallonRange.of(new BigDecimal("49")));
        assertEquals(Optional.of(GallonRange.FROM_50_TO_74), GallonRange.of(new BigDecimal("49.01")));
        assertEquals(Optional.of(GallonRange.FROM_50_TO_74), GallonRange.of(new BigDecimal("74")));
        assertEquals(Optional.of(GallonRange.FROM_75_TO_124), GallonRange.of(new BigDecimal("75")));
        assertEquals(Optional.of(GallonRange.FROM_125_TO_249), GallonRange.of(new BigDecimal("249")));
        assertEquals(Optional.of(GallonRange.FROM_250_TO_499), GallonRange.of(new BigDecimal("499")));
        assertEquals(Optional.of(GallonRange.FROM_500), GallonRange.of(new BigDecimal("500")));
    }

    @Test
    void escalatesCumulativeAlertsAcrossCalculatedDaysIgnoringIncompleteDaysAndRecoversWhenBackInLimit() {
        VariationAlertSetting setting = defaultSetting("0.5", 2);
        List<LocalDate> days = List.of(
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 2),
                LocalDate.of(2026, 8, 3), // incomplete measurement: must not break streak
                LocalDate.of(2026, 8, 4),
                LocalDate.of(2026, 8, 5),
                LocalDate.of(2026, 8, 6),
                LocalDate.of(2026, 8, 7)); // recovers inside 0.5%

        List<FuelVariation> variations = Arrays.asList(
                okVariation("1000", "-10", false),    // cum: -10 / 1000 = -1.0% -> REVISAR (1)
                okVariation("1000", "-10", false),    // cum: -20 / 2000 = -1.0% -> REVISAR (2)
                incompleteVariation(),                                        // skipped
                okVariation("1000", "-10", false),    // prev 2 were REVISAR -> AYUDA (1)
                okVariation("1000", "-10", false),    // prev was AYUDA -> AYUDA (2)
                okVariation("1000", "-10", false),    // prev 2 were AYUDA -> ALARMA
                okVariation("10000", "50", false));   // cum: 0 / 15000 = 0% -> OK

        FuelMonthlyVariation result = MonthlyVariationCalculator.calculate(
                FuelType.MOTOR, days, variations, setting, new BigDecimal("20000"));

        List<DailyVariationEntry> entries = result.getDays();
        assertEquals(AlertLevel.REVISAR, entries.get(0).getCumulativeStatus());
        assertEquals(AlertLevel.REVISAR, entries.get(1).getCumulativeStatus());
        assertNull(entries.get(2).getCumulativeStatus());
        assertEquals(AlertLevel.AYUDA, entries.get(3).getCumulativeStatus());
        assertEquals(AlertLevel.AYUDA, entries.get(4).getCumulativeStatus());
        assertEquals(AlertLevel.ALARMA, entries.get(5).getCumulativeStatus());
        assertEquals(AlertLevel.OK, entries.get(6).getCumulativeStatus());

        assertEquals(AlertLevel.OK, result.getSummary().getCumulativeStatus());
        assertEquals(6, result.getSummary().getCalculatedDays());
        assertEquals(LocalDate.of(2026, 8, 7), result.getSummary().getLastCalculatedDate());
        // total sold = 15,000; target = 20,000 -> remaining = 5,000; avg over 6 selling days = 2,500.000
        assertEquals(new BigDecimal("15000"), result.getTotals().getSoldGallons());
        assertEquals(new BigDecimal("5000"), result.getRemainingGallons());
        assertEquals(new BigDecimal("2500.000"), result.getTotals().getAverageDailySoldGallons());
    }

    @Test
    void classifiesShortageRangeLimitsAndCountsSurplusesSeparately() {
        VariationAlertSetting setting = defaultSetting("0.5", 3);
        setting.setShortageLimit250To499(2);
        setting.setShortageLimit500OrMore(1);

        List<LocalDate> days = List.of(
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 2),
                LocalDate.of(2026, 8, 3),
                LocalDate.of(2026, 8, 4),
                LocalDate.of(2026, 8, 5));

        List<FuelVariation> variations = List.of(
                okVariation("1000", "-300", true),  // FROM_250_TO_499 #1
                okVariation("1000", "-350", true),  // FROM_250_TO_499 #2 -> equals limit (REVISAR)
                okVariation("1000", "-600", true),  // FROM_500 #1
                okVariation("1000", "-520", true),  // FROM_500 #2 -> exceeds limit 1 (ALARMA)
                okVariation("1000", "80", false));  // surplus FROM_75_TO_124 #1

        FuelMonthlyVariation result = MonthlyVariationCalculator.calculate(
                FuelType.DIESEL, days, variations, setting, null);

        assertNull(result.getRemainingGallons());
        assertEquals(4, result.getSummary().getOutOfToleranceDays());

        ShortageRangeCount range1To49 = shortageOf(result, GallonRange.FROM_1_TO_49);
        assertEquals(0, range1To49.getCount());
        assertEquals(AlertLevel.OK, range1To49.getStatus());

        ShortageRangeCount range250 = shortageOf(result, GallonRange.FROM_250_TO_499);
        assertEquals(2, range250.getCount());
        assertEquals(AlertLevel.REVISAR, range250.getStatus());

        ShortageRangeCount range500 = shortageOf(result, GallonRange.FROM_500);
        assertEquals(2, range500.getCount());
        assertEquals(AlertLevel.ALARMA, range500.getStatus());

        SurplusRangeCount surplus75 = result.getSurplusRanges().stream()
                .filter(s -> s.getRange() == GallonRange.FROM_75_TO_124)
                .findFirst()
                .orElseThrow();
        assertEquals(1, surplus75.getCount());
    }

    @Test
    void returnsNullFiguresWhenNoDaysHaveMeasurements() {
        VariationAlertSetting setting = defaultSetting("0.5", 3);
        List<LocalDate> days = List.of(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 2));

        FuelMonthlyVariation result = MonthlyVariationCalculator.calculate(
                FuelType.EXTRA, days, Arrays.asList(null, null), setting, new BigDecimal("500"));

        assertEquals(0, result.getSummary().getCalculatedDays());
        assertNull(result.getSummary().getCumulativeStatus());
        assertNull(result.getTotals().getSoldGallons());
        assertNull(result.getRemainingGallons());
        assertTrue(result.getShortageRanges().stream().allMatch(r -> r.getStatus() == null));
    }

    private static FuelVariation okVariation(String sold, String variance, boolean outOfTolerance) {
        return FuelVariation.builder()
                .fuelType(FuelType.MOTOR)
                .status(VariationStatus.OK)
                .initialGallons(new BigDecimal("5000"))
                .entriesGallons(BigDecimal.ZERO)
                .soldGallons(new BigDecimal(sold))
                .theoreticalGallons(new BigDecimal("4000"))
                .measuredGallons(new BigDecimal("4000").add(new BigDecimal(variance)))
                .varianceGallons(new BigDecimal(variance))
                .toleranceGallons(new BigDecimal("50"))
                .outOfTolerance(outOfTolerance)
                .build();
    }

    private static FuelVariation incompleteVariation() {
        return FuelVariation.builder()
                .fuelType(FuelType.MOTOR)
                .status(VariationStatus.INCOMPLETE_MEASUREMENT)
                .entriesGallons(BigDecimal.ZERO)
                .build();
    }

    private static VariationAlertSetting defaultSetting(String limitPercentage, int escalationDays) {
        VariationAlertSetting setting = new VariationAlertSetting();
        setting.setId(1L);
        setting.setValidFrom(LocalDate.of(2000, 1, 1));
        setting.setCumulativeLimitPercentage(new BigDecimal(limitPercentage));
        setting.setEscalationDays(escalationDays);
        setting.setShortageLimit1To49(12);
        setting.setShortageLimit50To74(8);
        setting.setShortageLimit75To124(6);
        setting.setShortageLimit125To249(4);
        setting.setShortageLimit250To499(2);
        setting.setShortageLimit500OrMore(1);
        return setting;
    }

    private static ShortageRangeCount shortageOf(FuelMonthlyVariation result, GallonRange range) {
        return result.getShortageRanges().stream()
                .filter(s -> s.getRange() == range)
                .findFirst()
                .orElseThrow();
    }
}
