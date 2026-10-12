package com.edu.unicauca.gasstation.backend.security.domain;

import com.edu.unicauca.gasstation.backend.security.api.dto.AuthRequest;
import com.edu.unicauca.gasstation.backend.security.api.dto.AuthResponse;
import com.edu.unicauca.gasstation.backend.security.infrastructure.jwt.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService - User Authentication and Token Generation (S1-10)")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("Should authenticate successfully and return JWT token when credentials are valid")
    void authenticatesSuccessfullyAndReturnsJwtToken() {
        AuthRequest request = AuthRequest.builder()
                .username("admin")
                .password("correctPassword")
                .build();

        User user = User.builder()
                .id(1L)
                .username("admin")
                .password("$2a$10$encodedPassword")
                .role(Role.ADMIN)
                .build();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(new UsernamePasswordAuthenticationToken("admin", "correctPassword"));
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn("mocked.jwt.token");

        AuthResponse response = authService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("mocked.jwt.token");

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository).findByUsername("admin");
        verify(jwtService).generateToken(user);
    }

    @Test
    @DisplayName("Should throw BadCredentialsException when credentials are invalid")
    void throwsBadCredentialsExceptionWhenPasswordIsIncorrect() {
        AuthRequest request = AuthRequest.builder()
                .username("admin")
                .password("wrongPassword")
                .build();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Bad credentials");

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository, never()).findByUsername(any());
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    @DisplayName("Should throw UsernameNotFoundException when user does not exist in repository")
    void throwsUsernameNotFoundExceptionWhenUserNotFoundAfterAuth() {
        AuthRequest request = AuthRequest.builder()
                .username("unknownUser")
                .password("password")
                .build();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(new UsernamePasswordAuthenticationToken("unknownUser", "password"));
        when(userRepository.findByUsername("unknownUser")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("Usuario no encontrado");

        verify(jwtService, never()).generateToken(any());
    }
}
