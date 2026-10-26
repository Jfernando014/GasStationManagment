package com.edu.unicauca.gasstation.backend.inventory.api;

import com.edu.unicauca.gasstation.backend.inventory.FuelType;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.FuelVariation;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.VariationStatus;
import com.edu.unicauca.gasstation.backend.inventory.domain.services.FuelVariationService;
import com.edu.unicauca.gasstation.backend.inventory.infrastructure.mappers.FuelVariationMapperImpl;
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
        controllers = FuelVariationController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class))
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = "server.port=0")
@Import(FuelVariationMapperImpl.class)
class FuelVariationControllerTest {

    private static final String URL = "/api/v1/inventory/variations";
    private static final LocalDate DATE = LocalDate.of(2026, 8, 5);

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FuelVariationService variationService;

    @Test
    void returnsOneRowPerFuelWithTheCalculatedValues() throws Exception {
        when(variationService.calculate(DATE)).thenReturn(List.of(okVariation(), incompleteVariation()));

        mockMvc.perform(get(URL).param("journalDate", "2026-08-05"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].fuelType").value("MOTOR"))
                .andExpect(jsonPath("$[0].status").value("OK"))
                .andExpect(jsonPath("$[0].tankIds.length()").value(3))
                .andExpect(jsonPath("$[0].initialGallons").value(1558.05))
                .andExpect(jsonPath("$[0].entriesGallons").value(3970))
                .andExpect(jsonPath("$[0].soldGallons").value(784.587))
                .andExpect(jsonPath("$[0].theoreticalGallons").value(4743.463))
                .andExpect(jsonPath("$[0].measuredGallons").value(4703.6))
                .andExpect(jsonPath("$[0].varianceGallons").value(-39.863))
                .andExpect(jsonPath("$[0].toleranceGallons").value(30.0))
                .andExpect(jsonPath("$[0].outOfTolerance").value(true));
    }

    @Test
    void rowsThatCannotBeCalculatedCarryTheReasonAndNullValues() throws Exception {
        when(variationService.calculate(DATE)).thenReturn(List.of(okVariation(), incompleteVariation()));

        mockMvc.perform(get(URL).param("journalDate", "2026-08-05"))
                .andExpect(jsonPath("$[1].fuelType").value("DIESEL"))
                .andExpect(jsonPath("$[1].status").value("INCOMPLETE_MEASUREMENT"))
                .andExpect(jsonPath("$[1].measuredGallons").doesNotExist())
                .andExpect(jsonPath("$[1].varianceGallons").doesNotExist())
                .andExpect(jsonPath("$[1].outOfTolerance").value(false));
    }

    @Test
    void passesTheJournalDateToTheService() throws Exception {
        when(variationService.calculate(DATE)).thenReturn(List.of());

        mockMvc.perform(get(URL).param("journalDate", "2026-08-05"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(variationService).calculate(DATE);
    }

    @Test
    void requiresTheJournalDate() throws Exception {
        mockMvc.perform(get(URL))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("The required parameter is missing 'journalDate'"));
        verifyNoInteractions(variationService);
    }

    @Test
    void rejectsAnInvalidJournalDate() throws Exception {
        mockMvc.perform(get(URL).param("journalDate", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("The value of parameter 'journalDate' is invalid"));
        verifyNoInteractions(variationService);
    }

    private FuelVariation okVariation() {
        return FuelVariation.builder()
                .fuelType(FuelType.MOTOR)
                .status(VariationStatus.OK)
                .tankIds(List.of(1L, 2L, 3L))
                .initialGallons(new BigDecimal("1558.05"))
                .entriesGallons(new BigDecimal("3970"))
                .soldGallons(new BigDecimal("784.587"))
                .theoreticalGallons(new BigDecimal("4743.463"))
                .measuredGallons(new BigDecimal("4703.60"))
                .varianceGallons(new BigDecimal("-39.863"))
                .toleranceGallons(new BigDecimal("30.000"))
                .outOfTolerance(true)
                .build();
    }

    private FuelVariation incompleteVariation() {
        return FuelVariation.builder()
                .fuelType(FuelType.DIESEL)
                .status(VariationStatus.INCOMPLETE_MEASUREMENT)
                .tankIds(List.of(4L))
                .entriesGallons(BigDecimal.ZERO)
                .build();
    }
}