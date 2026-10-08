package com.edu.unicauca.gasstation.backend.security.infrastructure.config;

import com.edu.unicauca.gasstation.backend.security.domain.Role;
import com.edu.unicauca.gasstation.backend.security.domain.User;
import com.edu.unicauca.gasstation.backend.security.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.findByUsername("admin").isEmpty()) {
            User admin = User.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("admin123"))
                    .role(Role.ADMIN)
                    .build();
            userRepository.save(admin);
            System.out.println("Usuario administrador creado por defecto: admin / admin123");
        }
    }
}
