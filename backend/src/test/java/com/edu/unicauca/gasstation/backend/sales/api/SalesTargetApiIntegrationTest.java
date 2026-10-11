package com.edu.unicauca.gasstation.backend.sales.api;

import com.edu.unicauca.gasstation.backend.sales.domain.services.SalesTargetService;
import com.edu.unicauca.gasstation.backend.sales.SalesTargetDistributionTestData;
import com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence.adapters.DistributionProfileRepositoryAdapter;
import com.edu.unicauca.gasstation.backend.sales.infrastructure.adapters.PublicShiftCoverageResolver;
import com.edu.unicauca.gasstation.backend.shifts.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
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
            SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class,
            SalesDistributionController.class, DistributionProfileRepositoryAdapter.class, PublicShiftCoverageResolver.class,
            com.edu.unicauca.gasstation.backend.sales.application.SalesDistributionService.class})
    static class Application { }

    @Container @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired JwtService jwt;
    @MockBean UserDetailsService users;
    @MockBean AuthenticationProvider provider;
    @MockBean ShiftExternalService shifts;
    @MockBean ScheduleWorkerDirectory workers;
    @Autowired ObjectMapper json;
    String token;

    @BeforeEach
    void setup() {
        jdbc.execute("TRUNCATE TABLE sales_targets");
        jdbc.execute("TRUNCATE TABLE sales_distribution_profiles, sales_distribution_hour_weights, sales_distribution_day_baselines, sales_distribution_support_codes");
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

    private long createProfile() throws Exception {
        var response=mvc.perform(post("/api/v1/sales/distribution-profiles").header("Authorization",token)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(SalesTargetDistributionTestData.workbook())))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.createdAt").isNotEmpty()).andReturn().getResponse();
        var id=json.readTree(response.getContentAsString()).get("id").asLong();
        mvc.perform(get(response.getHeader("Location")).header("Authorization",token)).andExpect(status().isOk());
        return id;
    }

    @Test
    void persistsCompleteImmutableParameterSnapshotsAndNumericValues() throws Exception {
        long first=createProfile(), second=createProfile();
        assertThat(second).isNotEqualTo(first);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM sales_distribution_hour_weights WHERE profile_id=?",Integer.class,first)).isEqualTo(24);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM sales_distribution_day_baselines WHERE profile_id=?",Integer.class,first)).isEqualTo(7);
        assertThat(jdbc.queryForObject("SELECT weight FROM sales_distribution_hour_weights WHERE profile_id=? AND hour=0",BigDecimal.class,first)).isEqualByComparingTo("1.1");
        assertThat(jdbc.queryForObject("SELECT titular_share FROM sales_distribution_profiles WHERE id=?",BigDecimal.class,first)).isEqualByComparingTo("0.7");
        assertThatThrownBy(() -> jdbc.update("INSERT INTO sales_distribution_hour_weights VALUES (?,24,1)",first)).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("INSERT INTO sales_distribution_day_baselines VALUES (?,0,1)",first)).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("UPDATE sales_distribution_hour_weights SET weight='NaN' WHERE profile_id=? AND hour=0",first)).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("UPDATE sales_distribution_profiles SET support_share=0.4 WHERE id=?",first)).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    @Test
    void calculatesHttpUsingSuppliedScheduleAndOnlyPublicWorkerAndShiftContracts() throws Exception {
        long profile=createProfile();
        var response=mvc.perform(post(SalesTargetControllerTest.BASE).header("Authorization",token).contentType(MediaType.APPLICATION_JSON)
                .content(SalesTargetControllerTest.BODY)).andExpect(status().isCreated()).andReturn().getResponse();
        long target=json.readTree(response.getContentAsString()).get("id").asLong();
        var worker=new UUID(0,1);var role=new RoleInfo(new UUID(0,10),"TITULAR",1);
        when(workers.getWorkersByIds(List.of(worker))).thenReturn(Map.of(worker,new ScheduleWorkerInfo(worker,"Explicit fixture",role.id(),true)));
        when(shifts.getActiveCatalog()).thenReturn(List.of(new ShiftCodeInfo(new UUID(0,20),"DIA","Day",role,"DAY",
                List.of(new ShiftCodeInfo.Segment(6,18)),12,false,null,true)));
        var body="{\"profileId\":"+profile+",\"outputScale\":12,\"assignments\":[{\"date\":\"2026-08-01\",\"workerId\":\""+worker+"\",\"shiftCode\":\"DIA\"}]}";
        var result=mvc.perform(post(SalesTargetControllerTest.BASE+"/"+target+"/distribution").header("Authorization",token)
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk())
                .andExpect(jsonPath("$.assignmentSource").value("EXPLICIT_CALLER_SUPPLIED_WORKSHEET_DATE_ASSIGNMENTS"))
                .andExpect(jsonPath("$.profileId").value(profile)).andExpect(jsonPath("$.targetId").value(target))
                .andExpect(jsonPath("$.byHour[0].date").value("2026-08-01"))
                .andExpect(jsonPath("$.byHour[0].hour").value(6))
                .andExpect(jsonPath("$.allocations.length()").value(12)).andReturn().getResponse();
        var parsed=json.readTree(result.getContentAsString());
        assertThat(new BigDecimal(parsed.get("byWorker").get(worker.toString()).get("official").asText())).isEqualByComparingTo("26920");
        org.mockito.Mockito.verify(workers).getWorkersByIds(List.of(worker));org.mockito.Mockito.verify(shifts).getActiveCatalog();
        mvc.perform(post(SalesTargetControllerTest.BASE+"/"+target+"/distribution").header("Authorization",token)
                .contentType(MediaType.APPLICATION_JSON).content(body.replace("2026-08-01","2026-09-01"))).andExpect(status().isBadRequest());
    }

    @Test
    void compactRangeMatchesExplicitDatesUsingStoredProfileAndRealJwt() throws Exception {
        long profile=createProfile();
        var response=mvc.perform(post(SalesTargetControllerTest.BASE).header("Authorization",token).contentType(MediaType.APPLICATION_JSON)
                .content(SalesTargetControllerTest.BODY)).andExpect(status().isCreated()).andReturn().getResponse();
        long target=json.readTree(response.getContentAsString()).get("id").asLong();
        var worker=new UUID(0,1);var role=new RoleInfo(new UUID(0,10),"TITULAR",1);
        when(workers.getWorkersByIds(List.of(worker))).thenReturn(Map.of(worker,new ScheduleWorkerInfo(worker,"Explicit fixture",role.id(),true)));
        when(shifts.getActiveCatalog()).thenReturn(List.of(new ShiftCodeInfo(new UUID(0,20),"DIA","Day",role,"DAY",
                List.of(new ShiftCodeInfo.Segment(6,18)),12,false,null,true)));
        String prefix="{\"profileId\":"+profile+",\"outputScale\":2,\"assignments\":[";
        String row="{\"date\":\"2026-08-01\",\"workerId\":\""+worker+"\",\"shiftCode\":\"DIA\"}";
        String explicit=prefix+row+","+row.replace("08-01","08-02")+","+row.replace("08-01","08-03")+"]}";
        String compact=prefix+row.replace("\"shiftCode\":\"DIA\"","\"shiftCode\":\"DIA\",\"endDate\":\"2026-08-03\"")+"]}";
        var route=SalesTargetControllerTest.BASE+"/"+target+"/distribution";
        var daily=mvc.perform(post(route).header("Authorization",token).contentType(MediaType.APPLICATION_JSON).content(explicit))
                .andExpect(status().isOk()).andReturn().getResponse();
        var range=mvc.perform(post(route).header("Authorization",token).contentType(MediaType.APPLICATION_JSON).content(compact))
                .andExpect(status().isOk()).andExpect(jsonPath("$.byDate.length()").value(3))
                .andExpect(jsonPath("$.allocations.length()").value(36)).andReturn().getResponse();
        assertThat(json.readTree(range.getContentAsString())).isEqualTo(json.readTree(daily.getContentAsString()));
        assertThat(json.readTree(range.getContentAsString()).get("monthlyTotal").get("official").decimalValue())
                .isEqualByComparingTo("26920");
    }

    @Test
    void invalidRangesNeverQueryPublicSources() throws Exception {
        String row="{\"date\":\"2026-08-01\",\"workerId\":\"00000000-0000-0000-0000-000000000001\",\"shiftCode\":\"DIA\"}";
        String prefix="{\"profileId\":1,\"outputScale\":2,\"assignments\":[";
        for (String end : List.of("2026-07-31", "2026-09-01", "invalid")) {
            String body=prefix+row.replace("\"shiftCode\":\"DIA\"","\"shiftCode\":\"DIA\",\"endDate\":\""+end+"\"")+"]}";
            mvc.perform(post(SalesTargetControllerTest.BASE+"/1/distribution").header("Authorization",token)
                    .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
        }
        mvc.perform(post(SalesTargetControllerTest.BASE+"/1/distribution").header("Authorization",token)
                .contentType(MediaType.APPLICATION_JSON).content(prefix+row+","+row+"]}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(workers,shifts);
    }

    @Test
    void invalidConfigurationCannotRoundOrCreateRows() throws Exception {
        var body=json.writeValueAsString(SalesTargetDistributionTestData.workbook());
        for(var invalid:List.of(body.replace("1.1,","0.0000000000001,"),body.replace("\"supportShare\":0.3","\"supportShare\":0.4"))) {
            mvc.perform(post("/api/v1/sales/distribution-profiles").header("Authorization",token).contentType(MediaType.APPLICATION_JSON)
                    .content(invalid)).andExpect(status().isBadRequest());
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM sales_distribution_profiles",Long.class)).isZero();
    }

    @Test
    void bothNewPostRoutesDenyAnonymousAndNonAdminBeforePublicQueriesOrWrites() throws Exception {
        var reader=User.withUsername("reader-test").password("unused").authorities("READ_ONLY").build();
        when(users.loadUserByUsername("reader-test")).thenReturn(reader);
        var readerToken="Bearer "+jwt.generateToken(reader);
        var config=json.writeValueAsString(SalesTargetDistributionTestData.workbook());
        // An invalid range would be 400 if expanded; 403 proves authorization runs first.
        var calculation="{\"profileId\":1,\"outputScale\":12,\"assignments\":[{\"date\":\"2026-08-01\",\"endDate\":\"2026-07-31\",\"workerId\":\"00000000-0000-0000-0000-000000000001\",\"shiftCode\":\"DIA\"}]}";
        for(var route:List.of("/api/v1/sales/distribution-profiles",SalesTargetControllerTest.BASE+"/1/distribution")) {
            var body=route.endsWith("profiles")?config:calculation;
            mvc.perform(post(route).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isForbidden());
            mvc.perform(post(route).header("Authorization",readerToken).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isForbidden());
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM sales_distribution_profiles",Long.class)).isZero();
        org.mockito.Mockito.verifyNoInteractions(workers,shifts);
    }
}
