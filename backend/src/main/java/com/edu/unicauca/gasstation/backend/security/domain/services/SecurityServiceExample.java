package com.edu.unicauca.gasstation.backend.security.domain.services;

import com.edu.unicauca.gasstation.backend.security.domain.models.SecurityModelExample;
import java.util.Optional;

/**
 * Example domain service interface for security module.
 */
public interface SecurityServiceExample {

    Optional<SecurityModelExample> findById(Long id);

    SecurityModelExample save(SecurityModelExample model);
}
