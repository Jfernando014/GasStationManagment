package com.edu.unicauca.gasstation.backend.security.api;

import com.edu.unicauca.gasstation.backend.security.api.dto.AuthRequest;
import com.edu.unicauca.gasstation.backend.security.api.dto.AuthResponse;
import com.edu.unicauca.gasstation.backend.security.api.dto.ChangePasswordRequest;
import com.edu.unicauca.gasstation.backend.security.domain.AuthService;
import com.edu.unicauca.gasstation.backend.security.exception.AccountLockedException;
import com.edu.unicauca.gasstation.backend.security.exception.InvalidPasswordException;
import com.edu.unicauca.gasstation.backend.security.infrastructure.config.SecurityConfig;
import com.edu.unicauca.gasstation.backend.security.infrastructure.jwt.JwtAuthenticationFilter;
import com.edu.unicauca.gasstation.backend.security.infrastructure.jwt.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class, properties = "server.port=0")
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class})
@DisplayName("AuthController - Authentication Endpoints, Lockout and Password Management (S2-05)")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private AuthenticationProvider authenticationProvider;

    @Test
    @DisplayName("POST /api/v1/auth/login should return 200 OK with token when credentials are correct")
    void loginReturnsOkWithTokenWhenCredentialsAreCorrect() throws Exception {
        AuthRequest request = AuthRequest.builder()
                .username("admin")
                .password("admin123")
                .build();

        AuthResponse response = AuthResponse.builder()
                .token("valid.jwt.token")
                .build();

        when(authService.login(any(AuthRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("valid.jwt.token"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login should return 401 Unauthorized when credentials are bad")
    void loginReturns401UnauthorizedWhenCredentialsAreBad() throws Exception {
        AuthRequest request = AuthRequest.builder()
                .username("admin")
                .password("wrongPassword")
                .build();

        when(authService.login(any(AuthRequest.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/v1/auth/login should return 401 Unauthorized when user is not found")
    void loginReturns401UnauthorizedWhenUserNotFound() throws Exception {
        AuthRequest request = AuthRequest.builder()
                .username("unknown")
                .password("password")
                .build();

        when(authService.login(any(AuthRequest.class)))
                .thenThrow(new UsernameNotFoundException("User not found"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/v1/auth/login should be accessible without Authorization header")
    void loginIsPermittedWithoutAuthorizationHeader() throws Exception {
        AuthRequest request = AuthRequest.builder()
                .username("admin")
                .password("admin123")
                .build();

        when(authService.login(any(AuthRequest.class)))
                .thenReturn(AuthResponse.builder().token("jwt").build());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/v1/auth/login should return 423 Locked when account is locked")
    void loginReturns423LockedWhenAccountLocked() throws Exception {
        AuthRequest request = AuthRequest.builder()
                .username("admin")
                .password("password")
                .build();

        when(authService.login(any(AuthRequest.class)))
                .thenThrow(new AccountLockedException("Account locked", LocalDateTime.now().plusMinutes(15), 15));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isLocked())
                .andExpect(jsonPath("$.error").value("ACCOUNT_LOCKED"))
                .andExpect(jsonPath("$.minutesRemaining").value(15));
    }

    @Test
    @DisplayName("POST /api/v1/auth/change-password should return 200 OK when authenticated")
    void changePasswordReturnsOkWhenAuthenticated() throws Exception {
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("oldPass123")
                .newPassword("newPass456")
                .confirmPassword("newPass456")
                .build();

        org.springframework.security.core.userdetails.UserDetails mockUserDetails =
                org.springframework.security.core.userdetails.User.builder()
                        .username("admin")
                        .password("password")
                        .authorities(List.of(new SimpleGrantedAuthority("ADMIN")))
                        .build();

        when(jwtService.extractUsername("valid.token")).thenReturn("admin");
        when(userDetailsService.loadUserByUsername("admin")).thenReturn(mockUserDetails);
        when(jwtService.isTokenValid(eq("valid.token"), any())).thenReturn(true);

        mockMvc.perform(post("/api/v1/auth/change-password")
                        .header("Authorization", "Bearer valid.token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password changed successfully"));

        verify(authService).changePassword(eq("admin"), any(ChangePasswordRequest.class));
    }

    @Test
    @DisplayName("POST /api/v1/auth/change-password should return 400 Bad Request when validation fails")
    void changePasswordReturns400WhenInvalid() throws Exception {
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("wrongOldPass")
                .newPassword("newPass456")
                .confirmPassword("newPass456")
                .build();

        org.springframework.security.core.userdetails.UserDetails mockUserDetails =
                org.springframework.security.core.userdetails.User.builder()
                        .username("admin")
                        .password("password")
                        .authorities(List.of(new SimpleGrantedAuthority("ADMIN")))
                        .build();

        when(jwtService.extractUsername("valid.token")).thenReturn("admin");
        when(userDetailsService.loadUserByUsername("admin")).thenReturn(mockUserDetails);
        when(jwtService.isTokenValid(eq("valid.token"), any())).thenReturn(true);

        doThrow(new InvalidPasswordException("Current password is incorrect"))
                .when(authService).changePassword(eq("admin"), any(ChangePasswordRequest.class));

        mockMvc.perform(post("/api/v1/auth/change-password")
                        .header("Authorization", "Bearer valid.token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Current password is incorrect"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/change-password should return 403 Forbidden when unauthenticated")
    void changePasswordReturns403WhenUnauthenticated() throws Exception {
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("oldPass123")
                .newPassword("newPass456")
                .confirmPassword("newPass456")
                .build();

        mockMvc.perform(post("/api/v1/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
