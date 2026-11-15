package com.edu.unicauca.gasstation.backend.sales.api;

import com.edu.unicauca.gasstation.backend.sales.domain.Journal;
import com.edu.unicauca.gasstation.backend.sales.domain.JournalStatus;
import com.edu.unicauca.gasstation.backend.sales.domain.JournalStep;
import com.edu.unicauca.gasstation.backend.sales.domain.StepStatus;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.InventoryClosingReadPort.InventoryAlertSummary;
import com.edu.unicauca.gasstation.backend.sales.domain.services.JournalClosingSummaryService;
import com.edu.unicauca.gasstation.backend.sales.domain.services.JournalClosingSummaryService.ClosingSummaryResult;
import com.edu.unicauca.gasstation.backend.sales.domain.services.JournalService;
import com.edu.unicauca.gasstation.backend.sales.domain.services.JournalStep3ValidationService;
import com.edu.unicauca.gasstation.backend.sales.domain.services.JournalStepService;
import com.edu.unicauca.gasstation.backend.sales.application.CashBalanceStepService;
import com.edu.unicauca.gasstation.backend.sales.exception.CashBalanceNotReadyException;
import com.edu.unicauca.gasstation.backend.security.infrastructure.config.SecurityConfig;
import com.edu.unicauca.gasstation.backend.security.infrastructure.jwt.JwtAuthenticationFilter;
import com.edu.unicauca.gasstation.backend.security.infrastructure.jwt.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = JournalController.class, properties = "server.port=0")
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class, JournalExceptionHandler.class})
@DisplayName("JournalController - Daily Journal REST API (S1-13, S1-14, S3-03, S3-05)")
class JournalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockBean
    private JournalService journalService;

    @MockBean
    private JournalStepService journalStepService;

    @MockBean
    private JournalStep3ValidationService journalStep3ValidationService;

    @MockBean
    private JournalClosingSummaryService journalClosingSummaryService;

    @MockBean
    private CashBalanceStepService cashBalanceStepService;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private AuthenticationProvider authenticationProvider;

    private String getBearerToken() {
        return getBearerToken("ADMIN");
    }

    private String getBearerToken(String authority) {
        var user = User.withUsername("admin")
                .password("unused")
                .authorities(authority)
                .build();
        when(userDetailsService.loadUserByUsername("admin")).thenReturn(user);
        return "Bearer " + jwtService.generateToken(user);
    }

    @Test
    @DisplayName("POST /api/v1/journals/open should return 200 OK and journal response with steps")
    void opensJournalSuccessfullyWithAuthenticatedUser() throws Exception {
        LocalDate date = LocalDate.of(2026, 8, 5);

        List<JournalStep> steps = new ArrayList<>();
        for (int i = 1; i <= 6; i++) {
            steps.add(JournalStep.builder()
                    .id((long) i)
                    .stepNumber(i)
                    .name("Paso " + i)
                    .status(StepStatus.PENDING)
                    .build());
        }

        Journal journal = Journal.builder()
                .id(1L)
                .date(date)
                .consecutive(10L)
                .status(JournalStatus.OPEN)
                .steps(steps)
                .build();

        when(journalService.getOrCreateJournal(eq(date))).thenReturn(journal);

        mockMvc.perform(post("/api/v1/journals/open")
                        .header("Authorization", getBearerToken())
                        .param("date", "2026-08-05"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.date").value("2026-08-05"))
                .andExpect(jsonPath("$.consecutive").value(10))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.steps").isArray())
                .andExpect(jsonPath("$.steps.length()").value(6))
                .andExpect(jsonPath("$.steps[0].stepNumber").value(1))
                .andExpect(jsonPath("$.steps[0].status").value("PENDING"));
    }

    @Test
    @DisplayName("POST /api/v1/journals/{id}/steps/{stepNumber}/complete should return 200 OK and updated step")
    void completesStepSuccessfullyWithAuthenticatedUser() throws Exception {
        Long journalId = 1L;
        Integer stepNumber = 2;
        LocalDateTime now = LocalDateTime.now();

        JournalStep step = JournalStep.builder()
                .id(20L)
                .stepNumber(stepNumber)
                .name("Asignación de turnos")
                .status(StepStatus.COMPLETED)
                .completedAt(now)
                .build();

        when(journalStepService.markStepAsCompleted(eq(journalId), eq(stepNumber))).thenReturn(step);

        mockMvc.perform(post("/api/v1/journals/{journalId}/steps/{stepNumber}/complete", journalId, stepNumber)
                        .header("Authorization", getBearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(20))
                .andExpect(jsonPath("$.stepNumber").value(2))
                .andExpect(jsonPath("$.name").value("Asignación de turnos"))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.completedAt").isNotEmpty());
    }

    @Test
    @DisplayName("Completing step 4 validates the cash balance before marking it complete")
    void completesCashBalanceStepForAnAdmin() throws Exception {
        JournalStep step = JournalStep.builder()
                .id(40L)
                .stepNumber(4)
                .name("Saldo en caja")
                .status(StepStatus.COMPLETED)
                .completedAt(LocalDateTime.now())
                .build();
        when(cashBalanceStepService.complete(1L)).thenReturn(step);

        mockMvc.perform(post("/api/v1/journals/{journalId}/steps/{stepNumber}/complete", 1L, 4)
                        .header("Authorization", getBearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stepNumber").value(4))
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        verify(cashBalanceStepService).complete(1L);
        verify(journalStepService, never()).markStepAsCompleted(1L, 4);
    }

    @Test
    @DisplayName("Step 4 returns its missing figures when the cash balance is incomplete")
    void reportsMissingCashBalanceFigures() throws Exception {
        when(cashBalanceStepService.complete(1L))
                .thenThrow(new CashBalanceNotReadyException(List.of("Falta digitar el saldo acumulado de Dominus.")));

        mockMvc.perform(post("/api/v1/journals/{journalId}/steps/{stepNumber}/complete", 1L, 4)
                        .header("Authorization", getBearerToken()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CASH_BALANCE_NOT_READY"))
                .andExpect(jsonPath("$.missing[0]").value("Falta digitar el saldo acumulado de Dominus."));
    }

    @Test
    @DisplayName("Step 4 completion is restricted to administrators")
    void refusesCashBalanceCompletionForNonAdmin() throws Exception {
        mockMvc.perform(post("/api/v1/journals/{journalId}/steps/{stepNumber}/complete", 1L, 4)
                        .header("Authorization", getBearerToken("USER")))
                .andExpect(status().isForbidden());

        verify(cashBalanceStepService, never()).complete(1L);
    }

    @Test
    @DisplayName("POST /api/v1/journals/open should reject unauthenticated request with 403 Forbidden")
    void rejectsUnauthenticatedRequest() throws Exception {
        mockMvc.perform(post("/api/v1/journals/open")
                        .param("date", "2026-08-05"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/v1/journals/{id}/close should return 200 OK with CLOSED status and closedAt timestamp")
    void closesJournalSuccessfully() throws Exception {
        LocalDate date = LocalDate.of(2026, 8, 5);
        LocalDateTime closedAt = LocalDateTime.of(2026, 8, 5, 22, 15);

        JournalStep step6 = JournalStep.builder()
                .id(6L)
                .stepNumber(6)
                .name("Medición final y cuadre")
                .status(StepStatus.COMPLETED)
                .completedAt(closedAt)
                .build();

        Journal closedJournal = Journal.builder()
                .id(1L)
                .date(date)
                .consecutive(10L)
                .status(JournalStatus.CLOSED)
                .steps(List.of(step6))
                .build();

        when(journalService.closeJournal(1L)).thenReturn(closedJournal);

        mockMvc.perform(post("/api/v1/journals/1/close")
                        .header("Authorization", getBearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("CLOSED"))
                .andExpect(jsonPath("$.closedAt").isNotEmpty());
    }

    @Test
    @DisplayName("POST /api/v1/journals/{id}/steps/{step}/complete should return 409 Conflict when journal is closed")
    void returnsConflictWhenEditingClosedJournal() throws Exception {
        when(journalStepService.markStepAsCompleted(1L, 2))
                .thenThrow(new IllegalStateException("La jornada ya está cerrada y no permite modificaciones."));

        mockMvc.perform(post("/api/v1/journals/1/steps/2/complete")
                        .header("Authorization", getBearerToken()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("JOURNAL_STATE_CONFLICT"));
    }

    @Test
    @DisplayName("GET /api/v1/journals/{id}/closing-summary should return 200 OK with new alerts and step summary")
    void returnsClosingSummarySuccessfully() throws Exception {
        LocalDate date = LocalDate.of(2026, 8, 5);
        ClosingSummaryResult summaryResult = new ClosingSummaryResult(
                1L,
                date,
                10L,
                JournalStatus.OPEN,
                null,
                5,
                5,
                1,
                1,
                0,
                1,
                List.of(),
                List.of(),
                List.of(new InventoryAlertSummary(7L, "AUTONOMY", "DIESEL", "CRITICAL", "ACTIVE", new BigDecimal("1.50"), date))
        );

        when(journalClosingSummaryService.getClosingSummary(1L)).thenReturn(summaryResult);

        mockMvc.perform(get("/api/v1/journals/1/closing-summary")
                        .header("Authorization", getBearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.journalId").value(1))
                .andExpect(jsonPath("$.completedStepsCount").value(5))
                .andExpect(jsonPath("$.newAlertsCount").value(1))
                .andExpect(jsonPath("$.newAlerts[0].fuelType").value("DIESEL"))
                .andExpect(jsonPath("$.newAlerts[0].level").value("CRITICAL"));
    }
}
