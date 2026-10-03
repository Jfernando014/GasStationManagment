package com.edu.unicauca.gasstation.backend.inventory.domain.services;

import com.edu.unicauca.gasstation.backend.inventory.domain.models.InventoryModelExample;
import java.util.Optional;

/**
 * Example domain service interface for inventory module.
 */
public interface InventoryServiceExample {

    Optional<InventoryModelExample> findById(Long id);

    InventoryModelExample save(InventoryModelExample model);
}
