package com.edu.unicauca.gasstation.backend.sales.domain.repositories;

import com.edu.unicauca.gasstation.backend.sales.domain.models.*;
import java.util.List;
import java.util.Optional;

/** Registration and read port. No update or delete operation. */
public interface SalesTargetRepository {
    StoredSalesTarget register(SalesTarget target);
    Optional<StoredSalesTarget> findById(Long id);
    Optional<StoredSalesTarget> findByIdentity(SalesTargetIdentity identity);
    List<StoredSalesTarget> findByYear(int year);
}
