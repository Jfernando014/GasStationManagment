package com.edu.unicauca.gasstation.backend.inventory.api;

import com.edu.unicauca.gasstation.backend.inventory.api.dto.AttendAlertRequest;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.AlertStatus;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.AutonomyLevel;
import com.edu.unicauca.gasstation.backend.inventory.domain.services.InventoryAlertService;
import com.edu.unicauca.gasstation.backend.inventory.exception.InventoryAlertConflictException;
import com.edu.unicauca.gasstation.backend.inventory.exception.InventoryAlertNotFoundException;
import com.edu.unicauca.gasstation.backend.inventory.infrastructure.mappers.InventoryAlertMapperImpl;
import com.edu.unicauca.gasstation.backend.security.infrastructure.jwt.JwtAuthenticationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.time.LocalDate;
import java.util.List;

import static com.edu.unicauca.gasstation.backend.inventory.AlertTestData.alert;
import static com.edu.unicauca.gasstation.backend.inventory.FuelType.DIESEL;
import static com.edu.unicauca.gasstation.backend.inventory.FuelType.MOTOR;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = InventoryAlertController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class))
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = "server.port=0")
@Import(InventoryAlertMapperImpl.class)
class InventoryAlertControllerTest {

    private static final String URL = "/api/v1/inventory/alerts";
    private static final LocalDate DATE = LocalDate.of(2026, 8, 5);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private InventoryAlertService alertService;

    // ---------- list and count ----------

    @Test
    void listReturnsTheOpenAlerts() throws Exception {
        when(alertService.findAll(null)).thenReturn(List.of(
                alert(1L, MOTOR, AutonomyLevel.CRITICAL, AlertStatus.ACTIVE, "1.50", DATE),
                alert(2L, DIESEL, AutonomyLevel.WARNING, AlertStatus.READ, "4.00", DATE)));

        mockMvc.perform(MockMvcRequestBuilders.get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].alertType").value("AUTONOMY"))
                .andExpect(jsonPath("$[0].fuelType").value("MOTOR"))
                .andExpect(jsonPath("$[0].level").value("CRITICAL"))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$[0].autonomyDays").value(1.5))
                .andExpect(jsonPath("$[0].openedOn").value("2026-08-05"))
                .andExpect(jsonPath("$[1].status").value("READ"));
    }

    @Test
    void listPassesTheStatusFilter() throws Exception {
        when(alertService.findAll(AlertStatus.RESOLVED)).thenReturn(List.of());

        mockMvc.perform(MockMvcRequestBuilders.get(URL).param("status", "RESOLVED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        verify(alertService).findAll(AlertStatus.RESOLVED);
    }

    @Test
    void listRejectsAnUnknownStatus() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get(URL).param("status", "XYZ"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("The value of parameter 'status' is invalid"));
        verifyNoInteractions(alertService);
    }

    @Test
    void countReturnsThePendingAlerts() throws Exception {
        when(alertService.countPending()).thenReturn(3L);

        mockMvc.perform(MockMvcRequestBuilders.get(URL + "/count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pending").value(3));
    }

    // ---------- read ----------

    @Test
    void markReadReturnsTheAlert() throws Exception {
        when(alertService.markRead(1L)).thenReturn(
                alert(1L, MOTOR, AutonomyLevel.WARNING, AlertStatus.READ, "4.00", DATE));

        mockMvc.perform(MockMvcRequestBuilders.put(URL + "/1/read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READ"));
    }

    @Test
    void markReadReturnsNotFoundForAnUnknownAlert() throws Exception {
        when(alertService.markRead(99L)).thenThrow(new InventoryAlertNotFoundException(99L));

        mockMvc.perform(MockMvcRequestBuilders.put(URL + "/99/read"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not found"));
    }

    // ---------- attend ----------

    @Test
    void attendReturnsTheAlertWithTheNote() throws Exception {
        when(alertService.attend(1L, "Fuel ordered")).thenReturn(
                alert(1L, MOTOR, AutonomyLevel.CRITICAL, AlertStatus.ATTENDED, "1.00", DATE));

        mockMvc.perform(MockMvcRequestBuilders.put(URL + "/1/attend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request("Fuel ordered"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ATTENDED"))
                .andExpect(jsonPath("$.attendNote").value("Fuel ordered"));
        verify(alertService).attend(1L, "Fuel ordered");
    }

    @Test
    void attendAcceptsAnEmptyBody() throws Exception {
        when(alertService.attend(1L, null)).thenReturn(
                alert(1L, MOTOR, AutonomyLevel.CRITICAL, AlertStatus.ATTENDED, "1.00", DATE));

        mockMvc.perform(MockMvcRequestBuilders.put(URL + "/1/attend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
    }

    @Test
    void attendRejectsATooLongNote() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.put(URL + "/1/attend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request("x".repeat(201)))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields[0].field").value("note"))
                .andExpect(jsonPath("$.fields[0].message").value(AttendAlertRequest.NOTE_SIZE));
        verifyNoInteractions(alertService);
    }

    @Test
    void attendWithoutBodyIsABadRequest() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.put(URL + "/1/attend"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(alertService);
    }

    @Test
    void attendReturnsConflictForAResolvedAlert() throws Exception {
        when(alertService.attend(1L, "Late"))
                .thenThrow(new InventoryAlertConflictException("The alert is already resolved"));

        mockMvc.perform(MockMvcRequestBuilders.put(URL + "/1/attend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request("Late"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("The alert is already resolved"));
    }

    @Test
    void attendReturnsNotFoundForAnUnknownAlert() throws Exception {
        when(alertService.attend(99L, "Note")).thenThrow(new InventoryAlertNotFoundException(99L));

        mockMvc.perform(MockMvcRequestBuilders.put(URL + "/99/attend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request("Note"))))
                .andExpect(status().isNotFound());
    }

    private AttendAlertRequest request(String note) {
        AttendAlertRequest request = new AttendAlertRequest();
        request.setNote(note);
        return request;
    }
}