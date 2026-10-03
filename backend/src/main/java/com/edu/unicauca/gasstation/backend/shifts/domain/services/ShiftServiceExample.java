package com.edu.unicauca.gasstation.backend.shifts.domain.services;

import com.edu.unicauca.gasstation.backend.shifts.domain.models.ShiftModelExample;
import java.util.Optional;

/**
 * Example domain service interface for shifts module.
 */
public interface ShiftServiceExample {

    Optional<ShiftModelExample> findById(Long id);

    ShiftModelExample save(ShiftModelExample model);
}
