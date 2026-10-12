package com.edu.unicauca.gasstation.backend.security.infrastructure.jwt;

import com.edu.unicauca.gasstation.backend.security.domain.Role;
import com.edu.unicauca.gasstation.backend.security.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JwtService - Token Generation and Extraction (S1-10)")
class JwtServiceTest {

    private JwtService jwtService;

    private static final String SECRET_KEY = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private static final long EXPIRATION = 86400000L;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", SECRET_KEY);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", EXPIRATION);
    }

    @Test
    @DisplayName("Should generate valid token and extract username correctly")
    void generatesTokenAndExtractsUsername() {
        User user = User.builder()
                .id(1L)
                .username("admin")
                .password("encoded")
                .role(Role.ADMIN)
                .build();

        String token = jwtService.generateToken(user);

        assertThat(token).isNotBlank();
        String extractedUsername = jwtService.extractUsername(token);
        assertThat(extractedUsername).isEqualTo("admin");
    }

    @Test
    @DisplayName("Should validate token successfully for the same user")
    void validatesTokenSuccessfullyForSameUser() {
        User user = User.builder()
                .id(1L)
                .username("admin")
                .password("encoded")
                .role(Role.ADMIN)
                .build();

        String token = jwtService.generateToken(user);

        boolean isValid = jwtService.isTokenValid(token, user);
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Should return false when validating token for a different user")
    void returnsFalseWhenValidatingTokenForDifferentUser() {
        User user = User.builder()
                .id(1L)
                .username("admin")
                .password("encoded")
                .role(Role.ADMIN)
                .build();

        User otherUser = User.builder()
                .id(2L)
                .username("otherUser")
                .password("encoded")
                .role(Role.ADMIN)
                .build();

        String token = jwtService.generateToken(user);

        boolean isValid = jwtService.isTokenValid(token, otherUser);
        assertThat(isValid).isFalse();
    }
}
