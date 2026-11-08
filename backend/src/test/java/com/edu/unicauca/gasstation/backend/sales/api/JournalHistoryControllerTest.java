package com.edu.unicauca.gasstation.backend.sales.api;

import com.edu.unicauca.gasstation.backend.sales.domain.models.JournalHistoryModels.JournalCalendarDay;
import com.edu.unicauca.gasstation.backend.sales.domain.models.JournalHistoryModels.JournalHistoryCalendar;
import com.edu.unicauca.gasstation.backend.sales.domain.models.JournalHistoryModels.JournalHistoryDayDetail;
import com.edu.unicauca.gasstation.backend.sales.domain.services.JournalHistoryService;
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
import java.time.YearMonth;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = JournalHistoryController.class, properties = "server.port=0")
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class})
@DisplayName("JournalHistoryController - Journal History REST API (C-10, S3-06)")
class JournalHistoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockBean
    private JournalHistoryService journalHistoryService;

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
    @DisplayName("GET /api/v1/journals/history should return monthly calendar with migrated and closed counts")
    void returnsMonthlyHistoryCalendar() throws Exception {
        JournalHistoryCalendar calendar = new JournalHistoryCalendar(
                "2026-08",
                1,
                27,
                0,
                8,
                List.of(
                        new JournalCalendarDay(
                                LocalDate.of(2026, 8, 1), 1, "MIGRATED", "CLOSED", null, null, true, false, List.of("descuadre")
                        ),
                        new JournalCalendarDay(
                                LocalDate.of(2026, 8, 5), 5, "JOURNAL", "CLOSED", 5L, 293L, false, true, List.of("alerta")
                        )
                )
        );

        when(journalHistoryService.getMonthCalendar(YearMonth.of(2026, 8))).thenReturn(calendar);

        mockMvc.perform(get("/api/v1/journals/history")
                        .header("Authorization", getBearerToken())
                        .param("yearMonth", "2026-08"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.yearMonth").value("2026-08"))
                .andExpect(jsonPath("$.closedInSystemCount").value(1))
                .andExpect(jsonPath("$.migratedDaysCount").value(27))
                .andExpect(jsonPath("$.days[0].type").value("MIGRATED"))
                .andExpect(jsonPath("$.days[1].type").value("JOURNAL"))
                .andExpect(jsonPath("$.days[1].status").value("CLOSED"));
    }

    @Test
    @DisplayName("GET /api/v1/journals/history/{date} should return migrated day detail")
    void returnsMigratedDayDetail() throws Exception {
        LocalDate date = LocalDate.of(2026, 8, 1);
        JournalHistoryDayDetail detail = new JournalHistoryDayDetail(
                date, "MIGRATED", "CLOSED", null, null, null,
                true, false, List.of("descuadre"),
                List.of(), null, List.of(), null, List.of()
        );

        when(journalHistoryService.getDayDetail(date)).thenReturn(detail);

        mockMvc.perform(get("/api/v1/journals/history/2026-08-01")
                        .header("Authorization", getBearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value("2026-08-01"))
                .andExpect(jsonPath("$.type").value("MIGRATED"))
                .andExpect(jsonPath("$.status").value("CLOSED"))
                .andExpect(jsonPath("$.hasDiscrepancy").value(true));
    }
}
