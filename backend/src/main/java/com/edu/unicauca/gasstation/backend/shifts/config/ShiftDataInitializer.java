package com.edu.unicauca.gasstation.backend.shifts.config;

import com.edu.unicauca.gasstation.backend.shifts.domain.models.Role;
import com.edu.unicauca.gasstation.backend.shifts.domain.models.ShiftCode;
import com.edu.unicauca.gasstation.backend.shifts.domain.models.ShiftPeriod;
import com.edu.unicauca.gasstation.backend.shifts.infrastructure.persistence.RoleRepository;
import com.edu.unicauca.gasstation.backend.shifts.infrastructure.persistence.ShiftCodeRepository;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Loads the 2 roles and 11 shift codes if they do not exist yet (matched by name and code),
 * so it can run on every startup without duplicating rows.
 * Hours come from the "PARAMETROS" sheet (hour/shift matrix) of OBJETIVOS VENDEDORES AGOSTO 2026.
 */
@Component
@RequiredArgsConstructor
class ShiftDataInitializer implements ApplicationRunner {

    private static final String TITULAR = "TITULAR";
    private static final String APOYO = "APOYO";

    private static final Map<String, Integer> ROLES = Map.of(TITULAR, 1, APOYO, 3);

    // Titular codes go first so support codes can resolve the code they pair with.
    // TODO: confirm pairing for 2-9pm, 6-9am, 2-6pm and 6-9y2-6 (not documented in the Excel).
    private static final List<ShiftCodeSeed> SHIFT_CODES = List.of(
            new ShiftCodeSeed("DIA", "Día", TITULAR, ShiftPeriod.DAY, 6, 18, null, null, false, null),
            new ShiftCodeSeed("DIA6", "Día 6 horas", TITULAR, ShiftPeriod.DAY, 6, 12, null, null, true, null),
            new ShiftCodeSeed("NOCHE", "Noche", TITULAR, ShiftPeriod.NIGHT, 18, 6, null, null, false, null),
            new ShiftCodeSeed("9-6", "9am a 6pm", TITULAR, ShiftPeriod.DAY, 9, 18, null, null, true, null),
            new ShiftCodeSeed("12-7", "12m a 7pm", APOYO, ShiftPeriod.DAY, 12, 19, null, null, false, "DIA6"),
            new ShiftCodeSeed("6-9y5-9", "6-9am y 5-9pm", APOYO, ShiftPeriod.DAY, 6, 9, 17, 21, false, "9-6"),
            new ShiftCodeSeed("2-9pm", "2pm a 9pm", APOYO, ShiftPeriod.DAY, 14, 21, null, null, false, null),
            new ShiftCodeSeed("6-9am", "6am a 9am", APOYO, ShiftPeriod.DAY, 6, 9, null, null, false, null),
            new ShiftCodeSeed("2-6pm", "2pm a 6pm", APOYO, ShiftPeriod.DAY, 14, 18, null, null, false, null),
            new ShiftCodeSeed("6-9y2-6", "6-9am y 2-6pm", APOYO, ShiftPeriod.DAY, 6, 9, 14, 18, false, null),
            new ShiftCodeSeed("DESCANSO", "Descanso", null, null, null, null, null, null, false, null));

    private final RoleRepository roleRepository;
    private final ShiftCodeRepository shiftCodeRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        ROLES.forEach((name, dispenser) -> {
            if (roleRepository.findByName(name).isEmpty()) {
                roleRepository.save(new Role(name, dispenser));
            }
        });

        for (ShiftCodeSeed seed : SHIFT_CODES) {
            if (shiftCodeRepository.findByCode(seed.code()).isPresent()) {
                continue;
            }
            shiftCodeRepository.save(ShiftCode.builder()
                    .code(seed.code())
                    .name(seed.name())
                    .role(seed.role() == null ? null : roleRepository.findByName(seed.role()).orElseThrow())
                    .period(seed.period())
                    .startHour1(toShort(seed.startHour1()))
                    .endHour1(toShort(seed.endHour1()))
                    .startHour2(toShort(seed.startHour2()))
                    .endHour2(toShort(seed.endHour2()))
                    .requiresSupport(seed.requiresSupport())
                    .pairedWithId(seed.pairedWith() == null
                            ? null
                            : shiftCodeRepository.findByCode(seed.pairedWith()).orElseThrow().getId())
                    .active(true)
                    .build());
        }
    }

    private static Short toShort(Integer value) {
        return value == null ? null : value.shortValue();
    }

    private record ShiftCodeSeed(
            String code,
            String name,
            String role,
            ShiftPeriod period,
            Integer startHour1,
            Integer endHour1,
            Integer startHour2,
            Integer endHour2,
            boolean requiresSupport,
            String pairedWith) {
    }
}
