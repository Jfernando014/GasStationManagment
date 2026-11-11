package com.edu.unicauca.gasstation.backend.statistics.api;

import com.edu.unicauca.gasstation.backend.security.infrastructure.config.SecurityConfig;
import com.edu.unicauca.gasstation.backend.security.infrastructure.jwt.*;
import com.edu.unicauca.gasstation.backend.statistics.application.SellerPerformanceService;
import com.edu.unicauca.gasstation.backend.statistics.domain.calculation.SellerPerformance.*;
import com.edu.unicauca.gasstation.backend.statistics.domain.ports.SellerPerformanceParameterSource;
import com.edu.unicauca.gasstation.backend.statistics.domain.ports.SellerPerformanceParameterSource.Snapshot;
import com.edu.unicauca.gasstation.backend.statistics.domain.repositories.ShiftSaleHistoryRepository;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.userdetails.*;
import org.springframework.test.web.servlet.MockMvc;
import static com.edu.unicauca.gasstation.backend.statistics.SellerPerformanceTestData.*;
import static com.edu.unicauca.gasstation.backend.statistics.domain.calculation.SellerPerformance.Measure.*;
import static com.edu.unicauca.gasstation.backend.statistics.domain.calculation.SellerPerformance.ShiftPeriod.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = SellerPerformanceController.class, properties = "server.port=0")
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class, SellerPerformanceService.class})
class SellerPerformanceControllerTest {
    private static final String URL = "/api/v1/statistics/seller-performance";
    @Autowired MockMvc mvc;
    @Autowired JwtService jwt;
    @MockBean UserDetailsService users;
    @MockBean AuthenticationProvider provider;
    @MockBean ShiftSaleHistoryRepository history;
    @MockBean SellerPerformanceParameterSource parameters;

    @BeforeEach void missingParameters() {
        when(parameters.read(SINGLE_DAY, FUEL_GALLONS)).thenReturn(Snapshot.unavailable());
    }

    private String bearer(String authority) {
        var user = User.withUsername("performance-test").password("unused").authorities(authority).build();
        when(users.loadUserByUsername("performance-test")).thenReturn(user);
        return "Bearer " + jwt.generateToken(user);
    }

    @Test void getReadsRecordedSalesWithoutUploadsAndExplainsUnavailableDependencies() throws Exception {
        when(history.readRange(DATE, DATE)).thenReturn(history(SINGLE_DAY, sale(A, "12.345", "99999")));
        mvc.perform(get(URL).header("Authorization", bearer("ADMIN"))
                        .param("from", DATE.toString()).param("to", DATE.toString()).param("measure", "FUEL_GALLONS"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.source").value("CONFIRMED_SHIFT_SALES_HISTORY"))
                .andExpect(jsonPath("$.unit").value("GALLONS")).andExpect(jsonPath("$.outputScale").value(2))
                .andExpect(jsonPath("$.compliance.sellers[0].sales").value(12.345))
                .andExpect(jsonPath("$.compliance.sellers[0].target").isEmpty())
                .andExpect(jsonPath("$.compliance.sellers[0].percent").isEmpty())
                .andExpect(jsonPath("$.compliance.sellers[0].reason").value("TARGET_NOT_PROVIDED"))
                .andExpect(jsonPath("$.comparison.reason").value("SHIFT_CLASSIFICATION_NOT_PROVIDED"))
                .andExpect(jsonPath("$.normalization.availability").value("UNAVAILABLE"))
                .andExpect(jsonPath("$.compliance.coverage[0].status").value("PROCESSED"))
                .andExpect(jsonPath("$.compliance.sellers[0].vendorDocument").doesNotExist());
        verify(history).readRange(DATE, DATE);
        verifyNoMoreInteractions(history);
    }

    @Test void configuredTestSourcesProduceAllThreeResultsWithExactRatiosAndProvenance() throws Exception {
        var observations = history(SINGLE_DAY, sale(A, "1", "0"), sale(B, "2", "0"));
        when(history.readRange(DATE, DATE)).thenReturn(observations);
        when(parameters.read(SINGLE_DAY, FUEL_GALLONS)).thenReturn(new Snapshot(
                List.of(target(A, SINGLE_DAY, FUEL_GALLONS, "3"), target(B, SINGLE_DAY, FUEL_GALLONS, "3")),
                classified(observations, NIGHT), List.of(new ShiftMultiplier(SINGLE_DAY, FUEL_GALLONS, NIGHT,
                        new BigDecimal("1.5"), "approved multiplier test fixture"))));
        mvc.perform(get(URL).header("Authorization", bearer("ADMIN"))
                        .param("from", DATE.toString()).param("to", DATE.toString()).param("measure", "FUEL_GALLONS"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.roundingMode").value("HALF_EVEN"))
                .andExpect(jsonPath("$.compliance.sellers[0].workerId").value(B.toString()))
                .andExpect(jsonPath("$.compliance.sellers[0].percent").value(66.67))
                .andExpect(jsonPath("$.compliance.sellers[0].ratio.numerator").value(2))
                .andExpect(jsonPath("$.compliance.sellers[0].ratio.denominator").value(3))
                .andExpect(jsonPath("$.compliance.sellers[0].targetSource").value("explicit assigned target fixture"))
                .andExpect(jsonPath("$.comparison.sellers[0].stationObservations").value(2))
                .andExpect(jsonPath("$.comparison.sellers[0].scenario.period").value("NIGHT"))
                .andExpect(jsonPath("$.normalization.sellers[0].normalizedSales").value(1.5))
                .andExpect(jsonPath("$.normalization.sellers[0].multiplierSource").value("approved multiplier test fixture"));
    }

    @ParameterizedTest @ValueSource(strings = {"SELLER", "ROLE_ADMIN"})
    void onlyTheExistingExactAdminAuthorityCanReadPerformance(String authority) throws Exception {
        mvc.perform(get(URL).header("Authorization", bearer(authority))
                        .param("from", DATE.toString()).param("to", DATE.toString()).param("measure", "FUEL_GALLONS"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(history, parameters);
    }

    @Test void anonymousRequestsNeverReadSources() throws Exception {
        mvc.perform(get(URL).param("from", DATE.toString()).param("to", DATE.toString())
                .param("measure", "FUEL_GALLONS")).andExpect(status().is4xxClientError());
        verifyNoInteractions(history, parameters);
    }

    @ParameterizedTest @ValueSource(strings = {"REVERSED", "TOO_LONG", "YEAR_ZERO", "BAD_DATE", "MISSING_FROM", "MEASURE", "SCALE", "NEGATIVE_SCALE"})
    void invalidInputsReturn400BeforeReadingAnySource(String invalid) throws Exception {
        var query = new org.springframework.util.LinkedMultiValueMap<String, String>();
        query.set("from", DATE.toString()); query.set("to", DATE.toString()); query.set("measure", "FUEL_GALLONS");
        switch (invalid) {
            case "REVERSED" -> query.set("to", DATE.minusDays(1).toString());
            case "TOO_LONG" -> query.set("to", DATE.plusDays(3660).toString());
            case "YEAR_ZERO" -> query.set("from", "0000-01-01");
            case "BAD_DATE" -> query.set("from", "2026-02-30");
            case "MISSING_FROM" -> query.remove("from");
            case "MEASURE" -> query.set("measure", "MONEY_AND_GALLONS");
            case "SCALE" -> query.set("outputScale", "13");
            case "NEGATIVE_SCALE" -> query.set("outputScale", "-1");
        }
        mvc.perform(get(URL).header("Authorization", bearer("ADMIN")).params(query)).andExpect(status().isBadRequest()).andExpect(content().contentTypeCompatibleWith("application/problem+json"));
        verifyNoInteractions(history, parameters);
    }

    @Test void upstreamFailuresReturn502WithoutSqlOrFakeEmptyReports() throws Exception {
        when(history.readRange(DATE, DATE)).thenThrow(new IllegalStateException("sensitive database details"));
        mvc.perform(get(URL).header("Authorization", bearer("ADMIN"))
                        .param("from", DATE.toString()).param("to", DATE.toString()).param("measure", "FUEL_GALLONS"))
                .andExpect(status().isBadGateway()).andExpect(jsonPath("$.code").value("PERFORMANCE_SOURCE_FAILURE"))
                .andExpect(jsonPath("$.detail").value("Performance sources could not be read or validated"))
                .andExpect(jsonPath("$.compliance").doesNotExist());
        verifyNoInteractions(parameters);
    }
}
