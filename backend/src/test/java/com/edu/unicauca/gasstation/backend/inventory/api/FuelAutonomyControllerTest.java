package com.edu.unicauca.gasstation.backend.inventory.api;

import com.edu.unicauca.gasstation.backend.inventory.FuelType;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.AutonomyLevel;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.AutonomyStatus;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.FuelAutonomy;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.TankStock;
import com.edu.unicauca.gasstation.backend.inventory.domain.services.FuelAutonomyService;
import com.edu.unicauca.gasstation.backend.inventory.infrastructure.mappers.FuelAutonomyMapperImpl;
import com.edu.unicauca.gasstation.backend.security.infrastructure.jwt.JwtAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = FuelAutonomyController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class))
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = "server.port=0")
@Import(FuelAutonomyMapperImpl.class)
class FuelAutonomyControllerTest {

    private static final String URL = "/api/v1/inventory/autonomy";
    private static final LocalDate DATE = LocalDate.of(2026, 8, 5);

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FuelAutonomyService autonomyService;

    @Test
    void returnsTheAutonomyOfEachFuelWithItsTanks() throws Exception {
        when(autonomyService.calculate(DATE)).thenReturn(List.of(calculated(), notCalculated()));

        mockMvc.perform(get(URL).param("journalDate", "2026-08-05"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].fuelType").value("MOTOR"))
                .andExpect(jsonPath("$[0].status").value("CALCULATED"))
                .andExpect(jsonPath("$[0].stockGallons").value(3000.0))
                .andExpect(jsonPath("$[0].expectedDailySalesGallons").value(700.0))
                .andExpect(jsonPath("$[0].autonomyDays").value(4.29))
                .andExpect(jsonPath("$[0].level").value("WARNING"))
                .andExpect(jsonPath("$[0].warningDays").value(5.5))
                .andExpect(jsonPath("$[0].criticalDays").value(2.5))
                .andExpect(jsonPath("$[0].tanks.length()").value(1))
                .andExpect(jsonPath("$[0].tanks[0].tankId").value(1))
                .andExpect(jsonPath("$[0].tanks[0].tankCode").value("T1C1"))
                .andExpect(jsonPath("$[0].tanks[0].heightCm").value(100.5))
                .andExpect(jsonPath("$[0].tanks[0].gallons").value(1800.25));
    }

    @Test
    void rowsThatCannotBeCalculatedHaveNoDaysAndAreNotEvaluated() throws Exception {
        when(autonomyService.calculate(DATE)).thenReturn(List.of(calculated(), notCalculated()));

        mockMvc.perform(get(URL).param("journalDate", "2026-08-05"))
                .andExpect(jsonPath("$[1].fuelType").value("DIESEL"))
                .andExpect(jsonPath("$[1].status").value("NO_SALES_HISTORY"))
                .andExpect(jsonPath("$[1].autonomyDays").doesNotExist())
                .andExpect(jsonPath("$[1].level").value("NOT_EVALUATED"))
                .andExpect(jsonPath("$[1].tanks.length()").value(0));
    }

    @Test
    void passesTheJournalDateToTheService() throws Exception {
        when(autonomyService.calculate(DATE)).thenReturn(List.of());

        mockMvc.perform(get(URL).param("journalDate", "2026-08-05")).andExpect(status().isOk());

        verify(autonomyService).calculate(DATE);
    }

    @Test
    void requiresTheJournalDate() throws Exception {
        mockMvc.perform(get(URL))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("The required parameter is missing 'journalDate'"));
        verifyNoInteractions(autonomyService);
    }

    @Test
    void rejectsAnInvalidJournalDate() throws Exception {
        mockMvc.perform(get(URL).param("journalDate", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("The value of parameter 'journalDate' is invalid"));
        verifyNoInteractions(autonomyService);
    }

    private FuelAutonomy calculated() {
        return FuelAutonomy.builder()
                .fuelType(FuelType.MOTOR)
                .status(AutonomyStatus.CALCULATED)
                .stockGallons(new BigDecimal("3000.00"))
                .expectedDailySalesGallons(new BigDecimal("700.00"))
                .autonomyDays(new BigDecimal("4.29"))
                .level(AutonomyLevel.WARNING)
                .warningDays(new BigDecimal("5.5"))
                .criticalDays(new BigDecimal("2.5"))
                .tanks(List.of(new TankStock(1L, "T1C1", new BigDecimal("100.5"), new BigDecimal("1800.25"))))
                .build();
    }

    private FuelAutonomy notCalculated() {
        return FuelAutonomy.builder()
                .fuelType(FuelType.DIESEL)
                .status(AutonomyStatus.NO_SALES_HISTORY)
                .level(AutonomyLevel.NOT_EVALUATED)
                .tanks(List.of())
                .build();
    }
}