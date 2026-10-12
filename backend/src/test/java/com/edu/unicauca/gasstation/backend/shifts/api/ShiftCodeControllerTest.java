package com.edu.unicauca.gasstation.backend.shifts.api;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.edu.unicauca.gasstation.backend.security.infrastructure.jwt.JwtAuthenticationFilter;
import com.edu.unicauca.gasstation.backend.shifts.ShiftTestData;
import com.edu.unicauca.gasstation.backend.shifts.domain.services.ShiftCatalogService;
import com.edu.unicauca.gasstation.backend.shifts.exception.ShiftCodeNotFoundException;
import com.edu.unicauca.gasstation.backend.shifts.infrastructure.mappers.ShiftCodeMapperImpl;
import java.util.List;
import org.junit.jupiter.api.Test;
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

@WebMvcTest(
        controllers = ShiftCodeController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class))
@AutoConfigureMockMvc(addFilters = false)
@Import(ShiftCodeMapperImpl.class)
@TestPropertySource(properties = "server.port=0")
class ShiftCodeControllerTest {

    private static final String BASE_URL = "/api/shifts/catalog";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ShiftCatalogService shiftCatalogService;

    @Test
    void listReturnsEachCodeWithRoleAndScheduleAndDescansoWithout() throws Exception {
        when(shiftCatalogService.listActive()).thenReturn(List.of(
                ShiftTestData.shiftCode("DIA6"), ShiftTestData.shiftCode("DESCANSO")));

        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].role.name").value("TITULAR"))
                .andExpect(jsonPath("$[0].role.dispenser").value(1))
                .andExpect(jsonPath("$[0].schedule.segments[0].startHour").value(6))
                .andExpect(jsonPath("$[0].schedule.segments[0].endHour").value(12))
                .andExpect(jsonPath("$[0].schedule.totalHours").value(6))
                .andExpect(jsonPath("$[1].role").value(nullValue()))
                .andExpect(jsonPath("$[1].schedule.segments", empty()));
    }

    @Test
    void getByCodeAcceptsCodesWithHyphens() throws Exception {
        when(shiftCatalogService.getByCode("6-9y5-9")).thenReturn(ShiftTestData.shiftCode("6-9y5-9"));

        mockMvc.perform(get(BASE_URL + "/6-9y5-9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("6-9y5-9"))
                .andExpect(jsonPath("$.schedule.totalHours").value(7));
    }

    @Test
    void getByCodeReturnsNotFoundAsProblemDetails() throws Exception {
        when(shiftCatalogService.getByCode("XYZ")).thenThrow(new ShiftCodeNotFoundException("XYZ"));

        mockMvc.perform(get(BASE_URL + "/XYZ"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("SHIFT_CODE_NOT_FOUND"))
                .andExpect(jsonPath("$.detail").value("No se encontró el código de turno 'XYZ'"));
    }
}
