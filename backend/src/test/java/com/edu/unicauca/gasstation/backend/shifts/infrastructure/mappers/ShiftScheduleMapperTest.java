package com.edu.unicauca.gasstation.backend.shifts.infrastructure.mappers;

import static org.assertj.core.api.Assertions.assertThat;

import com.edu.unicauca.gasstation.backend.shifts.ShiftTestData;
import com.edu.unicauca.gasstation.backend.shifts.api.dtos.DayCoverage;
import com.edu.unicauca.gasstation.backend.shifts.api.dtos.ScheduleDay;
import com.edu.unicauca.gasstation.backend.shifts.api.dtos.ScheduleResponse;
import com.edu.unicauca.gasstation.backend.shifts.api.dtos.ScheduleUpdateResponse;
import com.edu.unicauca.gasstation.backend.shifts.api.dtos.WorkerSchedule;
import com.edu.unicauca.gasstation.backend.shifts.domain.models.DailyCoverage;
import com.edu.unicauca.gasstation.backend.shifts.domain.models.ScheduleChange;
import com.edu.unicauca.gasstation.backend.shifts.domain.models.ScheduleView;
import com.edu.unicauca.gasstation.backend.shifts.domain.models.ShiftAssignment;
import com.edu.unicauca.gasstation.backend.shifts.domain.models.WorkerShifts;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ShiftScheduleMapperTest {

    private static final UUID WORKER_ID = UUID.fromString("d3c8c22b-9a86-4678-a326-ac90b26057cb");
    private static final LocalDate DAY_1 = LocalDate.of(2026, 11, 2);
    private static final LocalDate DAY_2 = DAY_1.plusDays(1);

    private final ShiftScheduleMapper mapper = new ShiftScheduleMapperImpl();

    @Test
    void scheduleChangeBecomesUpdateResponse() {
        assertThat(mapper.toResponse(new ScheduleChange(14, List.of(DAY_1, DAY_2))))
                .isEqualTo(new ScheduleUpdateResponse(14, List.of(DAY_1, DAY_2)));
    }

    @Test
    void scheduleViewBecomesResponseWithDaysAndCalculatedCoverage() {
        ScheduleView view = new ScheduleView(DAY_1, DAY_2,
                List.of(new WorkerShifts(WORKER_ID, "Ana Villamil", List.of(
                        ShiftTestData.assignment(WORKER_ID, DAY_1, "NOCHE"),
                        new ShiftAssignment(WORKER_ID, DAY_2, ShiftTestData.shiftCode("DIA6"), "Cita médica")))),
                List.of(new DailyCoverage(DAY_1, true, true, true), new DailyCoverage(DAY_2, true, false, true)));

        ScheduleResponse response = mapper.toResponse(view);

        assertThat(response).isEqualTo(new ScheduleResponse(DAY_1, DAY_2,
                List.of(new WorkerSchedule(WORKER_ID, "Ana Villamil", List.of(
                        new ScheduleDay(DAY_1, "NOCHE", null),
                        new ScheduleDay(DAY_2, "DIA6", "Cita médica")))),
                List.of(new DayCoverage(DAY_1, true, true, true, true), new DayCoverage(DAY_2, true, false, true, false))));
    }
}
