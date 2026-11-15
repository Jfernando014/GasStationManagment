package com.edu.unicauca.gasstation.backend.workers.domain.models;

import java.time.LocalDate;
import java.util.ArrayList;
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

    /** Every version, newest first, each with the day it stops applying and whether it is the one in force today. */
    public List<OvertimeParameterVersion> versionsNewestFirst(LocalDate today) {
        OvertimeParameters inForce = effectiveOn(today).orElse(null);
        List<OvertimeParameterVersion> result = new ArrayList<>();
        for (int i = versions.size() - 1; i >= 0; i--) {
            OvertimeParameters version = versions.get(i);
            LocalDate validTo = i + 1 < versions.size() ? versions.get(i + 1).validFrom().minusDays(1) : null;
            result.add(new OvertimeParameterVersion(version, validTo, version == inForce));
        }
        return List.copyOf(result);
    }

    /** Every version, newest first. */
    public List<OvertimeParameters> newestFirst() {
        return versions.reversed();
    }
}
