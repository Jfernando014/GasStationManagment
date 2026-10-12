package com.edu.unicauca.gasstation.backend.sales.api;

import com.edu.unicauca.gasstation.backend.sales.domain.Journal;
import com.edu.unicauca.gasstation.backend.sales.domain.JournalStatus;
import com.edu.unicauca.gasstation.backend.sales.domain.JournalStep;
import com.edu.unicauca.gasstation.backend.sales.domain.StepStatus;
import com.edu.unicauca.gasstation.backend.sales.domain.services.JournalService;
import com.edu.unicauca.gasstation.backend.sales.domain.services.JournalStepService;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = JournalController.class, properties = "server.port=0")
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class})
@DisplayName("JournalController - Daily Journal REST API (S1-13, S1-14)")
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
    private UserDetailsService userDetailsService;

    @MockBean
    private AuthenticationProvider authenticationProvider;

    private String getBearerToken() {
        var user = User.withUsername("admin")
                .password("unused")
                .authorities("ADMIN")
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
    @DisplayName("POST /api/v1/journals/open should reject unauthenticated request with 403 Forbidden")
    void rejectsUnauthenticatedRequest() throws Exception {
        mockMvc.perform(post("/api/v1/journals/open")
                        .param("date", "2026-08-05"))
                .andExpect(status().isForbidden());
    }
}
