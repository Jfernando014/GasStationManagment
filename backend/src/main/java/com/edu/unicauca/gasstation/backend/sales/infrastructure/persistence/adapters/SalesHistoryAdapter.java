package com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence.adapters;

import com.edu.unicauca.gasstation.backend.inventory.FuelType;
import com.edu.unicauca.gasstation.backend.inventory.SalesHistoryPort;
import com.edu.unicauca.gasstation.backend.sales.SellerCloseQuery;
import com.edu.unicauca.gasstation.backend.sales.SellerCloseQuery.DaySummary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.Map;

/**
 * Average gallons sold on the same day of the week, over the most recent occurrences strictly
 * before the journal date. Days without sales closes are ignored.
 */
@Component
@RequiredArgsConstructor
public class SalesHistoryAdapter implements SalesHistoryPort {

    private static final int AVERAGE_SCALE = 3;

    private final SellerCloseQuery sellerCloseQuery;

    @Override
    public Map<FuelType, BigDecimal> averageGallonsForSameWeekday(LocalDate journalDate, int weeks) {
        if (journalDate == null || weeks <= 0) {
            return Map.of();
        }
        Map<FuelType, BigDecimal> totals = new EnumMap<>(FuelType.class);
        Map<FuelType, Integer> days = new EnumMap<>(FuelType.class);
        for (int week = 1; week <= weeks; week++) {
            sellerCloseQuery.findDaySummary(journalDate.minusWeeks(week))
                    .ifPresent(day -> accumulate(day, totals, days));
        }
        return averages(totals, days);
    }

    private void accumulate(DaySummary day, Map<FuelType, BigDecimal> totals, Map<FuelType, Integer> days) {
        for (FuelType fuelType : FuelType.values()) {
            BigDecimal gallons = gallonsOf(day, fuelType);
            if (gallons == null) {
                continue;
            }
            totals.merge(fuelType, gallons, BigDecimal::add);
            days.merge(fuelType, 1, Integer::sum);
        }
    }

    private BigDecimal gallonsOf(DaySummary day, FuelType fuelType) {
        return switch (fuelType) {
            case MOTOR -> day.gallonsMotor();
            case DIESEL -> day.gallonsDiesel();
            case EXTRA -> day.gallonsExtra();
            case MAX_PRO -> day.gallonsMaxpro();
        };
    }

    private Map<FuelType, BigDecimal> averages(Map<FuelType, BigDecimal> totals, Map<FuelType, Integer> days) {
        Map<FuelType, BigDecimal> averages = new EnumMap<>(FuelType.class);
        totals.forEach((fuelType, total) -> {
            int count = days.getOrDefault(fuelType, 0);
            if (count > 0) {
                averages.put(fuelType, total.divide(BigDecimal.valueOf(count), AVERAGE_SCALE, RoundingMode.HALF_UP));
            }
        });
        return averages;
    }
}
