package com.edu.unicauca.gasstation.backend.sales.domain.services;

import com.edu.unicauca.gasstation.backend.sales.domain.models.SaleModelExample;
import java.util.Optional;

/**
 * Example domain service interface for sales module.
 */
public interface SaleServiceExample {

    Optional<SaleModelExample> findById(Long id);

    SaleModelExample save(SaleModelExample model);
}
