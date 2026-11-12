package com.edu.unicauca.gasstation.backend.workers.domain.services;

import com.edu.unicauca.gasstation.backend.shifts.catalog.ShiftCodeInfo;
import com.edu.unicauca.gasstation.backend.shifts.catalog.ShiftExternalService;
import com.edu.unicauca.gasstation.backend.workers.domain.models.OvertimeParameterHistory;
import com.edu.unicauca.gasstation.backend.workers.domain.models.OvertimeParameterOverview;
import com.edu.unicauca.gasstation.backend.workers.domain.models.OvertimeParameters;
import com.edu.unicauca.gasstation.backend.workers.domain.repositories.HoursSyncRepository;
import com.edu.unicauca.gasstation.backend.workers.domain.repositories.OvertimeParameterRepository;
import com.edu.unicauca.gasstation.backend.workers.exception.ParameterOutOfRangeException;
import com.edu.unicauca.gasstation.backend.workers.exception.ParameterValidFromException;
import com.edu.unicauca.gasstation.backend.workers.exception.ParameterVersionExistsException;
import com.edu.unicauca.gasstation.backend.workers.exception.UnknownParameterRoleException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Versions of the overtime parameters. A change is a new version with a validity date; versions are never edited or
 * deleted, and a new one must start after the last day that has hours kept, so a day already used keeps its values.
 * "Today" is Bogotá time.
 */
@Service
@RequiredArgsConstructor
public class OvertimeParameterService {

    private final OvertimeParameterRepository repository;
    private final HoursSyncRepository syncRepository;
    private final ShiftExternalService shiftExternalService;
    private final Clock clock;

    /** The version in force on {@code date} (today if null), the scheduled ones after it and the history. */
    public OvertimeParameterOverview overview(LocalDate date) {
        LocalDate day = date != null ? date : LocalDate.now(clock);
        OvertimeParameterHistory history = OvertimeParameterHistory.of(repository.findAll());
        return new OvertimeParameterOverview(history.applyingOn(day), history.scheduledAfter(day),
                history.newestFirst());
    }

    /**
     * Creates a new version from {@code validFrom}.
     *
     * @param fixedHedoRoles roles with the fixed overtime switch on; every other role has it off
     */
    public OvertimeParameters create(LocalDate validFrom, int nightStartHour, int restHours, int restMinShiftHours,
                                     int fixedHedoHours, Collection<String> fixedHedoRoles, String createdBy) {
        Objects.requireNonNull(validFrom, "validFrom");
        requireInRange("nightStartHour", nightStartHour, OvertimeParameters.MIN_NIGHT_START_HOUR,
                OvertimeParameters.MAX_NIGHT_START_HOUR);
        requireInRange("restHours", restHours, 0, OvertimeParameters.MAX_REST_HOURS);
        requireInRange("restMinShiftHours", restMinShiftHours, OvertimeParameters.MIN_REST_MIN_SHIFT_HOURS,
                OvertimeParameters.MAX_REST_MIN_SHIFT_HOURS);
        requireInRange("fixedHedoHours", fixedHedoHours, 0, OvertimeParameters.MAX_FIXED_HEDO_HOURS);
        Set<String> roles = knownRoles(fixedHedoRoles);
        syncRepository.findLastDate().filter(last -> !validFrom.isAfter(last)).ifPresent(last -> {
            throw new ParameterValidFromException(last);
        });
        if (repository.findAll().stream().anyMatch(version -> version.validFrom().equals(validFrom))) {
            throw new ParameterVersionExistsException(validFrom);
        }
        return repository.save(new OvertimeParameters(null, validFrom, nightStartHour, restHours,
                restMinShiftHours, fixedHedoHours, roles, Instant.now(clock), createdBy));
    }

    private Set<String> knownRoles(Collection<String> requested) {
        if (requested == null || requested.isEmpty()) {
            return Set.of();
        }
        Set<String> catalogRoles = shiftExternalService.getActiveCatalog().stream()
                .map(ShiftCodeInfo::role)
                .filter(Objects::nonNull)
                .map(role -> role.name())
                .collect(Collectors.toSet());
        Set<String> roles = new LinkedHashSet<>();
        for (String role : requested) {
            String name = role == null ? "" : role.strip();
            if (!catalogRoles.contains(name)) {
                throw new UnknownParameterRoleException(name);
            }
            roles.add(name);
        }
        return roles;
    }

    private static void requireInRange(String name, int value, int min, int max) {
        if (value < min || value > max) {
            throw new ParameterOutOfRangeException(name, min, max);
        }
    }
}
