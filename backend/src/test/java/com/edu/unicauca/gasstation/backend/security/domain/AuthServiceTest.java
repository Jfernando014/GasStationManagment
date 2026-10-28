package com.edu.unicauca.gasstation.backend.security.domain;

import com.edu.unicauca.gasstation.backend.security.api.dto.AuthRequest;
import com.edu.unicauca.gasstation.backend.security.api.dto.AuthResponse;
import com.edu.unicauca.gasstation.backend.security.api.dto.ChangePasswordRequest;
import com.edu.unicauca.gasstation.backend.security.exception.AccountLockedException;
import com.edu.unicauca.gasstation.backend.security.exception.InvalidPasswordException;
import com.edu.unicauca.gasstation.backend.security.infrastructure.jwt.JwtService;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService - User Authentication, Lockout (S2-05) and Password Management")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    private User adminUser;

    @BeforeEach
    void setUp() {
        adminUser = User.builder()
                .id(1L)
                .username("admin")
                .password("$2a$10$encodedPassword")
                .role(Role.ADMIN)
                .failedAttempts(0)
                .lockedUntil(null)
                .build();
    }

    @Test
    @DisplayName("Should authenticate successfully and reset failed attempts counter")
    void authenticatesSuccessfullyAndResetsFailedAttempts() {
        adminUser.setFailedAttempts(3);

        AuthRequest request = AuthRequest.builder()
                .username("admin")
                .password("correctPassword")
                .build();

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(new UsernamePasswordAuthenticationToken("admin", "correctPassword"));
        when(jwtService.generateToken(adminUser)).thenReturn("mocked.jwt.token");

        AuthResponse response = authService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("mocked.jwt.token");
        assertThat(adminUser.getFailedAttempts()).isEqualTo(0);
        assertThat(adminUser.getLockedUntil()).isNull();
        verify(userRepository).save(adminUser);
    }

    @Test
    @DisplayName("Should increment failed attempts on invalid credentials")
    void incrementsFailedAttemptsOnBadCredentials() {
        AuthRequest request = AuthRequest.builder()
                .username("admin")
                .password("wrongPassword")
                .build();

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("4 attempt(s) remaining");

        assertThat(adminUser.getFailedAttempts()).isEqualTo(1);
        assertThat(adminUser.getLockedUntil()).isNull();
        verify(userRepository).save(adminUser);
    }

    @Test
    @DisplayName("Should lock account after 5 consecutive failed attempts")
    void locksAccountAfter5FailedAttempts() {
        adminUser.setFailedAttempts(4);

        AuthRequest request = AuthRequest.builder()
                .username("admin")
                .password("wrongPassword")
                .build();

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(AccountLockedException.class)
                .hasMessageContaining("temporarily locked after 5 failed login attempts");

        assertThat(adminUser.getFailedAttempts()).isEqualTo(5);
        assertThat(adminUser.getLockedUntil()).isNotNull();
        assertThat(adminUser.getLockedUntil()).isAfter(LocalDateTime.now());
        verify(userRepository).save(adminUser);
    }

    @Test
    @DisplayName("Should immediately reject login if account is currently locked")
    void rejectsLoginIfAccountCurrentlyLocked() {
        adminUser.setFailedAttempts(5);
        adminUser.setLockedUntil(LocalDateTime.now().plusMinutes(10));

        AuthRequest request = AuthRequest.builder()
                .username("admin")
                .password("anyPassword")
                .build();

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(AccountLockedException.class)
                .hasMessageContaining("Account is temporarily locked");

        verify(authenticationManager, never()).authenticate(any());
    }

    @Test
    @DisplayName("Should allow login and reset counter if lock duration has expired")
    void allowsLoginIfLockDurationExpired() {
        adminUser.setFailedAttempts(5);
        adminUser.setLockedUntil(LocalDateTime.now().minusMinutes(1)); // Lock expired

        AuthRequest request = AuthRequest.builder()
                .username("admin")
                .password("correctPassword")
                .build();

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(new UsernamePasswordAuthenticationToken("admin", "correctPassword"));
        when(jwtService.generateToken(adminUser)).thenReturn("new.token");

        AuthResponse response = authService.login(request);

        assertThat(response).isNotNull();
        assertThat(adminUser.getFailedAttempts()).isEqualTo(0);
        assertThat(adminUser.getLockedUntil()).isNull();
    }

    @Test
    @DisplayName("Should throw UsernameNotFoundException when user does not exist in repository")
    void throwsUsernameNotFoundExceptionWhenUserNotFound() {
        AuthRequest request = AuthRequest.builder()
                .username("unknownUser")
                .password("password")
                .build();

        when(userRepository.findByUsername("unknownUser")).thenReturn(Optional.empty());
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class);
    }

    // --- CHANGE PASSWORD TESTS ---

    @Test
    @DisplayName("Should change password successfully when current password matches")
    void changesPasswordSuccessfully() {
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("oldSecret123")
                .newPassword("newSecret456")
                .confirmPassword("newSecret456")
                .build();

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(passwordEncoder.matches("oldSecret123", adminUser.getPassword())).thenReturn(true);
        when(passwordEncoder.matches("newSecret456", adminUser.getPassword())).thenReturn(false);
        when(passwordEncoder.encode("newSecret456")).thenReturn("$2a$10$newHashedPassword");

        authService.changePassword("admin", request);

        assertThat(adminUser.getPassword()).isEqualTo("$2a$10$newHashedPassword");
        assertThat(adminUser.getPasswordChangedAt()).isNotNull();
        verify(userRepository).save(adminUser);
    }

    @Test
    @DisplayName("Should throw InvalidPasswordException when new password and confirmation do not match")
    void throwsWhenNewPasswordAndConfirmationMismatch() {
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("oldSecret123")
                .newPassword("newSecret456")
                .confirmPassword("differentPassword")
                .build();

        assertThatThrownBy(() -> authService.changePassword("admin", request))
                .isInstanceOf(InvalidPasswordException.class)
                .hasMessageContaining("do not match");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw InvalidPasswordException when current password is incorrect")
    void throwsWhenCurrentPasswordIsIncorrect() {
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("wrongCurrentPassword")
                .newPassword("newSecret456")
                .confirmPassword("newSecret456")
                .build();

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(passwordEncoder.matches("wrongCurrentPassword", adminUser.getPassword())).thenReturn(false);

        assertThatThrownBy(() -> authService.changePassword("admin", request))
                .isInstanceOf(InvalidPasswordException.class)
                .hasMessageContaining("Current password is incorrect");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw InvalidPasswordException when new password is the same as current password")
    void throwsWhenNewPasswordIsSameAsCurrent() {
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("oldSecret123")
                .newPassword("oldSecret123")
                .confirmPassword("oldSecret123")
                .build();

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(passwordEncoder.matches("oldSecret123", adminUser.getPassword())).thenReturn(true);

        assertThatThrownBy(() -> authService.changePassword("admin", request))
                .isInstanceOf(InvalidPasswordException.class)
                .hasMessageContaining("cannot be the same");

        verify(userRepository, never()).save(any());
    }
}
