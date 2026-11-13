package com.edu.unicauca.gasstation.backend.sales.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.edu.unicauca.gasstation.backend.sales.application.MonthlyReconciliationService;
import com.edu.unicauca.gasstation.backend.sales.domain.reconciliation.DayFranchiseTotals;
import com.edu.unicauca.gasstation.backend.sales.domain.reconciliation.MonthlyReconciliation;
import com.edu.unicauca.gasstation.backend.sales.domain.reconciliation.MonthlyReconciliationCalculator;
import com.edu.unicauca.gasstation.backend.sales.exception.SalesParameterNotFoundException;
import com.edu.unicauca.gasstation.backend.security.infrastructure.config.SecurityConfig;
import com.edu.unicauca.gasstation.backend.security.infrastructure.jwt.JwtAuthenticationFilter;
import com.edu.unicauca.gasstation.backend.security.infrastructure.jwt.JwtService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = MonthlyReconciliationController.class, properties = "server.port=0")
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class})
class MonthlyReconciliationControllerTest {

    private static final String URL = "/api/v1/sales/reconciliation/monthly/";

    @Autowired MockMvc mvc;
    @Autowired JwtService jwt;
    @MockBean UserDetailsService users;
    @MockBean AuthenticationProvider provider;
    @MockBean MonthlyReconciliationService service;

    private String bearer() {
        var user = User.withUsername("monthly-test").password("unused").authorities("ADMIN").build();
        when(users.loadUserByUsername("monthly-test")).thenReturn(user);
        return "Bearer " + jwt.generateToken(user);
    }

    private static BigDecimal money(String value) {
        return new BigDecimal(value).setScale(2);
    }

    private static MonthlyReconciliation august() {
        LocalDate day5 = LocalDate.of(2026, 8, 5);
        return MonthlyReconciliationCalculator.calculate(YearMonth.of(2026, 8),
                Map.of(day5, new DayFranchiseTotals(money("0"), money("3618384"), money("2072928"), money("0"),
                        money("0"), money("0"), money("876008"))),
                Map.of(), Map.of(day5, money("41380")), new BigDecimal("0.026500"));
    }

    @Test
    void returnsTheMonthlyTableWithTotalsAndCommission() throws Exception {
        when(service.get(YearMonth.of(2026, 8))).thenReturn(august());

        mvc.perform(get(URL + "2026-08").header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.month").value("2026-08"))
                .andExpect(jsonPath("$.days.length()").value(31))
                .andExpect(jsonPath("$.days[0].date").value("2026-08-01"))
                .andExpect(jsonPath("$.days[4].mastercard").value(3618384.0))
                .andExpect(jsonPath("$.days[4].totalEds").value(5691312.0))
                .andExpect(jsonPath("$.days[4].qr").value(876008.0))
                .andExpect(jsonPath("$.days[4].qrSource").value("PLANILLA"))
                .andExpect(jsonPath("$.days[4].totalWithQr").value(6567320.0))
                .andExpect(jsonPath("$.days[4].points").value(41380.0))
                .andExpect(jsonPath("$.days[30].hasData").value(false))
                .andExpect(jsonPath("$.daysWithData").value(1))
                .andExpect(jsonPath("$.totals.totalEds").value(5691312.0))
                .andExpect(jsonPath("$.totals.points").value(41380.0))
                .andExpect(jsonPath("$.commission.rate").value(0.0265))
                .andExpect(jsonPath("$.commission.base").value(5691312.0))
                .andExpect(jsonPath("$.commission.amount").value(150819.77));
        verify(service).get(YearMonth.of(2026, 8));
    }

    @ParameterizedTest
    @ValueSource(strings = {"2026-13", "2026-8", "agosto", "2026-08-05"})
    void aMonthWithAnInvalidFormatIsRejectedNamingTheField(String month) throws Exception {
        mvc.perform(get(URL + month).header("Authorization", bearer()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_RECONCILIATION_VALUE"))
                .andExpect(jsonPath("$.errors[0].field").value("month"))
                .andExpect(jsonPath("$.errors[0].message").value("El mes debe tener el formato yyyy-MM"));
        verifyNoInteractions(service);
    }

    @Test
    void withoutAnEffectiveCommissionRateTheMonthIsNotFound() throws Exception {
        when(service.get(any())).thenThrow(new SalesParameterNotFoundException(
                "No hay un valor de 'credibanco-commission-rate' vigente el 2025-12-31"));

        mvc.perform(get(URL + "2025-12").header("Authorization", bearer()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SALES_PARAMETER_NOT_FOUND"));
    }
}
