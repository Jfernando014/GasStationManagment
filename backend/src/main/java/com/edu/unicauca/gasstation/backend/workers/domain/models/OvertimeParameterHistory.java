package com.edu.unicauca.gasstation.backend.workers.domain.models;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Versions of the overtime parameters ordered by validity. The only place that decides which version applies to a
 * date: the latest one that started on or before it.
 */
public final class OvertimeParameterHistory {

    private final List<OvertimeParameters> versions;

    private OvertimeParameterHistory(List<OvertimeParameters> versions) {
        this.versions = versions;
    }

    public static OvertimeParameterHistory of(Collection<OvertimeParameters> versions) {
        return new OvertimeParameterHistory(
                versions.stream().sorted(Comparator.comparing(OvertimeParameters::validFrom)).toList());
    }

    /** Version in force on the date; empty if the first version starts later. */
    public Optional<OvertimeParameters> effectiveOn(LocalDate date) {
        return versions.stream().filter(version -> !version.validFrom().isAfter(date)).reduce((older, newer) -> newer);
    }

    /** Version in force on the date, or the first one when the date is before all of them. */
    public OvertimeParameters applyingOn(LocalDate date) {
        return effectiveOn(date).orElseGet(() -> versions.stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("No hay parámetros de horas extra configurados")));
    }

    /** Versions that start after the date, soonest first. */
    public List<OvertimeParameters> scheduledAfter(LocalDate date) {
        return versions.stream().filter(version -> version.validFrom().isAfter(date)).toList();
    }

    /** Every version, newest first. */
    public List<OvertimeParameters> newestFirst() {
        return versions.reversed();
    }

    public boolean isEmpty() {
        return versions.isEmpty();
    }
}
