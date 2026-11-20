package com.edu.unicauca.gasstation.backend.statistics.infrastructure.adapters;

import com.edu.unicauca.gasstation.backend.sales.IndividualTargetQuery;
import com.edu.unicauca.gasstation.backend.shifts.holidays.HolidayQuery;
import com.edu.unicauca.gasstation.backend.statistics.domain.calculation.SellerPerformance.DayType;
import com.edu.unicauca.gasstation.backend.statistics.domain.calculation.SellerPerformance.IndividualTarget;
import com.edu.unicauca.gasstation.backend.statistics.domain.calculation.SellerPerformance.Measure;
import com.edu.unicauca.gasstation.backend.statistics.domain.calculation.SellerPerformance.ShiftClassification;
import com.edu.unicauca.gasstation.backend.statistics.domain.calculation.SellerPerformance.ShiftMultiplier;
import com.edu.unicauca.gasstation.backend.statistics.domain.calculation.SellerPerformance.ShiftPeriod;
import com.edu.unicauca.gasstation.backend.statistics.domain.calculation.SellerPerformance.Window;
import com.edu.unicauca.gasstation.backend.statistics.domain.models.ShiftSaleHistory;
import com.edu.unicauca.gasstation.backend.statistics.domain.models.ShiftSaleHistoryRange;
import com.edu.unicauca.gasstation.backend.statistics.domain.ports.SellerPerformanceParameterSource;
import com.edu.unicauca.gasstation.backend.statistics.domain.repositories.ShiftSaleHistoryRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Production source of the performance parameters, built only from public contracts of other modules.
 *
 * <ul>
 *   <li>Classification: the period comes from the shift code that was actually worked and is stored with each close,
 *       never from today's shift catalog; the day type comes from the public holiday query for the operational date.</li>
 *   <li>Targets: the individual targets of sales for exactly the requested period; none when it has none.</li>
 *   <li>Multipliers: one per period, calculated from the confirmed history of the queried window itself: the average of
 *       all its classified shifts divided by the average of the shifts of the period. A period with fewer than
 *       {@value #MIN_SHIFTS_PER_PERIOD} shifts, a window with unconfirmed days or a zero average has no multiplier.</li>
 * </ul>
 */
@Component
public class PublicContractsSellerPerformanceParameterSource implements SellerPerformanceParameterSource {

    static final String CLASSIFICATION_SOURCE =
            "WORKED_SHIFT_CODE(NOCHE=NIGHT;DIA,DIA6,9-6,12-7,6-9y5-9,2-9pm,6-9am,2-6pm,6-9y2-6=DAY)"
                    + "+HOLIDAY_QUERY(shifts.holidays)";

    /** Fewest shifts of a period in the window for its multiplier to be calculated. */
    static final int MIN_SHIFTS_PER_PERIOD = 5;
    private static final int MULTIPLIER_SCALE = 12;
    private static final Logger LOG = LoggerFactory.getLogger(PublicContractsSellerPerformanceParameterSource.class);

    private static final String NIGHT_CODE = "NOCHE";
    /** Codes of the shift scheme that work in the daytime, as the workbook's shift-to-hours matrix defines them. */
    private static final Set<String> DAY_CODES = Set.of(
            "DIA", "DIA6", "9-6", "12-7", "6-9y5-9", "2-9pm", "6-9am", "2-6pm", "6-9y2-6");

    private final ShiftSaleHistoryRepository history;
    private final HolidayQuery holidays;
    private final IndividualTargetQuery individualTargets;

    public PublicContractsSellerPerformanceParameterSource(
            ShiftSaleHistoryRepository history, HolidayQuery holidays, IndividualTargetQuery individualTargets) {
        this.history = history;
        this.holidays = holidays;
        this.individualTargets = individualTargets;
    }

    @Override
    public Snapshot read(Window window, Measure measure) {
        ShiftSaleHistoryRange range = history.readRange(window.from(), window.to());
        List<ClassifiedSale> classified = classify(range.sales());
        return new Snapshot(targets(window, measure), classifications(classified),
                multipliers(window, measure, range.complete(), classified));
    }

    private List<IndividualTarget> targets(Window window, Measure measure) {
        var salesMeasure = switch (measure) {
            case FUEL_GALLONS -> IndividualTargetQuery.Measure.FUEL_GALLONS;
            case BASKET_COP -> IndividualTargetQuery.Measure.BASKET_COP;
        };
        return individualTargets.findIndividualTargets(window.from(), window.to(), salesMeasure).stream()
                .map(target -> new IndividualTarget(target.workerId(), window, measure, target.amount(), target.source()))
                .toList();
    }

    private record ClassifiedSale(ShiftSaleHistory sale, ShiftPeriod period, DayType dayType) { }

    /** One entry per recorded close; a close whose worked code has no defined period is left out, never guessed. */
    private List<ClassifiedSale> classify(List<ShiftSaleHistory> sales) {
        Map<LocalDate, Boolean> holidayByDate = new HashMap<>();
        List<ClassifiedSale> result = new ArrayList<>();
        for (ShiftSaleHistory sale : sales) {
            periodOf(sale.workedCode()).ifPresent(period -> {
                boolean holiday = holidayByDate.computeIfAbsent(sale.identity().journalDate(), holidays::isHoliday);
                result.add(new ClassifiedSale(sale, period, holiday ? DayType.HOLIDAY : DayType.REGULAR));
            });
        }
        return result;
    }

    private static List<ShiftClassification> classifications(List<ClassifiedSale> classified) {
        return classified.stream()
                .map(item -> new ShiftClassification(
                        item.sale().identity(), item.period(), item.dayType(), CLASSIFICATION_SOURCE))
                .toList();
    }

    /**
     * Multiplier of a period = average of all classified shifts of the window / average of the shifts of that period.
     * The history of a window only exposes sales of processed and confirmed days; a window that is not complete
     * gets no multipliers at all.
     */
    private List<ShiftMultiplier> multipliers(
            Window window, Measure measure, boolean confirmedWindow, List<ClassifiedSale> classified) {
        if (!confirmedWindow || classified.isEmpty()) {
            LOG.info("No multipliers for {}: the window has unconfirmed days or no classified shifts", window);
            return List.of();
        }
        BigDecimal totalSales = sum(classified, measure);
        int totalShifts = classified.size();
        List<ShiftMultiplier> result = new ArrayList<>();
        for (ShiftPeriod period : ShiftPeriod.values()) {
            List<ClassifiedSale> ofPeriod = classified.stream().filter(item -> item.period() == period).toList();
            BigDecimal periodSales = sum(ofPeriod, measure);
            if (ofPeriod.size() < MIN_SHIFTS_PER_PERIOD || periodSales.signum() <= 0 || totalSales.signum() <= 0) {
                LOG.info("No {} multiplier for {} ({}): {} shifts (minimum {}), period sales {}, window sales {}",
                        period, window, measure, ofPeriod.size(), MIN_SHIFTS_PER_PERIOD, periodSales, totalSales);
                continue;
            }
            BigDecimal multiplier = totalSales.multiply(BigDecimal.valueOf(ofPeriod.size()))
                    .divide(periodSales.multiply(BigDecimal.valueOf(totalShifts)), MULTIPLIER_SCALE, RoundingMode.HALF_EVEN);
            if (multiplier.signum() > 0) {
                result.add(new ShiftMultiplier(window, measure, period, multiplier,
                        multiplierSource(window, measure, period, totalShifts, ofPeriod.size())));
            }
        }
        return result;
    }

    private static String multiplierSource(Window window, Measure measure, ShiftPeriod period, int allShifts,
                                           int periodShifts) {
        return "CALCULATED_FROM_QUERIED_WINDOW_HISTORY: average " + measure.unit() + " of all " + allShifts
                + " confirmed shifts / average of the " + periodShifts + " " + period + " shifts, window "
                + window.from() + ".." + window.to() + ", processed days only, minimum "
                + MIN_SHIFTS_PER_PERIOD + " shifts per period";
    }

    private static BigDecimal sum(List<ClassifiedSale> sales, Measure measure) {
        return sales.stream().map(item -> measure.value(item.sale())).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    static Optional<ShiftPeriod> periodOf(String workedCode) {
        if (NIGHT_CODE.equals(workedCode)) {
            return Optional.of(ShiftPeriod.NIGHT);
        }
        return DAY_CODES.contains(workedCode) ? Optional.of(ShiftPeriod.DAY) : Optional.empty();
    }
}
