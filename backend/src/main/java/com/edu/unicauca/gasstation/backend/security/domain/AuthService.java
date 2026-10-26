package com.edu.unicauca.gasstation.backend.security.domain;

import com.edu.unicauca.gasstation.backend.security.api.dto.AuthRequest;
import com.edu.unicauca.gasstation.backend.security.api.dto.AuthResponse;
import com.edu.unicauca.gasstation.backend.security.exception.AccountLockedException;
import com.edu.unicauca.gasstation.backend.security.infrastructure.jwt.JwtService;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    public static final int MAX_FAILED_ATTEMPTS = 5;
    public static final int LOCKOUT_DURATION_MINUTES = 15;

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse login(AuthRequest request) {
        Optional<User> userOpt = userRepository.findByUsername(request.getUsername());

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            // Check if user is currently locked
            if (user.getLockedUntil() != null) {
                LocalDateTime now = LocalDateTime.now();
                if (now.isBefore(user.getLockedUntil())) {
                    long minutesRemaining = Math.max(1, Duration.between(now, user.getLockedUntil()).toMinutes());
                    log.warn("Login attempt for locked account: {} (locked until {})", user.getUsername(), user.getLockedUntil());
                    throw new AccountLockedException(
                            "Account is temporarily locked due to multiple failed login attempts. Try again in " + minutesRemaining + " minute(s).",
                            user.getLockedUntil(),
                            minutesRemaining
                    );
                } else {
                    // Lock expired, reset attempts
                    user.setFailedAttempts(0);
                    user.setLockedUntil(null);
                    userRepository.save(user);
                }
            }
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()
                    )
            );
        } catch (BadCredentialsException ex) {
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                int attempts = (user.getFailedAttempts() != null ? user.getFailedAttempts() : 0) + 1;
                user.setFailedAttempts(attempts);

                if (attempts >= MAX_FAILED_ATTEMPTS) {
                    LocalDateTime lockedUntil = LocalDateTime.now().plusMinutes(LOCKOUT_DURATION_MINUTES);
                    user.setLockedUntil(lockedUntil);
                    userRepository.save(user);
                    log.warn("Account {} locked after {} failed attempts until {}", user.getUsername(), attempts, lockedUntil);
                    throw new AccountLockedException(
                            "Account has been temporarily locked after 5 failed login attempts. Try again in " + LOCKOUT_DURATION_MINUTES + " minutes.",
                            lockedUntil,
                            LOCKOUT_DURATION_MINUTES
                    );
                }

                userRepository.save(user);
                int remaining = MAX_FAILED_ATTEMPTS - attempts;
                throw new BadCredentialsException("Invalid credentials. " + remaining + " attempt(s) remaining before account lockout.");
            }
            throw ex;
        }

        // On successful authentication, reset failed attempts
        User user = userOpt.orElseGet(() -> userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado")));

        if (user.getFailedAttempts() != null && user.getFailedAttempts() > 0) {
            user.setFailedAttempts(0);
            user.setLockedUntil(null);
            userRepository.save(user);
        }

        String jwtToken = jwtService.generateToken(user);

        return AuthResponse.builder()
                .token(jwtToken)
                .build();
    }
}
