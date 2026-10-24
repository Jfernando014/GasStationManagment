package com.edu.unicauca.gasstation.backend.competition.api;

import com.edu.unicauca.gasstation.backend.competition.domain.services.*;
import com.edu.unicauca.gasstation.backend.competition.infrastructure.persistence.adapters.*;
import com.edu.unicauca.gasstation.backend.inventory.domain.services.FuelPriceService;
import com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.adapters.FuelPriceRepositoryAdapter;
import com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.mappers.FuelPricePersistenceMapper;
import com.edu.unicauca.gasstation.backend.security.infrastructure.config.SecurityConfig;
import com.edu.unicauca.gasstation.backend.security.infrastructure.jwt.*;
import com.fasterxml.jackson.databind.*;
import java.math.BigDecimal;
import java.util.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.userdetails.*;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Only the disposable @ServiceConnection PostgreSQL is used. Never reads a user datasource. */
@SpringBootTest(classes = CompetitionApiIntegrationTest.Application.class, properties = {
        "server.port=0", "spring.jpa.hibernate.ddl-auto=validate", "spring.jpa.show-sql=false",
        "spring.flyway.enabled=true", "spring.flyway.clean-disabled=true",
        "springdoc.swagger-ui.path=/swagger-ui.html", "springdoc.api-docs.path=/v3/api-docs"})
@AutoConfigureMockMvc
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CompetitionApiIntegrationTest {
    static final String BASE = "/api/v1/competition";
    @Configuration @EnableAutoConfiguration
    @EntityScan({"com.edu.unicauca.gasstation.backend.competition.infrastructure.persistence.entities",
            "com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.entities"})
    @EnableJpaRepositories({"com.edu.unicauca.gasstation.backend.competition.infrastructure.persistence.repositories",
            "com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.repositories"})
    @Import({CompetitionController.class, CompetitionExceptionHandler.class, CompetitionApiDocumentation.class,
            CompetitionService.class, MarketPriceService.class, CompetitionRepositoryAdapter.class, ObservationRepositoryAdapter.class,
            FuelPriceService.class, FuelPriceRepositoryAdapter.class, FuelPricePersistenceMapper.class,
            SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class})
    static class Application { }
    @Container @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper json;
    @Autowired JwtService jwt;
    @Autowired Flyway flyway;
    @MockBean UserDetailsService users;
    @MockBean AuthenticationProvider provider;
    String admin;
    String reader;

    @BeforeEach void setup() {
        jdbc.execute("TRUNCATE competitor_price_observation, competitor RESTART IDENTITY");
        jdbc.execute("TRUNCATE fuel_price RESTART IDENTITY");
        admin = token("competition-admin", "ADMIN"); reader = token("competition-reader", "USER");
    }
    String token(String name, String authority) {
        var user = User.withUsername(name).password("unused").authorities(authority).build();
        when(users.loadUserByUsername(name)).thenReturn(user);
        return "Bearer " + jwt.generateToken(user);
    }
    ResultActions call(MockHttpServletRequestBuilder request) throws Exception {
        return mvc.perform(request.header("Authorization", admin));
    }
    MockHttpServletRequestBuilder body(MockHttpServletRequestBuilder request, String content) {
        return request.contentType(MediaType.APPLICATION_JSON).content(content);
    }
    JsonNode result(ResultActions action) throws Exception {
        return json.readTree(action.andReturn().getResponse().getContentAsString());
    }
    long competitor(String name) throws Exception {
        var response = call(body(post(BASE + "/competitors"), json.writeValueAsString(Map.of(
                "name", name, "brand", "Independent", "location", "Test zone"))))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.active").value(true)).andReturn().getResponse();
        call(get(response.getHeader("Location"))).andExpect(status().isOk());
        return json.readTree(response.getContentAsString()).get("id").asLong();
    }
    long observe(long id, String fuel, String price, String date) throws Exception {
        return result(call(body(post(BASE + "/observations"), observation(id, fuel, price, date)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.createdAt").isNotEmpty())).get("id").asLong();
    }
    String observation(long id, String fuel, String price, String date) {
        return "{\"competitorId\":" + id + ",\"fuelType\":\"" + fuel + "\",\"pricePerGallon\":" + price + ",\"observedAt\":\"" + date + "\"}";
    }

    @Test void registersUpdatesFiltersAndDeactivatesWithoutLosingHistory() throws Exception {
        long id = competitor("Original");
        long observation = observe(id, "MAX_PRO", "12345.67", "2020-01-01T10:00:00-05:00");
        call(body(put(BASE + "/competitors/" + id), "{\"name\":\"Updated\",\"brand\":\"New\",\"location\":\"Near bridge\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Updated"));
        call(body(patch(BASE + "/competitors/" + id + "/status"), "{\"active\":false}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.active").value(false));
        call(get(BASE + "/competitors").param("active", "true")).andExpect(jsonPath("$.length()").value(0));
        call(get(BASE + "/competitors").param("active", "false")).andExpect(jsonPath("$.length()").value(1));
        call(get(BASE + "/observations/" + observation)).andExpect(status().isOk())
                .andExpect(jsonPath("$.observedAt").value("2020-01-01T15:00:00Z"));
        call(get(BASE + "/observations").param("competitorId", Long.toString(id)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        call(get(BASE + "/observations/latest")).andExpect(jsonPath("$.length()").value(0));
        call(get(BASE + "/observations/latest").param("includeInactive", "true")).andExpect(jsonPath("$.length()").value(1));
        call(body(post(BASE + "/observations"), observation(id, "MOTOR", "100", "2020-01-01T00:00:00Z")))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("INACTIVE_COMPETITOR"));
        call(body(patch(BASE + "/competitors/" + id + "/status"), "{\"active\":true}")).andExpect(status().isOk());
        call(get(BASE + "/observations/latest")).andExpect(jsonPath("$.length()").value(1));
    }

    @Test void latestUsesObservationTimeThenIdAndOneValuePerStationAndFuel() throws Exception {
        long a = competitor("A"), b = competitor("B");
        observe(a, "MOTOR", "10", "2020-01-01T00:00:00Z");
        observe(a, "MOTOR", "100", "2020-01-03T00:00:00Z");
        long tieWinner = observe(a, "MOTOR", "110", "2020-01-03T00:00:00Z");
        // Registered later, observed earlier: must not replace the latest actual survey.
        observe(a, "MOTOR", "20", "2020-01-02T00:00:00Z");
        observe(a, "DIESEL", "200", "2020-01-03T00:00:00Z");
        observe(b, "MOTOR", "130", "2020-01-03T00:00:00Z");
        var latest = result(call(get(BASE + "/observations/latest").param("fuelType", "MOTOR"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2)));
        assertThat(latest.get(0).get("id").asLong()).isEqualTo(tieWinner);
        call(get(BASE + "/observations/latest").param("competitorId", Long.toString(a)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
        jdbc.update("INSERT INTO fuel_price(fuel_type,price_per_gallon,valid_from) VALUES ('MOTOR',132,'2019-01-01')");
        call(get(BASE + "/comparison").param("fuelType", "MOTOR"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.mode").value("CURRENT_LATEST"))
                .andExpect(jsonPath("$.fuels[0].competition.competitorCount").value(2))
                .andExpect(jsonPath("$.fuels[0].competition.average").value(120))
                .andExpect(jsonPath("$.fuels[0].competition.minimum").value(110))
                .andExpect(jsonPath("$.fuels[0].versusAverage.absoluteCopPerGallon").value(12))
                .andExpect(jsonPath("$.fuels[0].versusAverage.percentage").value(10));
    }

    @Test void historicalCutoffUsesEarlierSurveysAndDailyEffectiveOwnPrice() throws Exception {
        long id = competitor("A");
        observe(id, "MOTOR", "100", "2020-01-01T10:00:00Z");
        observe(id, "MOTOR", "200", "2020-01-03T10:00:00Z");
        jdbc.update("INSERT INTO fuel_price(fuel_type,price_per_gallon,valid_from) VALUES ('MOTOR',120,'2020-01-01'),('MOTOR',220,'2020-01-02')");
        call(get(BASE + "/comparison").param("fuelType", "MOTOR").param("asOf", "2020-01-02T02:00:00Z"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.mode").value("AS_OF"))
                .andExpect(jsonPath("$.bellavistaDate").value("2020-01-01"))
                .andExpect(jsonPath("$.fuels[0].bellavistaPrice").value(120))
                .andExpect(jsonPath("$.fuels[0].competition.average").value(100));
    }

    @Test void historyFiltersInclusiveInstantsFuelCompetitorAndPages() throws Exception {
        long a = competitor("A"), b = competitor("B");
        observe(a, "MOTOR", "100.01", "2020-01-01T00:00:00Z");
        observe(a, "MOTOR", "101.02", "2020-01-02T00:00:00Z");
        observe(a, "DIESEL", "200", "2020-01-02T00:00:00Z");
        observe(b, "MOTOR", "300", "2020-01-02T00:00:00Z");
        call(get(BASE + "/observations").param("competitorId", Long.toString(a)).param("fuelType", "MOTOR")
                .param("from", "2020-01-01T00:00:00Z").param("to", "2020-01-02T00:00:00Z").param("size", "1").param("page", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content.length()").value(1)).andExpect(jsonPath("$.content[0].pricePerGallon").value(100.01));
    }

    @Test void missingDataIsNullAndInactiveStationsDoNotContributeToComparisons() throws Exception {
        call(get(BASE + "/comparison")).andExpect(status().isOk()).andExpect(jsonPath("$.fuels.length()").value(4))
                .andExpect(jsonPath("$.fuels[0].competition.competitorCount").value(0))
                .andExpect(jsonPath("$.fuels[0].competition.average").isEmpty())
                .andExpect(jsonPath("$.fuels[0].bellavistaPrice").isEmpty())
                .andExpect(jsonPath("$.fuels[0].versusAverage.percentage").isEmpty());
        long id = competitor("Inactive"); observe(id, "MOTOR", "10", "2020-01-01T00:00:00Z");
        call(body(patch(BASE + "/competitors/" + id + "/status"), "{\"active\":false}"));
        call(get(BASE + "/comparison").param("fuelType", "MOTOR")).andExpect(jsonPath("$.fuels[0].competition.competitorCount").value(0));
    }

    @Test void simulationWithAndWithoutExplicitCostDoesNotPersistAnything() throws Exception {
        long id = competitor("A"); observe(id, "MOTOR", "100", "2020-01-01T00:00:00Z");
        var counts = jdbc.queryForMap("SELECT (SELECT count(*) FROM fuel_price) own, (SELECT count(*) FROM competitor_price_observation) observations");
        call(body(post(BASE + "/simulation"), "{\"fuelType\":\"MOTOR\",\"proposedPrice\":125,\"acquisitionCost\":100}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.versusAverage.percentage").value(25))
                .andExpect(jsonPath("$.estimatedMargin.grossCopPerGallon").value(25))
                .andExpect(jsonPath("$.estimatedMargin.grossMarginPercentage").value(20));
        call(body(post(BASE + "/simulation"), "{\"fuelType\":\"MOTOR\",\"proposedPrice\":125}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estimatedMargin.grossMarginPercentage").isEmpty());
        assertThat(jdbc.queryForMap("SELECT (SELECT count(*) FROM fuel_price) own, (SELECT count(*) FROM competitor_price_observation) observations")).isEqualTo(counts);
    }

    @ParameterizedTest @ValueSource(strings = {"0", "-1", "0.001", "10000000000", "null"})
    void rejectsInvalidObservationPricesAtTheApi(String price) throws Exception {
        long id = competitor("A");
        call(body(post(BASE + "/observations"), observation(id, "MOTOR", price, "2020-01-01T00:00:00Z")))
                .andExpect(status().isBadRequest());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM competitor_price_observation", Long.class)).isZero();
    }

    @Test void rejectsIncompleteInvalidDatesFuelsIdsAndPagination() throws Exception {
        long id = competitor("A");
        for (String invalid : List.of("{}", observation(id, "MAXPRO", "100", "2020-01-01T00:00:00Z"),
                observation(id, "MOTOR", "100", "2020-01-01T00:00:00"), observation(id, "MOTOR", "100", "9999-01-01T00:00:00Z")))
            call(body(post(BASE + "/observations"), invalid)).andExpect(status().isBadRequest());
        call(body(post(BASE + "/competitors"), "{\"name\":\" \",\"brand\":\"X\",\"location\":\"Y\"}")).andExpect(status().isBadRequest());
        call(body(patch(BASE + "/competitors/" + id + "/status"), "{}")).andExpect(status().isBadRequest());
        call(get(BASE + "/competitors/-1")).andExpect(status().isBadRequest());
        call(get(BASE + "/competitors/9999")).andExpect(status().isNotFound());
        call(get(BASE + "/observations/9999")).andExpect(status().isNotFound());
        call(get(BASE + "/observations").param("size", "201")).andExpect(status().isBadRequest());
        call(get(BASE + "/observations").param("from", "2020-01-02T00:00:00Z").param("to", "2020-01-01T00:00:00Z")).andExpect(status().isBadRequest());
        call(get(BASE + "/comparison").param("fuelType", "GASOLINE")).andExpect(status().isBadRequest());
        call(get(BASE + "/comparison").param("asOf", "9999-01-01T00:00:00Z")).andExpect(status().isBadRequest());
        call(body(post(BASE + "/simulation"), "{\"fuelType\":\"MOTOR\",\"proposedPrice\":100,\"acquisitionCost\":-1}")).andExpect(status().isBadRequest());
    }

    @Test void realJwtAndExactAdminAuthorityProtectEveryOperation() throws Exception {
        long id = competitor("A");
        var identity = "{\"name\":\"A\",\"brand\":\"B\",\"location\":\"C\"}";
        for (var request : List.of(get(BASE + "/competitors"), get(BASE + "/competitors/" + id),
                body(post(BASE + "/competitors"), identity), body(put(BASE + "/competitors/" + id), identity),
                body(patch(BASE + "/competitors/" + id + "/status"), "{\"active\":false}"),
                body(post(BASE + "/observations"), observation(id, "MOTOR", "100", "2020-01-01T00:00:00Z")),
                get(BASE + "/observations"), get(BASE + "/observations/1"), get(BASE + "/observations/latest"), get(BASE + "/comparison"),
                body(post(BASE + "/simulation"), "{\"fuelType\":\"MOTOR\",\"proposedPrice\":100}"))) {
            mvc.perform(request.header("Authorization", reader)).andExpect(status().isForbidden());
        }
        // The existing global security chain returns 403 for a missing token.
        mvc.perform(get(BASE + "/competitors")).andExpect(status().isForbidden());
        mvc.perform(post(BASE + "/competitors").contentType(MediaType.APPLICATION_JSON).content(identity)).andExpect(status().isForbidden());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM competitor", Long.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM competitor_price_observation", Long.class)).isZero();
    }

    @Test void freshV18SchemaEnforcesReferentialFuelMoneyAndDateIntegrity() throws Exception {
        assertThat(Arrays.stream(flyway.info().applied()).anyMatch(m -> "18".equals(m.getVersion().getVersion()))).isTrue();
        flyway.validate();
        long id = competitor("A");
        observe(id, "MOTOR", "1.23", "2020-01-01T00:00:00Z");
        assertThatThrownBy(() -> jdbc.update("DELETE FROM competitor WHERE id=?", id)).isInstanceOf(DataIntegrityViolationException.class);
        for (String invalid : List.of("0", "-1", "'NaN'", "'Infinity'"))
            assertThatThrownBy(() -> jdbc.update("INSERT INTO competitor_price_observation(competitor_id,fuel_type,price_per_gallon,observed_at) VALUES (?,'MOTOR'," + invalid + ",'2020-01-01')", id))
                    .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("INSERT INTO competitor_price_observation(competitor_id,fuel_type,price_per_gallon,observed_at) VALUES (9999,'MOTOR',100,'2020-01-01')"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("INSERT INTO competitor_price_observation(competitor_id,fuel_type,price_per_gallon,observed_at) VALUES (?,'UNKNOWN',100,'2020-01-01')", id))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("INSERT INTO competitor_price_observation(competitor_id,fuel_type,price_per_gallon,observed_at) VALUES (?,'MOTOR',100,'9999-01-01')", id))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(jdbc.queryForObject("SELECT price_per_gallon FROM competitor_price_observation", BigDecimal.class)).isEqualByComparingTo("1.23");
    }

    @Test void upgradesAnIsolatedV17SchemaPreservingExistingDataAndIsRepeatable() {
        var baseline = Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .schemas("competition_upgrade").locations("classpath:db/migration").target("17").load();
        assertThat(baseline.migrate().migrationsExecuted).isEqualTo(17);
        jdbc.execute("INSERT INTO competition_upgrade.fuel_price(fuel_type,price_per_gallon,valid_from) VALUES ('MOTOR',12345.67,'2020-01-01')");
        jdbc.execute("INSERT INTO competition_upgrade.product(code,name,category,price) VALUES ('kept','Kept product','OTRO',10)");
        var upgrade = Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .schemas("competition_upgrade").locations("classpath:db/migration").target("18").load();
        assertThat(upgrade.migrate().migrationsExecuted).isEqualTo(1);
        assertThat(upgrade.info().current().getVersion().getVersion()).isEqualTo("18");
        assertThat(upgrade.migrate().migrationsExecuted).isZero(); upgrade.validate();
        assertThat(jdbc.queryForObject("SELECT price_per_gallon FROM competition_upgrade.fuel_price", BigDecimal.class)).isEqualByComparingTo("12345.67");
        assertThat(jdbc.queryForObject("SELECT name FROM competition_upgrade.product", String.class)).isEqualTo("Kept product");
    }
}
