package com.edu.unicauca.gasstation.backend.sales.application;

import com.edu.unicauca.gasstation.backend.inventory.FuelType;
import com.edu.unicauca.gasstation.backend.sales.SellerCloseQuery;
import com.edu.unicauca.gasstation.backend.sales.SellerCloseQuery.DaySummary;
import com.edu.unicauca.gasstation.backend.sales.domain.models.SalesParameterKey;
import com.edu.unicauca.gasstation.backend.sales.domain.models.SalesParameterValue;
import com.edu.unicauca.gasstation.backend.sales.domain.profit.FuelDayInput;
import com.edu.unicauca.gasstation.backend.sales.domain.profit.FuelProfitCalculator;
import com.edu.unicauca.gasstation.backend.sales.domain.profit.FuelProfitCardSetting;
import com.edu.unicauca.gasstation.backend.sales.domain.profit.FuelProfitReport;
import com.edu.unicauca.gasstation.backend.sales.domain.profit.FuelUnitValues;
import com.edu.unicauca.gasstation.backend.sales.domain.profit.MonthlyFuelSales;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.FuelMonthlyTargetReader;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.FuelPriceReadPort;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.FuelProfitCardSettingRepository;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.FuelPurchasePriceReadPort;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.SalesParameterRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Monthly profit per fuel, from the sales certified in step 2 and the figures in force each day. */
@Service
@RequiredArgsConstructor
public class FuelProfitService {

    private final SellerCloseQuery sellerCloseQuery;
    private final FuelPriceReadPort salePrices;
    private final FuelPurchasePriceReadPort purchasePrices;
    private final SalesParameterService parameters;
    private final SalesParameterRepository parameterValues;
    private final FuelMonthlyTargetReader targets;
    private final FuelProfitCardSettingRepository cardSettings;

    /**
     * Uses the commission rate effective on the last day of the month.
     *
     * @param today decides the reference date while the month is in progress
     */
    @Transactional(readOnly = true)
    public FuelProfitReport report(YearMonth month, LocalDate today) {
        BigDecimal commissionRate = parameters
                .effective(SalesParameterKey.CREDIBANCO_COMMISSION_RATE, month.atEndOfMonth()).value();
        LocalDate referenceDate = FuelProfitCalculator.referenceDate(month, today);
        Map<FuelType, FuelUnitValues> referenceValues = new EnumMap<>(FuelType.class);
        for (FuelType fuel : FuelType.values()) {
            referenceValues.put(fuel, valuesOn(fuel, referenceDate));
        }
        return FuelProfitCalculator.calculate(month, referenceDate, salesOf(month), referenceValues,
                targets.findGallonTargets(month), cardSetting(month), commissionRate);
    }

    @Transactional(readOnly = true)
    public FuelProfitCardSetting cardSetting(YearMonth month) {
        return cardSettings.findByMonth(month).orElseGet(() -> FuelProfitCardSetting.real(month));
    }

    @Transactional
    public FuelProfitCardSetting saveCardSetting(FuelProfitCardSetting setting) {
        cardSettings.save(setting);
        return setting;
    }

    private MonthlyFuelSales salesOf(YearMonth month) {
        Map<FuelType, List<FuelDayInput>> days = new EnumMap<>(FuelType.class);
        BigDecimal cardSales = BigDecimal.ZERO;
        BigDecimal totalSales = BigDecimal.ZERO;
        int daysWithData = 0;
        for (int dayOfMonth = 1; dayOfMonth <= month.lengthOfMonth(); dayOfMonth++) {
            LocalDate date = month.atDay(dayOfMonth);
            Optional<DaySummary> summary = sellerCloseQuery.findDaySummary(date);
            if (summary.isEmpty()) {
                continue;
            }
            DaySummary day = summary.get();
            daysWithData++;
            cardSales = cardSales.add(orZero(day.totalCredibanco()));
            totalSales = totalSales.add(orZero(day.totalDominusTirilla()));
            for (FuelType fuel : FuelType.values()) {
                BigDecimal gallons = orZero(gallonsOf(day, fuel));
                if (gallons.signum() != 0) {
                    days.computeIfAbsent(fuel, key -> new ArrayList<>())
                            .add(new FuelDayInput(date, gallons, valuesOn(fuel, date)));
                }
            }
        }
        return new MonthlyFuelSales(daysWithData, days, cardSales, totalSales);
    }

    private FuelUnitValues valuesOn(FuelType fuel, LocalDate date) {
        return new FuelUnitValues(
                salePrices.findEffectivePrice(fuel.name(), date).orElse(null),
                purchasePrices.findEffectivePrice(fuel.name(), date).orElse(null),
                parameterOn(SalesParameterKey.FREIGHT_PER_GALLON, date),
                parameterOn(SalesParameterKey.FINANCIAL_COST_PER_GALLON, date));
    }

    private BigDecimal parameterOn(SalesParameterKey key, LocalDate date) {
        return parameterValues.findEffective(key, date).map(SalesParameterValue::value).orElse(null);
    }

    private static BigDecimal gallonsOf(DaySummary day, FuelType fuel) {
        return switch (fuel) {
            case MOTOR -> day.gallonsMotor();
            case DIESEL -> day.gallonsDiesel();
            case EXTRA -> day.gallonsExtra();
            case MAX_PRO -> day.gallonsMaxpro();
        };
    }

    private static BigDecimal orZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
