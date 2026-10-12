package com.edu.unicauca.gasstation.backend.shifts;

import com.edu.unicauca.gasstation.backend.shifts.domain.models.Role;
import com.edu.unicauca.gasstation.backend.shifts.domain.models.ShiftAssignment;
import com.edu.unicauca.gasstation.backend.shifts.domain.models.ShiftCode;
import com.edu.unicauca.gasstation.backend.shifts.domain.models.ShiftPeriod;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class ShiftTestData {

    public static final UUID TITULAR_ROLE_ID = UUID.fromString("7efc5d6e-e6b5-4093-9348-e062a03eb51a");
    public static final UUID SUPPORT_ROLE_ID = UUID.fromString("2bb7696c-c19e-4b0b-b8ac-bc2cd46f8e7e");

    /** The 11 codes of the catalog, with the same hours as ShiftDataInitializer. */
    public static final List<String> CATALOG_CODES = List.of(
            "DIA", "DIA6", "NOCHE", "9-6", "12-7", "6-9y5-9", "2-9pm", "6-9am", "2-6pm", "6-9y2-6", "DESCANSO");

    private static final List<Seed> SEEDS = List.of(
            new Seed("DIA", "Día", true, ShiftPeriod.DAY, 6, 18, null, null, false, null),
            new Seed("DIA6", "Día 6 horas", true, ShiftPeriod.DAY, 6, 12, null, null, true, null),
            new Seed("NOCHE", "Noche", true, ShiftPeriod.NIGHT, 18, 6, null, null, false, null),
            new Seed("9-6", "9am a 6pm", true, ShiftPeriod.DAY, 9, 18, null, null, true, null),
            new Seed("12-7", "12m a 7pm", false, ShiftPeriod.DAY, 12, 19, null, null, false, "DIA6"),
            new Seed("6-9y5-9", "6-9am y 5-9pm", false, ShiftPeriod.DAY, 6, 9, 17, 21, false, "9-6"),
            new Seed("2-9pm", "2pm a 9pm", false, ShiftPeriod.DAY, 14, 21, null, null, false, null),
            new Seed("6-9am", "6am a 9am", false, ShiftPeriod.DAY, 6, 9, null, null, false, null),
            new Seed("2-6pm", "2pm a 6pm", false, ShiftPeriod.DAY, 14, 18, null, null, false, null),
            new Seed("6-9y2-6", "6-9am y 2-6pm", false, ShiftPeriod.DAY, 6, 9, 14, 18, false, null),
            new Seed("DESCANSO", "Descanso", null, null, null, null, null, null, false, null));

    private ShiftTestData() {
    }

    /** TITULAR role, dispenser 1. */
    public static Role titularRole() {
        return new Role(TITULAR_ROLE_ID, "TITULAR", 1);
    }

    /** APOYO role, dispenser 3. */
    public static Role supportRole() {
        return new Role(SUPPORT_ROLE_ID, "APOYO", 3);
    }

    /** Fixed id of a catalog code, so paired codes can point to it. */
    public static UUID shiftCodeId(String code) {
        return UUID.nameUUIDFromBytes(("shift-code-" + code).getBytes(StandardCharsets.UTF_8));
    }

    /** A catalog code with its id, its role (titularRole or supportRole) and its pairing, active. */
    public static ShiftCode shiftCode(String code) {
        Seed seed = seed(code);
        Role role = seed.titular() == null ? null : seed.titular() ? titularRole() : supportRole();
        return build(seed, shiftCodeId(code), role, seed.pairedWith() == null ? null : shiftCodeId(seed.pairedWith()));
    }

    /** A catalog code not saved yet (no id), with the given role and pairing; used by the persistence tests. */
    public static ShiftCode newShiftCode(String code, Role role, UUID pairedWithId) {
        return build(seed(code), null, role, pairedWithId);
    }

    public static ScheduleWorkerInfo worker(UUID id, String fullName, UUID roleId, boolean active) {
        return new ScheduleWorkerInfo(id, fullName, roleId, active);
    }

    /** A new assignment of a catalog code, without note. */
    public static ShiftAssignment assignment(UUID workerId, LocalDate date, String code) {
        return new ShiftAssignment(workerId, date, shiftCode(code), null);
    }

    private static ShiftCode build(Seed seed, UUID id, Role role, UUID pairedWithId) {
        return ShiftCode.builder()
                .id(id)
                .code(seed.code())
                .name(seed.name())
                .role(role)
                .period(seed.period())
                .startHour1(toShort(seed.startHour1()))
                .endHour1(toShort(seed.endHour1()))
                .startHour2(toShort(seed.startHour2()))
                .endHour2(toShort(seed.endHour2()))
                .requiresSupport(seed.requiresSupport())
                .pairedWithId(pairedWithId)
                .active(true)
                .build();
    }

    private static Seed seed(String code) {
        return SEEDS.stream().filter(seed -> seed.code().equals(code)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown shift code " + code));
    }

    private static Short toShort(Integer value) {
        return value == null ? null : value.shortValue();
    }

    /** @param titular true for TITULAR, false for APOYO, null for DESCANSO */
    private record Seed(String code, String name, Boolean titular, ShiftPeriod period,
                        Integer startHour1, Integer endHour1, Integer startHour2, Integer endHour2,
                        boolean requiresSupport, String pairedWith) {
    }
}
