package com.edu.unicauca.gasstation.backend.shifts.infrastructure.mappers;

import static com.edu.unicauca.gasstation.backend.shifts.ShiftTestData.SUPPORT_ROLE_ID;
import static org.assertj.core.api.Assertions.assertThat;

import com.edu.unicauca.gasstation.backend.shifts.ShiftTestData;
import com.edu.unicauca.gasstation.backend.shifts.api.dtos.RoleResponse;
import com.edu.unicauca.gasstation.backend.shifts.api.dtos.ShiftCodeResponse;
import com.edu.unicauca.gasstation.backend.shifts.api.dtos.ShiftSchedule;
import java.util.List;
import org.junit.jupiter.api.Test;

class ShiftCodeMapperTest {

    private final ShiftCodeMapper mapper = new ShiftCodeMapperImpl();

    @Test
    void codeWithRoleBecomesResponseWithScheduleAndHours() {
        ShiftCodeResponse response = mapper.toResponse(ShiftTestData.shiftCode("12-7"));

        assertThat(response).isEqualTo(new ShiftCodeResponse(
                ShiftTestData.shiftCodeId("12-7"), "12-7", "12m a 7pm",
                new RoleResponse(SUPPORT_ROLE_ID, "APOYO", 3), "DAY",
                new ShiftSchedule(List.of(new ShiftSchedule.Segment(12, 19)), 7),
                false, ShiftTestData.shiftCodeId("DIA6"), true));
        assertThat(mapper.toResponse(ShiftTestData.shiftCode("NOCHE")).schedule())
                .as("a night segment keeps an end lower than its start")
                .isEqualTo(new ShiftSchedule(List.of(new ShiftSchedule.Segment(18, 6)), 12));
    }

    @Test
    void descansoHasNoRoleNoPeriodAndNoSegments() {
        ShiftCodeResponse response = mapper.toResponse(ShiftTestData.shiftCode("DESCANSO"));

        assertThat(response.role()).isNull();
        assertThat(response.period()).isNull();
        assertThat(response.schedule()).isEqualTo(new ShiftSchedule(List.of(), 0));
    }
}
