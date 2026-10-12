package com.edu.unicauca.gasstation.backend.sales.api;

import com.edu.unicauca.gasstation.backend.security.infrastructure.config.SecurityConfig;
import com.edu.unicauca.gasstation.backend.security.infrastructure.jwt.*;
import org.junit.jupiter.api.Test;
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

@WebMvcTest(controllers=SalesStatisticsController.class, properties="server.port=0")
@Import({SecurityConfig.class,JwtAuthenticationFilter.class,JwtService.class})
class SalesTargetStatisticsControllerTest {
    static final String AVERAGES="/api/v1/sales/averages/explicit",COMPARE="/api/v1/sales/scenarios/compare/explicit";
    static final String SALES="""
        {"from":"2026-08-03","to":"2026-08-10","category":"FUEL_AGGREGATE","outputScale":2,
         "observations":[{"date":"2026-08-03","journey":"DAY","value":10},
                         {"date":"2026-08-10","journey":"DAY","value":20},
                         {"date":"2026-08-03","journey":"NIGHT","value":8}]}
        """;
    static String comparison(String sales) {
        return "{\"sales\":"+sales+",\"first\":{\"dayOfWeek\":\"MONDAY\",\"journey\":\"DAY\"},\"second\":{\"dayOfWeek\":\"MONDAY\",\"journey\":\"NIGHT\"}}";
    }
    @Autowired MockMvc mvc;
    @Autowired JwtService jwt;
    @MockBean UserDetailsService users;
    @MockBean AuthenticationProvider provider;
    String bearer(String authority) {
        var user=User.withUsername("statistics-test").password("unused").authorities(authority).build();
        when(users.loadUserByUsername("statistics-test")).thenReturn(user);
        return "Bearer "+jwt.generateToken(user);
    }
    @Test void averagesHaveExplicitSourceDayNightLabelsAndComparableUnits() throws Exception {
        mvc.perform(post(AVERAGES).header("Authorization",bearer("ADMIN")).contentType(MediaType.APPLICATION_JSON).content(SALES))
                .andExpect(status().isOk()).andExpect(jsonPath("$.source").value("CLIENT_SUPPLIED_OBSERVATIONS"))
                .andExpect(jsonPath("$.unit").value("GALLONS")).andExpect(jsonPath("$.roundingMode").value("HALF_EVEN"))
                .andExpect(jsonPath("$.series[0].label").value("Diurna")).andExpect(jsonPath("$.series[0].average").value(15))
                .andExpect(jsonPath("$.series[0].observations").value(2)).andExpect(jsonPath("$.series[0].available").value(true))
                .andExpect(jsonPath("$.series[1].journey").value("NIGHT")).andExpect(jsonPath("$.series[1].average").value(8));
    }
    @Test void comparisonReturnsOrderedLabelledChartSeries() throws Exception {
        mvc.perform(post(COMPARE).header("Authorization",bearer("ADMIN")).contentType(MediaType.APPLICATION_JSON)
                .content(comparison(SALES.replace("FUEL_AGGREGATE","BASKET"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.unit").value("COP"))
                .andExpect(jsonPath("$.series[0].label").value("lunes / Diurna"))
                .andExpect(jsonPath("$.series[0].dayOfWeek").value("MONDAY"))
                .andExpect(jsonPath("$.series[0].average").value(15)).andExpect(jsonPath("$.series[1].average").value(8));
    }
    @Test void repeatingDecimalsReturnBothDisplayValueAndExactFraction() throws Exception {
        var sales=SALES.replace("\"value\":10","\"value\":1").replace("\"value\":20","\"value\":2")
                .replace("\"journey\":\"NIGHT\",\"value\":8","\"journey\":\"DAY\",\"value\":1")
                .replace("{\"date\":\"2026-08-03\",\"journey\":\"DAY\",\"value\":1}]","{\"date\":\"2026-08-04\",\"journey\":\"DAY\",\"value\":1}]");
        mvc.perform(post(AVERAGES).header("Authorization",bearer("ADMIN")).contentType(MediaType.APPLICATION_JSON).content(sales))
                .andExpect(status().isOk()).andExpect(jsonPath("$.series[0].average").value(1.33))
                .andExpect(jsonPath("$.series[0].exactAverage.numerator").value("4"))
                .andExpect(jsonPath("$.series[0].exactAverage.denominator").value("3"));
    }
    @Test void halfEvenDisplayPolicyIsExplicitAndDoesNotDiscardExactInput() throws Exception {
        var sales=SALES.replace("\"value\":10","\"value\":1.005").replace("\"value\":20","\"value\":1.005");
        mvc.perform(post(AVERAGES).header("Authorization",bearer("ADMIN")).contentType(MediaType.APPLICATION_JSON).content(sales))
                .andExpect(status().isOk()).andExpect(jsonPath("$.series[0].average").value(1.00))
                .andExpect(jsonPath("$.series[0].exactAverage.numerator").value("201"))
                .andExpect(jsonPath("$.series[0].exactAverage.denominator").value("200"));
    }
    @Test void noObservationsProduceUnavailableAveragesNotInventedSales() throws Exception {
        var empty="{\"from\":\"2026-08-03\",\"to\":\"2026-08-03\",\"category\":\"BASKET\",\"outputScale\":2,\"observations\":[]}";
        for(var route:new String[]{AVERAGES,COMPARE})
            mvc.perform(post(route).header("Authorization",bearer("ADMIN")).contentType(MediaType.APPLICATION_JSON)
                    .content(route.equals(AVERAGES)?empty:comparison(empty))).andExpect(status().isOk())
                    .andExpect(jsonPath("$.series[0].available").value(false)).andExpect(jsonPath("$.series[0].observations").value(0))
                    .andExpect(jsonPath("$.series[0].average").isEmpty()).andExpect(jsonPath("$.series[0].exactAverage").isEmpty());
    }
    @Test void zeroRecordedSalesRemainAvailable() throws Exception {
        mvc.perform(post(AVERAGES).header("Authorization",bearer("ADMIN")).contentType(MediaType.APPLICATION_JSON)
                .content(SALES.replace("\"value\":10","\"value\":0").replace("\"value\":20","\"value\":0")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.series[0].available").value(true))
                .andExpect(jsonPath("$.series[0].average").value(0));
    }
    @Test void unavailableScenarioIsNotInventedWhenTheOtherScenarioHasObservations() throws Exception {
        var body=comparison(SALES).replace("\"second\":{\"dayOfWeek\":\"MONDAY\"","\"second\":{\"dayOfWeek\":\"TUESDAY\"");
        mvc.perform(post(COMPARE).header("Authorization",bearer("ADMIN")).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.series[0].available").value(true))
                .andExpect(jsonPath("$.series[0].average").value(15)).andExpect(jsonPath("$.series[1].available").value(false))
                .andExpect(jsonPath("$.series[1].average").isEmpty()).andExpect(jsonPath("$.series[1].observations").value(0));
    }
    @ParameterizedTest @ValueSource(strings={"DAY_BAD","NEGATIVE","NULL_VALUE","NULL_JOURNEY","NULL_OBSERVATIONS","NULL_FROM","INVALID_DATE","REVERSED","OUTSIDE","DUPLICATE","SCALE","JSON"})
    void invalidObservationsProduceProblemDetail400(String invalid) throws Exception {
        var sales=switch(invalid) {
            case "DAY_BAD" -> SALES.replace("\"DAY\"","\"DIURNA_INVALID\"");
            case "NEGATIVE" -> SALES.replace("\"value\":10","\"value\":-1");
            case "NULL_VALUE" -> SALES.replace("\"value\":10","\"value\":null");
            case "NULL_JOURNEY" -> SALES.replace("\"journey\":\"DAY\"","\"journey\":null");
            case "NULL_OBSERVATIONS" -> "{\"from\":\"2026-08-03\",\"to\":\"2026-08-10\",\"category\":\"BASKET\",\"outputScale\":2,\"observations\":null}";
            case "NULL_FROM" -> SALES.replace("\"from\":\"2026-08-03\"","\"from\":null");
            case "INVALID_DATE" -> SALES.replace("2026-08-03","2026-02-30");
            case "REVERSED" -> SALES.replace("\"to\":\"2026-08-10\"","\"to\":\"2026-08-01\"");
            case "OUTSIDE" -> SALES.replace("\"to\":\"2026-08-10\"","\"to\":\"2026-08-04\"");
            case "DUPLICATE" -> SALES.replace("\"date\":\"2026-08-10\"","\"date\":\"2026-08-03\"");
            case "SCALE" -> SALES.replace("\"outputScale\":2","\"outputScale\":13");
            default -> "{";
        };
        mvc.perform(post(AVERAGES).header("Authorization",bearer("ADMIN")).contentType(MediaType.APPLICATION_JSON).content(sales))
                .andExpect(status().isBadRequest()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }
    @ParameterizedTest @ValueSource(strings={"READ_ONLY","ROLE_ADMIN"})
    void bothOperationsRequireLiteralAdminBeforeCalculation(String authority) throws Exception {
        var reversed=SALES.replace("\"to\":\"2026-08-10\"","\"to\":\"2026-08-01\"");
        for(var route:new String[]{AVERAGES,COMPARE})
            mvc.perform(post(route).header("Authorization",bearer(authority)).contentType(MediaType.APPLICATION_JSON)
                    .content(route.equals(AVERAGES)?reversed:comparison(reversed))).andExpect(status().isForbidden());
    }
    @Test void anonymousIsDeniedWithFiltersActive() throws Exception {
        for(var route:new String[]{AVERAGES,COMPARE})
            mvc.perform(post(route).contentType(MediaType.APPLICATION_JSON).content(route.equals(AVERAGES)?SALES:comparison(SALES)))
                    .andExpect(status().isForbidden());
    }
    @Test void invalidScenarioAndMissingScenarioAreBadRequests() throws Exception {
        for(var body:new String[]{comparison(SALES).replace("MONDAY","FESTIVO"),comparison(SALES).replace("\"first\":{\"dayOfWeek\":\"MONDAY\",\"journey\":\"DAY\"}","\"first\":null")})
            mvc.perform(post(COMPARE).header("Authorization",bearer("ADMIN")).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
    }
}
