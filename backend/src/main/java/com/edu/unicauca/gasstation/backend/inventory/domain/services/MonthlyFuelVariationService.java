package com.edu.unicauca.gasstation.backend.inventory.domain.services;

import com.edu.unicauca.gasstation.backend.inventory.FuelMonthlyTargetPort;
import com.edu.unicauca.gasstation.backend.inventory.FuelType;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.FuelMonthlyVariation;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.FuelVariation;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.MonthlyVariation;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.VariationAlertSetting;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/** Daily control of a whole month per fuel, with its totals and alerts. */
@Service
@RequiredArgsConstructor
public class MonthlyFuelVariationService {

    private final FuelVariationService variationService;
    private final VariationAlertSettingService settingService;
    private final FuelMonthlyTargetPort targetPort;
    private final Clock clock;

    /**
     * The whole month uses the alert setting in force on its reference date: the last day of the month, or today
     * while the month is in progress.
     */
    @Transactional(readOnly = true)
    public MonthlyVariation calculate(YearMonth month) {
        LocalDate today = LocalDate.now(clock);
        LocalDate referenceDate = YearMonth.from(today).equals(month) ? today : month.atEndOfMonth();
        VariationAlertSetting setting = settingService.getEffectiveOn(referenceDate);

        List<LocalDate> days = month.atDay(1).datesUntil(month.atEndOfMonth().plusDays(1)).toList();
        Map<LocalDate, List<FuelVariation>> variations =
                variationService.calculateRange(month.atDay(1), month.atEndOfMonth());

        List<FuelMonthlyVariation> fuels = Arrays.stream(FuelType.values())
                .map(fuelType -> MonthlyVariationCalculator.calculate(fuelType, days,
                        ofFuel(fuelType, days, variations), setting,
                        targetPort.findTargetGallons(fuelType, month).orElse(null)))
                .toList();

        return MonthlyVariation.builder()
                .month(month)
                .referenceDate(referenceDate)
                .alertSetting(setting)
                .fuels(fuels)
                .build();
    }

    private List<FuelVariation> ofFuel(FuelType fuelType, List<LocalDate> days,
                                       Map<LocalDate, List<FuelVariation>> variations) {
        List<FuelVariation> result = new ArrayList<>(days.size());
        for (LocalDate day : days) {
            result.add(variations.getOrDefault(day, List.of()).stream()
                    .filter(v -> v.getFuelType() == fuelType)
                    .findFirst()
                    .orElse(null));
        }
        return result;
    }
}
