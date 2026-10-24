package com.edu.unicauca.gasstation.backend.sales.api;

import com.edu.unicauca.gasstation.backend.sales.*;
import com.edu.unicauca.gasstation.backend.sales.application.*;
import com.edu.unicauca.gasstation.backend.inventory.ProductCategory;
import com.edu.unicauca.gasstation.backend.sales.domain.calculation.*;
import com.edu.unicauca.gasstation.backend.sales.domain.calculation.DistributionCalculation.*;
import com.edu.unicauca.gasstation.backend.sales.domain.models.*;
import com.edu.unicauca.gasstation.backend.sales.domain.services.SalesTargetService;
import com.edu.unicauca.gasstation.backend.security.infrastructure.config.SecurityConfig;
import com.edu.unicauca.gasstation.backend.security.infrastructure.jwt.*;
import com.edu.unicauca.gasstation.backend.shifts.ScheduleWorkerDirectory;
import java.time.LocalDate;
import java.time.Instant;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.userdetails.*;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = SalesReportingController.class, properties = "server.port=0")
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class, BasketStatisticsService.class, SalesChartsService.class})
class SalesReportingControllerTest {
    static final String BASE = "/api/v1/sales/statistics";
    static final String QUERY = "?from=2026-08-01&to=2026-08-31";
    static final String TREND = """
        {"period":"DAY","sales":{"from":"2026-08-01","to":"2026-08-31","category":"BASKET","outputScale":2,
        "observations":[{"date":"2026-08-01","journey":"DAY","value":10},{"date":"2026-08-03","journey":"DAY","value":20}]}}
        """;
    static final String DISTRIBUTION = """
        {"profileId":1,"outputScale":2,"assignments":[{"date":"2026-08-01","workerId":"00000000-0000-0000-0000-000000000001","shiftCode":"DIA"}]}
        """;
    @Autowired MockMvc mvc;
    @Autowired JwtService jwt;
    @MockBean UserDetailsService users;
    @MockBean AuthenticationProvider provider;
    @MockBean BasketSalesReadPort sales;
    @MockBean WorkedShiftsReadPort shifts;
    @MockBean ScheduleWorkerDirectory workers;
    @MockBean SalesTargetService targets;
    @MockBean SalesDistributionService distribution;

    String bearer(String authority) {
        var user = User.withUsername("reporting-test").password("unused").authorities(authority).build();
        when(users.loadUserByUsername("reporting-test")).thenReturn(user);
        return "Bearer " + jwt.generateToken(user);
    }

    @Test void disconnectedSalesReturnExplicitUnavailableWithoutZeros() throws Exception {
        when(sales.read(any(), any())).thenReturn(new BasketSalesReadPort.Snapshot(false, List.of()));
        mvc.perform(get(BASE + "/basket" + QUERY).header("Authorization", bearer("ADMIN")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.charts.source").value("REAL_HISTORICAL_SALES"))
                .andExpect(jsonPath("$.charts.availability").value("UNAVAILABLE"))
                .andExpect(jsonPath("$.charts.reason").value("SALES_SOURCE_UNAVAILABLE"))
                .andExpect(jsonPath("$.summary").isEmpty()).andExpect(jsonPath("$.groups").isEmpty())
                .andExpect(jsonPath("$.charts.series").isEmpty());
        verifyNoInteractions(targets, distribution, workers, shifts);
    }
    @Test void basketEndpointFiltersRealSourceAndProvidesTypedSellerCategoryAndUnits() throws Exception {
        var worker = new UUID(0, 1);
        var date = LocalDate.of(2026, 8, 1);
        when(sales.read(any(), any())).thenReturn(new BasketSalesReadPort.Snapshot(true, List.of(
                new BasketSalesReadPort.Sale("line1", date, worker, 1, ProductCategory.ADITIVO,
                        new BigDecimal("12.50"), BigDecimal.ONE))));
        when(shifts.read(any(), any())).thenReturn(new WorkedShiftsReadPort.Snapshot(false, List.of()));
        when(workers.getWorkersByIds(any())).thenReturn(Map.of());
        mvc.perform(get(BASE + "/basket" + QUERY + "&workerId=" + worker + "&category=ADITIVO&period=MONTH").header("Authorization", bearer("ADMIN")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.groups[0].workerId").value(worker.toString()))
                .andExpect(jsonPath("$.groups[0].category").value("ADITIVO"))
                .andExpect(jsonPath("$.groups[0].participationPercent").value(100))
                .andExpect(jsonPath("$.summary.amount").value(12.5)).andExpect(jsonPath("$.summary.quantity").value(1))
                .andExpect(jsonPath("$.summary.averageAmountPerWorkedShift").isEmpty())
                .andExpect(jsonPath("$.moneyUnit").value("COP")).andExpect(jsonPath("$.quantityUnit").value("PRODUCT_UNITS"));
    }
    @Test void observedTrendIsClientDataWithOrderedNonPaddedSeries() throws Exception {
        mvc.perform(post(BASE + "/trends/explicit").header("Authorization", bearer("ADMIN")).contentType(MediaType.APPLICATION_JSON).content(TREND))
                .andExpect(status().isOk()).andExpect(jsonPath("$.source").value("CLIENT_SUPPLIED_OBSERVATIONS"))
                .andExpect(jsonPath("$.series[0].chartType").value("LINE"))
                .andExpect(jsonPath("$.series[0].unit").value("COP"))
                .andExpect(jsonPath("$.series[0].points.length()").value(2))
                .andExpect(jsonPath("$.series[0].points[1].changePercent").value(100));
    }
    @Test void targetChartLabelsStoredAmountsAsProjectionsAndPreservesPrecision() throws Exception {
        var target = new SalesTarget(new SalesTargetIdentity(SalesTargetPeriod.annual(2026), SalesTargetCategory.BASKET),
                new BigDecimal("10.005"), SalesTargetUnit.COP);
        when(targets.findById(1L)).thenReturn(Optional.of(new StoredSalesTarget(1L, target, Instant.EPOCH)));
        mvc.perform(get(BASE + "/targets/1").header("Authorization", bearer("ADMIN")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.source").value("TARGET_PROJECTION"))
                .andExpect(jsonPath("$.decimalPolicy").value("UNROUNDED_STORED_VALUES"))
                .andExpect(jsonPath("$.roundingMode").isEmpty())
                .andExpect(jsonPath("$.series[0].points[0].value").value(10.005))
                .andExpect(jsonPath("$.series[0].points[0].key").value("2026"));
    }
    @Test void distributionChartKeepsCalculationPolicyAndWarnsAboutPartialCoverage() throws Exception {
        var target = new SalesTarget(new SalesTargetIdentity(SalesTargetPeriod.monthly(2026, 8), SalesTargetCategory.BASKET),
                BigDecimal.TEN, SalesTargetUnit.COP);
        var date = LocalDate.of(2026, 8, 1);
        var assignment = new Assignment(date, new UUID(0, 1), "DIA");
        var calculation = new SalesTargetDistributionEngine().calculate(new Input(target,
                SalesTargetDistributionTestData.workbook(), List.of(new Coverage(assignment, Set.of(6))), 2));
        when(distribution.calculate(1L, 1L, 2, List.of(assignment))).thenReturn(calculation);
        mvc.perform(post(BASE + "/targets/1/distribution").header("Authorization", bearer("ADMIN")).contentType(MediaType.APPLICATION_JSON).content(DISTRIBUTION))
                .andExpect(status().isOk()).andExpect(jsonPath("$.source").value("TARGET_PROJECTION"))
                .andExpect(jsonPath("$.reason").value("PARTIAL_SHIFT_COVERAGE"))
                .andExpect(jsonPath("$.projection.assignmentSource").value("EXPLICIT_CALLER_SUPPLIED_WORKSHEET_DATE_ASSIGNMENTS"))
                .andExpect(jsonPath("$.series[0].points[0].value").value(10))
                .andExpect(jsonPath("$.series[1].chartType").value("BAR"));
        verify(distribution).calculate(1L, 1L, 2, List.of(assignment));
    }
    @ParameterizedTest @ValueSource(strings = {"READ_ONLY", "ROLE_ADMIN"})
    void everyNewEndpointRequiresLiteralAdmin(String authority) throws Exception {
        String token = bearer(authority);
        for (var path : new String[]{"/basket" + QUERY, "/targets/1"})
            mvc.perform(get(BASE + path).header("Authorization", token)).andExpect(status().isForbidden());
        mvc.perform(post(BASE + "/trends/explicit").header("Authorization", token).contentType(MediaType.APPLICATION_JSON).content(TREND)).andExpect(status().isForbidden());
        mvc.perform(post(BASE + "/targets/1/distribution").header("Authorization", token).contentType(MediaType.APPLICATION_JSON).content(DISTRIBUTION)).andExpect(status().isForbidden());
        verifyNoInteractions(sales, shifts, workers, targets, distribution);
    }
    @Test void anonymousCannotAccessAnyNewEndpoint() throws Exception {
        mvc.perform(get(BASE + "/basket" + QUERY)).andExpect(status().isForbidden());
        mvc.perform(get(BASE + "/targets/1")).andExpect(status().isForbidden());
        mvc.perform(post(BASE + "/trends/explicit").contentType(MediaType.APPLICATION_JSON).content(TREND)).andExpect(status().isForbidden());
        mvc.perform(post(BASE + "/targets/1/distribution").contentType(MediaType.APPLICATION_JSON).content(DISTRIBUTION)).andExpect(status().isForbidden());
        verifyNoInteractions(sales, targets, distribution);
    }
    @ParameterizedTest @ValueSource(strings = {"&period=YEAR", "&category=FUEL", "&workerId=invalid", "&workerId=00000000-0000-0000-0000-000000000000", "&outputScale=-1", "&outputScale=13"})
    void invalidParametersProduceBadRequest(String query) throws Exception {
        mvc.perform(get(BASE + "/basket" + QUERY + query).header("Authorization", bearer("ADMIN")))
                .andExpect(status().isBadRequest()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        verifyNoInteractions(sales);
    }
    @ParameterizedTest @ValueSource(strings = {"?from=2026-02-30&to=2026-08-31", "?from=2026-09-01&to=2026-08-01", "?from=2026-08-01", "?from=2026-08-01&to=2040-08-01"})
    void invalidDateRangesAreBadRequests(String query) throws Exception {
        mvc.perform(get(BASE + "/basket" + query).header("Authorization", bearer("ADMIN"))).andExpect(status().isBadRequest());
        verifyNoInteractions(sales);
    }
    @ParameterizedTest @ValueSource(strings = {"null", "\"YEAR\""})
    void invalidTrendPeriod(String period) throws Exception {
        mvc.perform(post(BASE + "/trends/explicit").header("Authorization", bearer("ADMIN")).contentType(MediaType.APPLICATION_JSON)
                .content(TREND.replace("\"period\":\"DAY\"", "\"period\":" + period))).andExpect(status().isBadRequest());
    }
    @Test void corruptSourceReturns502AndMissingTarget404() throws Exception {
        when(sales.read(any(), any())).thenReturn(null);
        mvc.perform(get(BASE + "/basket" + QUERY).header("Authorization", bearer("ADMIN"))).andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("INVALID_STATISTICS_SOURCE"));
        when(targets.findById(1L)).thenReturn(Optional.empty());
        mvc.perform(get(BASE + "/targets/1").header("Authorization", bearer("ADMIN"))).andExpect(status().isNotFound());
        mvc.perform(get(BASE + "/targets/0").header("Authorization", bearer("ADMIN"))).andExpect(status().isBadRequest());
    }
}
