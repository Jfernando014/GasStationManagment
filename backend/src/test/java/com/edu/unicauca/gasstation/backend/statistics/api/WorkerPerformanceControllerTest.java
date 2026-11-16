package com.edu.unicauca.gasstation.backend.statistics.api;

import com.edu.unicauca.gasstation.backend.security.infrastructure.config.SecurityConfig;
import com.edu.unicauca.gasstation.backend.security.infrastructure.jwt.JwtAuthenticationFilter;
import com.edu.unicauca.gasstation.backend.security.infrastructure.jwt.JwtService;
import com.edu.unicauca.gasstation.backend.shifts.holidays.HolidayQuery;
import com.edu.unicauca.gasstation.backend.statistics.application.WorkerPerformanceService;
import com.edu.unicauca.gasstation.backend.statistics.domain.models.ShiftSaleHistory.EffectiveRole;
import com.edu.unicauca.gasstation.backend.statistics.domain.repositories.ShiftSaleHistoryRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import static com.edu.unicauca.gasstation.backend.statistics.SellerPerformanceTestData.A;
import static com.edu.unicauca.gasstation.backend.statistics.SellerPerformanceTestData.B;
import static com.edu.unicauca.gasstation.backend.statistics.SellerPerformanceTestData.sale;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = WorkerPerformanceController.class, properties = "server.port=0")
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class, WorkerPerformanceService.class})
class WorkerPerformanceControllerTest {
    private static final String URL_TEMPLATE = "/api/v1/statistics/workers/{workerId}/performance";

    @Autowired
    private MockMvc mvc;
    @Autowired
    private JwtService jwt;
    @MockBean
    private ShiftSaleHistoryRepository history;
    @MockBean
    private HolidayQuery holidayQuery;
    @MockBean
    private UserDetailsService users;
    @MockBean
    private AuthenticationProvider provider;

    private String bearer(String authority) {
        var user = User.withUsername("statistics-test").password("unused").authorities(authority).build();
        when(users.loadUserByUsername("statistics-test")).thenReturn(user);
        return "Bearer " + jwt.generateToken(user);
    }

    @Test
    void adminReceivesCalculatedWorkerPerformanceJsonContract() throws Exception {
        LocalDate monday = LocalDate.of(2026, 8, 3);
        LocalDate holiday = LocalDate.of(2026, 8, 7);
        when(history.findAllProcessed()).thenReturn(List.of(
                sale(monday, A, "DIA", EffectiveRole.TITULAR, "120", "100"),
                sale(monday, B, "DIA", EffectiveRole.TITULAR, "80", "100"),
                sale(holiday, A, "NOCHE", EffectiveRole.APOYO, "95.5", "0")));
        when(holidayQuery.getHolidays(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31)))
                .thenReturn(Set.of(holiday));

        mvc.perform(get(URL_TEMPLATE, A).header("Authorization", bearer("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.periodFrom").value("2026-08-03"))
                .andExpect(jsonPath("$.periodTo").value("2026-08-07"))
                .andExpect(jsonPath("$.closedShifts.total").value(2))
                .andExpect(jsonPath("$.closedShifts.asHolder").value(1))
                .andExpect(jsonPath("$.closedShifts.asSupport").value(1))
                .andExpect(jsonPath("$.byWeekday", hasSize(8)))
                .andExpect(jsonPath("$.byWeekday[0].weekday").value("MONDAY"))
                .andExpect(jsonPath("$.byWeekday[0].asHolder.shifts").value(1))
                .andExpect(jsonPath("$.byWeekday[0].asHolder.averageGallons").value(120.0))
                .andExpect(jsonPath("$.byWeekday[0].asHolder.performancePercent").value(120.0))
                .andExpect(jsonPath("$.byWeekday[7].weekday").value("HOLIDAY"))
                .andExpect(jsonPath("$.byWeekday[7].asSupport.shifts").value(1))
                .andExpect(jsonPath("$.byWeekday[7].asSupport.averageGallons").value(95.5))
                .andExpect(jsonPath("$.byWeekday[7].asSupport.performancePercent").value(100.0))
                .andExpect(jsonPath("$.maxSale.gallons").value(120.0))
                .andExpect(jsonPath("$.maxSale.date").value("2026-08-03"))
                .andExpect(jsonPath("$.maxSale.shiftCode").value("DIA"));
    }

    @Test
    void workerWithoutClosedHistoryReceivesEmptyReportWith200() throws Exception {
        when(history.findAllProcessed()).thenReturn(List.of());

        mvc.perform(get(URL_TEMPLATE, A).header("Authorization", bearer("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.periodFrom").value(nullValue()))
                .andExpect(jsonPath("$.periodTo").value(nullValue()))
                .andExpect(jsonPath("$.maxSale").value(nullValue()))
                .andExpect(jsonPath("$.closedShifts.total").value(0))
                .andExpect(jsonPath("$.byWeekday", hasSize(8)));
        verifyNoInteractions(holidayQuery);
    }

    @Test
    void nonAdminAndAnonymousRequestsAreRejected() throws Exception {
        mvc.perform(get(URL_TEMPLATE, A).header("Authorization", bearer("SELLER")))
                .andExpect(status().isForbidden());
        mvc.perform(get(URL_TEMPLATE, A).header("Authorization", bearer("ROLE_ADMIN")))
                .andExpect(status().isForbidden());
        mvc.perform(get(URL_TEMPLATE, A))
                .andExpect(status().is4xxClientError());
        verifyNoInteractions(history, holidayQuery);
    }

    @Test
    void zeroUuidAndMalformedUuidReturn400BadRequest() throws Exception {
        mvc.perform(get(URL_TEMPLATE, "00000000-0000-0000-0000-000000000000")
                        .header("Authorization", bearer("ADMIN")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PERFORMANCE_REQUEST"));

        mvc.perform(get(URL_TEMPLATE, "not-a-uuid").header("Authorization", bearer("ADMIN")))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(history, holidayQuery);
    }

    @Test
    void upstreamFailureReturns502WithoutLeakingDetails() throws Exception {
        when(history.findAllProcessed()).thenThrow(new IllegalStateException("sensitive database details"));

        mvc.perform(get(URL_TEMPLATE, A).header("Authorization", bearer("ADMIN")))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("PERFORMANCE_SOURCE_FAILURE"))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("sensitive database details"))));
    }
}
