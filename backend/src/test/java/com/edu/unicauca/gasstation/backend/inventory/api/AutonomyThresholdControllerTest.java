package com.edu.unicauca.gasstation.backend.inventory.api;

import com.edu.unicauca.gasstation.backend.inventory.api.dto.AutonomyThresholdRequest;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.AutonomyThreshold;
import com.edu.unicauca.gasstation.backend.inventory.domain.services.AutonomyThresholdService;
import com.edu.unicauca.gasstation.backend.inventory.exception.AutonomyThresholdAlreadyExistsException;
import com.edu.unicauca.gasstation.backend.inventory.exception.InvalidAutonomyThresholdException;
import com.edu.unicauca.gasstation.backend.inventory.infrastructure.mappers.AutonomyThresholdMapperImpl;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = AutonomyThresholdController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class))
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = "server.port=0")
@Import(AutonomyThresholdMapperImpl.class)
class AutonomyThresholdControllerTest {

    private static final String URL = "/api/v1/inventory/autonomy-thresholds";
    private static final LocalDate FROM = LocalDate.of(2026, 8, 1);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AutonomyThresholdService thresholdService;

    // ---------- POST ----------

    @Test
    void createReturnsCreatedWithTheThreshold() throws Exception {
        when(thresholdService.create(any(AutonomyThreshold.class))).thenReturn(threshold(7L, "5.0", "2.0"));

        postThreshold(validRequest())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.validFrom").value("2026-08-01"))
                .andExpect(jsonPath("$.warningDays").value(5.0))
                .andExpect(jsonPath("$.criticalDays").value(2.0))
                .andExpect(jsonPath("$.createdAt").value("2026-08-05T10:00:00"));
    }

    @Test
    void createSendsTheRequestDataToTheService() throws Exception {
        when(thresholdService.create(any(AutonomyThreshold.class))).thenReturn(threshold(7L, "5.0", "2.0"));

        postThreshold(validRequest()).andExpect(status().isCreated());

        verify(thresholdService).create(argThat(t -> t.getId() == null
                && t.getValidFrom().equals(FROM)
                && t.getWarningDays().compareTo(new BigDecimal("5")) == 0
                && t.getCriticalDays().compareTo(new BigDecimal("2")) == 0));
    }

    @Test
    void createRejectsAMissingValidityDate() throws Exception {
        AutonomyThresholdRequest request = validRequest();
        request.setValidFrom(null);

        postThreshold(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields[0].field").value("validFrom"))
                .andExpect(jsonPath("$.fields[0].message").value(AutonomyThresholdRequest.VALID_FROM_REQUIRED));
        verifyNoInteractions(thresholdService);
    }

    @Test
    void createRejectsMissingThresholds() throws Exception {
        AutonomyThresholdRequest noWarning = validRequest();
        noWarning.setWarningDays(null);
        postThreshold(noWarning)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields[0].message").value(AutonomyThresholdRequest.WARNING_REQUIRED));

        AutonomyThresholdRequest noCritical = validRequest();
        noCritical.setCriticalDays(null);
        postThreshold(noCritical)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields[0].message").value(AutonomyThresholdRequest.CRITICAL_REQUIRED));
        verifyNoInteractions(thresholdService);
    }

    @Test
    void createRejectsAZeroOrNegativeThreshold() throws Exception {
        for (String days : List.of("0", "-1")) {
            AutonomyThresholdRequest request = validRequest();
            request.setCriticalDays(new BigDecimal(days));

            postThreshold(request)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fields[0].field").value("criticalDays"))
                    .andExpect(jsonPath("$.fields[0].message").value(AutonomyThresholdRequest.DAYS_POSITIVE));
        }
        verifyNoInteractions(thresholdService);
    }

    @Test
    void createRejectsAThresholdWithTooManyDigits() throws Exception {
        for (String days : List.of("2.55", "1234")) {
            AutonomyThresholdRequest request = validRequest();
            request.setWarningDays(new BigDecimal(days));

            postThreshold(request)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fields[0].field").value("warningDays"))
                    .andExpect(jsonPath("$.fields[0].message").value(AutonomyThresholdRequest.DAYS_DIGITS));
        }
        verifyNoInteractions(thresholdService);
    }

    @Test
    void createReturnsBadRequestWhenTheServiceRejectsTheThresholds() throws Exception {
        when(thresholdService.create(any(AutonomyThreshold.class))).thenThrow(
                new InvalidAutonomyThresholdException("The critical threshold must be lower than the warning threshold"));

        postThreshold(validRequest())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid data"))
                .andExpect(jsonPath("$.message")
                        .value("The critical threshold must be lower than the warning threshold"));
    }

    @Test
    void createReturnsConflictWhenTheValidityDateIsTaken() throws Exception {
        when(thresholdService.create(any(AutonomyThreshold.class)))
                .thenThrow(new AutonomyThresholdAlreadyExistsException(FROM));

        postThreshold(validRequest())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value(new AutonomyThresholdAlreadyExistsException(FROM).getMessage()));
    }

    // ---------- GET ----------

    @Test
    void findAllReturnsTheThresholds() throws Exception {
        AutonomyThreshold september = threshold(2L, "7.0", "3.0");
        september.setValidFrom(LocalDate.of(2026, 9, 1));
        when(thresholdService.findAll()).thenReturn(List.of(september, threshold(1L, "5.0", "2.0")));

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].validFrom").value("2026-09-01"))
                .andExpect(jsonPath("$[1].validFrom").value("2026-08-01"));
    }

    @Test
    void findAllReturnsAnEmptyListWhenNothingIsConfigured() throws Exception {
        when(thresholdService.findAll()).thenReturn(List.of());

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ---------- helpers ----------

    private AutonomyThresholdRequest validRequest() {
        AutonomyThresholdRequest request = new AutonomyThresholdRequest();
        request.setValidFrom(FROM);
        request.setWarningDays(new BigDecimal("5"));
        request.setCriticalDays(new BigDecimal("2"));
        return request;
    }

    private AutonomyThreshold threshold(Long id, String warning, String critical) {
        AutonomyThreshold threshold = new AutonomyThreshold();
        threshold.setId(id);
        threshold.setValidFrom(FROM);
        threshold.setWarningDays(new BigDecimal(warning));
        threshold.setCriticalDays(new BigDecimal(critical));
        threshold.setCreatedAt(LocalDateTime.of(2026, 8, 5, 10, 0));
        return threshold;
    }

    private ResultActions postThreshold(AutonomyThresholdRequest request) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.post(URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }
}