package com.edu.unicauca.gasstation.backend.statistics.application;

import com.edu.unicauca.gasstation.backend.statistics.domain.calculation.SellerPerformance.*;
import com.edu.unicauca.gasstation.backend.statistics.domain.calculation.SellerPerformanceCalculator;
import com.edu.unicauca.gasstation.backend.statistics.domain.models.ShiftSaleHistoryRange;
import com.edu.unicauca.gasstation.backend.statistics.domain.ports.SellerPerformanceParameterSource;
import com.edu.unicauca.gasstation.backend.statistics.domain.ports.SellerPerformanceParameterSource.Snapshot;
import com.edu.unicauca.gasstation.backend.statistics.domain.repositories.ShiftSaleHistoryRepository;
import com.edu.unicauca.gasstation.backend.statistics.exception.SellerPerformanceSourceException;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static com.edu.unicauca.gasstation.backend.statistics.domain.calculation.SellerPerformance.validateScale;

/**
 * Read-only application use cases, internal to statistics. Inputs are already sourced parameters, not sales.
 * Automatic reports use an optional internal parameter source; no HTTP input supplies sales or parameters.
 * Empty lists explicitly leave those calculations unavailable; storage failures propagate instead of becoming zero.
 */
@Service
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class SellerPerformanceService {
    private final ShiftSaleHistoryRepository history;
    private final SellerPerformanceCalculator calculator = new SellerPerformanceCalculator();

    private final SellerPerformanceParameterSource parameters;

    public SellerPerformanceService(ShiftSaleHistoryRepository history) { this(history, Optional.empty()); }

    @Autowired
    public SellerPerformanceService(ShiftSaleHistoryRepository history,
                                    Optional<SellerPerformanceParameterSource> parameters) {
        this.history = Objects.requireNonNull(history);
        this.parameters = parameters.orElse((window, measure) -> Snapshot.unavailable());
    }

    /** Queries sources once, reuses the existing engines, and performs no registration or recovery writes. */
    public Report report(Window window, Measure measure, int scale) {
        Objects.requireNonNull(window, "window");
        Objects.requireNonNull(measure, "measure");
        validateScale(scale);
        try {
            var observations = read(window, measure, scale);
            // Missing history/observations already prevent all calculations. Do not consult unrelated sources.
            var inputs = observations.complete() && !observations.sales().isEmpty()
                    && observations.sales().stream().noneMatch(measure::containsAdjustment)
                    ? Objects.requireNonNull(parameters.read(window, measure), "performance parameters response")
                    : Snapshot.unavailable();
            return new Report("CONFIRMED_SHIFT_SALES_HISTORY", measure.unit(), scale, RoundingMode.HALF_EVEN.name(),
                    calculator.compliance(observations, measure, inputs.targets(), scale),
                    calculator.compare(observations, measure, inputs.classifications(), scale),
                    calculator.normalize(observations, measure, inputs.classifications(), inputs.multipliers(), scale));
        } catch (RuntimeException failure) {
            throw new SellerPerformanceSourceException(failure);
        }
    }

    public ComplianceReport compliance(Window window, Measure measure, List<IndividualTarget> targets, int scale) {
        Objects.requireNonNull(targets, "targets");
        return calculator.compliance(read(window, measure, scale), measure, targets, scale);
    }

    public ComparisonReport compare(Window window, Measure measure, List<ShiftClassification> classifications, int scale) {
        Objects.requireNonNull(classifications, "classifications");
        return calculator.compare(read(window, measure, scale), measure, classifications, scale);
    }

    public NormalizationReport normalize(Window window, Measure measure, List<ShiftClassification> classifications,
                                         List<ShiftMultiplier> multipliers, int scale) {
        Objects.requireNonNull(classifications, "classifications");
        Objects.requireNonNull(multipliers, "multipliers");
        return calculator.normalize(read(window, measure, scale), measure, classifications, multipliers, scale);
    }

    private ShiftSaleHistoryRange read(Window window, Measure measure, int scale) {
        Objects.requireNonNull(window, "window");
        Objects.requireNonNull(measure, "measure");
        validateScale(scale);
        var result = Objects.requireNonNull(history.readRange(window.from(), window.to()), "history response");
        if (!window.from().equals(result.from()) || !window.to().equals(result.to())) {
            throw new IllegalStateException("History returned a different operational-date range");
        }
        return result;
    }
}
