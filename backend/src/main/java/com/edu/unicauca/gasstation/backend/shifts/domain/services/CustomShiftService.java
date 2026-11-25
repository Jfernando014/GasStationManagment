package com.edu.unicauca.gasstation.backend.shifts.domain.services;

import com.edu.unicauca.gasstation.backend.shifts.catalog.CustomShiftCode;
import com.edu.unicauca.gasstation.backend.shifts.catalog.ShiftCodeInfo;
import com.edu.unicauca.gasstation.backend.shifts.domain.models.Role;
import com.edu.unicauca.gasstation.backend.shifts.domain.models.ShiftCode;
import com.edu.unicauca.gasstation.backend.shifts.domain.models.ShiftPeriod;
import com.edu.unicauca.gasstation.backend.shifts.domain.models.ShiftSegment;
import com.edu.unicauca.gasstation.backend.shifts.domain.repositories.RoleRepository;
import com.edu.unicauca.gasstation.backend.shifts.domain.repositories.ShiftCodeRepository;
import com.edu.unicauca.gasstation.backend.shifts.exception.ShiftError;
import com.edu.unicauca.gasstation.backend.shifts.exception.ShiftException;
import com.edu.unicauca.gasstation.backend.shifts.spi.ScheduleWorkerInfo;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Turns the hours typed for a support worker into a shift code: the standard support code with exactly those hours when
 * there is one, so it behaves the same as choosing it; otherwise a custom code, created the first time those hours are
 * used and reused afterwards.
 */
@Service
@RequiredArgsConstructor
public class CustomShiftService {

    private final ShiftCodeRepository shiftCodeRepository;
    private final RoleRepository roleRepository;

    /**
     * @param hours one or two blocks of whole hours of the same day, in order
     * @throws ShiftException {@code HOURS_ONLY_FOR_SUPPORT} if the worker is not a support worker
     * @throws ShiftException {@code INVALID_SHIFT_HOURS} if the hours do not make a valid shift
     */
    @Transactional
    public ShiftCode codeFor(ScheduleWorkerInfo worker, List<ShiftSegment> hours) {
        Role support = roleRepository.findByName(ScheduleCoverageCalculator.APOYO)
                .orElseThrow(() -> new IllegalStateException("The support role does not exist"));
        if (!support.getId().equals(worker.roleId())) {
            throw new ShiftException(ShiftError.HOURS_ONLY_FOR_SUPPORT);
        }
        requireValid(hours);
        return standardCode(support, hours).orElseGet(() -> customCode(support, hours));
    }

    private Optional<ShiftCode> standardCode(Role support, List<ShiftSegment> hours) {
        return shiftCodeRepository.findActive().stream()
                .filter(code -> !code.isCustom()
                        && code.getRole() != null
                        && code.getRole().getId().equals(support.getId())
                        && code.segments().equals(hours))
                .findFirst();
    }

    private ShiftCode customCode(Role support, List<ShiftSegment> hours) {
        List<ShiftCodeInfo.Segment> segments = hours.stream()
                .map(hour -> new ShiftCodeInfo.Segment(hour.startHour(), hour.endHour()))
                .toList();
        String code = CustomShiftCode.of(segments);
        return shiftCodeRepository.findByCode(code).orElseGet(() -> {
            ShiftSegment first = hours.get(0);
            ShiftSegment second = hours.size() > 1 ? hours.get(1) : null;
            return shiftCodeRepository.save(ShiftCode.builder()
                    .code(code)
                    .name(name(hours))
                    .role(support)
                    .period(CustomShiftCode.isNight(segments) ? ShiftPeriod.NIGHT : ShiftPeriod.DAY)
                    .startHour1((short) first.startHour())
                    .endHour1((short) first.endHour())
                    .startHour2(second == null ? null : (short) second.startHour())
                    .endHour2(second == null ? null : (short) second.endHour())
                    .active(true)
                    .custom(true)
                    .build());
        });
    }

    private static String name(List<ShiftSegment> hours) {
        return hours.stream()
                .map(hour -> "%02d:00-%02d:00".formatted(hour.startHour(), hour.endHour()))
                .collect(Collectors.joining(" y ", "Apoyo ", ""));
    }

    /** One or two blocks of the same day, the second after a pause; a single block cannot take the whole day. */
    private static void requireValid(List<ShiftSegment> hours) {
        if (hours.isEmpty() || hours.size() > 2) {
            throw invalid("Indique uno o dos rangos de horas");
        }
        int previousEnd = -1;
        for (ShiftSegment hour : hours) {
            if (hour.startHour() < 0 || hour.endHour() > 24 || hour.endHour() <= hour.startHour()) {
                throw invalid("Cada rango debe empezar entre las 0 y las 23 y terminar después, el mismo día, "
                        + "a más tardar a las 24");
            }
            if (hour.startHour() <= previousEnd) {
                throw invalid("El segundo rango debe empezar después de que termine el primero");
            }
            previousEnd = hour.endHour();
        }
        if (hours.get(0).startHour() == 0 && hours.get(0).endHour() == 24) {
            throw invalid("Un turno no puede cubrir las 24 horas del día");
        }
    }

    private static ShiftException invalid(String message) {
        return new ShiftException(ShiftError.INVALID_SHIFT_HOURS, message);
    }
}
