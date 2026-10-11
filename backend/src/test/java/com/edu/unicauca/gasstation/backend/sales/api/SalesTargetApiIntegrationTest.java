package com.edu.unicauca.gasstation.backend.sales.api;

import com.edu.unicauca.gasstation.backend.sales.domain.services.SalesTargetService;
import com.edu.unicauca.gasstation.backend.sales.infrastructure.mappers.SalesTargetApiMapper;
import com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence.adapters.SalesTargetRepositoryAdapter;
import com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence.mappers.SalesTargetPersistenceMapper;
import com.edu.unicauca.gasstation.backend.security.infrastructure.config.SecurityConfig;
import com.edu.unicauca.gasstation.backend.security.infrastructure.jwt.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.*;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.userdetails.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = SalesTargetApiIntegrationTest.Application.class, properties = {
        "server.port=0", "spring.jpa.hibernate.ddl-auto=validate", "spring.jpa.show-sql=false",
        "springdoc.swagger-ui.path=/swagger-ui.html", "springdoc.api-docs.path=/v3/api-docs"})
@AutoConfigureMockMvc
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class SalesTargetApiIntegrationTest {
    @Configuration
    @EnableAutoConfiguration
    @EntityScan("com.edu.unicauca.gasstation.backend")
    @EnableJpaRepositories("com.edu.unicauca.gasstation.backend")
    @Import({SalesTargetController.class, SalesTargetExceptionHandler.class, SalesTargetApiMapper.class,
            SalesTargetService.class, SalesTargetRepositoryAdapter.class, SalesTargetPersistenceMapper.class,
            SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class})
    static class Application { }

    @Container @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired JwtService jwt;
    @MockBean UserDetailsService users;
    @MockBean AuthenticationProvider provider;
    String token;

    @BeforeEach
    void setup() {
        jdbc.execute("TRUNCATE TABLE sales_targets");
        var user = User.withUsername("admin-test").password("unused").authorities("ADMIN").build();
        when(users.loadUserByUsername("admin-test")).thenReturn(user);
        token = "Bearer " + jwt.generateToken(user);
    }

    @Test
    void httpRegistersQueriesAndRejectsBothKindsOfDuplicates() throws Exception {
        var base = SalesTargetControllerTest.BASE;
        for (String month : new String[]{"null", "8"}) {
            var body = SalesTargetControllerTest.BODY.replace("\"month\":8", "\"month\":" + month)
                    .replace("26920", "1.2300000000000");
            var response = mvc.perform(post(base).header("Authorization", token).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isCreated()).andExpect(jsonPath("$.createdAt").isNotEmpty()).andReturn().getResponse();
            mvc.perform(get(response.getHeader("Location")).header("Authorization", token)).andExpect(status().isOk());
            var identity = get(base + "/by-identity").param("year", "2026").param("category", "FUEL_AGGREGATE").header("Authorization", token);
            if (!month.equals("null")) identity.param("month", month);
            mvc.perform(identity).andExpect(status().isOk());
            mvc.perform(post(base).header("Authorization", token).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isConflict());
        }
        mvc.perform(get(base).header("Authorization", token).param("year", "2026"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
        assertThat(jdbc.queryForList("SELECT target_value FROM sales_targets", BigDecimal.class))
                .allSatisfy(v -> assertThat(v.compareTo(new BigDecimal("1.23"))).isZero());
    }

    @Test
    void invalidDecimalsNeverCreateRowsAndZeroBasketIsAccepted() throws Exception {
        for (String value : new String[]{"1E18", "1E-13"}) {
            mvc.perform(post(SalesTargetControllerTest.BASE).header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                    .content(SalesTargetControllerTest.BODY.replace("26920", value))).andExpect(status().isBadRequest());
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM sales_targets", Long.class)).isZero();
        mvc.perform(post(SalesTargetControllerTest.BASE).header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                .content(SalesTargetControllerTest.BODY.replace("FUEL_AGGREGATE", "BASKET").replace("GALLONS", "COP").replace("26920", "0")))
                .andExpect(status().isCreated());
    }
}
