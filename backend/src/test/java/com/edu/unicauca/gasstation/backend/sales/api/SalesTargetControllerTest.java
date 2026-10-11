package com.edu.unicauca.gasstation.backend.sales.api;

import com.edu.unicauca.gasstation.backend.sales.SalesTargetTestData;
import com.edu.unicauca.gasstation.backend.sales.domain.models.*;
import com.edu.unicauca.gasstation.backend.sales.domain.services.*;
import com.edu.unicauca.gasstation.backend.sales.exception.DuplicateSalesTargetException;
import com.edu.unicauca.gasstation.backend.sales.infrastructure.mappers.SalesTargetApiMapper;
import com.edu.unicauca.gasstation.backend.security.infrastructure.config.SecurityConfig;
import com.edu.unicauca.gasstation.backend.security.infrastructure.jwt.*;
import java.time.Instant;
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
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = SalesTargetController.class, properties = "server.port=0")
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class, SalesTargetApiMapper.class})
class SalesTargetControllerTest {
    static final String BASE = "/api/v1/sales/targets";
    static final String BODY = "{\"year\":2026,\"month\":8,\"category\":\"FUEL_AGGREGATE\",\"value\":26920,\"unit\":\"GALLONS\"}";
    @Autowired MockMvc mvc;
    @Autowired JwtService jwt;
    @Autowired RequestMappingHandlerMapping mappings;
    @Autowired SalesTargetController controller;
    @MockBean SalesTargetService service;
    @MockBean UserDetailsService users;
    @MockBean AuthenticationProvider provider;

    String bearer(String authority) {
        var user = User.withUsername("test").password("unused").authorities(authority).build();
        when(users.loadUserByUsername("test")).thenReturn(user);
        return "Bearer " + jwt.generateToken(user);
    }

    @BeforeEach
    void setup() {
        when(service.register(any())).thenAnswer(call -> {
            SalesTarget target = call.getArgument(0);
            SalesTargetStoragePolicy.validate(target.value());
            return new StoredSalesTarget(1L, target, Instant.parse("2026-08-01T00:00:00Z"));
        });
    }

    @Test
    void adminCreatesWithLocationAndMetadata() throws Exception {
        mvc.perform(post(BASE).header("Authorization", bearer("ADMIN")).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated()).andExpect(header().string("Location", "http://localhost" + BASE + "/1"))
                .andExpect(jsonPath("$.id").value(1)).andExpect(jsonPath("$.month").value(8))
                .andExpect(jsonPath("$.unit").value("GALLONS"))
                .andExpect(jsonPath("$.createdAt").value("2026-08-01T00:00:00Z"));
        verify(service).register(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"READ_ONLY", "ROLE_ADMIN"})
    void authenticatedWithoutExactAdminCannotWrite(String authority) throws Exception {
        mvc.perform(post(BASE).header("Authorization", bearer(authority)).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
        verifyNoInteractions(service);
    }

    @Test
    void anonymousRequestsAreDeniedByActiveSecurityChain() throws Exception {
        mvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(BODY)).andExpect(status().isForbidden());
        mvc.perform(get(BASE).param("year", "2026")).andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test
    void localGuardAlsoRejectsMissingAnonymousAndUnauthenticatedPrincipals() {
        var request = new com.edu.unicauca.gasstation.backend.sales.api.dto.CreateSalesTargetRequest(
                2026, null, SalesTargetCategory.BASKET, java.math.BigDecimal.ONE, SalesTargetUnit.COP);
        assertThatThrownBy(() -> controller.register(request, null))
                .isInstanceOf(org.springframework.security.authentication.AuthenticationCredentialsNotFoundException.class);
        var anonymous = new org.springframework.security.authentication.AnonymousAuthenticationToken("test", "anonymous",
                List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ADMIN")));
        assertThatThrownBy(() -> controller.register(request, anonymous))
                .isInstanceOf(org.springframework.security.authentication.AuthenticationCredentialsNotFoundException.class);
        var unauthenticated = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken("test", "unused");
        assertThatThrownBy(() -> controller.register(request, unauthenticated))
                .isInstanceOf(org.springframework.security.authentication.AuthenticationCredentialsNotFoundException.class);
        verifyNoInteractions(service);
    }

    @Test
    void unexpectedDatabaseFailuresAreNotTranslatedToClientErrors() {
        doThrow(new org.springframework.dao.DataIntegrityViolationException("unexpected constraint"))
                .when(service).register(any());
        assertThatThrownBy(() -> mvc.perform(post(BASE).header("Authorization", bearer("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON).content(BODY)))
                .hasRootCauseInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{", "{\"year\":2026,\"month\":0,\"category\":\"FUEL_AGGREGATE\",\"value\":1,\"unit\":\"GALLONS\"}",
            "{\"year\":2026,\"month\":13,\"category\":\"FUEL_AGGREGATE\",\"value\":1,\"unit\":\"GALLONS\"}",
            "{\"year\":2026,\"category\":\"UNKNOWN\",\"value\":1,\"unit\":\"COP\"}",
            "{\"year\":2026,\"category\":\"BASKET\",\"value\":-1,\"unit\":\"COP\"}",
            "{\"year\":2026,\"category\":\"BASKET\",\"value\":1,\"unit\":\"GALLONS\"}"})
    void invalidBodiesNeverReachService(String body) throws Exception {
        mvc.perform(post(BASE).header("Authorization", bearer("ADMIN")).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @ValueSource(strings = {"1E18", "1E-13"})
    void storagePolicyErrorsAreBadRequests(String value) throws Exception {
        mvc.perform(post(BASE).header("Authorization", bearer("ADMIN")).contentType(MediaType.APPLICATION_JSON)
                .content(BODY.replace("26920", value))).andExpect(status().isBadRequest());
    }

    @Test
    void decimalAndAnnualBasketAreMappedWithoutNormalization() throws Exception {
        String body = "{\"year\":2026,\"category\":\"BASKET\",\"value\":1.2300000000000,\"unit\":\"COP\"}";
        mvc.perform(post(BASE).header("Authorization", bearer("ADMIN")).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.month").isEmpty());
        var captor = org.mockito.ArgumentCaptor.forClass(SalesTarget.class);
        verify(service).register(captor.capture());
        assertThat(captor.getValue().value()).isEqualTo(new java.math.BigDecimal("1.2300000000000"));
        assertThat(captor.getValue().value().scale()).isEqualTo(13);
    }

    @Test
    void duplicateIsConflict() throws Exception {
        doThrow(new DuplicateSalesTargetException(SalesTargetTestData.fuel(8, "1").identity(), null)).when(service).register(any());
        mvc.perform(post(BASE).header("Authorization", bearer("ADMIN")).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("DUPLICATE_SALES_TARGET"));
    }

    @Test
    void queriesUseServiceAndAnnualIdentityUsesNullMonth() throws Exception {
        var stored = new StoredSalesTarget(1L, SalesTargetTestData.fuel(null, "1"), Instant.now());
        when(service.findById(1L)).thenReturn(Optional.of(stored));
        when(service.findByIdentity(any())).thenReturn(Optional.of(stored));
        when(service.findByYear(2026)).thenReturn(List.of(stored));
        var token = bearer("ADMIN");
        mvc.perform(get(BASE + "/1").header("Authorization", token)).andExpect(status().isOk());
        mvc.perform(get(BASE + "/by-identity").header("Authorization", token).param("year", "2026").param("category", "FUEL_AGGREGATE"))
                .andExpect(status().isOk());
        verify(service).findByIdentity(stored.target().identity());
        mvc.perform(get(BASE + "/by-identity").header("Authorization", token).param("year", "2026").param("month", "8").param("category", "BASKET"))
                .andExpect(status().isOk());
        verify(service).findByIdentity(new SalesTargetIdentity(SalesTargetPeriod.monthly(2026, 8), SalesTargetCategory.BASKET));
        mvc.perform(get(BASE).header("Authorization", token).param("year", "2026")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get(BASE).header("Authorization", token).param("year", "2027")).andExpect(status().isOk()).andExpect(content().json("[]"));
        mvc.perform(get(BASE + "/99").header("Authorization", token)).andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "0", "13", "abc"})
    void invalidIdentityMonthNeverQueries(String month) throws Exception {
        mvc.perform(get(BASE + "/by-identity").header("Authorization", bearer("ADMIN")).param("year", "2026")
                .param("category", "BASKET").param("month", month)).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void missingIdentityAndInvalidParameters() throws Exception {
        var token = bearer("ADMIN");
        mvc.perform(get(BASE + "/by-identity").header("Authorization", token).param("year", "2026").param("category", "BASKET"))
                .andExpect(status().isNotFound());
        mvc.perform(get(BASE).header("Authorization", token)).andExpect(status().isBadRequest());
        mvc.perform(get(BASE + "/abc").header("Authorization", token)).andExpect(status().isBadRequest());
    }

    @Test
    void onlyOneRegistrationRouteAndNoEditOrDelete() throws Exception {
        var routes = mappings.getHandlerMethods().entrySet().stream()
                .filter(e -> e.getValue().getBeanType().equals(SalesTargetController.class)).toList();
        var writes = routes.stream().filter(e -> e.getKey().getMethodsCondition().getMethods().contains(RequestMethod.POST)).toList();
        assertThat(writes).hasSize(1);
        assertThat(writes.get(0).getValue().getMethod().getName()).isEqualTo("register");
        assertThat(writes.get(0).getKey().getPatternValues()).containsExactly(BASE);
        assertThat(routes).noneMatch(e -> e.getKey().getMethodsCondition().getMethods().stream()
                .anyMatch(m -> Set.of(RequestMethod.PUT, RequestMethod.PATCH, RequestMethod.DELETE).contains(m)));
    }
}
