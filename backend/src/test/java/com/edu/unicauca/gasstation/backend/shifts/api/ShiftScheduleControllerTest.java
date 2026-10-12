package com.edu.unicauca.gasstation.backend.shifts.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.edu.unicauca.gasstation.backend.security.infrastructure.jwt.JwtAuthenticationFilter;
import com.edu.unicauca.gasstation.backend.shifts.ShiftTestData;
import com.edu.unicauca.gasstation.backend.shifts.domain.models.DailyCoverage;
import com.edu.unicauca.gasstation.backend.shifts.domain.models.ScheduleChange;
import com.edu.unicauca.gasstation.backend.shifts.domain.models.ScheduleView;
import com.edu.unicauca.gasstation.backend.shifts.domain.models.ShiftPeriod;
import com.edu.unicauca.gasstation.backend.shifts.domain.models.WorkerShifts;
import com.edu.unicauca.gasstation.backend.shifts.domain.services.ShiftScheduleService;
import com.edu.unicauca.gasstation.backend.shifts.exception.PastDateException;
import com.edu.unicauca.gasstation.backend.shifts.exception.RotationNotAllowedException;
import com.edu.unicauca.gasstation.backend.shifts.exception.ShiftRoleMismatchException;
import com.edu.unicauca.gasstation.backend.shifts.exception.WorkerNotAvailableException;
import com.edu.unicauca.gasstation.backend.shifts.infrastructure.mappers.ShiftScheduleMapperImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(
        controllers = ShiftScheduleController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class))
@AutoConfigureMockMvc(addFilters = false)
@Import(ShiftScheduleMapperImpl.class)
@TestPropertySource(properties = "server.port=0")
class ShiftScheduleControllerTest {

    private static final String BASE_URL = "/api/shifts/schedule";
    private static final UUID WORKER_ID = UUID.fromString("d3c8c22b-9a86-4678-a326-ac90b26057cb");
    private static final LocalDate START = LocalDate.of(2026, 11, 2);
    private static final LocalDate END = LocalDate.of(2026, 11, 15);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ShiftScheduleService shiftScheduleService;

    // ---- POST /rotation ----

    @Test
    void generateRotationPassesTheBodyToTheServiceAndReturnsTheResult() throws Exception {
        when(shiftScheduleService.generateRotation(WORKER_ID, START, END))
                .thenReturn(new ScheduleChange(14, List.of(START)));

        postRotation(rotationBody(WORKER_ID, START.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saved").value(14))
                .andExpect(jsonPath("$.uncoveredDates[0]").value("2026-11-02"));
    }

    @Test
    void generateRotationRejectsAMissingWorker() throws Exception {
        postRotation(rotationBody(null, START.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].field").value("workerId"))
                .andExpect(jsonPath("$.errors[0].message").value("El vendedor es obligatorio"));
        verifyNoInteractions(shiftScheduleService);
    }

    @Test
    void generateRotationRejectsADateInAnotherFormat() throws Exception {
        postRotation(rotationBody(WORKER_ID, "01/11/2026"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
        verifyNoInteractions(shiftScheduleService);
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("serviceErrors")
    void generateRotationTranslatesServiceErrors(RuntimeException error, int status, String code) throws Exception {
        when(shiftScheduleService.generateRotation(any(), any(), any())).thenThrow(error);

        postRotation(rotationBody(WORKER_ID, START.toString()))
                .andExpect(status().is(status))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.detail").value(error.getMessage()));
    }

    // ---- PUT /assignments ----

    @Test
    void assignPassesWorkerDateCodeAndNoteToTheService() throws Exception {
        when(shiftScheduleService.assign(WORKER_ID, START, "12-7", "Reemplazo"))
                .thenReturn(new ScheduleChange(1, List.of()));

        putAssignment(assignBody("12-7", "Reemplazo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saved").value(1));

        verify(shiftScheduleService).assign(WORKER_ID, START, "12-7", "Reemplazo");
    }

    @Test
    void assignRejectsANoteLongerThan255Characters() throws Exception {
        putAssignment(assignBody("DIA", "a".repeat(256)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("note"))
                .andExpect(jsonPath("$.errors[0].message").value("La nota no puede superar 255 caracteres"));
        verifyNoInteractions(shiftScheduleService);
    }

    @Test
    void assignReturnsConflictWhenTheRoleDoesNotMatch() throws Exception {
        when(shiftScheduleService.assign(any(), any(), anyString(), any()))
                .thenThrow(new ShiftRoleMismatchException("DIA", "TITULAR"));

        putAssignment(assignBody("DIA", null))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ROLE_MISMATCH"))
                .andExpect(jsonPath("$.detail")
                        .value("El turno DIA es para el rol TITULAR y no corresponde al rol del vendedor"));
    }

    // ---- GET ----

    @Test
    void getSchedulePassesTheFiltersAndReturnsWorkersAndCoverage() throws Exception {
        when(shiftScheduleService.getSchedule(START, START, WORKER_ID, ShiftPeriod.DAY)).thenReturn(new ScheduleView(
                START, START,
                List.of(new WorkerShifts(WORKER_ID, "Ana Villamil",
                        List.of(ShiftTestData.assignment(WORKER_ID, START, "DIA6")))),
                List.of(new DailyCoverage(START, true, false, false))));

        mockMvc.perform(get(BASE_URL)
                        .param("from", "2026-11-02").param("to", "2026-11-02")
                        .param("workerId", WORKER_ID.toString()).param("period", "DAY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workers[0].fullName").value("Ana Villamil"))
                .andExpect(jsonPath("$.workers[0].days[0].date").value("2026-11-02"))
                .andExpect(jsonPath("$.workers[0].days[0].shiftCode").value("DIA6"))
                .andExpect(jsonPath("$.coverage[0].dayCovered").value(true))
                .andExpect(jsonPath("$.coverage[0].covered").value(false));
    }

    @Test
    void getScheduleRejectsAnUnknownPeriod() throws Exception {
        mockMvc.perform(get(BASE_URL).param("from", "2026-11-02").param("to", "2026-11-15").param("period", "OTRO"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"))
                .andExpect(jsonPath("$.detail").value("El valor del parámetro 'period' no es válido"));
        verifyNoInteractions(shiftScheduleService);
    }

    @Test
    void getScheduleRejectsAMissingDateWithoutCode() throws Exception {
        // Spring's default handler answers this case: problem details without the "code" property
        mockMvc.perform(get(BASE_URL).param("to", "2026-11-15"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").doesNotExist());
        verifyNoInteractions(shiftScheduleService);
    }

    // ---- sources and helpers ----

    static Stream<Arguments> serviceErrors() {
        return Stream.of(
                Arguments.of(new PastDateException(START), 400, "PAST_DATE"),
                Arguments.of(new WorkerNotAvailableException(WORKER_ID), 409, "WORKER_INACTIVE"),
                Arguments.of(new RotationNotAllowedException(), 409, "ROTATION_NOT_ALLOWED"));
    }

    private static Map<String, Object> rotationBody(UUID workerId, String startDate) {
        Map<String, Object> body = new HashMap<>();
        body.put("workerId", workerId);
        body.put("startDate", startDate);
        body.put("endDate", END.toString());
        return body;
    }

    private static Map<String, Object> assignBody(String shiftCode, String note) {
        Map<String, Object> body = new HashMap<>();
        body.put("workerId", WORKER_ID);
        body.put("date", START.toString());
        body.put("shiftCode", shiftCode);
        body.put("note", note);
        return body;
    }

    private ResultActions postRotation(Map<String, Object> body) throws Exception {
        return mockMvc.perform(post(BASE_URL + "/rotation")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    private ResultActions putAssignment(Map<String, Object> body) throws Exception {
        return mockMvc.perform(put(BASE_URL + "/assignments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }
}
