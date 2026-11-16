package com.edu.unicauca.gasstation.backend.statistics.application;

import com.edu.unicauca.gasstation.backend.shifts.holidays.HolidayQuery;
import com.edu.unicauca.gasstation.backend.statistics.domain.calculation.WorkerPerformance;
import com.edu.unicauca.gasstation.backend.statistics.domain.calculation.WorkerPerformance.Report;
import com.edu.unicauca.gasstation.backend.statistics.domain.calculation.WorkerPerformanceCalculator;
import com.edu.unicauca.gasstation.backend.statistics.domain.models.ShiftSaleHistory;
import com.edu.unicauca.gasstation.backend.statistics.domain.repositories.ShiftSaleHistoryRepository;
import com.edu.unicauca.gasstation.backend.statistics.exception.SellerPerformanceSourceException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read-only application use case computing a worker's historical performance from confirmed
 * {@code statistics_shift_sale} records and holiday metadata.
 */
@Service
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class WorkerPerformanceService {
    private final ShiftSaleHistoryRepository history;
    private final HolidayQuery holidayQuery;
    private final WorkerPerformanceCalculator calculator = new WorkerPerformanceCalculator();

    public WorkerPerformanceService(ShiftSaleHistoryRepository history, HolidayQuery holidayQuery) {
        this.history = Objects.requireNonNull(history, "history");
        this.holidayQuery = Objects.requireNonNull(holidayQuery, "holidayQuery");
    }

    public Report report(UUID workerId) {
        WorkerPerformance.validateWorker(workerId);
        try {
            List<ShiftSaleHistory> sales = Objects.requireNonNull(history.findAllProcessed(), "history response");
            Map<YearMonth, Set<LocalDate>> holidaysByMonth = new HashMap<>();
            Predicate<LocalDate> isHoliday = date -> holidaysByMonth
                    .computeIfAbsent(YearMonth.from(date), month -> Set.copyOf(Objects.requireNonNull(
                            holidayQuery.getHolidays(month.atDay(1), month.atEndOfMonth()),
                            "holidays response")))
                    .contains(date);
            return calculator.calculate(workerId, sales, isHoliday);
        } catch (RuntimeException failure) {
            throw new SellerPerformanceSourceException(failure);
        }
    }
}
