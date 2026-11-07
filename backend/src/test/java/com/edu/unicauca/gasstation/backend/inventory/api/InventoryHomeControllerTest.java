package com.edu.unicauca.gasstation.backend.inventory.api;

import com.edu.unicauca.gasstation.backend.inventory.domain.models.AlertStatus;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.AutonomyLevel;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.FuelStockSummary;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.InventoryHome;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.TankSnapshot;
import com.edu.unicauca.gasstation.backend.inventory.domain.services.InventoryHomeService;
import com.edu.unicauca.gasstation.backend.inventory.infrastructure.mappers.InventoryAlertMapperImpl;
import com.edu.unicauca.gasstation.backend.inventory.infrastructure.mappers.InventoryHomeMapperImpl;
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
import java.time.LocalDateTime;
import java.util.List;

import static com.edu.unicauca.gasstation.backend.inventory.AlertTestData.alert;
import static com.edu.unicauca.gasstation.backend.inventory.FuelType.MOTOR;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = InventoryHomeController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class))
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = "server.port=0")
@Import({InventoryHomeMapperImpl.class, InventoryAlertMapperImpl.class})
class InventoryHomeControllerTest {

    private static final String URL = "/api/v1/inventory/home";
    private static final LocalDate DATE = LocalDate.of(2026, 8, 5);

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private InventoryHomeService homeService;

    @Test
    void returnsTheStockPerFuelAndPerTank() throws Exception {
        when(homeService.get()).thenReturn(InventoryHome.builder()
                .fuels(List.of(FuelStockSummary.builder().fuelType(MOTOR)
                        .currentGallons(new BigDecimal("800")).capacityGallons(new BigDecimal("2000"))
                        .percentage(new BigDecimal("40.0")).complete(true)
                        .alertLevel(AutonomyLevel.WARNING).build()))
                .tanks(List.of(
                        TankSnapshot.builder().tankId(1L).tankCode("T1").fuelType(MOTOR)
                                .heightCm(new BigDecimal("100")).gallons(new BigDecimal("500"))
                                .capacityGallons(new BigDecimal("1000")).percentage(new BigDecimal("50.0"))
                                .measuredOn(DATE).measuredAt(LocalDateTime.of(2026, 8, 5, 9, 30))
                                .alertLevel(AutonomyLevel.WARNING).build(),
                        TankSnapshot.builder().tankId(3L).tankCode("T3").fuelType(MOTOR)
                                .capacityGallons(new BigDecimal("2000")).build()))
                .mostCriticalAlert(alert(1L, MOTOR, AutonomyLevel.WARNING, AlertStatus.ACTIVE, "4.00", DATE))
                .pendingAlerts(1)
                .build());

        mockMvc.perform(MockMvcRequestBuilders.get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fuels[0].fuelType").value("MOTOR"))
                .andExpect(jsonPath("$.fuels[0].currentGallons").value(800))
                .andExpect(jsonPath("$.fuels[0].percentage").value(40.0))
                .andExpect(jsonPath("$.fuels[0].complete").value(true))
                .andExpect(jsonPath("$.fuels[0].alertLevel").value("WARNING"))
                .andExpect(jsonPath("$.tanks[0].tankCode").value("T1"))
                .andExpect(jsonPath("$.tanks[0].heightCm").value(100))
                .andExpect(jsonPath("$.tanks[0].measuredOn").value("2026-08-05"))
                .andExpect(jsonPath("$.tanks[1].gallons").value(nullValue()))
                .andExpect(jsonPath("$.tanks[1].measuredOn").value(nullValue()))
                .andExpect(jsonPath("$.mostCriticalAlert.id").value(1))
                .andExpect(jsonPath("$.mostCriticalAlert.level").value("WARNING"))
                .andExpect(jsonPath("$.pendingAlerts").value(1));
    }

    @Test
    void withoutAlertsTheBannerIsNull() throws Exception {
        when(homeService.get()).thenReturn(InventoryHome.builder()
                .fuels(List.of()).tanks(List.of()).pendingAlerts(0).build());

        mockMvc.perform(MockMvcRequestBuilders.get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mostCriticalAlert").value(nullValue()))
                .andExpect(jsonPath("$.pendingAlerts").value(0));
    }
}