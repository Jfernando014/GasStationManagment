package com.edu.unicauca.gasstation.backend.sales;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Public query for the individual sales targets of the sellers, for other modules (Statistics).
 *
 * <p>A target is assigned for a whole calendar month: the monthly station target is divided among the sellers with
 * the latest registered distribution profile, using the effective day staff of every day of the month. A period that
 * is not exactly one calendar month is never prorated, so it has no targets.
 */
public interface IndividualTargetQuery {

    /** Metric of a target: fuel gallons or basket pesos. */
    enum Measure { FUEL_GALLONS, BASKET_COP }

    /**
     * Individual targets of the inclusive period for the metric.
     *
     * @return one target per seller, or an empty list when the period is not exactly one calendar month, the month
     *         has no registered target for the metric, there is no distribution profile, or the staff of the month
     *         cannot be resolved. Empty never means a zero target.
     */
    List<IndividualTargetInfo> findIndividualTargets(LocalDate from, LocalDate to, Measure measure);

    /**
     * @param workerId worker identifier, the same as in the {@code workers} module
     * @param amount   target of the worker for the whole month, in gallons or pesos according to the metric
     * @param source   provenance of the figure: the target, the profile and the way the staff was taken
     */
    record IndividualTargetInfo(UUID workerId, BigDecimal amount, String source) {}
}
