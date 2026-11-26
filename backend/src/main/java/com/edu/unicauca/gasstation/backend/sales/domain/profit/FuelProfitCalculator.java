package com.edu.unicauca.gasstation.backend.sales.domain.profit;

import com.edu.unicauca.gasstation.backend.inventory.FuelType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Fuel profit of a month. Pure Java: no Spring, no database.
 *
 * <ul>
 *   <li>Sales of a day = gallons x sale price of that day.</li>
 *   <li>Profit of a day = gallons x (sale price - purchase cost - freight - financial cost) of that day.</li>
 *   <li>Target profit = target gallons x margin on the reference date.</li>
 *   <li>Commission = sales x card percentage x commission rate; it is never subtracted from the real profit.</li>
 *   <li>A figure that cannot be calculated is null, never zero.</li>
 * </ul>
 *
 * <p>Money is rounded HALF_UP to 2 decimals once per fuel; the totals add the rounded values.
 */
public final class FuelProfitCalculator {

    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final int MONEY_SCALE = 2;
    private static final int PERCENTAGE_SCALE = 4;
    private static final String REAL_PERCENTAGE_NOTE = "El porcentaje real es el Credibanco del mes dividido entre "
            + "la tirilla Dominus del mes. Ambos incluyen también las ventas de canasta y lubricantes, no solo "
            + "combustible.";

    private FuelProfitCalculator() {
    }

    /** The last day of the month, or today while the month is in progress. */
    public static LocalDate referenceDate(YearMonth month, LocalDate today) {
        return YearMonth.from(today).equals(month) ? today : month.atEndOfMonth();
    }

    /**
     * @param referenceValues figures of each fuel in force on the reference date
     * @param targets target gallons of the fuels that have one
     * @param commissionRate fraction effective on the last day of the month
     */
    public static FuelProfitReport calculate(YearMonth month, LocalDate referenceDate, MonthlyFuelSales sales,
                                             Map<FuelType, FuelUnitValues> referenceValues,
                                             Map<FuelType, BigDecimal> targets,
                                             FuelProfitCardSetting setting, BigDecimal commissionRate) {
        CardCommission card = card(month, sales, setting, commissionRate);
        Map<FuelType, FuelProfit> results = new EnumMap<>(FuelType.class);
        for (FuelType fuel : FuelType.values()) {
            results.put(fuel, fuel(fuel, sales.days().getOrDefault(fuel, List.of()),
                    referenceValues.get(fuel), targets.get(fuel), card));
        }
        List<FuelProfit> fuels = List.copyOf(results.values());
        return new FuelProfitReport(month.toString(), referenceDate, month.lengthOfMonth(), sales.daysWithData(),
                card, fuels, totals(fuels));
    }

    private static CardCommission card(YearMonth month, MonthlyFuelSales sales, FuelProfitCardSetting setting,
                                       BigDecimal commissionRate) {
        BigDecimal real = percentage(sales.cardSales(), sales.totalSales(), PERCENTAGE_SCALE);
        BigDecimal applied = setting.mode() == CardPercentageMode.MANUAL ? setting.manualPercentage() : real;
        return new CardCommission(setting.mode(), Decimals.plain(applied), setting.manualPercentage(),
                Decimals.plain(real), money(sales.cardSales()), money(sales.totalSales()), REAL_PERCENTAGE_NOTE,
                commissionRate, month.atEndOfMonth());
    }

    private static FuelProfit fuel(FuelType fuel, List<FuelDayInput> inputs, FuelUnitValues reference,
                                   BigDecimal target, CardCommission card) {
        List<FuelDayDetail> days = new ArrayList<>();
        Map<MissingDatum, LocalDate> missing = new EnumMap<>(MissingDatum.class);
        BigDecimal gallons = BigDecimal.ZERO;
        BigDecimal sales = BigDecimal.ZERO;
        BigDecimal profit = BigDecimal.ZERO;
        for (FuelDayInput input : inputs) {
            FuelUnitValues values = input.values();
            values.missing().forEach(datum -> missing.putIfAbsent(datum, input.date()));
            BigDecimal margin = values.marginPerGallon();
            BigDecimal daySales = multiply(input.gallons(), values.salePrice());
            BigDecimal dayProfit = multiply(input.gallons(), margin);
            gallons = gallons.add(input.gallons());
            sales = add(sales, daySales);
            profit = add(profit, dayProfit);
            days.add(new FuelDayDetail(input.date(), input.gallons(), values.salePrice(), values.purchaseCost(),
                    values.freightPerGallon(), values.financialCostPerGallon(), margin, money(daySales),
                    money(dayProfit)));
        }
        BigDecimal roundedSales = money(sales);
        BigDecimal realProfit = money(profit);
        BigDecimal referenceMargin = reference == null ? null : reference.marginPerGallon();
        BigDecimal commission = commission(roundedSales, card);
        List<MissingData> missingData = missing.entrySet().stream()
                .map(entry -> new MissingData(entry.getKey(), entry.getValue()))
                .toList();
        return new FuelProfit(fuel, missingData.isEmpty(), missingData, gallons, roundedSales, realProfit, target,
                percentage(gallons, target, MONEY_SCALE), money(multiply(target, referenceMargin)),
                referenceMargin, commission, subtract(realProfit, commission), reference, List.copyOf(days));
    }

    private static BigDecimal commission(BigDecimal sales, CardCommission card) {
        if (sales == null || card.appliedPercentage() == null) {
            return null;
        }
        return money(sales.multiply(card.appliedPercentage()).movePointLeft(2).multiply(card.commissionRate()));
    }

    private static FuelProfitTotals totals(List<FuelProfit> fuels) {
        List<FuelType> incomplete = fuels.stream().filter(f -> !f.complete()).map(FuelProfit::fuelType).toList();
        List<FuelType> withoutTarget = fuels.stream().filter(f -> f.targetGallons() == null)
                .map(FuelProfit::fuelType).toList();
        BigDecimal gallons = sum(fuels, FuelProfit::gallons);
        BigDecimal targetGallons = sum(fuels, FuelProfit::targetGallons);
        return new FuelProfitTotals(incomplete.isEmpty(), incomplete, withoutTarget, gallons,
                sum(fuels, FuelProfit::sales), sum(fuels, FuelProfit::realProfit),
                sum(fuels, FuelProfit::commission), sum(fuels, FuelProfit::profitAfterCommission),
                targetGallons, percentage(gallons, targetGallons, MONEY_SCALE),
                sum(fuels, FuelProfit::targetProfit));
    }

    /** Null as soon as one fuel has no value. */
    private static BigDecimal sum(List<FuelProfit> fuels, Function<FuelProfit, BigDecimal> field) {
        BigDecimal total = BigDecimal.ZERO;
        for (FuelProfit fuel : fuels) {
            total = add(total, field.apply(fuel));
        }
        return total;
    }

    private static BigDecimal percentage(BigDecimal part, BigDecimal whole, int scale) {
        if (part == null || whole == null || whole.signum() == 0) {
            return null;
        }
        return part.multiply(HUNDRED).divide(whole, scale, RoundingMode.HALF_UP);
    }

    private static BigDecimal add(BigDecimal left, BigDecimal right) {
        return left == null || right == null ? null : left.add(right);
    }

    private static BigDecimal subtract(BigDecimal left, BigDecimal right) {
        return left == null || right == null ? null : left.subtract(right);
    }

    private static BigDecimal multiply(BigDecimal left, BigDecimal right) {
        return left == null || right == null ? null : left.multiply(right);
    }

    private static BigDecimal money(BigDecimal value) {
        return value == null ? null : value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }
}
