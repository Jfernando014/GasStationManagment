package com.edu.unicauca.gasstation.backend.inventory.api;

import com.edu.unicauca.gasstation.backend.inventory.ProductCategory;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.AutonomyLevel;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.AutonomyStatus;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.DailyControl;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.FuelControl;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.FuelVariation;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.ProductControl;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.StockStatus;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.VariationStatus;
import com.edu.unicauca.gasstation.backend.inventory.domain.services.DailyControlService;
import com.edu.unicauca.gasstation.backend.inventory.infrastructure.mappers.DailyControlMapperImpl;
import com.edu.unicauca.gasstation.backend.inventory.infrastructure.mappers.FuelAutonomyMapperImpl;
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static com.edu.unicauca.gasstation.backend.inventory.AlertTestData.autonomy;
import static com.edu.unicauca.gasstation.backend.inventory.FuelType.MOTOR;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = DailyControlController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class))
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = "server.port=0")
@Import({DailyControlMapperImpl.class, FuelVariationMapperImpl.class, FuelAutonomyMapperImpl.class})
class DailyControlControllerTest {

    private static final String URL = "/api/v1/inventory/daily-control";
    private static final LocalDate DATE = LocalDate.of(2026, 8, 5);

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DailyControlService controlService;

    @Test
    void returnsTheFuelsAndTheProductsOfTheJournal() throws Exception {
        when(controlService.findByJournalDate(DATE)).thenReturn(DailyControl.builder()
                .journalDate(DATE)
                .fuels(List.of(FuelControl.builder()
                        .fuelType(MOTOR)
                        .variation(FuelVariation.builder().fuelType(MOTOR).status(VariationStatus.OK)
                                .varianceGallons(new BigDecimal("-12.5")).outOfTolerance(true).build())
                        .autonomy(autonomy(MOTOR, AutonomyStatus.CALCULATED, AutonomyLevel.WARNING, "3.75"))
                        .estimatedDepletionDate(DATE.plusDays(3))
                        .build()))
                .products(List.of(ProductControl.builder()
                        .productId(1L).productCode("80220").productName("Product 80220")
                        .category(ProductCategory.OTRO).sold(3).officeStock(32).counterStock(10)
                        .status(StockStatus.OK).depleted(false).build()))
                .build());

        mockMvc.perform(MockMvcRequestBuilders.get(URL).param("journalDate", "2026-08-05"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.journalDate").value("2026-08-05"))
                .andExpect(jsonPath("$.fuels[0].fuelType").value("MOTOR"))
                .andExpect(jsonPath("$.fuels[0].variation.status").value("OK"))
                .andExpect(jsonPath("$.fuels[0].variation.varianceGallons").value(-12.5))
                .andExpect(jsonPath("$.fuels[0].variation.outOfTolerance").value(true))
                .andExpect(jsonPath("$.fuels[0].autonomy.level").value("WARNING"))
                .andExpect(jsonPath("$.fuels[0].autonomy.autonomyDays").value(3.75))
                .andExpect(jsonPath("$.fuels[0].estimatedDepletionDate").value("2026-08-08"))
                .andExpect(jsonPath("$.products[0].productCode").value("80220"))
                .andExpect(jsonPath("$.products[0].sold").value(3))
                .andExpect(jsonPath("$.products[0].officeStock").value(32))
                .andExpect(jsonPath("$.products[0].counterStock").value(10))
                .andExpect(jsonPath("$.products[0].depleted").value(false));
    }

    @Test
    void requiresTheJournalDate() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get(URL))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("The required parameter is missing 'journalDate'"));
        verifyNoInteractions(controlService);
    }

    @Test
    void rejectsAnInvalidJournalDate() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get(URL).param("journalDate", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("The value of parameter 'journalDate' is invalid"));
        verifyNoInteractions(controlService);
    }
}