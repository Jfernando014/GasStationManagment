package com.edu.unicauca.gasstation.backend.security.infrastructure.config;

import com.edu.unicauca.gasstation.backend.security.api.AuthController;
import com.edu.unicauca.gasstation.backend.security.api.SecurityControllerExample;
import com.edu.unicauca.gasstation.backend.security.domain.AuthService;
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

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {SecurityControllerExample.class, AuthController.class}, properties = "server.port=0")
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class})
@DisplayName("SecurityConfig - Endpoint Protection and Public Routes (S1-11)")
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockBean
    private AuthService authService;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private AuthenticationProvider authenticationProvider;

    private String getValidBearerToken() {
        var user = User.withUsername("admin")
                .password("unused")
                .authorities("ADMIN")
                .build();
        when(userDetailsService.loadUserByUsername("admin")).thenReturn(user);
        return "Bearer " + jwtService.generateToken(user);
    }

    @Test
    @DisplayName("Protected endpoint should reject request without token")
    void protectedEndpointRejectsRequestsWithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/security/example"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Protected endpoint should return 200 OK when called with valid JWT token")
    void protectedEndpointAllowsAccessWithValidToken() throws Exception {
        mockMvc.perform(get("/api/v1/security/example")
                        .header("Authorization", getValidBearerToken()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Public Swagger endpoint should not be rejected by security filter chain")
    void swaggerEndpointsArePubliclyAccessible() throws Exception {
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    org.assertj.core.api.Assertions.assertThat(status).isNotIn(401, 403);
                });
    }
}
