package com.edu.unicauca.gasstation.backend.security.domain;

import com.edu.unicauca.gasstation.backend.security.api.dto.AuthRequest;
import com.edu.unicauca.gasstation.backend.security.api.dto.AuthResponse;
import com.edu.unicauca.gasstation.backend.security.infrastructure.jwt.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthResponse login(AuthRequest request) {
        // 1. Autentica las credenciales con el AuthenticationManager de Spring
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        // 2. Si la autenticación es exitosa, buscamos al usuario
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));

        // 3. Generamos el token JWT
        String jwtToken = jwtService.generateToken(user);

        return AuthResponse.builder()
                .token(jwtToken)
                .build();
    }
}
